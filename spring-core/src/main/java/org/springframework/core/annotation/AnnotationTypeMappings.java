/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSet;
import org.springframework.core.annotation.AnnotationTypeMapping.Reference;
import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Provides {@link AnnotationTypeMapping} information for a single source
 * {@link AnnotationType}. Performs a recursive breadth first crawl of all
 * meta-annotations to ultimately provide a quick way to map a
 * {@link DeclaredAnnotation} to a {@link TypeMappedAnnotation}.
 * <p>
 * Supports convention based merging of meta-annotations as well as implicit and
 * explicit {@link AliasFor @AliasFor} aliases.
 * <p>
 * This class is designed to be cached so that meta-annotations only need to be
 * searched once, regardless of how many times they are actually used.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see #getMapping(String)
 * @see #getAll()
 * @see AnnotationTypeMapping
 */
class AnnotationTypeMappings {

	private static Map<ClassLoader, Cache> cache = new ConcurrentReferenceHashMap<>();

	static final String ALIAS_FOR_ANNOTATION = AliasFor.class.getName();

	private final List<AnnotationTypeMapping> mappings;

	private final Map<String, AnnotationTypeMapping> mappingForType;

	/**
	 * Create a new {@link AliasedAnnotationType} instance for the given source.
	 * @param classLoader the classloader used to read annotations
	 * @param repeatableContainers strategy to extract repeatable containers
	 * @param source the source annotation type
	 */
	AnnotationTypeMappings(ClassLoader classLoader,
			RepeatableContainers repeatableContainers, AnnotationType source) {
		Assert.notNull(repeatableContainers, "RepeatableContainers must not be null");
		Assert.notNull(source, "Source must not be null");
		this.mappings = new MappingsBuilder(classLoader, repeatableContainers).build(
				source);
		this.mappingForType = groupByType(this.mappings);
		processAliasForAnnotations();
	}

	private Map<String, AnnotationTypeMapping> groupByType(
			List<AnnotationTypeMapping> mappings) {
		Map<String, AnnotationTypeMapping> mappingForType = new HashMap<>();
		mappings.forEach(mapping -> mappingForType.putIfAbsent(
				mapping.getAnnotationType().getClassName(), mapping));
		return Collections.unmodifiableMap(mappingForType);
	}

	private void processAliasForAnnotations() {
		for (AnnotationTypeMapping mapping : this.mappings) {
			processAliasForAnnotations(mapping);
		}
	}

	private void processAliasForAnnotations(AnnotationTypeMapping mapping) {
		MultiValueMap<Reference, Reference> ultimateTargets = new LinkedMultiValueMap<>();
		for (AttributeType attribute : mapping.getAnnotationType().getAttributeTypes()) {
			Reference source = new Reference(mapping, attribute);
			AliasForDescriptor targetDescriptor = getAliasForDescriptor(source,
					attribute);
			if (targetDescriptor != null) {
				Reference target = getTarget(source, targetDescriptor);
				verifyAliasFor(source, target);
				target.getMapping().addAlias(target, source);
				ultimateTargets.add(getUltimateTarget(target), source);
			}
		}
		for (List<Reference> references : ultimateTargets.values()) {
			if (references.size() > 1) {
				mapping.addMirrorSet(new MirrorSet(references));
			}
		}
	}

	private Reference getUltimateTarget(Reference target) {
		AliasForDescriptor descriptor = getAliasForDescriptor(target,
				target.getAttribute());
		if (descriptor == null) {
			return target;
		}
		Reference nextTarget = getTarget(target, descriptor);
		if (nextTarget.isForSameAnnotation(target)) {
			int compare = target.getAttribute().getAttributeName().compareTo(
					nextTarget.getAttribute().getAttributeName());
			return (compare < 0) ? target : nextTarget;
		}
		return getUltimateTarget(nextTarget);
	}

