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
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * Class that can be used to select beans from a {@link FunctionalBeanFactory} or
 * {@link InjectionContext}.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @param <T> the resulting type
 * @see #byName(String)
 * @see #byType(Class)
 * @see #byType(ResolvableType)
 * @see #all()
 */
public final class BeanSelector<T> {

	@Nullable
	private final PrimarySelectorType primarySelectorType;

	@Nullable
	private final Object primarySelector;

	private final boolean onlyUsingPrimarySelector;

	private final Predicate<FunctionalBeanDefinition<?>> predicate;

	@Nullable
	private final Supplier<String> description;

	private final Set<Flag> flags;

	private String descriptionString;

	private BeanSelector(@Nullable PrimarySelectorType primarySelectorType, @Nullable Object primarySelector,
			boolean onlyUsingPrimarySelector, Predicate<FunctionalBeanDefinition<?>> predicate,
			Supplier<String> description, Set<Flag> flags) {
		this.primarySelectorType = primarySelectorType;
		this.primarySelector = primarySelector;
		this.onlyUsingPrimarySelector = onlyUsingPrimarySelector;
		this.predicate = predicate;
		this.description = description;
		this.flags = flags;
	}

	/**
	 * Return the {@link PrimarySelectorType} being used by the selector.
	 * @return the primary selector type or {@code null}
	 */
	@Nullable
	PrimarySelectorType getPrimarySelectorType() {
		return this.primarySelectorType;
	}

	/**
	 * Return the primary selector object being used by the selector.
	 * @return the primary selector
	 * @see #getPrimarySelectorType()
	 */
	@Nullable
	Object getPrimarySelector() {
		return this.primarySelector;
	}

	/**
	 * Return {@code true} if only the primary selector is being used to test beans.
	 * @return if only the primary selector is being used
	 */
	boolean isOnlyUsingPrimarySelector() {
		return this.onlyUsingPrimarySelector;
	}

	/**
	 * Return a new {@link BeanSelector} that further restricts the selection to
	 * {@link FunctionalBeanDefinition bean definitions} with the specified qualifier.
	 * @param qualifier the required qualifier
	 * @return a new {@link BeanSelector} instance with the additional restriction
	 */
	public BeanSelector<T> withQualifier(String qualifier) {
		return withQualifier(Qualifier.of(qualifier));
	}

	/**
	 * Return a new {@link BeanSelector} that further restricts the selection to
	 * {@link FunctionalBeanDefinition bean definitions} with the specified qualifier.
	 * @param qualifier the required qualifier
	 * @return a new {@link BeanSelector} instance with the additional restriction
	 */
	public BeanSelector<T> withQualifier(Qualifier qualifier) {
		return withFilter(() -> String.format("with qualifier of '%s'", qualifier),
				beanDefinition -> beanDefinition.hasQualifier(qualifier));
	}

	/**
	 * Return a new {@link BeanSelector} that further restricts selection to
	 * {@link FunctionalBeanDefinition bean definitions} that are in the singleton scope
	 * (filtering prototype beans or beans with a custom scope).
	 * @return a new {@link BeanSelector} instance with the additional restriction
	 */
	public BeanSelector<T> withSingletonScope() {
		// FIXME implement scope support
		return this;
	}

	/**
	 * Return a new {@link BeanSelector} that further restricts the selection based on the
	 * given anonymous filter. If possible, use the {@link #withFilter(String, Predicate)}
	 * or {@link #withFilter(Supplier, Predicate)} variants instead of this method as they
	 * produce better error messages.
	 * @param filter the filter to apply
	 * @return a new {@link BeanSelector} instance with the additional restriction
	 */
	public BeanSelector<T> withFilter(Predicate<FunctionalBeanDefinition<?>> filter) {
		return withFilter((Supplier<String>) null, filter);
	}

	/**
	 * Return a new {@link BeanSelector} that further restricts the selection based on the
	 * given anonymous filter.
	 * @param description a description of the filter (e.g. "with my restriction")
	 * @param filter the filter to apply
	 * @return a new {@link BeanSelector} instance with the additional restriction
	 */
	public BeanSelector<T> withFilter(String description, Predicate<FunctionalBeanDefinition<?>> filter) {
		return withFilter(() -> description, filter);
	}

	/**
	 * Return a new {@link BeanSelector} that further restricts the selection based on the
	 * given anonymous filter.
	 * @param description a description of the filter (e.g. "with my restriction")
	 * @param filter the filter to apply
	 * @return a new {@link BeanSelector} instance with the additional restriction
	 */
	public BeanSelector<T> withFilter(Supplier<String> description, Predicate<FunctionalBeanDefinition<?>> filter) {
		EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
		flags.addAll(this.flags);
		if (description == null) {
			flags.add(Flag.HAS_UNDESCRIBED_PREDICATE);
		}
		else {
			flags.remove(Flag.MERGE_DESCRIPTION_WITHOUT_AND);
		}
		return new BeanSelector<>(this.primarySelectorType, this.primarySelector, false, this.predicate.and(filter),
				merge(this.flags, this.description, description), flags);
	}

	/**
	 * Test the given {@link FunctionalBeanDefinition} to see if it should be selected.
	 * @param beanDefinition the definition to test
	 * @return {@code true} if the definition should be selected
	 */
	public boolean test(FunctionalBeanDefinition<?> beanDefinition) {
		return this.predicate.test(beanDefinition);
	}

