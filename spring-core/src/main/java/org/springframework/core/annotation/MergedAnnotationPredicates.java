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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.util.ObjectUtils;

/**
 * Predicate implementations exposed via {@link MergedAnnotation} or
 * {@link MergedAnnotations}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public final class MergedAnnotationPredicates {

	private MergedAnnotationPredicates() {
	}

	public static <A extends Annotation> Predicate<MergedAnnotation<A>> runOf(Function<? super MergedAnnotation<A>, ?> valueExtractor) {
		return new RunOfPredicate<>(valueExtractor);
	}

	public static <A extends Annotation, K> Predicate<MergedAnnotation<A>> unique(Function<? super MergedAnnotation<A>, K> keyExtractor) {
		return new UniquePredicate<>(keyExtractor);
	}

	public static <A extends Annotation> Predicate<MergedAnnotation<A>> typeIn(
			String... typeNames) {
		return annotation -> ObjectUtils.containsElement(typeNames, annotation.getType());
	}

	@SafeVarargs
	static <A extends Annotation> Predicate<MergedAnnotation<A>> typeIn(
			Class<? extends Annotation>... types) {
		return annotation -> Arrays.stream(types).anyMatch(
				type -> type.getName().equals(annotation.getType()));
	}

	static <A extends Annotation> Predicate<MergedAnnotation<A>> typeIn(
			Collection<?> types) {
		return annotation -> types.stream().map(
				type -> type instanceof Class ? ((Class<?>) type).getName() : type.toString()).anyMatch(
								typeName -> typeName.equals(annotation.getType()));
	}



	private static class RunOfPredicate<A extends Annotation>
			implements Predicate<MergedAnnotation<A>> {

		private final Function<? super MergedAnnotation<A>, ?> valueExtractor;

		private boolean hasLastValue;

		private Object lastValue;

		public RunOfPredicate(
				Function<? super MergedAnnotation<A>, ?> valueExtractor) {
			this.valueExtractor = valueExtractor;
		}

		@Override
		public boolean test(MergedAnnotation<A> annotation) {
			if(!this.hasLastValue) {
				this.hasLastValue = true;
				this.lastValue = this.valueExtractor.apply(annotation);
			}
			Object value = this.valueExtractor.apply(annotation);
			return ObjectUtils.nullSafeEquals(value, this.lastValue);

		}

	}

	private static class UniquePredicate<A extends Annotation, K> implements Predicate<MergedAnnotation<A>> {

		private final Function<? super MergedAnnotation<A>, K> keyExtractor;

		private final Set<K> seen = new HashSet<>();

		public UniquePredicate(Function<? super MergedAnnotation<A>, K> keyExtractor) {
			this.keyExtractor = keyExtractor;
		}

		@Override
		public boolean test(MergedAnnotation<A> annotation) {
			K key = this.keyExtractor.apply(annotation);
			return this.seen.add(key);
		}

	}


}
