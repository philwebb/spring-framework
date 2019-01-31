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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.ClassReference;
import org.springframework.core.annotation.type.DeclaredAttribute;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.core.annotation.type.EnumValueReference;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Abstract base class for {@link MergedAnnotation} implementations.
 *
 * @author Phillip Webb
 * @since 5.2
 * @param <A> the annotation type
 */
abstract class AbstractMergedAnnotation<A extends Annotation>
		implements MergedAnnotation<A> {

	private static final Map<Class<?>, Object> EMPTY_ARRAY;
	static {
		Map<Class<?>, Object> emptyArray = new HashMap<>();
		DeclaredAttributes.SUPPORTED_TYPES.stream().filter(Class::isArray).forEach(
				type -> emptyArray.put(type,
						Array.newInstance(type.getComponentType(), 0)));
		emptyArray.put(Class.class, new Class<?>[0]);
		emptyArray.put(Object.class, new Object[0]);
		EMPTY_ARRAY = Collections.unmodifiableMap(emptyArray);
	}

	private volatile A synthesizedAnnotation;

	@Override
	public String getType() {
		return getAnnotationType().getClassName();
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
	public int getDepth() {
		return getParent() != null ? getParent().getDepth() + 1 : 0;
	}

	@Override
	public boolean hasNonDefaultValue(String attributeName) {
		return !hasDefaultValue(attributeName);
	}

	@Override
	public boolean hasDefaultValue(String attributeName) {
		Object value = getRequiredValue(attributeName, null);
		AttributeType type = getAttributeType(attributeName, true);
		return ObjectUtils.nullSafeEquals(value, type.getDefaultValue());
	}

	public byte getByte(String attributeName) {
		return getRequiredValue(attributeName, Byte.class);
	}

	public byte[] getByteArray(String attributeName) {
		return getRequiredValue(attributeName, byte[].class);
	}

	public boolean getBoolean(String attributeName) {
		return getRequiredValue(attributeName, Boolean.class);
	}

	public boolean[] getBooleanArray(String attributeName) {
		return getRequiredValue(attributeName, boolean[].class);
	}

	public char getChar(String attributeName) {
		return getRequiredValue(attributeName, Character.class);
	}

	public char[] getCharArray(String attributeName) {
		return getRequiredValue(attributeName, char[].class);
	}

	public short getShort(String attributeName) {
		return getRequiredValue(attributeName, Short.class);
	}

	public short[] getShortArray(String attributeName) {
		return getRequiredValue(attributeName, short[].class);
	}

	public int getInt(String attributeName) {
		return getRequiredValue(attributeName, Integer.class);
	}

	public int[] getIntArray(String attributeName) {
		return getRequiredValue(attributeName, int[].class);
	}

	public long getLong(String attributeName) {
		return getRequiredValue(attributeName, Long.class);
	}

	public long[] getLongArray(String attributeName) {
		return getRequiredValue(attributeName, long[].class);
	}

	public double getDouble(String attributeName) {
		return getRequiredValue(attributeName, Double.class);
	}

	public double[] getDoubleArray(String attributeName) {
		return getRequiredValue(attributeName, double[].class);
	}

	public float getFloat(String attributeName) {
		return getRequiredValue(attributeName, Float.class);
	}

	public float[] getFloatArray(String attributeName) {
		return getRequiredValue(attributeName, float[].class);
	}

	public String getString(String attributeName) {
		return getRequiredValue(attributeName, String.class);
	}

	public String[] getStringArray(String attributeName) {
		return getRequiredValue(attributeName, String[].class);
	}

	public Class<?> getClass(String attributeName) {
		return getRequiredValue(attributeName, Class.class);
	}

	public Class<?>[] getClassArray(String attributeName) {
		return getRequiredValue(attributeName, Class[].class);
	}

	public <E extends Enum<E>> E getEnum(String attributeName, Class<E> type) {
		Assert.notNull(type, "Type must not be null");
		return getRequiredValue(attributeName, type);
	}

	@SuppressWarnings("unchecked")
	public <E extends Enum<E>> E[] getEnumArray(String attributeName, Class<E> type) {
		Assert.notNull(type, "Type must not be null");
		Class<?> arrayType = Array.newInstance(type, 0).getClass();
		return (E[]) getRequiredValue(attributeName, arrayType);
	}

	@Override
	public <T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName,
			Class<T> type) throws NoSuchElementException {
		Assert.notNull(attributeName, "AttributeName must not be null");
		Assert.notNull(type, "Type must not be null");
		return getNested(attributeName, type);
	}

	@Override
	public <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(
			String attributeName, Class<T> type) throws NoSuchElementException {
		Assert.notNull(attributeName, "AttributeName must not be null");
		Assert.notNull(type, "Type must not be null");
		return getNestedArray(attributeName, type);
	}

	private <T extends Annotation> MergedAnnotation<T> getNested(String attributeName,
			@Nullable Class<?> expectedType) {
		AttributeType attributeType = getAttributeType(attributeName, true);
		Assert.state(!isArrayType(attributeType),
				() -> "Attribute '" + attributeName + "' is an array type");
		AnnotationType nestedType = resolveAnnotationType(attributeType.getClassName());
		assertType(attributeName, nestedType, expectedType);
		DeclaredAttributes nestedAttributes = getRequiredValue(attributeName,
				DeclaredAttributes.class);
		return createNested(nestedType, nestedAttributes);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private final <T extends Annotation> MergedAnnotation<T>[] getNestedArray(
			String attributeName, @Nullable Class<?> expectedElementType) {
		AttributeType attributeType = getAttributeType(attributeName, true);
		Assert.state(isArrayType(attributeType),
				() -> "Attribute '" + attributeName + "' is not an array type");
		String arrayType = attributeType.getClassName();
		String elementType = arrayType.substring(0, arrayType.length() - 2);
		AnnotationType nestedType = resolveAnnotationType(elementType);
		assertType(attributeName, nestedType, expectedElementType);
		DeclaredAttributes[] nestedAttributes = getRequiredValue(attributeName,
				DeclaredAttributes[].class);
		MergedAnnotation[] result = new MergedAnnotation[nestedAttributes.length];
		for (int i = 0; i < nestedAttributes.length; i++) {
			result[i] = createNested(nestedType, nestedAttributes[i]);
		}
		return result;
	}

	private void assertType(String attributeName, AnnotationType actualType,
			Class<?> expectedType) {
		if (expectedType != null) {
			String expectedName = expectedType.getName();
			String actualName = actualType.getClassName();
			Assert.state(expectedName.equals(actualName),
					() -> "Attribute '" + attributeName + "' is a " + actualName
							+ " and cannot be cast to " + expectedName);
		}
	}

	@Override
	public Optional<Object> getValue(String attributeName) {
		return getValue(attributeName, Object.class);
	}

	@Override
	public <T> Optional<T> getValue(String attributeName, Class<T> type) {
		return Optional.ofNullable(getValue(attributeName, type, false));
	}

	@Override
	public Optional<Object> getDefaultValue(String attributeName) {
		return getDefaultValue(attributeName, Object.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Optional<T> getDefaultValue(String attributeName, Class<T> type) {
		Assert.hasText(attributeName, "AttributeName must not be null");
		AttributeType attributeType = getAttributeType(attributeName, false);
		if (attributeType == null) {
			return Optional.empty();
		}
		T value = (T) adapt(attributeType.getDefaultValue(), attributeType, type);
		return Optional.ofNullable(value);
	}

	@Override
	public MergedAnnotation<A> filterDefaultValues() {
		return filterAttributes(this::hasNonDefaultValue);
	}

	@Override
	public Map<String, Object> asMap(MapValues... options) {
		return asMap(null, options);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Map<String, Object>> T asMap(
			Function<MergedAnnotation<?>, T> factory, MapValues... options) {
		Assert.state(isPresent(), "Unable to get map for missing annotation");
		T map = (factory != null) ? factory.apply(this)
				: (T) new LinkedHashMap<String, Object>();
		if (map == null) {
			return null;
		}
		for (AttributeType attributeType : getAnnotationType().getAttributeTypes()) {
			Class<?> type = resolveTypeClass(attributeType.getClassName());
			type = ClassUtils.resolvePrimitiveIfNecessary(type);
			type = getTypeForMapValueOption(options, type);
			String name = attributeType.getAttributeName();
			Object value = getValue(name, type, false);
			if (value != null) {
				map.put(name, getValueForMapValueOption(value, factory, options));
			}
		}
		return (factory != null) ? map : (T) Collections.unmodifiableMap(map);
	}

	private Class<?> getTypeForMapValueOption(MapValues[] options, Class<?> type) {
		Class<?> componentType = type.isArray() ? type.getComponentType() : type;
		if (MapValues.CLASS_TO_STRING.isIn(options) && componentType == Class.class) {
			return type.isArray() ? String[].class : String.class;
		}
		if (MapValues.ANNOTATION_TO_MAP.isIn(options) && componentType.isAnnotation()) {
			return type.isArray() ? MergedAnnotation[].class : MergedAnnotation.class;
		}
		return type;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T extends Map<String, Object>> Object getValueForMapValueOption(Object value,
			Function<MergedAnnotation<?>, T> factory, MapValues... options) {
		if (MapValues.ANNOTATION_TO_MAP.isIn(options)) {
			if (value instanceof MergedAnnotation[]) {
				MergedAnnotation[] mergedAnnotations = (MergedAnnotation[]) value;
				Class<?> componentType = Map.class;
				if (factory != null) {
					componentType = factory.apply(this).getClass();
				}
				Object maps = Array.newInstance(componentType, mergedAnnotations.length);
				for (int i = 0; i < mergedAnnotations.length; i++) {
					Array.set(maps, i, mergedAnnotations[i].asMap(factory, options));
				}
				return maps;
			}
			if (value instanceof MergedAnnotation) {
				value = ((MergedAnnotation<?>) value).asMap(factory, options);
			}
		}
		return value;
	}

	@Override
	public Optional<A> synthesize(Predicate<? super MergedAnnotation<A>> condition)
			throws NoSuchElementException {
		if (condition.test(this)) {
			return Optional.of(synthesize());
		}
		return Optional.empty();
	}

	@Override
	public A synthesize() {
		if (!isPresent()) {
			throw new NoSuchElementException("Unable to synthesize missing annotation");
		}
		A synthesized = this.synthesizedAnnotation;
		if (synthesized == null) {
			synthesized = createSynthesized();
			this.synthesizedAnnotation = synthesized;
		}
		return synthesized;
	}

	/**
	 * Factory method used to create the synthesized annotation. By default this
	 * method create a proxy class. Subclasses may override to change how the
	 * annotation is synthesized.
	 */
	@SuppressWarnings("unchecked")
	protected A createSynthesized() {
		Class<A> type = (Class<A>) resolveTypeClass(getType());
		return SynthesizedMergedAnnotationInvocationHandler.createProxy(this, type);
	}

	private <T> T getRequiredValue(String attributeName, Class<T> type) {
		return getValue(attributeName, type, true);
	}

	/**
	 * Get an attribute value from the annotation.
	 * @param attributeName the attribute name
	 * @param type the attribute type. If {@code null} then the underlying
	 * declared value is returned. If {@code Object.class} is used then the type
	 * returned will match the actual annotation declaration (i.e.
	 * {@link ClassReference} and {@link EnumValueReference} instances will be
	 * resolved and {@link DeclaredAttributes} will be synthesize to an
	 * annotation)
	 * @param required if a non-null result is required
	 * @return the attribute value
	 */
	@SuppressWarnings("unchecked")
	private <T> T getValue(String attributeName, @Nullable Class<T> type,
			boolean required) {
		Assert.hasText(attributeName, "AttributeName must not be null");
		AttributeType attributeType = getAttributeType(attributeName, required);
		if (attributeType == null) {
			return null;
		}
		Object value = getAttributeValue(attributeName);
		if (value == null) {
			value = attributeType.getDefaultValue();
		}
		if (value == null && required) {
			throw new NoSuchElementException("No value found for attribute named '"
					+ attributeName + "' in merged annotation " + getType());
		}
		return (T) adapt(value, attributeType, type);
	}

	private Object adapt(@Nullable Object attributeValue, AttributeType attributeType,
			@Nullable Class<?> requiredType) {
		if (requiredType == null) {
			return attributeValue;
		}
		if (attributeValue instanceof ClassReference[]) {
			return adaptClassReferenceArray((ClassReference[]) attributeValue,
					attributeType, requiredType.getComponentType());
		}
		if (attributeValue instanceof ClassReference) {
			return adaptClassReference((ClassReference) attributeValue, attributeType,
					requiredType);
		}
		if (attributeValue instanceof EnumValueReference[]) {
			return adaptEnumValueReferenceArray((EnumValueReference[]) attributeValue,
					attributeType, requiredType.getComponentType());
		}
		if (attributeValue instanceof EnumValueReference) {
			return adaptEnumValueReference((EnumValueReference) attributeValue,
					attributeType, requiredType);
		}
		if (attributeValue instanceof DeclaredAttributes[]) {
			return adaptDeclaredAttributesArray((DeclaredAttributes[]) attributeValue,
					attributeType, requiredType.getComponentType());
		}
		if (attributeValue instanceof DeclaredAttributes) {
			return adaptDeclaredAttributes((DeclaredAttributes) attributeValue,
					attributeType, requiredType);
		}
		return extract(attributeValue, attributeType, requiredType);
	}

	private Object adaptClassReferenceArray(ClassReference[] attributeValue,
			AttributeType attributeType, Class<?> componentType) {
		if (componentType == null) {
			componentType = Class.class;
		}
		Object result = Array.newInstance(componentType, attributeValue.length);
		for (int i = 0; i < attributeValue.length; i++) {
			Array.set(result, i,
					adaptClassReference(attributeValue[i], attributeType, componentType));
		}
		return result;
	}

	private Object adaptClassReference(ClassReference attributeValue,
			AttributeType attributeType, Class<?> requiredType) {
		String className = attributeValue.getClassName();
		if (String.class.equals(requiredType)) {
			return className;
		}
		if (Class.class.equals(requiredType) || Object.class.equals(requiredType)) {
			return resolveValueClass(className);
		}
		return extract(attributeValue, attributeType, requiredType);
	}

	private Object adaptEnumValueReferenceArray(EnumValueReference[] attributeValue,
			AttributeType attributeType, Class<?> componentType) {
		if (componentType == null) {
			componentType = resolveTypeClass(getComponentClassName(attributeType));
		}
		Object result = Array.newInstance(componentType, attributeValue.length);
		for (int i = 0; i < attributeValue.length; i++) {
			Array.set(result, i, adaptEnumValueReference(attributeValue[i], attributeType,
					componentType));
		}
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object adaptEnumValueReference(EnumValueReference attributeValue,
			AttributeType attributeType, Class<?> requiredType) {
		if (Enum.class.isAssignableFrom(requiredType)
				|| Object.class.equals(requiredType)) {
			Class enumType = resolveTypeClass(attributeValue.getEnumType());
			return Enum.valueOf(enumType, attributeValue.getValue());
		}
		return extract(attributeValue, attributeType, requiredType);
	}

	private Object adaptDeclaredAttributesArray(DeclaredAttributes[] attributeValue,
			AttributeType attributeType, Class<?> componentType) {
		if (componentType == null) {
			componentType = resolveTypeClass(getComponentClassName(attributeType));
		}
		Object result = Array.newInstance(componentType, attributeValue.length);
		for (int i = 0; i < attributeValue.length; i++) {
			Array.set(result, i, adaptDeclaredAttributes(attributeValue[i], attributeType,
					componentType));
		}
		return result;
	}

	private Object adaptDeclaredAttributes(DeclaredAttributes attributeValue,
			AttributeType attributeType, Class<?> requiredType) {
		if (requiredType.isAnnotation() || MergedAnnotation.class.equals(requiredType)
				|| Object.class.equals(requiredType)) {
			AnnotationType nestedType = resolveAnnotationType(
					getComponentClassName(attributeType));
			MergedAnnotation<?> nestedAnnotation = createNested(nestedType,
					attributeValue);
			return requiredType.isAnnotation() || Object.class.equals(requiredType)
					? nestedAnnotation.synthesize()
					: nestedAnnotation;
		}
		return extract(attributeValue, attributeType, requiredType);
	}

	@SuppressWarnings("unchecked")
	private <T> T extract(Object attributeValue, AttributeType attributeType,
			Class<T> requiredType) {
		Assert.notNull(requiredType, "Type must not be null");
		if (attributeValue == null) {
			return null;
		}
		if (isArrayType(attributeType) && isEmptyObjectArray(attributeValue)) {
			Class<?> componentType = requiredType.isArray()
					? requiredType.getComponentType()
					: resolveTypeClass(getComponentClassName(attributeType));
			attributeValue = EMPTY_ARRAY.containsKey(componentType)
					? EMPTY_ARRAY.get(componentType)
					: Array.newInstance(componentType, 0);

		}
		if (Object.class.equals(requiredType)) {
			return (T) attributeValue;
		}
		assertForExtract(attributeValue, attributeType, requiredType);
		return (T) attributeValue;
	}

	private <T> void assertForExtract(Object attributeValue, AttributeType attributeType,
			Class<T> requiredType) {
		Assert.isTrue(isSupportedForExtract(requiredType),
				() -> "Type " + requiredType.getName() + " is not supported");
		Assert.state(requiredType.isInstance(attributeValue),
				() -> "Attribute '" + attributeType.getAttributeName()
						+ "' in annotation " + getType() + " should be of type "
						+ attributeType.getClassName() + " but a "
						+ attributeValue.getClass().getName() + " value was returned");
	}

	private <T> boolean isSupportedForExtract(Class<T> requiredType) {
		if (requiredType.isArray()) {
			Class<?> componentType = requiredType.getComponentType();
			if (Class.class.equals(componentType)
					|| Enum.class.isAssignableFrom(componentType)
					|| componentType.isAnnotation()
					|| MergedAnnotation.class.isAssignableFrom(componentType)) {
				return true;
			}
		}
		return DeclaredAttributes.SUPPORTED_TYPES.contains(requiredType);
	}

	private boolean isEmptyObjectArray(Object value) {
		return Objects.equals(value.getClass(), Object[].class)
				&& ((Object[]) value).length == 0;
	}

	private boolean isArrayType(AttributeType attributeType) {
		return attributeType.getClassName().endsWith("[]");
	}

	private String getComponentClassName(AttributeType attributeType) {
		return attributeType.getClassName().replace("[]", "");
	}

	private AttributeType getAttributeType(String attributeName, boolean required) {
		AttributeType attributeType = isFiltered(attributeName) ? null
				: getAnnotationType().getAttributeTypes().get(attributeName);
		if (attributeType == null && required) {
			throw new NoSuchElementException("No attribute named '" + attributeName
					+ "' present in merged annotation " + getType());
		}
		return attributeType;
	}

	private Class<?> resolveTypeClass(String className) {
		return ClassUtils.resolveClassName(className, getTypeClassLoader());
	}

	private Class<?> resolveValueClass(String className) {
		return ClassUtils.resolveClassName(className, getValueClassLoader());
	}

	private AnnotationType resolveAnnotationType(String className) {
		return AnnotationType.resolve(className, getTypeClassLoader());
	}

	@Override
	public String toString() {
		StringBuilder attributes = new StringBuilder();
		getAnnotationType().getAttributeTypes().forEach(attributeType -> {
			attributes.append(attributes.length() > 0 ? ", " : "");
			attributes.append(toString(attributeType));
		});
		return "@" + getType() + "(" + attributes + ")";
	}

	private String toString(AttributeType attributeType) {
		String name = attributeType.getAttributeName();
		Object value = getAttributeValue(name);
		if (value instanceof DeclaredAttributes) {
			value = getNested(name, null);
		}
		else if (value instanceof DeclaredAttributes[]) {
			value = getNestedArray(name, null);
		}
		if (value != null && value.getClass().isArray()) {
			StringBuilder content = new StringBuilder();
			content.append("[");
			for (int i = 0; i < Array.getLength(value); i++) {
				content.append(i > 0 ? ", " : "");
				content.append(toStringForValue(Array.get(value, i)));
			}
			content.append("]");
			value = content.toString();
		}
		return (value != null) ? name + "=" + toStringForValue(value) : "";
	}

	private String toStringForValue(Object value) {
		if (value instanceof Class) {
			return ((Class<?>) value).getName();
		}
		return String.valueOf(value);
	}

	/**
	 * Return the classloader that should be used to resolve types defined by
	 * the annotation. This includes any {@link AttributeType attribute types}
	 * as well as the {@link #getAnnotationType() annotation} itself.
	 * <p>
	 * For reflection based implementations, this classloader will be the one
	 * that actually loads the annotation (i.e.
	 * {@code annotation.annotationType().getClassLoader()}.
	 * @return the classloader used to resolve types
	 */
	@Nullable
	protected abstract ClassLoader getTypeClassLoader();

	/**
	 * Return the classloader that should be used to resolve
	 * {@link DeclaredAttribute#getValue() attribute values}. This may be
	 * different to the {@link #getTypeClassLoader() type classloader} since an
	 * annotation types loaded in a parent classloader may still be declared
	 * with {@code Class} or {@code Class[]} references in a child classloader.
	 * <p>
	 * For reflection based implementations, this classloader will be the one
	 * that loads the element that declares the annotation (i.e.
	 * {@code sourceClass.getClassLoader}.
	 * @return the classloader used to resolve attribute type
	 */
	@Nullable
	protected abstract ClassLoader getValueClassLoader();

	/**
	 * Return the actually annotation type for this instance.
	 * @return the annotation type
	 */
	protected abstract AnnotationType getAnnotationType();

	/**
	 * Test if the given attribute name should be filtered.
	 * @param attributeName the attribute name
	 * @return {@code true} if the attribute is filtered out
	 */
	protected abstract boolean isFiltered(String attributeName);

	/**
	 * Return the value of the given attribute.
	 * @param attributeName the attribute name
	 * @return the value or {@code null}
	 */
	protected abstract Object getAttributeValue(String attributeName);

	/**
	 * Return a new nested {@link MergedAnnotation} instance for the given type
	 * and attributes.
	 * @param type the nested annotation type
	 * @param attributes the nested annotation attributes
	 * @return the nested {@link MergedAnnotation}
	 */
	protected abstract <T extends Annotation> MergedAnnotation<T> createNested(
			AnnotationType type, DeclaredAttributes attributes);

}
