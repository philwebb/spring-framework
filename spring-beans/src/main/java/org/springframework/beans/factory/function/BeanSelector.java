/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.function;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

import org.springframework.core.ResolvableType;

/**
 * Class that can be used to select beans from a {@link FunctionalBeanFactory}
 * or {@link InjectionContext}.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @param <T> the resulting type
 */
public final class BeanSelector<T> {

	public BeanSelector<T> withQualifier(String qualifier) {
		return null;
	}

	public BeanSelector<T> withFilter(Predicate<FunctionBeanDefinition<?>> filter) {
		return null;
	}

	public BeanSelector<T> withFilter(String description,
			Predicate<FunctionBeanDefinition<?>> filter) {
		return null;
	}

	static <T> BeanSelector<T> all() {
		return null;
	}

	/**
	 * Return a {@link BeanSelection} that matches beans of the given type.
	 * @param <T> the bean type
	 * @param type the type to match
	 * @return a bean selector for the given type
	 */
	static <T> BeanSelector<T> byType(Class<? extends T> type) {
		return null;
	}

	static <T> BeanSelector<T> byType(ResolvableType requiredType) {
		return null;
	}

	/**
	 * Return a {@link BeanSelection} that matches beans with the given name or
	 * alias.
	 * @param <T> the bean type
	 * @param name the name to match
	 * @return a bean selector for the given name
	 */
	static <T> BeanSelector<T> byName(String name) {
		return null;
	}

	/**
	 * @param annotationType
	 * @return
	 */
	static <T> BeanSelector<T> byAnnotation(Class<? extends Annotation> annotationType) {
		return null;
	}

}
