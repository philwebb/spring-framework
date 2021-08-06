/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.util.function;

import java.util.function.Supplier;

/**
 * Supplies fully configured object instances where dependencies can be obtained
 * from a provided context object.
 *
 * @author Phillip Webb
 * @since 6.0
 * @param <C> the context object type
 * @param <T> the supplied instance type
 */
@FunctionalInterface
public interface InstanceSupplier<C, T> {

	/**
	 * Gets the fully configured object instance.
	 * @param context a context object that can be used to obtain dependencies
	 * @return the supplied instance
	 * @throws Throwable if the instance cannot be supplied
	 */
	T get(C context) throws Throwable;

	/**
	 * Returns a composed {@link InstanceSupplier} where the result of this
	 * supplier is provided as the context to the next.
	 * @param <V> the instance supplied by {@code next}
	 * @param next the next supplier to apply
	 * @return the composed supplier instance
	 */
	default <V> InstanceSupplier<C, V> and(
			InstanceSupplier<? super T, ? extends V> next) {
		return context -> next.get(get(context));
	}

	/**
	 * Return a new {@link InstanceSupplier} by adapting a standard
	 * {@link Supplier}.
	 * @param <C> the context object type
	 * @param <T> the supplied instance type
	 * @param supplier the supplier to adapt
	 * @return the adapted instance supplier
	 */
	static <C, T> InstanceSupplier<C, T> of(Supplier<? extends T> supplier) {
		return context -> supplier.get();
	}

	/**
	 * Return a new {@link InstanceSupplier} that extracts the instance by
	 * querying the context for an object of a particular type.
	 * @param <C> the context object type
	 * @param <T> the supplied instance type
	 * @param contextType the context object type
	 * @param extractor the extractor used to obtain the instance
	 * @param type instance type to extract
	 * @return the extracting instance supplier
	 */
	static <C, T> InstanceSupplier<C, T> via(Class<C> contextType, ByType<C, T> extractor,
			Class<T> type) {
		return context -> extractor.apply(context, type);
	}

	/**
	 * Function that can be used to obtain an object from a context by its type.
	 * @param <C> the context object type
	 * @param <T> the supplied instance type
	 */
	@FunctionalInterface
	interface ByType<C, T> {

		/**
		 * Return an object from the context of the given type.
		 * @param context the context
		 * @param type the type of object to obtain
		 * @return the resulting object
		 * @throws Throwable if the result cannot be obtained
		 */
		T apply(C context, Class<T> type) throws Throwable;

	}

}
