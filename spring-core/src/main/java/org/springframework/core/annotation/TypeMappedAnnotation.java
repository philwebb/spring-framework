/*
 * Copyright 2002-2019 the original author or authors.
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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link MergedAnnotation} implementation that applies
 * {@link AnnotationTypeMapping} rules to a root annotation.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class TypeMappedAnnotation<A extends Annotation> extends AbstractMergedAnnotation<A> {

	private final AnnotationTypeMapping mapping;

	@Nullable
	private final Object source;

	@Nullable
	private final Object rootAnnotation;

	private final AttributeValueExtractor attributeValueExtractor;

	private final int aggregateIndex;

	private final boolean useMergedValues;

	@Nullable
	private final Predicate<String> attributeFilter;

	private final int[] resolvedRootMirrors;

	private final int[] resolvedMirrors;

	private String string;

	TypeMappedAnnotation(AnnotationTypeMapping mapping, @Nullable Object source,
			Annotation annotation, int aggregateIndex) {
		this(mapping, source, annotation, AttributeValueExtractor::fromAnnotation,
				aggregateIndex);
	}

	<T> TypeMappedAnnotation(AnnotationTypeMapping mapping, @Nullable Object source,
			@Nullable Object rootAnnotation, AttributeValueExtractor valueExtractor,
			int aggregateIndex) {
		this(mapping, source, rootAnnotation, valueExtractor, aggregateIndex, null);
	}

	private <T> TypeMappedAnnotation(AnnotationTypeMapping mapping,
			@Nullable Object source, @Nullable Object rootAnnotation,
			AttributeValueExtractor valueExtractor, int aggregateIndex,
			@Nullable int[] resolvedRootMirrors) {
		this.source = source;
		this.rootAnnotation = rootAnnotation;
		this.attributeValueExtractor = valueExtractor;
		this.mapping = mapping;
		this.aggregateIndex = aggregateIndex;
		this.useMergedValues = true;
		this.attributeFilter = null;
		this.resolvedRootMirrors = resolvedRootMirrors != null ? resolvedRootMirrors
				: mapping.getRoot().getMirrorSets().resolve(source, rootAnnotation,
						this.attributeValueExtractor);
		this.resolvedMirrors = getDepth() == 0 ? this.resolvedRootMirrors
				: mapping.getMirrorSets().resolve(source, this,
						this::getValueForMirrorResolution);
	}

	private <T> TypeMappedAnnotation(AnnotationTypeMapping mapping,
			@Nullable Object source, @Nullable Object rootAnnotation,
			AttributeValueExtractor valueExtractor, int aggregateIndex,
			boolean useMergedValues, Predicate<String> attributeFilter,
			int[] resolvedRootMirrors, int[] resolvedMirrors) {
		this.source = source;
		this.rootAnnotation = rootAnnotation;
		this.attributeValueExtractor = valueExtractor;
		this.mapping = mapping;
		this.aggregateIndex = aggregateIndex;
		this.useMergedValues = useMergedValues;
		this.attributeFilter = attributeFilter;
		this.resolvedRootMirrors = resolvedRootMirrors;
		this.resolvedMirrors = resolvedMirrors;
	}

	private Object getValueForMirrorResolution(Object annotation, Method attribute) {
		int attributeIndex = this.mapping.getAttributes().indexOf(attribute);
		boolean valueAttribute = VALUE.equals(attribute.getName());
		return getValue(attributeIndex, !valueAttribute, false);
	}

	@Override
	public String getType() {
		return getAnnotationType().getName();
	}

	@Override
	public boolean isPresent() {
		return true;
	}

	@Override
	public int getDepth() {
		return this.mapping.getDepth();
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
	@Nullable
	public MergedAnnotation<?> getParent() {
		AnnotationTypeMapping parentMapping = this.mapping.getParent();
		if (parentMapping == null) {
			return null;
		}
		return new TypeMappedAnnotation<>(parentMapping, this.source, this.rootAnnotation,
				this.attributeValueExtractor, this.aggregateIndex,
				this.resolvedRootMirrors);
	}

	@Override
	public boolean hasDefaultValue(String attributeName) {
		int attributeIndex = getAttributeIndex(attributeName, true);
		Object value = getValue(attributeIndex, true, true);
		return value == null || AttributeValues.isDefault(
				this.mapping.getAttributes().get(attributeIndex), value,
				this.attributeValueExtractor);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName,
			Class<T> type) throws NoSuchElementException {
		Assert.notNull(attributeName, "AttributeName must not be null");
		Assert.notNull(type, "Type must not be null");
		int attributeIndex = getAttributeIndex(attributeName, true);
		Method attribute = this.mapping.getAttributes().get(attributeIndex);
		Assert.isAssignable(type, attribute.getReturnType(),
				"Attribute " + attributeName + " type mismatch:");
		return (MergedAnnotation<T>) getValue(attributeIndex, Object.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(
			String attributeName, Class<T> type) throws NoSuchElementException {
		Assert.notNull(attributeName, "AttributeName must not be null");
		Assert.notNull(type, "Type must not be null");
		int attributeIndex = getAttributeIndex(attributeName, true);
		Method attribute = this.mapping.getAttributes().get(attributeIndex);
		Class<?> componentType = attribute.getReturnType().getComponentType();
		Assert.notNull(componentType, "Attribute " + attributeName + " is not an array");
		Assert.isAssignable(type, componentType,
				"Attribute " + attributeName + " component type mismatch:");
		return (MergedAnnotation<T>[]) getValue(attributeIndex, Object.class);
	}

	@Override
	public <T> Optional<T> getDefaultValue(String attributeName, Class<T> type) {
		int attributeIndex = getAttributeIndex(attributeName, false);
		if (attributeIndex == -1) {
			return Optional.empty();
		}
		Method attribute = this.mapping.getAttributes().get(attributeIndex);
		return Optional.ofNullable(adapt(attribute, attribute.getDefaultValue(), type));
	}

	@Override
	public MergedAnnotation<A> filterAttributes(Predicate<String> predicate) {
		Assert.notNull(predicate, "Predicate must not be null");
		if (this.attributeFilter != null) {
			predicate = this.attributeFilter.and(predicate);
		}
		return new TypeMappedAnnotation<>(this.mapping, this.source, this.rootAnnotation,
				this.attributeValueExtractor, this.aggregateIndex, this.useMergedValues,
				predicate, this.resolvedRootMirrors, this.resolvedMirrors);
	}

	@Override
	public MergedAnnotation<A> withNonMergedAttributes() {
		return new TypeMappedAnnotation<>(this.mapping, this.source, this.rootAnnotation,
				this.attributeValueExtractor, this.aggregateIndex, false,
				this.attributeFilter, this.resolvedRootMirrors, this.resolvedMirrors);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Map<String, Object>> T asMap(
			Function<MergedAnnotation<?>, T> factory, MapValues... options) {
		T map = (factory != null) ? factory.apply(this)
				: (T) new LinkedHashMap<String, Object>();
		if (map == null) {
			return null;
		}
		AttributeMethods attributes = this.mapping.getAttributes();
		for (int i = 0; i < attributes.size(); i++) {
			Method attribute = attributes.get(i);
			Object value = isFiltered(attribute.getName()) ? null
					: getValue(i, getTypeForMapOptions(attribute, options));
			if (value != null) {
				map.put(attribute.getName(),
						adaptValueForMapOptions(attribute, value, factory, options));
			}
		}
		return (factory != null) ? map : (T) Collections.unmodifiableMap(map);
	}

	private Class<?> getTypeForMapOptions(Method attribute, MapValues[] options) {
		Class<?> attributeType = attribute.getReturnType();
		Class<?> componentType = attributeType.isArray()
				? attributeType.getComponentType()
				: attributeType;
		if (MapValues.CLASS_TO_STRING.isIn(options) && componentType == Class.class) {
			return attributeType.isArray() ? String[].class : String.class;
		}
		return Object.class;
	}

	private <T extends Map<String, Object>> Object adaptValueForMapOptions(
			Method attribute, Object value, Function<MergedAnnotation<?>, T> factory,
			MapValues[] options) {
		if (value instanceof MergedAnnotation) {
			MergedAnnotation<?> annotation = (MergedAnnotation<?>) value;
			return MapValues.ANNOTATION_TO_MAP.isIn(options)
					? annotation.asMap(factory, options)
					: annotation.synthesize();
		}
		if (value instanceof MergedAnnotation[]) {
			MergedAnnotation<?>[] annotations = (MergedAnnotation<?>[]) value;
			if (MapValues.ANNOTATION_TO_MAP.isIn(options)) {
				Class<?> componentType = factory.apply(this).getClass();
				Object result = Array.newInstance(componentType, annotations.length);
				for (int i = 0; i < annotations.length; i++) {
					Array.set(result, i, annotations[i].asMap(factory, options));
				}
				return result;
			}
			Object result = Array.newInstance(
					attribute.getReturnType().getComponentType(), annotations.length);
			for (int i = 0; i < annotations.length; i++) {
				Array.set(result, i, annotations[i].synthesize());
			}
			return result;
		}
		return value;
	}

	@Override
	protected A createSynthesized() {
		return SynthesizedMergedAnnotationInvocationHandler.createProxy(this,
				getAnnotationType());
	}

	@Override
	public String toString() {
		String string = this.string;
		if (string == null) {
			StringBuilder builder = new StringBuilder();
			builder.append("@");
			builder.append(getType());
			builder.append("(");
			for (int i = 0; i < this.mapping.getAttributes().size(); i++) {
				Method attribute = this.mapping.getAttributes().get(i);
				builder.append(i == 0 ? "" : ", ");
				builder.append(attribute.getName());
				builder.append("=");
				builder.append(toString(getValue(i, Object.class)));
			}
			builder.append(")");
			string = builder.toString();
			this.string = string;
		}
		return string;
	}

	private Object toString(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof Class) {
			return ((Class<?>) value).getName();
		}
		if (value.getClass().isArray()) {
			StringBuilder builder = new StringBuilder();
			builder.append("[");
			for (int i = 0; i < Array.getLength(value); i++) {
				builder.append(i == 0 ? "" : ", ");
				builder.append(toString(Array.get(value, i)));
			}
			builder.append("]");
			return builder.toString();
		}
		return String.valueOf(value);
	}

	protected <T> T getValue(String attributeName, Class<T> type, boolean required) {
		return getValue(getAttributeIndex(attributeName, required), type);
	}

	private <T> T getValue(int attributeIndex, Class<T> type) {
		Method attribute = this.mapping.getAttributes().get(attributeIndex);
		Object value = getValue(attributeIndex, true, true);
		if (value == null) {
			value = attribute.getDefaultValue();
		}
		return adapt(attribute, value, type);
	}

	private Object getValue(int attributeIndex, boolean useConventionMapping,
			boolean resolveMirrors) {
		AnnotationTypeMapping mapping = this.mapping;
		if (this.useMergedValues) {
			int mappedIndex = this.mapping.getAliasMapping(attributeIndex);
			if (mappedIndex == -1 && useConventionMapping) {
				mappedIndex = this.mapping.getConventionMapping(attributeIndex);
			}
			if (mappedIndex != -1) {
				mapping = mapping.getRoot();
				attributeIndex = mappedIndex;
			}
		}
		if (resolveMirrors) {
			attributeIndex = (mapping.getDepth() != 0 ? this.resolvedMirrors
					: this.resolvedRootMirrors)[attributeIndex];
		}
		if (attributeIndex == -1) {
			return null;
		}
		Method attribute = mapping.getAttributes().get(attributeIndex);
		if (mapping.getDepth() == 0) {
			return this.attributeValueExtractor.apply(this.rootAnnotation, attribute);
		}
		Annotation metaAnnotation = this.mapping.getAnnotation();
		return AttributeValueExtractor.fromAnnotation(metaAnnotation, attribute);
	}

	@SuppressWarnings("unchecked")
	private <T> T adapt(Method attribute, @Nullable Object value, Class<T> type) {
		if (value == null) {
			return null;
		}
		value = adaptForAttribute(attribute, value);
		if (type == Object.class) {
			type = (Class<T>) getDefaultAdaptType(attribute);
		}
		if (value instanceof String && type == Class.class) {
			// FIXME
		}
		else if (value instanceof String[] && type == Class[].class) {
			// FIXME
		}
		else if (value instanceof Class && type == String.class) {
			value = ((Class<?>) value).getName();
		}
		else if (value instanceof Class[] && type == String[].class) {
			Class<?>[] classes = (Class[]) value;
			String[] names = new String[classes.length];
			for (int i = 0; i < classes.length; i++) {
				names[i] = classes[i].getName();
			}
			value = names;
		}
		else if (value instanceof MergedAnnotation && type.isAnnotation()) {
			MergedAnnotation<?> annotation = (MergedAnnotation<?>) value;
			value = annotation.synthesize();
		}
		else if (value instanceof MergedAnnotation[] && type.isArray()
				&& type.getComponentType().isAnnotation()) {
			MergedAnnotation<?>[] annotations = (MergedAnnotation<?>[]) value;
			Object array = Array.newInstance(type.getComponentType(), annotations.length);
			for (int i = 0; i < annotations.length; i++) {
				Array.set(array, i, annotations[i].synthesize());
			}
			value = array;
		}
		Assert.isInstanceOf(type, value,
				"Unable to adapt to " + type.getName() + " from value of type ");
		return (T) value;
	}

	@SuppressWarnings("unchecked")
	private Object adaptForAttribute(Method attribute, Object value) {
		Class<?> attributeType = ClassUtils.resolvePrimitiveIfNecessary(
				attribute.getReturnType());
		if (attributeType.isArray() && !value.getClass().isArray()) {
			Object array = Array.newInstance(value.getClass(), 1);
			Array.set(array, 0, value);
			return adaptForAttribute(attribute, array);
		}
		if (attributeType.isAnnotation()) {
			return adaptToMergedAnnotation(value,
					(Class<? extends Annotation>) attributeType);
		}
		if (attributeType.isArray() && attributeType.getComponentType().isAnnotation()
				&& value.getClass().isArray()) {
			MergedAnnotation<?>[] result = new MergedAnnotation[Array.getLength(value)];
			for (int i = 0; i < result.length; i++) {
				result[i] = adaptToMergedAnnotation(Array.get(value, i),
						(Class<? extends Annotation>) attributeType.getComponentType());
			}
			return result;
		}
		if ((attributeType == Class.class && value instanceof String)
				|| (attributeType == Class[].class && value instanceof String[])) {
			return value;
		}
		Assert.state(attributeType.isInstance(value),
				"Attribute '" + attribute.getName() + "' in annotation " + getType()
						+ " should be compatible with " + attributeType.getName()
						+ " but a " + value.getClass().getName() + " value was returned");
		return value;
	}

	private MergedAnnotation<?> adaptToMergedAnnotation(Object value,
			Class<? extends Annotation> annotationType) {
		AnnotationFilter filter = AnnotationFilter.mostAppropriateFor(annotationType);
		AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(
				annotationType, filter).get(0);
		return new TypeMappedAnnotation<>(mapping, this.source, value,
				AttributeValueExtractor.forValue(value, this.attributeValueExtractor),
				this.aggregateIndex);
	}

	private Class<?> getDefaultAdaptType(Method attribute) {
		Class<?> attributeType = attribute.getReturnType();
		if (attributeType.isAnnotation()) {
			return MergedAnnotation.class;
		}
		if (attributeType.isArray() && attributeType.getComponentType().isAnnotation()) {
			return MergedAnnotation[].class;
		}
		return ClassUtils.resolvePrimitiveIfNecessary(attributeType);
	}

	private int getAttributeIndex(String attributeName, boolean required) {
		Assert.hasText(attributeName, "AttributeName must not be null");
		int attributeIndex = isFiltered(attributeName) ? -1
				: this.mapping.getAttributes().indexOf(attributeName);
		if (attributeIndex == -1 && required) {
			throw new NoSuchElementException("No attribute named '" + attributeName
					+ "' present in merged annotation " + getType());
		}
		return attributeIndex;
	}

	private boolean isFiltered(String attributeName) {
		if (this.attributeFilter != null) {
			return !this.attributeFilter.test(attributeName);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private Class<A> getAnnotationType() {
		return (Class<A>) this.mapping.getAnnotationType();
	}

	static <A extends Annotation> MergedAnnotation<A> from(@Nullable Object source,
			A annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				annotation.annotationType());
		return new TypeMappedAnnotation<>(mappings.get(0), source, annotation, 0);
	}

	static <A extends Annotation> MergedAnnotation<A> from(@Nullable Object source,
			Class<A> annotationType, @Nullable Map<String, ?> attributes) {
		Assert.notNull(annotationType, "AnnotationType must not be null");
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				annotationType);
		return new TypeMappedAnnotation<A>(mappings.get(0), source, attributes,
				AttributeValueExtractor::fromMap, 0);
	}

}
