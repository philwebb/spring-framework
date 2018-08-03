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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.core.annotation.AnnotationTypeMapping.Reference;
import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AnnotationTypeResolver;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * Provides {@link AnnotationTypeMapping} information for a single source
 * {@link AnnotationType}. Performs a recursive breadth first crawl of all
 * meta-annotations to ultimately provide a quick way to map a
 * {@link MappableAnnotation} to a {@link MappedAnnotation}.
 * <p>
 * Support convention based merging of meta-annotations as well as implicit and
 * explicit {@link AliasFor @AliasFor} aliases.
 * <p>
 * This class is designed to be cached so that meta-annotations only need to be
 * searched once, regardless of how many times they are actually used.
 *
 * @author Phillip Webb
 * @since 5.1
 * @see #getMapping(String)
 * @see #getAllMappings()
 * @see AnnotationTypeMapping
 */
class AnnotationTypeMappings {

	private static Map<ClassLoader, Cache> cache = new ConcurrentReferenceHashMap<>();

	static final String ALIAS_FOR_ANNOTATION = AliasFor.class.getName();

	private final AnnotationType source;

	private final List<AnnotationTypeMapping> mappings;

	private final Map<String, AnnotationTypeMapping> mappingForType;

	/**
	 * Create a new {@link AliasedAnnotationType} instance for the given source.
	 * @param resolver the {@link AnnotationTypeResolver} used to resolve
	 * meta-annotations
	 * @param source the source annotation type
	 */
	AnnotationTypeMappings(AnnotationTypeResolver resolver, AnnotationType source) {
		Assert.notNull(resolver, "Resolver must not be null");
		Assert.notNull(source, "Source must not be null");
		this.source = source;
		this.mappings = buildMappings(resolver);
		this.mappingForType = buildMappingForType(this.mappings);
		processAliasForAnnotations();
	}

	private List<AnnotationTypeMapping> buildMappings(AnnotationTypeResolver resolver) {
		List<AnnotationTypeMapping> mappings = new ArrayList<>();
		Deque<AnnotationTypeMapping> queue = new ArrayDeque<>();
		MappableAnnotation root = new MappableAnnotation(resolver, this.source,
				DeclaredAttributes.NONE);
		queue.add(new AnnotationTypeMapping(resolver, null, root));
		while (!queue.isEmpty()) {
			AnnotationTypeMapping mapping = queue.removeFirst();
			mappings.add(mapping);
			addMappings(queue, resolver, mapping, mapping.getAnnotationType());
		}
		return Collections.unmodifiableList(mappings);
	}

	private void addMappings(Deque<AnnotationTypeMapping> queue,
			AnnotationTypeResolver resolver, AnnotationTypeMapping parent,
			AnnotationType type) {
		MappableAnnotation.from(resolver, type.getDeclaredAnnotations()).forEach(
				annotation -> addMapping(queue, resolver, parent, annotation));
	}

	private void addMapping(Deque<AnnotationTypeMapping> queue,
			AnnotationTypeResolver resolver, AnnotationTypeMapping parent,
			MappableAnnotation annotation) {
		if (isMappable(parent, annotation)) {
			queue.addLast(new AnnotationTypeMapping(resolver, parent, annotation));
		}
	}

	private boolean isMappable(AnnotationTypeMapping parent,
			MappableAnnotation annotation) {
		String className = annotation.getAnnotationType().getClassName();
		return !(className.startsWith("java.lang.annotation")
				|| className.startsWith("org.springframework.lang.")
				|| parent.isDescendant(className));
	}

	private Map<String, AnnotationTypeMapping> buildMappingForType(
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
		for (List<Reference> mirrors : ultimateTargets.values()) {
			if (mirrors.size() > 1) {
				validateMirrors(mirrors);
				mapping.addMirrors(mirrors);
			}
		}
	}

	private Reference getUltimateTarget(Reference target) {
		AliasForDescriptor descriptor = getAliasForDescriptor(target,
				target.getAttribute());
		if (descriptor == null) {
			return null;
		}
		Reference nextTarget = getTarget(target, descriptor);
		boolean isExplicitMirror = nextTarget.isForSameAnnotation(target);
		if (isExplicitMirror) {
			int compare = target.getAttribute().getAttributeName().compareTo(
					nextTarget.getAttribute().getAttributeName());
			return (compare < 0) ? target : nextTarget;
		}
		return getUltimateTarget(nextTarget);
	}

	private Reference getTarget(Reference source, AliasForDescriptor targetDescriptor) {
		AnnotationTypeMapping targetNode = getMapping(targetDescriptor.getAnnotation());
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
		if (!Objects.equals(sourceReturnType, targetReturnType)) {
			throw new AnnotationConfigurationException(
					String.format("Misconfigured aliases: %s and %s must "
							+ "declare the same return type.", source, target));
		}
	}

	private boolean isDescriptorFor(AliasForDescriptor descriptor, Reference target) {
		String targetAnnotation = target.getMapping().getAnnotationType().getClassName();
		String targetAttribute = target.getAttribute().getAttributeName();
		return Objects.equals(descriptor.getAnnotation(), targetAnnotation)
				&& Objects.equals(descriptor.getAttribute(), targetAttribute);
	}

	private void validateMirrors(List<Reference> mirrors) {
		Iterator<Reference> iterator = mirrors.iterator();
		Reference source = iterator.next();
		while (iterator.hasNext()) {
			Reference mirror = iterator.next();
			Object sourceDefault = source.getAttribute().getDefaultValue();
			Object mirrorDefault = mirror.getAttribute().getDefaultValue();
			if (sourceDefault == null || mirrorDefault == null) {
				throw new AnnotationConfigurationException(String.format(
						"Misconfigured aliases: %s and %s must declare default values.",
						mirror, source));
			}
			if (!ObjectUtils.nullSafeEquals(sourceDefault, mirrorDefault)) {
				throw new AnnotationConfigurationException(String.format(
						"Misconfigured aliases: %s and %s must declare the same default value.",
						mirror, source));
			}
		}
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

	public Stream<AnnotationTypeMapping> getAllMappings() {
		return this.mappings.stream();
	}

	public AnnotationTypeMapping getMapping(String annotationType) {
		return this.mappingForType.get(annotationType);
	}

	public static AnnotationTypeMappings get(AnnotationTypeResolver resolver,
			AnnotationType type) {
		ClassLoader classLoader = resolver.getClassLoader();
		Cache perClassloadCache = cache.computeIfAbsent(classLoader, key -> new Cache());
		return perClassloadCache.get(resolver, type);
	}

	/**
	 * Type mapping cached per class loader.
	 */
	private static class Cache {

		private final Map<String, AnnotationTypeMappings> mappings = new ConcurrentReferenceHashMap<>();

		public AnnotationTypeMappings get(AnnotationTypeResolver resolver,
				AnnotationType type) {
			return this.mappings.computeIfAbsent(type.getClassName(),
					key -> new AnnotationTypeMappings(resolver, type));
		}

	}

}
