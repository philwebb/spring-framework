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
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

/**
 * {@link MergedAnnotations} implementation backed by a {@link Collection} of direct
 * {@link MergedAnnotation} instances.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see MergedAnnotations#of(Collection)
 */
class MergedAnnotationsCollection implements MergedAnnotations {

	public MergedAnnotationsCollection(Collection<MergedAnnotation<?>> directAnnotations) {
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Iterator<MergedAnnotation<Annotation>> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends Annotation> boolean isPresent(Class<A> annotationType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isPresent(String annotationType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends Annotation> boolean isDirectlyPresent(Class<A> annotationType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDirectlyPresent(String annotationType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			@Nullable MergedAnnotationSelector<A> selector) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			@Nullable MergedAnnotationSelector<A> selector) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends Annotation> Stream<MergedAnnotation<A>> stream(
			Class<A> annotationType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A extends Annotation> Stream<MergedAnnotation<A>> stream(
			String annotationType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Stream<MergedAnnotation<Annotation>> stream() {
		throw new UnsupportedOperationException();
	}

}
