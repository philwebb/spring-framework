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
import java.util.function.Supplier;

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * Class that can be used to select beans from a {@link FunctionalBeanFactory}
 * or {@link InjectionContext}.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @param <T> the resulting type
 */
public final class BeanSelector<T> {

	@Nullable
	private final PrimarySelectorType primarySelectorType;

	@Nullable
	private final Object primarySelector;

	@Nullable
	private final Supplier<String> description;

	private final Predicate<FunctionBeanDefinition<?>> predicate;

	private String descriptionString;

	private BeanSelector(PrimarySelectorType primarySelectorType, Object primarySelector,
			Supplier<String> description,
			Predicate<FunctionBeanDefinition<?>> predicate) {
		this.primarySelectorType = primarySelectorType;
		this.primarySelector = primarySelector;
		this.description = description;
		this.predicate = predicate;
	}

	public BeanSelector<T> withQualifier(String qualifier) {
		return withFilter(() -> String.format("with qualifier of '%s'", qualifier),
				beanDefinition -> beanDefinition.hasQualifier(qualifier));
	}

	public BeanSelector<T> withFilter(Predicate<FunctionBeanDefinition<?>> filter) {
		return withFilter((Supplier<String>) null, filter);
	}

	public BeanSelector<T> withFilter(String description,
			Predicate<FunctionBeanDefinition<?>> filter) {
		return withFilter(() -> description, filter);
	}

	public BeanSelector<T> withFilter(Supplier<String> description,
			Predicate<FunctionBeanDefinition<?>> filter) {
		return null;
	}

	public boolean test(FunctionBeanDefinition<?> beanDefinition) {
		return true;
	}

	@Override
	public String toString() {
		String string = this.descriptionString;
		if (string == null) {
			string = (this.description != null) ? this.description.get() : null;
			this.descriptionString = (string != null) ? string
					: "Predicate based selector";
		}
		return string;
	}

	public static <T> BeanSelector<T> all() {
		return new BeanSelector<>(null, null, () -> "All beans", (descriptor) -> true);
	}

	/**
	 * Return a {@link BeanSelection} that matches beans with the given name.
	 * @param <T> the bean type
	 * @param name the name to match
	 * @return a bean selector for the given name
	 */
	public static <T> BeanSelector<T> byName(String name) {
		return new BeanSelector<T>(PrimarySelectorType.TYPE, name,
				() -> String.format("Bean with name '%s'", name),
				(descriptor) -> descriptor.hasName(name));
	}

	/**
	 * Return a {@link BeanSelection} that matches beans of the given type.
	 * @param <T> the bean type
	 * @param type the type to match
	 * @return a bean selector for the given type
	 */
	public static <T> BeanSelector<T> byType(Class<? extends T> type) {
		return new BeanSelector<T>(PrimarySelectorType.TYPE, type,
				() -> String.format("Beans matching type '%s'", type.getName()),
				(descriptor) -> descriptor.hasType(type));
	}

	/**
	 * Return a {@link BeanSelection} that matches beans of the given type.
	 * @param <T> the bean type
	 * @param type the type to match
	 * @return a bean selector for the given type
	 */
	public static <T> BeanSelector<T> byType(ResolvableType type) {
		return new BeanSelector<T>(PrimarySelectorType.TYPE, type,
				() -> String.format("Beans matching type '%s'", type),
				(descriptor) -> descriptor.hasType(type));
	}

	/**
	 * @param annotationType
	 * @return
	 */
	public static <T> BeanSelector<T> byAnnotation(
			Class<? extends Annotation> annotationType) {
		return new BeanSelector<T>(PrimarySelectorType.ANNOTATION_TYPE, annotationType,
				() -> String.format("Beans annotated with '%s'",
						annotationType.getName()),
				(descriptor) -> descriptor.hasAnnotation(annotationType));
	}

	enum PrimarySelectorType {
		TYPE, RESOLVABLE_TYPE, ANNOTATION_TYPE
	}

}
