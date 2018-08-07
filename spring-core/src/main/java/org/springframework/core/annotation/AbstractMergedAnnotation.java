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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AnnotationTypeResolver;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.ClassReference;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.core.annotation.type.EnumValueReference;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Abstract base class for {@link MergedAnnotation} implementations.
 *
 * @author Phillip Webb
 * @since 5.1
 */
abstract class AbstractMergedAnnotation<A extends java.lang.annotation.Annotation>
		implements MergedAnnotation<A> {

	private static final Set<Class<?>> SUPPORTED_TYPES;
	static {
		Set<Class<?>> supportedTypes = new LinkedHashSet<>();
		supportedTypes.add(Byte.class);
		supportedTypes.add(byte[].class);
		supportedTypes.add(Boolean.class);
		supportedTypes.add(boolean[].class);
		supportedTypes.add(Character.class);
		supportedTypes.add(char[].class);
		supportedTypes.add(Short.class);
		supportedTypes.add(short[].class);
		supportedTypes.add(Integer.class);
		supportedTypes.add(int[].class);
		supportedTypes.add(Long.class);
		supportedTypes.add(long[].class);
		supportedTypes.add(Float.class);
		supportedTypes.add(float[].class);
		supportedTypes.add(Double.class);
		supportedTypes.add(double[].class);
		supportedTypes.add(String.class);
		supportedTypes.add(String[].class);
		supportedTypes.add(ClassReference.class);
		supportedTypes.add(ClassReference[].class);
		supportedTypes.add(EnumValueReference.class);
		supportedTypes.add(EnumValueReference[].class);
		supportedTypes.add(DeclaredAttributes.class);
		supportedTypes.add(DeclaredAttributes[].class);
		SUPPORTED_TYPES = Collections.unmodifiableSet(supportedTypes);
	}

	private static final Map<Class<?>, Object> EMPTY_ARRAY;
	static {
		Map<Class<?>, Object> emptyArray = new HashMap<>();
		SUPPORTED_TYPES.stream().filter(Class::isArray).forEach((type) -> emptyArray.put(
				type, Array.newInstance(type.getComponentType(), 0)));
		emptyArray.put(Object.class, new Object[0]);
		EMPTY_ARRAY = Collections.unmodifiableMap(emptyArray);
	}

	private final AnnotationTypeResolver resolver;

	private final AnnotationType annotationType;

	private final Predicate<String> attributeFilter;

	protected AbstractMergedAnnotation(AnnotationTypeResolver resolver,
			AnnotationType annotationType, Predicate<String> attributeFilter) {
		this.resolver = resolver;
		this.annotationType = annotationType;
		this.attributeFilter = attributeFilter;
	}

	protected final AnnotationTypeResolver getResolver() {
		return this.resolver;
	}

	protected final AnnotationType getAnnotationType() {
		return this.annotationType;
	}

	@Override
	public boolean isPresent() {
		return this.annotationType != null;
	}

	@Override
	public boolean isDirectlyPresent() {
		return isPresent() && getDepth() == 0;
	}

	@Override
	public boolean isMetaPresent() {
		return isPresent() && getDepth() > 0;
	}

	@Override
	public boolean isDescendant(MergedAnnotation<?> annotation) {
		return false;
	}

	@Override
	public boolean isFromInherited() {
		return false;
	}

	@Override
	public int getDepth() {
		return 0;
	}

	@Override
	public String getType() {
		if (!isPresent()) {
			throw new IllegalStateException(
					"Unable to get type for non-present annotation");
		}
		return this.annotationType.getClassName();
	}

	@Override
	public boolean hasNonDefaultValue(String attributeName) {
		return !hasDefaultValue(attributeName);
	}

	@Override
	public boolean hasDefaultValue(String attributeName) {
		Object value = getRequiredAttribute(attributeName, Object.class);
		AttributeType type = getAttributeType(attributeName);
		return ObjectUtils.nullSafeEquals(value, type.getDefaultValue());
	}

	public byte getByte(String attributeName) {
		return getRequiredAttribute(attributeName, Byte.class);
	}

	public byte[] getByteArray(String attributeName) {
		return getRequiredAttribute(attributeName, byte[].class);
	}

	public boolean getBoolean(String attributeName) {
		return getRequiredAttribute(attributeName, Boolean.class);
	}

	public boolean[] getBooleanArray(String attributeName) {
		return getRequiredAttribute(attributeName, boolean[].class);
	}

	public char getChar(String attributeName) {
		return getRequiredAttribute(attributeName, Character.class);
	}

	public char[] getCharArray(String attributeName) {
		return getRequiredAttribute(attributeName, char[].class);
	}

	public short getShort(String attributeName) {
		return getRequiredAttribute(attributeName, Short.class);
	}

	public short[] getShortArray(String attributeName) {
		return getRequiredAttribute(attributeName, short[].class);
	}

	public int getInt(String attributeName) {
		return getRequiredAttribute(attributeName, Integer.class);
	}

	public int[] getIntArray(String attributeName) {
		return getRequiredAttribute(attributeName, int[].class);
	}

	public long getLong(String attributeName) {
		return getRequiredAttribute(attributeName, Long.class);
	}

	public long[] getLongArray(String attributeName) {
		return getRequiredAttribute(attributeName, long[].class);
	}

	public double getDouble(String attributeName) {
		return getRequiredAttribute(attributeName, Double.class);
	}

	public double[] getDoubleArray(String attributeName) {
		return getRequiredAttribute(attributeName, double[].class);
	}

	public float getFloat(String attributeName) {
		return getRequiredAttribute(attributeName, Float.class);
	}

	public float[] getFloatArray(String attributeName) {
		return getRequiredAttribute(attributeName, float[].class);
	}

	public String getString(String attributeName) {
		return getRequiredAttribute(attributeName, String.class);
	}

	public String[] getStringArray(String attributeName) {
		return getRequiredAttribute(attributeName, String[].class);
	}

	public Class<?> getClass(String attributeName) {
		return getRequiredAttribute(attributeName, Class.class);
	}

	public Class<?>[] getClassArray(String attributeName) {
		return getRequiredAttribute(attributeName, Class[].class);
	}

	public <E extends Enum<E>> E getEnum(String attributeName, Class<E> type) {
		return getRequiredAttribute(attributeName, type);
	}

	@SuppressWarnings("unchecked")
	public <E extends Enum<E>> E[] getEnumArray(String attributeName, Class<E> type) {
		Class<?> arrayType = Array.newInstance(type, 0).getClass();
		return (E[]) getRequiredAttribute(attributeName, arrayType);
	}

	@Override
	public <T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName,
			Class<T> type) throws NoSuchElementException {
		return getAnnotation(attributeName);
	}

	protected <T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName) {
		DeclaredAttributes nestedAttributes = getRequiredAttribute(attributeName,
				DeclaredAttributes.class);
		AttributeType attributeType = getAttributeType(attributeName);
		AnnotationType nestedType = this.resolver.resolve(attributeType.getClassName());
		return createNested(nestedType, nestedAttributes);
	}

	@Override
	public <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(
			String attributeName, Class<T> type) throws NoSuchElementException {
		return getAnnotationArray(attributeName);
	}

	@SuppressWarnings("unchecked")
	protected final <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(
			String attributeName) {
		DeclaredAttributes[] nestedAttributes = getRequiredAttribute(attributeName,
				DeclaredAttributes[].class);
		AttributeType attributeType = getAttributeType(attributeName);
		String arrayType = attributeType.getClassName();
		String componentType = arrayType.substring(0, arrayType.length() - 2);
		AnnotationType nestedType = this.resolver.resolve(componentType);
		MergedAnnotation<T>[] result = new MergedAnnotation[nestedAttributes.length];
		for (int i = 0; i < nestedAttributes.length; i++) {
			result[i] = createNested(nestedType, nestedAttributes[i]);
		}
		return result;
	}

	@Override
	public <T> Optional<T> getAttribute(String attributeName, Class<T> type) {
		return Optional.ofNullable(getOptionalAttribute(attributeName, type, true));
	}

	@Override
	public <T> Optional<T> getNonMergedAttribute(String attributeName, Class<T> type) {
		return Optional.ofNullable(getOptionalAttribute(attributeName, type, false));
	}

	@Override
	public MergedAnnotation<A> filterDefaultValues() {
		return filterAttributes(this::hasNonDefaultValue);
	}

	@Override
	public MergedAnnotation<A> filterAttributes(Predicate<String> attributeFilter) {
		attributeFilter = (this.attributeFilter != null)
				? this.attributeFilter.and(attributeFilter)
				: attributeFilter;
		return cloneWithAttributeFilter(attributeFilter);
	}

	@Override
	public Map<String, Object> asMap(MapValues... options) {
		if (!isPresent()) {
			throw new IllegalStateException(
					"Unable to get map for non-present annotation");
		}
		Map<String, Object> map = new LinkedHashMap<>();
		for (AttributeType attributeType : this.annotationType.getAttributeTypes()) {
			Class<?> type = resolveClassName(attributeType.getClassName());
			String name = attributeType.getAttributeName();
			Object value = getOptionalAttribute(name, type, true);
			if (value != null) {
				map.put(name, value);
			}
		}
		return Collections.unmodifiableMap(map);
	}

	@Override
	@SuppressWarnings("unchecked")
	public A synthesize() {
		if (!isPresent()) {
			throw new IllegalStateException(
					"Unable to get type for non-present annotation");
		}
		ClassLoader classLoader = this.resolver.getClassLoader();
		Class<A> annotationType = (Class<A>) ClassUtils.resolveClassName(getType(),
				classLoader);
		InvocationHandler handler = new SynthesizedMergedAnnotationInvocationHandler<>(
				this, annotationType);
		Class<?>[] interfaces = new Class<?>[] { annotationType,
			SynthesizedAnnotation.class };
		return (A) Proxy.newProxyInstance(classLoader, interfaces, handler);
	}

	private <T> T getRequiredAttribute(String attributeName, Class<T> type) {
		T result = getOptionalAttribute(attributeName, type, true);
		if (result == null) {
			throw new NoSuchElementException(
					"No attribute named '" + attributeName + "' present");
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private <T> T getOptionalAttribute(String attributeName, Class<T> type,
			boolean merged) {
		AttributeType attributeType = getAttributeType(attributeName);
		if (attributeType == null) {
			return null;
		}
		Object value = getAttributeValue(attributeName, merged);
		if (value == null) {
			value = attributeType.getDefaultValue();
		}
		return (T) adapt(value, attributeType, type);
	}

	private Object adapt(Object attributeValue, AttributeType attributeType,
			Class<?> requiredType) {
		if (attributeValue instanceof ClassReference[] && requiredType.isArray()) {
			return adaptClassReferenceArray((ClassReference[]) attributeValue,
					requiredType.getComponentType());
		}
		if (attributeValue instanceof ClassReference) {
			return adaptClassReference((ClassReference) attributeValue, requiredType);
		}
		if (attributeValue instanceof EnumValueReference[] && requiredType.isArray()) {
			return adaptEnumValueReferenceArray((EnumValueReference[]) attributeValue,
					requiredType.getComponentType());
		}
		if (attributeValue instanceof EnumValueReference) {
			return adaptEnumValueReference((EnumValueReference) attributeValue,
					requiredType);
		}
		if (attributeValue instanceof DeclaredAttributes[]) {
			return adaptDeclaredAttributesArray((DeclaredAttributes[]) attributeValue,
					attributeType, requiredType.getComponentType());
		}
		if (attributeValue instanceof DeclaredAttributes) {
			return adaptDeclaredAttributes((DeclaredAttributes) attributeValue,
					attributeType, requiredType);
		}
		return extract(attributeValue, requiredType);
	}

	private Object adaptClassReferenceArray(ClassReference[] attributeValue,
			Class<?> componentType) {
		Object result = Array.newInstance(componentType, attributeValue.length);
		for (int i = 0; i < attributeValue.length; i++) {
			Array.set(result, i, adaptClassReference(attributeValue[i], componentType));
		}
		return result;
	}

	private Object adaptClassReference(ClassReference attributeValue,
			Class<?> requiredType) {
		String className = attributeValue.getClassName();
		if (String.class.equals(requiredType)) {
			return className;
		}
		if (Class.class.equals(requiredType)) {
			return resolveClassName(className);
		}
		return extract(attributeValue, requiredType);
	}

	private Object adaptEnumValueReferenceArray(EnumValueReference[] attributeValue,
			Class<?> componentType) {
		Object result = Array.newInstance(componentType, attributeValue.length);
		for (int i = 0; i < attributeValue.length; i++) {
			Array.set(result, i,
					adaptEnumValueReference(attributeValue[i], componentType));
		}
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object adaptEnumValueReference(EnumValueReference attributeValue,
			Class<?> requiredType) {
		if (Enum.class.isAssignableFrom(requiredType)) {
			String className = attributeValue.getEnumType().getClassName();
			Class enumType = resolveClassName(className);
			return Enum.valueOf(enumType, attributeValue.getValue());
		}
		return extract(attributeValue, requiredType);
	}

	private Object adaptDeclaredAttributesArray(DeclaredAttributes[] attributeValue,
			AttributeType attributeType, Class<?> componentType) {
		Object result = Array.newInstance(componentType, attributeValue.length);
		for (int i = 0; i < attributeValue.length; i++) {
			Array.set(result, i, adaptDeclaredAttributes(attributeValue[i], attributeType,
					componentType));
		}
		return result;
	}

	private Object adaptDeclaredAttributes(DeclaredAttributes attributeValue,
			AttributeType attributeType, Class<?> requiredType) {
		if (requiredType.isAnnotation()) {
			MergedAnnotation<?> nested = createNested(this.annotationType,
					attributeValue);
			Object result = nested.synthesize();
			if (requiredType.isInstance(result)) {
				return result;
			}
		}
		return extract(attributeValue, requiredType);
	}

	@SuppressWarnings("unchecked")
	private <T> T extract(Object value, Class<T> type) {
		Assert.notNull(type, "Type must not be null");
		if (type == Object.class) {
			return (T) value;
		}
		Assert.isTrue(SUPPORTED_TYPES.contains(type),
				() -> "Type " + type.getName() + " is not supported");
		if (value == null) {
			return null;
		}
		if (type.isArray() && isEmptyObjectArray(value)) {
			return (T) EMPTY_ARRAY.get(type);
		}
		Assert.state(type.isInstance(value),
				"Value " + value.getClass().getName() + " is not a " + type.getName());
		return (T) value;
	}

	private boolean isEmptyObjectArray(Object value) {
		return Objects.equals(value.getClass(), Object[].class)
				&& ((Object[]) value).length == 0;
	}

	private AttributeType getAttributeType(String attributeName) {
		if (this.annotationType == null) {
			return null;
		}
		if (this.attributeFilter != null && !this.attributeFilter.test(attributeName)) {
			return null;
		}
		return this.annotationType.getAttributeTypes().get(attributeName);
	}

	private Class<?> resolveClassName(String className) {
		Assert.state(this.resolver != null, "No resolver available");
		return ClassUtils.resolveClassName(className, this.resolver.getClassLoader());
	}

	/**
	 * Get a single underlying attribute value.
	 * @param attributeName The attribute name
	 * @param merged {@code true} if the merged value should be returned, or
	 * {@code false} to return the original unmerged value.
	 * @return the attribute value or {@code null}
	 */
	protected abstract Object getAttributeValue(String attributeName, boolean merged);

	/**
	 * Create a copy of the current {@link MergedAnnotation} with a different
	 * filter predicate.
	 * @param predicate the new filter predicate
	 * @return a new {@link MergedAnnotation} instance
	 */
	protected abstract AbstractMergedAnnotation<A> cloneWithAttributeFilter(
			Predicate<String> predicate);

	/**
	 * Create a {@link MergedAnnotation} for the given nested annotation
	 * details.
	 * @param type the nested annotation type
	 * @param attributes the nested annotation attributes
	 * @return a new {@link MergedAnnotation} instance
	 */
	protected abstract <T extends Annotation> MergedAnnotation<T> createNested(
			AnnotationType type, DeclaredAttributes attributes);

}
