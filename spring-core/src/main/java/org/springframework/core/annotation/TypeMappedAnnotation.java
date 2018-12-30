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
import java.util.function.Predicate;

import org.springframework.core.annotation.AnnotationTypeMapping.MirrorSet;
import org.springframework.core.annotation.AnnotationTypeMapping.Reference;
import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link MergedAnnotation} backed by a {@link AnnotationTypeMapping}.
 *
 * @author Phillip Webb
 * @since 5.2
 * @param <A> the annotation type
 */
class TypeMappedAnnotation<A extends Annotation> extends AbstractMergedAnnotation<A> {

	// FIXME do we need equals/hashcode

	private final AnnotationTypeMapping mapping;

	private final Object source;

	private final int aggregateIndex;

	private final TypeMappedAnnotation<?> parent;

	private final Attributes attributes;

	private final boolean nonMergedAttributes;

	private final Predicate<String> attributeFilter;

	TypeMappedAnnotation(AnnotationTypeMapping mapping, Object source, int aggregateIndex,
			DeclaredAttributes rootAttributes) {
		this.mapping = mapping;
		this.source = source;
		this.aggregateIndex = aggregateIndex;
		this.parent = mapping.getParent() != null
				? new TypeMappedAnnotation<>(mapping.getParent(), source, aggregateIndex,
						rootAttributes)
				: null;
		this.attributes = createAttributes(mapping, source, rootAttributes, this.parent);
		this.nonMergedAttributes = false;
		this.attributeFilter = null;
	}

	private TypeMappedAnnotation(AnnotationTypeMapping mapping, Object source,
			int aggregateIndex, TypeMappedAnnotation<?> parent, Attributes attributes,
			Predicate<String> attributeFilter, boolean nonMergedAttributes) {
		this.mapping = mapping;
		this.source = source;
		this.aggregateIndex = aggregateIndex;
		this.parent = parent;
		this.attributes = attributes;
		this.nonMergedAttributes = nonMergedAttributes;
		this.attributeFilter = attributeFilter;
	}

	private Attributes createAttributes(AnnotationTypeMapping mapping, Object source,
			DeclaredAttributes rootAttributes, TypeMappedAnnotation<?> parent) {
		Attributes attributes = parent != null
				? new ParentMappedAttributes(mapping, parent)
				: new RootAttributes(rootAttributes);
		if (!mapping.getMirrorSets().isEmpty()) {
			attributes = new MirroredAttributes(mapping, this.source, attributes);
		}
		return attributes;
	}

	@Override
	public boolean isPresent() {
		return true;
	}

	@Override
	public int getAggregateIndex() {
		return this.aggregateIndex;
	}

	@Override
	public Object getSource() {
		return this.source;
	}

	@Override
	public MergedAnnotation<?> getParent() {
		return this.parent;
	}

	@Override
	public MergedAnnotation<A> filterAttributes(Predicate<String> predicate) {
		Assert.notNull(predicate, "Predicate must not be null");
		if (this.attributeFilter != null) {
			predicate = this.attributeFilter.and(predicate);
		}
		return new TypeMappedAnnotation<>(this.mapping, this.source, this.aggregateIndex,
				this.parent, this.attributes, predicate, this.nonMergedAttributes);
	}