	private Reference getTarget(Reference source, AliasForDescriptor targetDescriptor) {
		AnnotationTypeMapping targetNode = get(targetDescriptor.getAnnotation());
		if (targetNode == null) {
			throw new AnnotationConfigurationException(String.format(
					"@AliasFor declaration on %s declares an "
							+ "alias for %s which is not meta-present.",
					source, targetDescriptor));
		}
		String targetAttributeName = targetDescriptor.getAttribute();
		AttributeType targetAttribute = targetNode.getAnnotationType().getAttributeTypes().get(
				targetAttributeName);
		if (targetAttribute == null) {
			if (Objects.equals(targetDescriptor.getAnnotation(),
					source.getMapping().getAnnotationType().getClassName())) {
				throw new AnnotationConfigurationException(String.format(
						"@AliasFor declaration on %s declares an "
								+ "alias for '%s' which is not present.",
						source, targetAttributeName));
			}
			throw new AnnotationConfigurationException(
					String.format("%s is declared as an @AliasFor " + "nonexistent %s.",
							source.toCapitalizedString(), targetDescriptor));
		}
		return new Reference(targetNode, targetAttribute);
	}

	private void verifyAliasFor(Reference source, Reference target) {
		if (source.isForSameAnnotation(target)) {
			AliasForDescriptor mirrorDescriptor = getMirrorAliasForDescriptor(source,
					target);
			if (!isDescriptorFor(mirrorDescriptor, source)) {
				throw new AnnotationConfigurationException(String.format(
						"%s must be declared as an @AliasFor '%s', not %s.",
						target.toCapitalizedString(),
						source.getAttribute().getAttributeName(), mirrorDescriptor));
			}
		}
		String sourceReturnType = source.getAttribute().getClassName();
		String targetReturnType = target.getAttribute().getClassName();
		if (!isCompatibleReturnType(sourceReturnType, targetReturnType)) {
			throw new AnnotationConfigurationException(
					String.format("Misconfigured aliases: %s and %s must "
							+ "declare the same return type.", source, target));
		}
	}

	private boolean isCompatibleReturnType(String sourceReturnType,
			String targetReturnType) {
		return Objects.equals(sourceReturnType, targetReturnType)
				|| Objects.equals(sourceReturnType, getComponentType(targetReturnType));
	}

	private String getComponentType(String type) {
		if (type.endsWith("[]")) {
			return type.substring(0, type.length() - 2);
		}
		return null;
	}

	private boolean isDescriptorFor(AliasForDescriptor descriptor, Reference target) {
		String targetAnnotation = target.getMapping().getAnnotationType().getClassName();
		String targetAttribute = target.getAttribute().getAttributeName();
		return Objects.equals(descriptor.getAnnotation(), targetAnnotation)
				&& Objects.equals(descriptor.getAttribute(), targetAttribute);
	}

	private AliasForDescriptor getMirrorAliasForDescriptor(Reference source,
			Reference target) {
		DeclaredAnnotation mirrorAliasFor = target.getAttribute().getDeclaredAnnotations().find(
				ALIAS_FOR_ANNOTATION);
		if (mirrorAliasFor == null) {
			throw new AnnotationConfigurationException(
					String.format("%s must be declared as an @AliasFor '%s'.",
							target.toCapitalizedString(),
							source.getAttribute().getAttributeName()));
		}
		return getAliasForDescriptor(target, mirrorAliasFor);
	}

	private AliasForDescriptor getAliasForDescriptor(Reference source,
			AttributeType attribute) {
		DeclaredAnnotation aliasFor = source.getAttribute().getDeclaredAnnotations().find(
				ALIAS_FOR_ANNOTATION);
		return getAliasForDescriptor(source, aliasFor);
	}

	private AliasForDescriptor getAliasForDescriptor(Reference source,
			DeclaredAnnotation aliasFor) {
		if (aliasFor == null) {
			return null;
		}
		return new AliasForDescriptor(
				source.getMapping().getAnnotationType().getClassName(),
				source.getAttribute().getAttributeName(), aliasFor);
	}

	public List<AnnotationTypeMapping> getAll() {
		return this.mappings;
	}

	public AnnotationTypeMapping get(String annotationType) {
		return this.mappingForType.get(annotationType);
	}

