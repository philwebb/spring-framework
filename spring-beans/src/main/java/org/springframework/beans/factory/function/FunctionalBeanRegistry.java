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
import java.util.function.Supplier;

/*
 * DESIGN NOTES
 *
 * Similar to BeanDefinitionRegistry
 */

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
	 * @param registration a consumer used to build the registration
	 */
	default <T> void register(Consumer<FunctionBeanDefinition.Builder<T>> registration) {
		register(FunctionBeanDefinition.of(registration), BeanConditions.NONE);
	}

	/**
	 * Register a new {@link FunctionBeanDefinition} built by the given
	 * consumer.
	 * @param <T> the bean type
	 * @param registration a consumer used to build the registration
	 * @param conditions the conditions that must match for the registration to
	 * be active
	 */
	default <T> void register(Consumer<FunctionBeanDefinition.Builder<T>> registration,
			BeanCondition... conditions) {
		register(FunctionBeanDefinition.of(registration), conditions);
	}

	/**
	 * Register a new {@link FunctionBeanDefinition}.
	 * @param <T> the bean type
	 * @param registration the registration to register
	 */
	default <T> void register(FunctionBeanDefinition<T> registration) {
		register(registration, BeanConditions.NONE);
	}

	/**
	 * Register a new {@link FunctionBeanDefinition}.
	 * @param <T> the bean type
	 * @param registration the registration to register
	 * @param conditions the conditions that must match for the registration to
	 * be active
	 */
	<T> void register(FunctionBeanDefinition<T> registration,
			BeanCondition... conditions);

	/**
	 * Register {@link FunctionBeanDefinition} instances provided by the given
	 * {@link FunctionalBeanRegistrar}.
	 * @param registrar the registrar to apply
	 */
	default void registerFrom(FunctionalBeanRegistrar registrar) {
		registerFrom(() -> registrar, BeanConditions.NONE);
	}

	/**
	 * Register {@link FunctionBeanDefinition} instances provided by the given
	 * {@link FunctionalBeanRegistrar}.
	 * @param registrar the registrar to apply
	 * @param conditions the conditions that must match for the registrar to be
	 * applied
	 */
	default void registerFrom(FunctionalBeanRegistrar registrar,
			BeanCondition... conditions) {
		registerFrom(() -> registrar, conditions);
	}

	/**
	 * Register {@link FunctionBeanDefinition} instances provided by the
	 * supplied {@link FunctionalBeanRegistrar}.
	 * @param registrar the registrar supplier
	 */
	default void registerFrom(Supplier<FunctionalBeanRegistrar> registrar) {
		registerFrom(registrar, BeanConditions.NONE);
	}

	/**
	 * Register {@link FunctionBeanDefinition} instances provided by the
	 * supplied {@link FunctionalBeanRegistrar}. The given supplier will only be
	 * called if all conditions match.
	 * @param registrar the registrar supplier
	 * @param conditions the conditions that must match for the registrar to be
	 * applied
	 */
	void registerFrom(Supplier<FunctionalBeanRegistrar> registrar,
			BeanCondition... conditions);

}
