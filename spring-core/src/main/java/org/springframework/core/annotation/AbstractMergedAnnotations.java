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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

/**
 * Abstract base class for {@link MergedAnnotations} implementations.
 *
 * @author Phillip Webb
 * @since 5.2
 */
abstract class AbstractMergedAnnotations implements MergedAnnotations {

	@Override
	public <A extends Annotation> boolean isPresent(Class<A> annotationType) {
		return isPresent(getClassName(annotationType));
	}

	@Override
	public <A extends Annotation> boolean isPresent(String annotationType) {
		return get(annotationType).isPresent();
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType) {
		return get(getClassName(annotationType), null);
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate) {
		return get(getClassName(annotationType), predicate);
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
		return get(annotationType, null);
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate) {
		Stream<MergedAnnotation<A>> matches = stream(annotationType);
		if (predicate != null) {
			matches = matches.filter(predicate);
		}
		return matches.findFirst().orElse(MergedAnnotation.missing());
	}

	@Override
	public <A extends Annotation> Set<MergedAnnotation<A>> getAll(
			Class<A> annotationType) {
		return getAll(getClassName(annotationType));
	}

	@Override
	public <A extends Annotation> Set<MergedAnnotation<A>> getAll(String annotationType) {
		Stream<MergedAnnotation<A>> annotations = stream(annotationType);
		Set<MergedAnnotation<A>> set = annotations.collect(
				Collectors.toCollection(LinkedHashSet<MergedAnnotation<A>>::new));
		return Collections.unmodifiableSet(set);
	}

	@Override
	public Iterator<MergedAnnotation<Annotation>> iterator() {
		return stream().iterator();
	}

	@Override
	public <A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> type) {
		return stream(getClassName(type));
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <A extends Annotation> Stream<MergedAnnotation<A>> stream(
			String annotationType) {
		return (Stream) stream().filter(
				annotation -> Objects.equals(annotation.getType(), annotationType));
	}

	@Override
	public Stream<MergedAnnotation<Annotation>> stream() {
		return getAll().stream();
	}

	protected final String getClassName(Class<?> annotationType) {
		return (annotationType != null) ? annotationType.getName() : null;
	}

}