	public static AnnotationTypeMappings forType(ClassLoader classLoader,
			RepeatableContainers repeatableContainers, AnnotationType type) {
		Assert.notNull(repeatableContainers, "RepeatableContainers must not be null");
		if (type == null) {
			return null;
		}
		if (classLoader == null) {
			classLoader = ClassUtils.getDefaultClassLoader();
		}
		Cache perClassloadCache = cache.computeIfAbsent(classLoader, key -> new Cache());
		return perClassloadCache.get(classLoader, repeatableContainers, type);
	}

	/**
	 * Builder used to create the mappings.
	 */
	private static class MappingsBuilder {

		private final ClassLoader classLoader;

		private final RepeatableContainers repeatableContainers;

		public MappingsBuilder(ClassLoader classLoader,
				RepeatableContainers repeatableContainers) {
			this.classLoader = classLoader;
			this.repeatableContainers = repeatableContainers;
		}

		public List<AnnotationTypeMapping> build(AnnotationType source) {
			if (!isMappable(source.getClassName())) {
				return Collections.emptyList();
			}
			List<AnnotationTypeMapping> result = new ArrayList<>();
			Deque<AnnotationTypeMapping> queue = new ArrayDeque<>();
			queue.add(new AnnotationTypeMapping(this.classLoader,
					this.repeatableContainers, source));
			while (!queue.isEmpty()) {
				AnnotationTypeMapping mapping = queue.removeFirst();
				result.add(mapping);
				addMappings(queue, mapping, mapping.getAnnotationType());
			}
			return Collections.unmodifiableList(result);
		}

		private void addMappings(Deque<AnnotationTypeMapping> queue,
				AnnotationTypeMapping parent, AnnotationType type) {
			for (DeclaredAnnotation metaAnnotation : type.getDeclaredAnnotations()) {
				this.repeatableContainers.visit(this.classLoader, metaAnnotation, (annotation,
						attributes) -> addMapping(queue, parent, annotation, attributes));
			}
		}

		private void addMapping(Deque<AnnotationTypeMapping> queue,
				AnnotationTypeMapping parent, AnnotationType annotation,
				DeclaredAttributes attributes) {
			if (isMappable(parent, annotation)) {
				AnnotationTypeMapping mapping = new AnnotationTypeMapping(
						this.classLoader, this.repeatableContainers, parent, annotation,
						attributes);
				queue.addLast(mapping);
			}
		}

		private boolean isMappable(AnnotationTypeMapping parent,
				AnnotationType annotation) {
			String annotationType = annotation.getClassName();
			return isMappable(annotationType) && !isAlreadyMapped(parent, annotationType);
		}

		private boolean isMappable(String annotationType) {
			return !(annotationType.startsWith("java.lang.annotation.")
					|| annotationType.startsWith("org.springframework.lang."));
		}

		private boolean isAlreadyMapped(AnnotationTypeMapping parent,
				String annotationType) {
			AnnotationTypeMapping mapping = parent;
			while (mapping != null) {
				if (mapping.getAnnotationType().getClassName().equals(annotationType)) {
					return true;
				}
				mapping = mapping.getParent();
			}
			return false;
		}

	}

	/**
	 * Type mapping cached per class loader.
	 */
	private static class Cache {

		private final Map<Key, AnnotationTypeMappings> mappings = new ConcurrentReferenceHashMap<>();

		public AnnotationTypeMappings get(ClassLoader classLoader,
				RepeatableContainers repeatableContainers, AnnotationType type) {
			Key key = new Key(repeatableContainers, type);
			return this.mappings.computeIfAbsent(key,
					k -> new AnnotationTypeMappings(classLoader, repeatableContainers,
							type));
		}

		/**
		 * The cache key.
		 */
		private static final class Key {

			private final RepeatableContainers repeatableContainers;

			private final String type;

			Key(RepeatableContainers repeatableContainers, AnnotationType type) {
				this.type = type.getClassName();
				this.repeatableContainers = repeatableContainers;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + this.repeatableContainers.hashCode();
				result = prime * result + this.type.hashCode();
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj) {
					return true;
				}
				if (obj == null || getClass() != obj.getClass()) {
					return false;
				}
				Key other = (Key) obj;
				return this.type.equals(other.type)
						&& this.repeatableContainers.equals(other.repeatableContainers);
			}

		}

	}

}
