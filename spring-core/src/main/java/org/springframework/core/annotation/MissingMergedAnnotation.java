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
	public Object getSource() {
		return null;
	}

	@Override
	public MergedAnnotation<?> getParent() {
		return null;
	}

	@Override
	public int getDepth() {
		return -1;
	}

	@Override
	public int getAggregateIndex() {
		return -1;
	}

	@Override
	public boolean hasNonDefaultValue(String attributeName) {
		throw new NoSuchElementException("Unable to check non-default value for missing annotation");
	}

	@Override
	public boolean hasDefaultValue(String attributeName) {
		throw new NoSuchElementException("Unable to check non-default value for missing annotation");
	}

	@Override
	public <T> Optional<T> getValue(String attributeName, Class<T> type) {
		return Optional.empty();
	}

	@Override
	public <T> Optional<T> getDefaultValue(String attributeName, Class<T> type) {
		return Optional.empty();
	}

	@Override
	public MergedAnnotation<A> filterAttributes(Predicate<String> predicate) {
		return this;
	}

	@Override
	public MergedAnnotation<A> withNonMergedAttributes() {
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
	public String toString() {
		return "(missing)";
	}

	@Override
	protected AnnotationType getAnnotationType() {
		throw new NoSuchElementException("Unable to get type for missing annotation");
	}

	@Override
	protected ClassLoader getTypeClassLoader() {
		return null;
	}

	@Override
	protected ClassLoader getValueClassLoader() {
		return null;
	}
	@Override
	protected boolean isFiltered(String attributeName) {
		return false;
	}

	@Override
	protected Object getAttributeValue(String attributeName) {
		throw new NoSuchElementException(
				"Unable to get attribute value for missing annotation");
	}

	@Override
	protected A createSynthesized() {
		throw new NoSuchElementException("Unable to synthesize missing annotation");
	}

	@Override
	protected <T extends Annotation> MergedAnnotation<T> createNested(AnnotationType type,
			DeclaredAttributes attributes) {
		throw new NoSuchElementException(
				"Unable to get nested value for missing annotation");
	}

}
