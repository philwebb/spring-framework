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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link MergedAnnotation} implementations.
 *
 * @author Phillip Webb
 * @since 5.2
 * @param <A> the annotation type
 */
abstract class AbstractMergedAnnotation<A extends Annotation>
		implements MergedAnnotation<A> {

	private volatile A synthesizedAnnotation;


	@Override
	public boolean isDirectlyPresent() {
		return isPresent() && getDepth() == 0;
	}

	@Override
	public boolean isMetaPresent() {
		return isPresent() && getDepth() > 0;
	}

	@Override
	public boolean hasNonDefaultValue(String attributeName) {
		return !hasDefaultValue(attributeName);
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

	private <T> T getRequiredValue(String attributeName, Class<T> type) {
		return getValue(attributeName, type, true);
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
	public MergedAnnotation<A> filterDefaultValues() {
		return filterAttributes(this::hasNonDefaultValue);
	}

	@Override
	public Map<String, Object> asMap(MapValues... options) {
		return asMap(null, options);
	}

	@Override
	public Optional<A> synthesize(
			@Nullable Predicate<? super MergedAnnotation<A>> condition)
			throws NoSuchElementException {

		if (condition == null || condition.test(this)) {
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
	 * Factory method used to create the synthesized annotation.
	 */
	protected abstract A createSynthesized();

	/**
	 * Get the underlying attribute value.
	 * @param attributeName the attribute name
	 * @param type the type to return (see {@link MergedAnnotation} class
	 * documentation for details).
	 * @param required if the value is required or optional
	 * @return the attribute value or {@code null} if the value is not found and
	 * is not required
	 * @throws IllegalArgumentException if the source type is not compatible
	 * @throws NoSuchElementException if the value is required but not found
	 */
	@Nullable
	protected abstract <T> T getValue(String attributeName, Class<T> type,
			boolean required);

}
