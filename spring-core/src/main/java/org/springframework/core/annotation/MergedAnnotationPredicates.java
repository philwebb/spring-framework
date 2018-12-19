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
class MergedAnnotationPredicates {

	static <A extends Annotation> Predicate<MergedAnnotation<A>> onRunOf(Function<? super MergedAnnotation<A>, ?> valueExtractor) {
		return new RunOfPredicate<>(valueExtractor);
	}

	static <A extends Annotation> Predicate<MergedAnnotation<A>> onUnique(Function<MergedAnnotation<A>, Object> keyExtractor) {
		return null;
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


}
