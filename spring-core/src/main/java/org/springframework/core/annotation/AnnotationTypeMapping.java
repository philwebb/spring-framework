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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.AttributeTypes;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Provides mapping information for a single annotation (or meta-annotation) in
 * the context of a source {@link AnnotationType}. Each mapping includes:
 * <ul>
 * <li>The source {@link MappableAnnotation}</li>
 * <li>The parent meta-annotation that defined it (if any)</li>
 * <li>Any aliases defined against it (i.e. the if this meta-annotation is a
 * {@code @AliasFor} target)</li>
 * <li>Any implicit or explicit mirrored attributes (i.e. attributes that all
 * should return the same value</li>
 * </ul>
 *
 * @author Phillip Webb
 * @since 5.2
 * @see AnnotationTypeMappings
 */
class AnnotationTypeMapping {

	private final ClassLoader classLoader;

	private final RepeatableContainers repeatableContainers;

	private final AnnotationFilter annotationFilter;

	private final AnnotationTypeMapping parent;

	private final AnnotationType annotationType;

	private final DeclaredAttributes annotationAttributes;

	private final Map<String, Reference> aliases = new LinkedHashMap<>();

	private final List<MirrorSet> mirrorSets = new ArrayList<>();

	AnnotationTypeMapping(ClassLoader classLoader,
			RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter,
			AnnotationType annotationType) {
		this(classLoader, repeatableContainers, annotationFilter, null, annotationType,
				DeclaredAttributes.NONE);
	}

	AnnotationTypeMapping(ClassLoader classLoader,
			RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter,
			AnnotationTypeMapping parent, AnnotationType annotationType,
			DeclaredAttributes attributes) {
		this.classLoader = classLoader;
		this.repeatableContainers = repeatableContainers;
		this.annotationFilter = annotationFilter;
		this.parent = parent;
		this.annotationType = annotationType;
		this.annotationAttributes = attributes;
	}

	void addAlias(Reference from, Reference to) {
		Assert.state(equals(from.getMapping()), "Invalid mirror mapping reference");
		addAlias(from.getAttribute().getAttributeName(), to);
	}

	void addAlias(String fromAttributeName, AnnotationTypeMapping toMapping,
			String toAttributeName) {
		addAlias(fromAttributeName, new Reference(toMapping,
				toMapping.getAnnotationType().getAttributeTypes().get(toAttributeName)));
	}

	void addAlias(String fromAttributeName, Reference to) {
		this.aliases.putIfAbsent(fromAttributeName, to);
	}

	void addMirrorSet(String... attributeNames) {
		List<Reference> references = new ArrayList<>(attributeNames.length);
		for (String attributeName : attributeNames) {
			AttributeType attributeType = getAnnotationType().getAttributeTypes().get(
					attributeName);
			Assert.notNull(attributeType, "Missing attribute " + attributeName);
			references.add(new Reference(this, attributeType));
		}
		addMirrorSet(new MirrorSet(references));
	}

	void addMirrorSet(MirrorSet mirrorSet) {
		for (Reference reference : mirrorSet) {
			Assert.state(equals(reference.getMapping()),
					"Invalid mirror mapping reference");
			String attributeName = reference.getAttribute().getAttributeName();
			Reference aliasTo = this.aliases.get(attributeName);
			if (aliasTo != null && equals(aliasTo.getMapping())) {
				this.aliases.remove(attributeName);
			}
		}
		this.mirrorSets.add(mirrorSet);
	}

	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	public AnnotationTypeMapping getParent() {
		return this.parent;
	}

	public AnnotationType getAnnotationType() {
		return this.annotationType;
	}

	public DeclaredAttributes getAnnotationAttributes() {
		return this.annotationAttributes;
	}

	public Map<String, Reference> getAliases() {
		return this.aliases;
	}

	public List<MirrorSet> getMirrorSets() {
		return this.mirrorSets;
	}

	public AnnotationTypeMapping getNested(String className) {
		return getNested(AnnotationType.resolve(className, this.classLoader));
	}

	public AnnotationTypeMapping getNested(AnnotationType type) {
		return AnnotationTypeMappings.forType(this.classLoader, this.repeatableContainers,
				this.annotationFilter, type).get(type.getClassName());
	}

	public boolean canSkipSynthesize() {
		try {
			if (this.parent != null || !this.mirrorSets.isEmpty()
					|| !this.aliases.isEmpty()) {
				return false;
			}
			return canSkipSynthesize(this.annotationType.getAttributeTypes());
		}
		catch (Exception ex) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private boolean canSkipSynthesize(AttributeTypes attributeTypes) {
		for (AttributeType attributeType : attributeTypes) {
			Class<?> type = ClassUtils.resolveClassName(
					attributeType.getClassName().replace("[]", ""), this.classLoader);
			if (type.isAnnotation()) {
				AnnotationType annotationType = AnnotationType.resolve(
						(Class<? extends Annotation>) type);
				if (!getNested(annotationType).canSkipSynthesize()) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * A set of mirror attribute references.
	 */
	static class MirrorSet implements Iterable<Reference> {

		private final Set<Reference> references;

		public MirrorSet(Collection<Reference> references) {
			Iterator<Reference> iterator = references.iterator();
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
			this.references = new LinkedHashSet<>(references);
			Assert.isTrue(references.size() > 1,
					"Mirrors must contain more than one reference");
		}

		@Override
		public Iterator<Reference> iterator() {
			return this.references.iterator();
		}

		@Override
		public String toString() {
			return this.references.toString();
		}

	}

	/**
	 * Holds a reference to an {@link AnnotationTypeMapping} and an
	 * {@link AttributeType}.
	 */
	static class Reference {

		private final AnnotationTypeMapping mapping;

		private final AttributeType attribute;

		Reference(AnnotationTypeMapping mapping, AttributeType attribute) {
			this.mapping = mapping;
			this.attribute = attribute;
		}

		public AnnotationTypeMapping getMapping() {
			return this.mapping;
		}

		public AttributeType getAttribute() {
			return this.attribute;
		}

		public boolean isForSameAnnotation(Reference other) {
			if (other == null) {
				return false;
			}
			return Objects.equals(getMapping().getAnnotationType().getClassName(),
					other.getMapping().getAnnotationType().getClassName());
		}

		public String toCapitalizedString() {
			return StringUtils.capitalize(toString());
		}

		@Override
		public String toString() {
			return String.format("attribute '%s' in annotation [%s]",
					this.attribute.getAttributeName(),
					this.mapping.getAnnotationType().getClassName());
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ this.mapping.getAnnotationType().getClassName().hashCode();
			result = prime * result + this.attribute.getAttributeName().hashCode();
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
			Reference other = (Reference) obj;
			return Objects.equals(this.mapping.getAnnotationType().getClassName(),
					other.mapping.getAnnotationType().getClassName())
					&& Objects.equals(this.attribute.getAttributeName(),
							other.attribute.getAttributeName());
		}

	}

}
