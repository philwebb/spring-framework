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
import java.lang.reflect.Method;
import java.util.function.Function;

import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * @param <A>
 * @author pwebb
 * @since 5.2
 * @see MethodIntrospector
 */
public interface MergedAnnotationFinder<A extends Annotation> {

	Object fromMethodsOf(Class<?> source);

	Object fromMethodsOf(Class<?> source, SearchStrategy searchStrategy);

	Object fromMethodsOf(Class<?> source,
			Function<Method, MergedAnnotations> annotationsProvider);

	<T, M> Object fromMethodsOf(T source, Function<T, String> classNameProvider,
			Function<T, M[]> methodsProvider,
			Function<Method, MergedAnnotations> annotationsProvider);

}
