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
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A {@link MappableAnnotation} used as the implementation of
 * {@link MergedAnnotation#missing()}.
 *
 * @author Phillip Webb
 * @since 5.1
 * @param <A> the annotation type
 */
final class MissingMergedAnnotation<A extends Annotation> implements MergedAnnotation<A> {

	private static final MissingMergedAnnotation<?> INSTANCE = new MissingMergedAnnotation<>();

	private MissingMergedAnnotation() {
	}

	@SuppressWarnings("unchecked")
	static <A extends Annotation> MergedAnnotation<A> getInstance() {
		return (MergedAnnotation<A>) INSTANCE;
	}

	@Override
	public boolean isPresent() {
		return false;
	}

	@Override
	public boolean isDirectlyPresent() {
		return false;
	}

	@Override
	public boolean isMetaPresent() {
		return false;
	}

	@Override
	public boolean isParentOf(MergedAnnotation<?> annotation) {
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
		throw new NoSuchElementException("Unable to get type for missing annotation");
	}

	@Override
	public boolean hasNonDefaultValue(String attributeName) {
		return !hasDefaultValue(attributeName);
	}

	@Override
	public boolean hasDefaultValue(String attributeName) {
		return true;
	}

	@Override
	public byte getByte(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public byte[] getByteArray(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public boolean getBoolean(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public boolean[] getBooleanArray(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public char getChar(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public char[] getCharArray(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public short getShort(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public short[] getShortArray(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public int getInt(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public int[] getIntArray(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public long getLong(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public long[] getLongArray(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public double getDouble(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public double[] getDoubleArray(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public float getFloat(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public float[] getFloatArray(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public String getString(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public String[] getStringArray(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public Class<?> getClass(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public Class<?>[] getClassArray(String attributeName) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public <E extends Enum<E>> E getEnum(String attributeName, Class<E> type)
			throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public <E extends Enum<E>> E[] getEnumArray(String attributeName, Class<E> type)
			throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public <T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName,
			Class<T> type) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(
			String attributeName, Class<T> type) throws NoSuchElementException {
		throw new NoAttributeAccessException();
	}

	@Override
	public <T> Optional<T> getAttribute(String attributeName, Class<T> type) {
		return Optional.empty();
	}

	@Override
	public <T> Optional<T> getNonMergedAttribute(String attributeName, Class<T> type) {
		return Optional.empty();
	}

	@Override
	public MergedAnnotation<A> filterDefaultValues() {
		return this;
	}

	@Override
	public MergedAnnotation<A> filterAttributes(Predicate<String> predicate) {
		return this;
	}

	@Override
	public Map<String, Object> asMap(MapValues... options) {
		return Collections.emptyMap();
	}

	@Override
	public <T extends Map<String, Object>> T asMap(
			Function<MergedAnnotation<?>, T> factory, MapValues... options) {
		return factory.apply(this);
	}

	@Override
	public A synthesize() {
		throw new NoSuchElementException("Unable to synthesize missing annotation");
	}

	@Override
	public Optional<A> synthesize(Predicate<MergedAnnotation<A>> conditioan) {
		return Optional.empty();
	}

	@Override
	public String toString() {
		return "(missing)";
	}

	private static class NoAttributeAccessException extends NoSuchElementException {

		NoAttributeAccessException() {
			super("Unable to get attribute value for missing annotation");
		}

	}

}
