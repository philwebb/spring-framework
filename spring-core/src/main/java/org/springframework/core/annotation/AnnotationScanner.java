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
import java.lang.reflect.AnnotatedElement;
import java.util.function.BiFunction;

import io.reactivex.functions.BiPredicate;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * @author pwebb
 * @since 5.1
 */
class AnnotationScanner {

	public static <C, R> R search(SearchStrategy searchStrategy, AnnotatedElement element,
			C criteria, Processor<C, R> operation) {
		return search(searchStrategy, element, criteria, null, operation);
	}

	public static <C, R> R search(SearchStrategy searchStrategy, AnnotatedElement element,
			C criteria, BiPredicate<C, Class<?>> classFilter, Processor<C, R> operation) {
		return null;
	}

	@FunctionalInterface
	static interface Processor<C, R> {

		R process(C criteria, int aggregateIndex, Object source, Annotation[] annotations);

		default R postProcess(R runResult) {
			return runResult;
		}

	}

}