	@Override
	public MergedAnnotation<A> withNonMergedAttributes() {
		return new TypeMappedAnnotation<>(this.mapping, this.source, this.aggregateIndex,
				this.parent, this.attributes, this.attributeFilter, true);
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
			return !this.attributeFilter.test(attributeName);
		}
		return false;
	}

	@Override
	protected Object getAttributeValue(String attributeName) {
		Assert.hasText(attributeName, "AttributeName must not be empty");
		return this.attributes.get(attributeName, this.nonMergedAttributes);
	}

	@Override
	protected <T extends Annotation> MergedAnnotation<T> createNested(AnnotationType type,
			DeclaredAttributes attributes) {
		AnnotationTypeMapping nestedMapping = getNestedMapping(type);
		return new TypeMappedAnnotation<>(nestedMapping, this.source, this.aggregateIndex,
				attributes);
	}

	private AnnotationTypeMapping getNestedMapping(AnnotationType type) {
		return AnnotationTypeMappings.forType(this.mapping.getClassLoader(),
				this.mapping.getRepeatableContainers(), type).get(type.getClassName());
	}

	/**
	 * Provides access to the attribute values with additional mapping and
	 * mirroring rules applied.
	 */
	@FunctionalInterface
	private static interface Attributes {

		Object get(String name, boolean nonMerged);

	}

	/**
	 * Simple adapter used to expose the root {@link DeclaredAttributes}.
	 */
	private static class RootAttributes implements Attributes {

		private final DeclaredAttributes declaredAttributes;

		RootAttributes(DeclaredAttributes declaredAttributes) {
			this.declaredAttributes = declaredAttributes;
		}

		@Override
		public Object get(String name, boolean nonMerged) {
			return this.declaredAttributes.get(name);
		}

	}

	/**
	 * Adapter used to expose attributes from the parent
	 * {@link TypeMappedAnnotation}.
	 */
	private static class ParentMappedAttributes implements Attributes {

		private final AnnotationTypeMapping mapping;

		private final TypeMappedAnnotation<?> parent;

		ParentMappedAttributes(AnnotationTypeMapping mapping,
				TypeMappedAnnotation<?> parent) {
			this.mapping = mapping;
			this.parent = parent;
		}

		@Override
		public Object get(String name, boolean nonMerged) {
			if (nonMerged) {
				return getNonMerged(name);
			}
			return getMerged(name);
		}

		private Object getNonMerged(String name) {
			return this.mapping.getAnnotationAttributes().get(name);
		}

		private Object getMerged(String name) {
			AnnotationType annotationType = this.mapping.getAnnotationType();
			AttributeType type = annotationType.getAttributeTypes().get(name);
			Object result = null;
			Reference alias = this.mapping.getAliases().get(name);
			if (alias != null) {
				TypeMappedAnnotation<?> aliasAnnotation = findParentWithMapping(
						alias.getMapping());
				if (aliasAnnotation != null) {
					result = aliasAnnotation.getAttributeValue(
							alias.getAttribute().getAttributeName());
				}
			}
			if (result == null && !isConventionRestricted(name)) {
				result = this.parent.getAttributeValue(name);
			}
			if (result == null) {
				result = this.mapping.getAnnotationAttributes().get(name);
			}
			if (result == null) {
				result = type != null ? type.getDefaultValue() : null;
			}
			if (result != null && isArrayAttributeType(type)
					&& !ObjectUtils.isArray(result)) {
				result = wrapInArray(result);
			}
			return result;
		}

		private TypeMappedAnnotation<?> findParentWithMapping(
				AnnotationTypeMapping mapping) {
			TypeMappedAnnotation<?> candidate = this.parent;
			while (candidate != null) {
				if (candidate.mapping.equals(mapping)) {
					return candidate;
				}
				candidate = candidate.parent;
			}
			return null;
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

	}

	/**
	 * Decorator used to support mirrored attributes.
	 */
	private static class MirroredAttributes implements Attributes {

		private final AnnotationTypeMapping mapping;

		private final Object source;

		private final Attributes attributes;

		private final Map<String, Reference> mirrors;

		public MirroredAttributes(AnnotationTypeMapping mapping, Object source,
				Attributes attributes) {
			this.mapping = mapping;
			this.source = source;
			this.attributes = attributes;
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
				Object value = this.attributes.get(attribute.getAttributeName(), false);
				if (value != null && !isSameAsDefaultValue(value, attribute)) {
					if (result != null) {
						checkMirrorPossibleAttributeResult(candidate, result, value,
								lastValue);
					}
					result = candidate;
					lastValue = value;
				}
			}
			return result;
		}

		private void checkMirrorPossibleAttributeResult(Reference candidate, Reference result,
				Object value, Object lastValue) {
			if (ObjectUtils.nullSafeEquals(value, lastValue)
					|| isShadow(candidate, result, lastValue)) {
				return;
			}
			String on = (this.source != null) ? " declared on " + this.source : "";
			String annotationType = result.getMapping().getAnnotationType().getClassName();
			String lastName = result.getAttribute().getAttributeName();
			throw new AnnotationConfigurationException(String.format(
					"Different @AliasFor mirror values for annotation [%s]%s, "
							+ "attribute '%s' and its alias '%s' are declared with values of [%s] and [%s].",
					annotationType, on, lastName, candidate.getAttribute().getAttributeName(),
					ObjectUtils.nullSafeToString(lastValue),
					ObjectUtils.nullSafeToString(value)));
		}

		private boolean isShadow(Reference candidate, Reference result,
				Object lastValue) {
			if (!this.mapping.getAliases().containsKey(
					candidate.getAttribute().getAttributeName())) {
				return false;
			}
			String name = result.getAttribute().getAttributeName();
			Object attributeValue = this.mapping.getAnnotationAttributes().get(name);
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
		public Object get(String name, boolean nonMerged) {
			Reference mirror = this.mirrors.get(name);
			if (mirror != null) {
				name = mirror.getAttribute().getAttributeName();
			}
			return this.attributes.get(name, nonMerged);
		}

	}

}
