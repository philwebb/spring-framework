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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSet;
import org.springframework.core.annotation.AnnotationTypeMapping.Reference;
import org.springframework.core.annotation.type.AbstractDeclaredAttributes;
import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link MergedAnnotation} backed by a {@link AnnotationTypeMapping}.
 *
 * @author Phillip Webb
 * @since 5.1
 * @param <A> the annotation type
 */
class TypeMappedAnnotation<A extends Annotation> extends AbstractMergedAnnotation<A> {

	private final AnnotationTypeMapping mapping;

	private final boolean inherited;

	private final DeclaredAttributes mappedAttributes;

	private final DeclaredAttributes nonMappedAttributes;

	private final TypeMappedAnnotation<?> parent;

	private final Predicate<String> attributeFilter;

	TypeMappedAnnotation(AnnotationTypeMapping mapping, boolean inherited,
			DeclaredAttributes rootAttributes) {
		TypeMappedAnnotation<?> parent = null;
		DeclaredAttributes mappedAttributes = rootAttributes;
		if (mapping.getParent() != null) {
			parent = new TypeMappedAnnotation<>(mapping.getParent(), inherited,
					rootAttributes);
			mappedAttributes = new ParentMappedAttributes(mapping,
					parent.mappedAttributes);
		}
		mappedAttributes = MirroredAttributes.applyIfNecessary(mapping, mappedAttributes);
		this.nonMappedAttributes = MirroredAttributes.applyIfNecessary(mapping,
				parent != null ? mapping.getAnnotationAttributes() : rootAttributes);
		this.mapping = mapping;
		this.inherited = inherited;
		this.mappedAttributes = mappedAttributes;
		this.parent = parent;
		this.attributeFilter = null;
	}

	private TypeMappedAnnotation(AnnotationTypeMapping mapping, boolean inherited,
			DeclaredAttributes mappedAttributes, DeclaredAttributes nonMappedAttributes,
			TypeMappedAnnotation<?> parent, Predicate<String> attributeFilter) {
		this.mapping = mapping;
		this.inherited = inherited;
		this.mappedAttributes = mappedAttributes;
		this.nonMappedAttributes = nonMappedAttributes;
		this.parent = parent;
		this.attributeFilter = attributeFilter;
	}

	@Override
	public boolean isPresent() {
		return true;
	}

	@Override
	public MergedAnnotation<?> getParent() {
		return this.parent;
	}

	@Override
	public boolean isFromInherited() {
		return this.inherited;
	}

	@Override
	public MergedAnnotation<A> filterAttributes(Predicate<String> predicate) {
		Assert.notNull(predicate, "Predicate must not be null");
		if (this.attributeFilter != null) {
			predicate = this.attributeFilter.and(predicate);
		}
		return new TypeMappedAnnotation<>(this.mapping, this.inherited,
				this.mappedAttributes, this.nonMappedAttributes, this.parent, predicate);
	}

	@Override
	protected ClassLoader getClassLoader() {
		return this.mapping.getClassLoader();
	}

	@Override
	protected AnnotationType getAnnotationType() {
		return this.mapping.getAnnotationType();
	}

	@Override
	protected boolean isFiltered(String attributeName) {
		if (this.attributeFilter != null) {
			return this.attributeFilter.test(attributeName);
		}
		return false;
	}

	@Override
	protected Object getAttributeValue(String attributeName, boolean nonMerged) {
		DeclaredAttributes attributes = nonMerged ? this.nonMappedAttributes
				: this.mappedAttributes;
		return attributes.get(attributeName);
	}

	@Override
	protected <T extends Annotation> MergedAnnotation<T> createNested(AnnotationType type,
			DeclaredAttributes attributes) {
		AnnotationTypeMapping nestedMapping = getNestedMapping(type);
		return new TypeMappedAnnotation<>(nestedMapping, this.inherited, attributes);
	}

	private AnnotationTypeMapping getNestedMapping(AnnotationType type) {
		return AnnotationTypeMappings.get(this.mapping.getClassLoader(),
				this.mapping.getRepeatableContainers(), type).getMapping(
						type.getClassName());
	}

	/**
	 * {@link DeclaredAttributes} decorator to apply mapping rules.
	 */
	private class ParentMappedAttributes extends AbstractDeclaredAttributes {

		private final AnnotationType annotationType;

		private final DeclaredAttributes annotationAttributes;

		private final Map<String, Reference> aliases;

		private final DeclaredAttributes parentAttributes;

		public ParentMappedAttributes(AnnotationTypeMapping mapping,
				DeclaredAttributes parentAttributes) {
			this.annotationType = mapping.getAnnotationType();
			this.annotationAttributes = mapping.getAnnotationAttributes();
			this.aliases = mapping.getAliases();
			this.parentAttributes = parentAttributes;
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

		MirroredAttributes(AnnotationTypeMapping mapping,
				DeclaredAttributes sourceAttributes) {
			this.annotationAttributes = mapping.getAnnotationAttributes();
			this.sourceAttributes = sourceAttributes;
			this.mirrors = getMirrors(mapping.getMirrorSets());
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

		public static DeclaredAttributes applyIfNecessary(AnnotationTypeMapping mapping,
				DeclaredAttributes attributes) {
			if (mapping.getMirrorSets().isEmpty()) {
				return attributes;
			}
			return new MirroredAttributes(mapping, attributes);
		}

	}

}
