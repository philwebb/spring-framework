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

import java.util.function.Consumer;

import org.springframework.core.ResolvableType;

/*
 * DESIGN NOTES
 *
 * A final immutable class will probably help later with concurrency
 * It also should simplify post processing since an update will result in a replaced
 * rather than mutated registration.
 */

/**
 * A description of a bean may be ultimately instantiated in a
 * {@link XBeanContainer}. A {@code BeanRegistration} is an immutable class that
 * provides all the information that a {@link XBeanContainer} will need in order
 * to create a fully-wired bean instance.
 * <p>
 * {@code BeanRegistrations} may be registered with a {@link FunctionalBeanRegistry} and
 * queried via the {@link XBeanRepository} interface.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @param <T> the type
 * @see XBeanContainer
 * @see FunctionalBeanRegistry
 * @see XBeanRepository
 */
public final class FunctionBeanDefinition<T>  {

	static <T> FunctionBeanDefinition<T> of(Consumer<Builder<T>> registration) {
		return null;
	}
	static interface Builder<T> {

	}




}
