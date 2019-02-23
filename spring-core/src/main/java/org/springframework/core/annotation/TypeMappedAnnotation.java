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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 *
 * @author Phillip Webb
 * @since 5.2
 */
public class TypeMappedAnnotation<A extends Annotation>
		extends AbstractMergedAnnotation<A> {

	@Nullable
	private final Object source;

	@Nullable
	private final Object annotation;

	private final BiFunction<Method, Object, Object> valueExtractor;

	private final AnnotationTypeMapping mapping;

	private final int aggregateIndex;

	private final boolean useNonMergedValues;

	@Nullable
	private final Predicate<String> attributeFilter;

	private final int[] resolvedRootMirrors;

	private final int[] resolvedMirrors;

	private String string;

	TypeMappedAnnotation(@Nullable Object source, Annotation annotation,
			AnnotationTypeMapping mapping, int aggregateIndex) {
		this(source, annotation, ReflectionUtils::invokeMethod, mapping, aggregateIndex);
	}

	<T> TypeMappedAnnotation(@Nullable Object source, @Nullable Object annotation,
			BiFunction<Method, Object, Object> valueExtractor,
			AnnotationTypeMapping mapping, int aggregateIndex) {
		this.source = source;
		this.annotation = annotation;
		this.valueExtractor = valueExtractor;
		this.mapping = mapping;
		this.aggregateIndex = aggregateIndex;
		this.useNonMergedValues = false;
		this.attributeFilter = null;
		this.resolvedRootMirrors = mapping.getRoot().getMirrorSets().resolve(source,
				annotation, valueExtractor);
		this.resolvedMirrors = getDepth() == 0 ? this.resolvedRootMirrors
				: mapping.getMirrorSets().resolve(source, this,
						TypeMappedAnnotation::getNonMirroredValue);
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
		// FIXME resolved root mirrors wont change so we could pass them in
		return new TypeMappedAnnotation<>(this.source, this.annotation,
				this.valueExtractor, parentMapping, this.aggregateIndex);
	}

	@Override
	public boolean hasDefaultValue(String attributeName) {
		// FIXME
		return false;
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
		// FIXME
		// return new TypeMappedAnnotation<>(this.source, this.attributes,
		// this.aggregateIndex, this.useNonMergedAttributes, predicate);
		return null;
	}

	@Override
	public MergedAnnotation<A> withNonMergedAttributes() {
		// return new TypeMappedAnnotation<>(this.source, this.attributes,
		// this.aggregateIndex, true, this.attributeFilter);
		// FIXME
		return null;
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
			Object value = getValue(i, getTypeForMapOptions(attribute, options));
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
				builder.append(attribute.getName());
				builder.append("=");
				appendValue(builder, getValue(i, Object.class));
			}
			builder.append(")");
			string = builder.toString();
			this.string = string;
		}
		return string;
	}

	protected <T> T getValue(String attributeName, Class<T> type, boolean required) {
		return getValue(getAttributeIndex(attributeName, required), type);
	}

	@SuppressWarnings("unchecked")
	private Class<A> getAnnotationType() {
		return (Class<A>) this.mapping.getAnnotationType();
	}

	private int getAttributeIndex(String attributeName, boolean required) {
		Assert.hasText(attributeName, "AttributeName must not be null");
		int attributeIndex = this.mapping.getAttributes().indexOf(attributeName);
		if (attributeIndex == -1 && required) {
			throw new NoSuchElementException("No attribute named '" + attributeName
					+ "' present in merged annotation " + getType());
		}
		return attributeIndex;
	}

	private <T> T getValue(int attributeIndex, Class<T> type) {
		Method attribute = this.mapping.getAttributes().get(attributeIndex);
		Object value = getMappedValue(attributeIndex);
		if (value == null) {
			// FIXME ??? Is this correct
			value = attribute.getDefaultValue();
		}
		return adapt(attribute, value, type);
	}

	private Object getMappedValue(int attributeIndex) {
		int mapped = this.useNonMergedValues ? -1
				: this.mapping.getMappedAttribute(attributeIndex);
		if (mapped != -1) {
			int resolved = this.resolvedRootMirrors[mapped];
			Method attribute = this.mapping.getRoot().getAttributes().get(resolved);
			return this.valueExtractor.apply(attribute, this.annotation);
		}
		int resolved = this.resolvedMirrors[attributeIndex];
		Method attribute = this.mapping.getAttributes().get(resolved);
		return getDepth() > 0
				? ReflectionUtils.invokeMethod(attribute, this.mapping.getAnnotation())
				: this.valueExtractor.apply(attribute, this.annotation);
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

		}
		else if (value instanceof String[] && type == Class[].class) {

		}
		else if (value instanceof Class && type == String.class) {

		}
		else if (value instanceof Class[] && type == String[].class) {

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
		Assert.isInstanceOf(type, value, "Unable to return attribute type "
				+ attribute.getReturnType() + " as a " + type);
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
		return new TypeMappedAnnotation<>(this.source, value, getValueExtractorFor(value),
				mapping, this.aggregateIndex);
	}

	private BiFunction<Method, Object, Object> getValueExtractorFor(Object value) {
		if (value instanceof Annotation) {
			return ReflectionUtils::invokeMethod;
		}
		if (value instanceof Map) {
			return TypeMappedAnnotation::getAttributeFromMap;
		}
		return this.valueExtractor;
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

	private static Object getNonMirroredValue(Method attribute,
			TypeMappedAnnotation<?> annotation) {
		AnnotationTypeMapping mapping = annotation.mapping;
		AttributeMethods attributes = mapping.getAttributes();
		int attributeIndex = attributes.indexOf(attribute);
		int mappedIndex = mapping.getMappedAttribute(attributeIndex);
		if (mappedIndex != -1) {
			Method mappedAttribute = mapping.getRoot().getAttributes().get(mappedIndex);
			return annotation.valueExtractor.apply(mappedAttribute,
					annotation.annotation);
		}
		return ReflectionUtils.invokeMethod(attribute, mapping.getAnnotation());
	}

	static <A extends Annotation> MergedAnnotation<A> from(@Nullable Object source,
			A annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				annotation.annotationType());
		return new TypeMappedAnnotation<>(source, annotation, mappings.get(0), 0);
	}

	static <A extends Annotation> MergedAnnotation<A> from(@Nullable Object source,
			Class<A> annotationType, @Nullable Map<String, ?> attributes) {
		Assert.notNull(annotationType, "AnnotationType must not be null");
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				annotationType);
		return new TypeMappedAnnotation<A>(source, attributes,
				TypeMappedAnnotation::getAttributeFromMap, mappings.get(0), 0);
	}

	@SuppressWarnings("unchecked")
	private static Object getAttributeFromMap(Method attribute, Object attributes) {
		Map<String, ?> map = (Map<String, ?>) attributes;
		return map != null ? map.get(attribute.getName()) : null;
	}

}
