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

package org.springframework.beans.factory.function;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/*
 * DESIGN NOTES
 *
 * Similar to ObjectProvider from Framework or Optional from the JDK.
 * Needs a lot more methods.
 */

/**
 * A selection of zero or more beans obtained from a {@link XBeanRepository}.
 *
 * @author Phillip Webb
 * @since 6.0.0
 */
public interface FunctionalBeanSelection<T> {

	// FIXME do we want to just use ObjectProvider

	/**
	 * Return the bean from a selection that is expected to uniquely match a
	 * single item.
	 * @return the single bean instance
	 */
	T get();

	/**
	 * Return a new {@link FunctionalBeanSelection} with values mapped via the given
	 * function.
	 * @param <U> the mapper result type
	 * @param mapper a mapper used for the instances
	 * @return a mapped bean selection
	 */
	default <U> FunctionalBeanSelection<U> map(Function<? super T, ? extends U> mapper) {
		return () -> mapper.apply(get());
	}

	default  FunctionalBeanSelection<T> ordered() {
		return null;
	}

	default void forEach(Consumer<T> action) {
	}


}
