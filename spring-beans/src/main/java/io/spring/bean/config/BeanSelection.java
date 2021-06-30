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

package io.spring.bean.config;

import java.util.function.Function;

/*
 * DESIGN NOTES
 *
 * Similar to ObjectProvider from Framework or Optional from the JDK.
 * Needs a lot more methods.
 */

/**
 * A selection of zero or more beans obtained from a {@link BeanRepository}.
 *
 * @author Phillip Webb
 * @since 6.0.0
 */
public interface BeanSelection<T> {

	/**
	 * Return the bean from a selection that is expected to uniquely match a
	 * single item.
	 * @return the single bean instance
	 */
	T get();

	/**
	 * Return a new {@link BeanSelection} with values mapped via the given
	 * function.
	 * @param <U> the mapper result type
	 * @param mapper a mapper used for the instances
	 * @return a mapped bean selection
	 */
	default <U> BeanSelection<U> map(Function<? super T, ? extends U> mapper) {
		return () -> mapper.apply(get());
	}

}
