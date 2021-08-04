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
import java.util.Set;

import org.springframework.core.ResolvableType;

/*
 * DESIGN NOTES
 *
 * A bit like a predicate but also needs to "leak" things like the class type
 * so that repositories can quickly limit candidates that need to be passed to `test`.
 */

/**
 * Strategy interface used to select beans from a {@link XBeanRepository}.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @param <T> the resulting type
 */
@FunctionalInterface
public interface BeanSelector<T> {

	/**
	 * Test if the given registration for selection.
	 * @param registration the registration to select
	 * @return {@code true} if the registration is selected
	 */
	boolean test(FunctionBeanDefinition<?> registration);

	/**
	 * Return the names of the beans that should be tested for selection.
	 * Returning a value from this method allows {@link XBeanRepository}
	 * optimizations to be applied.
	 * @return the names that should be tested
	 */
	default Set<String> getNames() {
		return null;
	}

	/**
	 * Return the types of the beans that should be tested for selection.
	 * Returning a value from this method allows {@link XBeanRepository}
	 * optimizations to be applied.
	 * @return the types that should be tested
	 */
	default Set<Class<?>> getTypes() {
		return null;
	}

	/**
	 * Returns a composed BeanSelector that represents a short-circuiting
	 * logical {@code AND} of this selector and another.
	 * @param other a selector that will be logically-ANDed with this one
	 * @return the composed selector
	 */
	default BeanSelector<T> and(BeanSelector<? super T> other) {
		return null;
	}

	/**
	 * Returns a composed BeanSelector that represents a short-circuiting
	 * logical {@code OR} of this selector and another.
	 * @param other a selector that will be logically-ORed with this one
	 * @return the composed selector
	 */
	default BeanSelector<T> or(BeanSelector<? super T> other) {
		return null;
	}

	/**
	 * Return a {@link BeanSelection} that matches beans of the given type.
	 * @param <T> the bean type
	 * @param type the type to match
	 * @return a bean selector for the given type
	 */
	static <T> BeanSelector<T> byType(Class<? extends T> type) {
		return new TypeBeanSelector<>(type);
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

	static <T> BeanSelector<T> byType(ResolvableType requiredType) {
		return null;
	}

	/**
	 * @param annotationType
	 * @return
	 */
	static <T> BeanSelector<T> byAnnotation(Class<? extends Annotation> annotationType) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
