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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.core.annotation.type.AbstractDeclaredAttributes;
import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.Assert;
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
 * <p>
 * An {@link AnnotationTypeMapping} can be used to
 * {@link #map(DeclaredAnnotation) map} a {@link MappableAnnotation} to a
 * {@link MergedAnnotation}.
 *
 * @author Phillip Webb
 * @since 5.1
 * @see #map(MappableAnnotation)
 */
class AnnotationTypeMapping {

	private final ClassLoader classLoader;

	private final RepeatableContainers repeatableContainers;

	private final AnnotationTypeMapping parent;

	private final int depth;

	private final AnnotationType annotationType;

	private final DeclaredAttributes annotationAttributes;

	private final Map<String, Reference> aliases = new LinkedHashMap<>();

	private final List<MirrorSet> mirrorSets = new ArrayList<>();

	AnnotationTypeMapping(ClassLoader classLoader,
			RepeatableContainers repeatableContainers, AnnotationType annotationType) {
		this(classLoader, repeatableContainers, null, annotationType,
				DeclaredAttributes.NONE);
	}

	AnnotationTypeMapping(ClassLoader classLoader,
			RepeatableContainers repeatableContainers, AnnotationTypeMapping parent,
			AnnotationType annotationType, DeclaredAttributes attributes) {
		this.classLoader = classLoader;
		this.repeatableContainers = repeatableContainers;
		this.parent = parent;
		this.depth = (parent == null ? 0 : parent.depth + 1);
		this.annotationType = annotationType;
		this.annotationAttributes = attributes;
	}

	public void addAlias(Reference from, Reference to) {
		this.aliases.putIfAbsent(from.getAttribute().getAttributeName(), to);
	}

	public void addMirrorSet(Collection<Reference> references) {
		for (Reference reference : references) {
			Assert.state(equals(reference.getMapping()),
					"Invalid mirror mapping reference");
			String attributeName = reference.getAttribute().getAttributeName();
			Reference aliasTo = this.aliases.get(attributeName);
			if (aliasTo != null && equals(aliasTo.getMapping())) {
				this.aliases.remove(attributeName);
			}
		}
		this.mirrorSets.add(new MirrorSet(references));
	}

	public ClassLoader getClassLoader() {
		return this.classLoader;
	}

	public RepeatableContainers getRepeatableContainers() {
		return this.repeatableContainers;
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

	public int getDepth() {
		return this.depth;
	}

	// FIXME perhaps return MappedAnnotation?
	public <A extends Annotation> TypeMappedAnnotation<A> map(
			DeclaredAttributes attributes, boolean inherited) {
		DeclaredAttributes mappedAttributes = mapAttributes(attributes);
		return new TypeMappedAnnotation<>(this, mappedAttributes, inherited, null);
	}

	// FIXME
	// public <A extends Annotation> MergedAnnotation<A> map(MappableAnnotation
	// annotation,
	// boolean inherited) {
	// if (annotation == null) {
	// return MergedAnnotation.missing();
	// }
	// try {
	// DeclaredAttributes mappedAttributes = mapAttributes(
	// annotation.getAttributes());
	// return new MappedAnnotation<>(this, mappedAttributes, inherited, null);
	// }
	// catch (Exception ex) {
	// throw new AnnotationConfigurationException("Unable to map attributes of "
	// + annotation.getAnnotationType().getClassName(), ex);
	// }
	// }

	private DeclaredAttributes mapAttributes(DeclaredAttributes rootAttributes) {
		DeclaredAttributes mappedAttributes = rootAttributes;
		if (this.parent != null) {
			DeclaredAttributes parentAttributes = this.parent.mapAttributes(
					rootAttributes);
			mappedAttributes = new ParentMappedAttributes(this.annotationType,
					this.annotationAttributes, parentAttributes, this.aliases);
		}
		if (!this.mirrorSets.isEmpty()) {
			mappedAttributes = new MirroredAttributes(this.annotationAttributes,
					mappedAttributes, this.mirrorSets);
		}
		return mappedAttributes;
	}

	/**
	 * {@link DeclaredAttributes} decorator to apply mapping rules.
	 */
	private class ParentMappedAttributes extends AbstractDeclaredAttributes {

		private final AnnotationType annotationType;

		private final DeclaredAttributes annotationAttributes;

		private final DeclaredAttributes parentAttributes;

		private final Map<String, Reference> aliases;

		public ParentMappedAttributes(AnnotationType annotationType,
				DeclaredAttributes annotationAttributes,
				DeclaredAttributes parentAttributes, Map<String, Reference> aliases) {
			this.annotationType = annotationType;
			this.annotationAttributes = annotationAttributes;
			this.parentAttributes = parentAttributes;
			this.aliases = aliases;
		}

		@Override
		public Object get(String name) {
			Assert.notNull(name, "Name must not be null");
			AttributeType type = this.annotationType.getAttributeTypes().get(name);
			Object result = null;
			Reference alias = this.aliases.get(name);
			if (alias != null) {
				String aliasName = alias.getAttribute().getAttributeName();
				result = this.parentAttributes.get(aliasName);
			}
			if (result == null && !isConventionRestricted(name)) {
				result = this.parentAttributes.get(name);
			}
			if (result == null) {
				result = this.annotationAttributes.get(name);
			}
			if (result != null && isArrayAttributeType(type)
					&& !ObjectUtils.isArray(result)) {
				result = wrapInArray(result);
			}
			return result;
		}

		private boolean isConventionRestricted(String name) {
			return "value".equals(name);
		}

		private boolean isArrayAttributeType(AttributeType type) {
			return type != null && type.getClassName().endsWith("[]");
		}

		private Object wrapInArray(Object result) {
			Object array = Array.newInstance(result.getClass(), 1);
			Array.set(array, 0, result);
			return array;
		}

		@Override
		public Set<String> names() {
			return this.annotationType.getAttributeTypes().names();
		}

	}

	/**
	 * {@link DeclaredAttributes} decorator to apply mirroring rules.
	 */
	private static class MirroredAttributes extends AbstractDeclaredAttributes {

		private final DeclaredAttributes annotationAttributes;

		private final DeclaredAttributes sourceAttributes;

		private final Map<String, Reference> mirrors;

		public MirroredAttributes(DeclaredAttributes annotationAttributes,
				DeclaredAttributes sourceAttributes, List<MirrorSet> mirrorSets) {
			this.annotationAttributes = annotationAttributes;
			this.sourceAttributes = sourceAttributes;
			this.mirrors = getMirrors(mirrorSets);
		}

		private Map<String, Reference> getMirrors(List<MirrorSet> mirrorSets) {
			Map<String, Reference> mirrors = new HashMap<>();
			for (MirrorSet mirrorSet : mirrorSets) {
				addMirrors(mirrors, mirrorSet);
			}
			return Collections.unmodifiableMap(mirrors);
		}

		private void addMirrors(Map<String, Reference> mirrors, MirrorSet mirrorSet) {
			Reference inUse = getMirrorAttributeInUse(mirrorSet);
			for (Reference mirror : mirrorSet) {
				mirrors.put(mirror.getAttribute().getAttributeName(), inUse);
			}
		}

		private Reference getMirrorAttributeInUse(MirrorSet mirrorSet) {
			Reference result = null;
			Object lastValue = null;
			for (Reference candidate : mirrorSet) {
				AttributeType attribute = candidate.getAttribute();
				String name = attribute.getAttributeName();
				Object value = this.sourceAttributes.get(name);
				if (value != null && !isSameAsDefaultValue(value, attribute)) {
					if (result != null) {
						checkMirrorPossibleAttributeResult(name, result, value,
								lastValue);
					}
					result = candidate;
					lastValue = value;
				}
			}
			return result;
		}

		private void checkMirrorPossibleAttributeResult(String name, Reference result,
				Object value, Object lastValue) {
			if (ObjectUtils.nullSafeEquals(value, lastValue)
					|| isShadow(result, value, lastValue)) {
				return;
			}
			// FIXME this.source.getDeclaringClass();
			Class<?> declaringClass = null;
			String on = (declaringClass != null)
					? " declared on " + declaringClass.getName()
					: "";
			String annotationType = result.getMapping().getAnnotationType().getClassName();
			String lastName = result.getAttribute().getAttributeName();
			throw new AnnotationConfigurationException(String.format(
					"Different @AliasFor mirror values for annotation [%s]%s, "
							+ "attribute '%s' and its alias '%s' are declared with values of [%s] and [%s].",
					annotationType, on, lastName, name,
					ObjectUtils.nullSafeToString(lastValue),
					ObjectUtils.nullSafeToString(value)));
		}

		private boolean isShadow(Reference result, Object value, Object lastValue) {
			Object attributeValue = this.annotationAttributes.get(
					result.getAttribute().getAttributeName());
			return ObjectUtils.nullSafeEquals(lastValue, attributeValue);
		}

		private boolean isSameAsDefaultValue(Object value, AttributeType attribute) {
			Object defaultValue = attribute.getDefaultValue();
			if (ObjectUtils.nullSafeEquals(value, defaultValue)) {
				return true;
			}
			if (isZeroLengthArray(defaultValue) && isZeroLengthArray(value)) {
				return true;
			}
			return false;
		}

		private boolean isZeroLengthArray(Object defaultValue) {
			return ObjectUtils.isArray(defaultValue)
					&& Array.getLength(defaultValue) == 0;
		}

		@Override
		public Set<String> names() {
			return this.sourceAttributes.names();
		}

		@Override
		public Object get(String name) {
			Reference mirror = this.mirrors.get(name);
			if (mirror != null) {
				name = mirror.getAttribute().getAttributeName();
			}
			return this.sourceAttributes.get(name);
		}

	}

	/**
	 * A set of mirror attribute references.
	 */
	private static class MirrorSet implements Iterable<Reference> {

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
