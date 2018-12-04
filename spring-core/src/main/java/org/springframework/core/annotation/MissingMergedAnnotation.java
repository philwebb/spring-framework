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

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.DeclaredAttributes;

/**
 * A {@link MappableAnnotation} used as the implementation of
 * {@link MergedAnnotation#missing()}.
 *
 * @author Phillip Webb
 * @since 5.2
 * @param <A> the annotation type
 */
final class MissingMergedAnnotation<A extends Annotation>
		extends AbstractMergedAnnotation<A> {

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
	public MergedAnnotation<?> getParent() {
		return null;
	}

	@Override
	public boolean isFromInherited() {
		return false;
	}

	@Override
	public boolean hasDefaultValue(String attributeName) {
		return true;
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

	@Override
	protected AnnotationType getAnnotationType() {
		throw new NoSuchElementException("Unable to get type for missing annotation");
	}

	@Override
	protected ClassLoader getClassLoader() {
		return null;
	}

	@Override
	protected AttributeType getAttributeType(String attributeName) {
		throw new NoAttributeAccessException();
	}

	@Override
	protected Object getAttributeValue(String attributeName, boolean nonMerged) {
		throw new NoAttributeAccessException();
	}

	@Override
	protected <T extends Annotation> MergedAnnotation<T> createNested(AnnotationType type,
			DeclaredAttributes attributes) {
		throw new NoAttributeAccessException();
	}

	private static class NoAttributeAccessException extends NoSuchElementException {

		NoAttributeAccessException() {
			super("Unable to get attribute value for missing annotation");
		}

	}

}
