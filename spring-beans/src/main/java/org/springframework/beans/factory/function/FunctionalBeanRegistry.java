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

/**
 * Interface that can be used to register {@link FunctionBeanDefinition
 * functional bean definitions}. Typically implemented by
 * {@link FunctionalBeanFactory FunctionalBeanFactories} in order to provide
 * registration capabilities.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @see FunctionalBeanFactory
 */
public interface FunctionalBeanRegistry {

	/**
	 * Register a new {@link FunctionBeanDefinition} built by the given
	 * consumer.
	 * @param <T> the bean type
	 * @param definition a consumer used to build the definition
	 */
	default <T> void register(Consumer<FunctionBeanDefinition.Builder<T>> definition)
			throws FunctionalBeanDefinitionOverrideException {
		register(FunctionBeanDefinition.of(definition));
	}

	/**
	 * Register a new {@link FunctionBeanDefinition}.
	 * @param <T> the bean type
	 * @param definition the definition to register
	 */
	<T> void register(FunctionBeanDefinition<T> definition)
			throws FunctionalBeanDefinitionOverrideException;

}
