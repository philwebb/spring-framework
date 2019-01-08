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

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Predicate implementations that provide various test operations for
 * {@link MergedAnnotation MergedAnnotations}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public final class MergedAnnotationPredicates {

	private MergedAnnotationPredicates() {
	}

	/**
	 * Returns a new {@link Predicate} that evaluates {@code true} if the
	 * {@link MergedAnnotation#getType() merged annotation type} is contained in
	 * the specified array.
	 * @param <A> the annotation type
	 * @param typeNames the names that should be matched
	 * @return a {@link Predicate} to test the annotation type
	 */
	public static <A extends Annotation> Predicate<MergedAnnotation<? extends A>> typeIn(
			String... typeNames) {
		Assert.notNull(typeNames, "TypeNames must not be null");
		return annotation -> ObjectUtils.containsElement(typeNames, annotation.getType());
	}

	/**
	 * Returns a new {@link Predicate} that evaluates {@code true} if the
	 * {@link MergedAnnotation#getType() merged annotation type} is contained in
	 * the specified array.
	 * @param <A> the annotation type
	 * @param types the types that should be matched
	 * @return a {@link Predicate} to test the annotation type
	 */
	@SafeVarargs
	public static <A extends Annotation> Predicate<MergedAnnotation<? extends A>> typeIn(
			Class<? extends Annotation>... types) {
		Assert.notNull(types, "Types must not be null");
		return annotation -> Arrays.stream(types).anyMatch(
				type -> type.getName().equals(annotation.getType()));
	}

	/**
	 * Returns a new {@link Predicate} that evaluates {@code true} if the
	 * {@link MergedAnnotation#getType() merged annotation type} is contained in
	 * the collection.
	 * @param <A> the annotation type
	 * @param types the type names or classes that should be matched
	 * @return a {@link Predicate} to test the annotation type
	 */
	public static <A extends Annotation> Predicate<MergedAnnotation<? extends A>> typeIn(
			Collection<?> types) {
		Assert.notNull(types, "Types must not be null");
		return annotation -> types.stream().map(
				type -> type instanceof Class ? ((Class<?>) type).getName()
						: type.toString()).anyMatch(
								typeName -> typeName.equals(annotation.getType()));
	}

	/**
	 * Returns a new stateful, single use {@link Predicate} that matches only
	 * the first run of an extracted value. For example,
	 * {@code MergedAnnotationPredicates.firstRunOf(MergedAnnotation::depth)}
	 * will return the first annotation and a subsequent run of the same depth.
	 * NOTE: this predicate only matches the first first run, once the extracted
	 * value changes the predicate always returns {@code false}.
	 * @param valueExtractor function used to extract the value to check
	 * @return a {@link Predicate} that matches the first run of the extracted
	 * values
	 */
	public static <A extends Annotation> Predicate<MergedAnnotation<A>> firstRunOf(
			Function<? super MergedAnnotation<A>, ?> valueExtractor) {
		Assert.notNull(valueExtractor, "ValueExtractor must not be null");
		return new FirstRunOfPredicate<>(valueExtractor);
	}

	/**
	 * Returns a new stateful, single use {@link Predicate} that matches
	 * annotations that are unique based on extracted key. For example
	 * {@code MergedAnnotationPredicates.unique(MergedAnnotation::type)} will
	 * match the first time a unique type is seen.
	 * @param keyExtractor function used to extract the key used to test for
	 * uniqueness
	 * @return a {@link Predicate} that matches unique annotation based on the
	 * extracted key
	 */
	public static <A extends Annotation, K> Predicate<MergedAnnotation<A>> unique(
			Function<? super MergedAnnotation<A>, K> keyExtractor) {
		Assert.notNull(keyExtractor, "KeyExtractor must not be null");
		return new UniquePredicate<>(keyExtractor);
	}

	/**
	 * {@link Predicate} implementation used for
	 * {@link MergedAnnotationPredicates#firstRunOf(Function)}.
	 */
	private static class FirstRunOfPredicate<A extends Annotation>
			implements Predicate<MergedAnnotation<A>> {

		private final Function<? super MergedAnnotation<A>, ?> valueExtractor;

		private boolean hasLastValue;

		private Object lastValue;

		public FirstRunOfPredicate(
				Function<? super MergedAnnotation<A>, ?> valueExtractor) {
			this.valueExtractor = valueExtractor;
		}

		@Override
		public boolean test(MergedAnnotation<A> annotation) {
			if (!this.hasLastValue) {
				this.hasLastValue = true;
				this.lastValue = this.valueExtractor.apply(annotation);
			}
			Object value = this.valueExtractor.apply(annotation);
			return ObjectUtils.nullSafeEquals(value, this.lastValue);

		}

	}

	/**
	 * {@link Predicate} implementation used for
	 * {@link MergedAnnotationPredicates#unique(Function)}.
	 */
	private static class UniquePredicate<A extends Annotation, K>
			implements Predicate<MergedAnnotation<A>> {

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