	@Override
	public String toString() {
		String string = this.descriptionString;
		if (string == null) {
			Supplier<String> description = this.description;
			if (this.flags.contains(Flag.HAS_UNDESCRIBED_PREDICATE)) {
				description = merge(this.flags, description, () -> "matching custom filter");
			}
			string = description.get();
			this.descriptionString = string;
		}
		return string;
	}

	private static Supplier<String> merge(Set<Flag> flags, Supplier<String> description, Supplier<String> additional) {
		if (additional == null) {
			return description;
		}
		return () -> description.get() + ((flags.contains(Flag.MERGE_DESCRIPTION_WITHOUT_AND)) ? " " : " and ")
				+ additional.get();
	}

	/**
	 * Return a {@link BeanSelection} that matches all beans. Note that this selector
	 * cannot be optimized by the {@link FunctionalBeanFactory} and will result in a full
	 * scan of every bean in the context. If possible, use one of the {@code by...}
	 * methods instead.
	 * @param <T> the bean type
	 * @return a bean selector for all beans
	 */
	public static <T> BeanSelector<T> all() {
		return new BeanSelector<>(null, null, false, descriptor -> true, () -> "All beans",
				EnumSet.of(Flag.MERGE_DESCRIPTION_WITHOUT_AND));
	}

	/**
	 * Return a {@link BeanSelection} that matches beans with the given name.
	 * @param <T> the bean type
	 * @param name the name to match
	 * @return a bean selector for the given name
	 */
	public static <T> BeanSelector<T> byName(String name) {
		return new BeanSelector<T>(PrimarySelectorType.NAME, name, true, descriptor -> descriptor.hasName(name),
				() -> String.format("Bean named '%s'", name), Collections.emptySet());
	}

	/**
	 * Return a {@link BeanSelection} that matches beans of the given type.
	 * @param <T> the bean type
	 * @param type the type to match
	 * @return a bean selector for the given type
	 */
	public static <T> BeanSelector<T> byType(Class<? extends T> type) {
		return new BeanSelector<T>(PrimarySelectorType.TYPE, type, true, descriptor -> descriptor.hasType(type),
				() -> String.format("Beans matching type '%s'", type.getName()), Collections.emptySet());
	}

	/**
	 * Return a {@link BeanSelection} that matches beans of the given type.
	 * @param <T> the bean type
	 * @param type the type to match
	 * @param includeNonSingletons whether to include prototype or scoped beans too or
	 * just singletons
	 * @return a bean selector for the given type
	 * @see #withSingletonScope()
	 */
	public static <T> BeanSelector<T> byType(Class<T> type, boolean includeNonSingletons) {
		return includeNonSingletons(byType(type), includeNonSingletons);
	}

	/**
	 * Return a {@link BeanSelection} that matches beans of the given type.
	 * @param <T> the bean type
	 * @param type the type to match
	 * @return a bean selector for the given type
	 */
	public static <T> BeanSelector<T> byType(ResolvableType type) {
		return new BeanSelector<T>(PrimarySelectorType.RESOLVABLE_TYPE, type, true,
				descriptor -> descriptor.hasType(type), () -> String.format("Beans matching type '%s'", type),
				Collections.emptySet());
	}

	/**
	 * Return a {@link BeanSelection} that matches beans of the given type.
	 * @param <T> the bean type
	 * @param type the type to match
	 * @param includeNonSingletons whether to include prototype or scoped beans too or
	 * just singletons
	 * @return a bean selector for the given type
	 * @see #withSingletonScope()
	 */
	public static <T> BeanSelector<T> byType(ResolvableType type, boolean includeNonSingletons) {
		return includeNonSingletons(byType(type), includeNonSingletons);
	}

	private static <T> BeanSelector<T> includeNonSingletons(BeanSelector<T> selector, boolean includeNonSingletons) {
		return (!includeNonSingletons) ? selector.withSingletonScope() : selector;
	}

	/**
	 * Return a {@link BeanSelection} that matches beans annotated or meta-annotated with
	 * the given type.
	 * @param annotationType the annotation type to match
	 * @return a bean selector for the given annotation type
	 */
	public static <T> BeanSelector<T> byAnnotation(Class<? extends Annotation> annotationType) {
		return new BeanSelector<T>(PrimarySelectorType.ANNOTATION_TYPE, annotationType, true,
				descriptor -> descriptor.hasAnnotation(annotationType),
				() -> String.format("Beans annotated with '%s'", annotationType.getName()), Collections.emptySet());
	}

	/**
	 * Primary selector types.
	 */
	enum PrimarySelectorType {

		/**
		 * Select by name using a {@link String} primary selector.
		 */
		NAME,

		/**
		 * Select by type using a {@link Class} primary selector.
		 */
		TYPE,

		/**
		 * Select by type using a {@link ResolvableType} primary selector.
		 */
		RESOLVABLE_TYPE,

		/**
		 * Select by annotation type using a {@link Class} primary selector.
		 */
		ANNOTATION_TYPE

	}

	/**
	 * Flags for the selector.
	 */
	enum Flag {

		/**
		 * The selector includes an predicate without a description.
		 */
		HAS_UNDESCRIBED_PREDICATE,

		/**
		 * The merged description does not need to use 'and'.
		 */
		MERGE_DESCRIPTION_WITHOUT_AND

	}

}
