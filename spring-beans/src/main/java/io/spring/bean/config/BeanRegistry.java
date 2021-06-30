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

package io.spring.bean.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

/*
 * DESIGN NOTES
 *
 * Similar to BeanDefinitionRegistry
 */

/**
 * Interface that can be used to register {@link BeanRegistration} instances.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @see BeanContainer
 */
public interface BeanRegistry {

	/**
	 * Register a new {@link BeanRegistration} built by the given consumer.
	 * @param <T> the bean type
	 * @param registration a consumer used to build the registration
	 */
	default <T> void register(Consumer<BeanRegistration.Builder<T>> registration) {
		register(BeanRegistration.of(registration), BeanConditions.NONE);
	}

	/**
	 * Register a new {@link BeanRegistration} built by the given consumer.
	 * @param <T> the bean type
	 * @param registration a consumer used to build the registration
	 * @param conditions the conditions that must match for the registration to
	 * be active
	 */
	default <T> void register(Consumer<BeanRegistration.Builder<T>> registration,
			BeanCondition... conditions) {
		register(BeanRegistration.of(registration), conditions);
	}

	/**
	 * Register a new {@link BeanRegistration}.
	 * @param <T> the bean type
	 * @param registration the registration to register
	 */
	default <T> void register(BeanRegistration<T> registration) {
		register(registration, BeanConditions.NONE);
	}

	/**
	 * Register a new {@link BeanRegistration}.
	 * @param <T> the bean type
	 * @param registration the registration to register
	 * @param conditions the conditions that must match for the registration to
	 * be active
	 */
	<T> void register(BeanRegistration<T> registration, BeanCondition... conditions);

	/**
	 * Register {@link BeanRegistration} instances provided by the given
	 * {@link BeanRegistrar}.
	 * @param registrar the registrar to apply
	 */
	default void registerFrom(BeanRegistrar registrar) {
		registerFrom(() -> registrar, BeanConditions.NONE);
	}

	/**
	 * Register {@link BeanRegistration} instances provided by the given
	 * {@link BeanRegistrar}.
	 * @param registrar the registrar to apply
	 * @param conditions the conditions that must match for the registrar to be
	 * applied
	 */
	default void registerFrom(BeanRegistrar registrar, BeanCondition... conditions) {
		registerFrom(() -> registrar, conditions);
	}

	/**
	 * Register {@link BeanRegistration} instances provided by the supplied
	 * {@link BeanRegistrar}.
	 * @param registrar the registrar supplier
	 */
	default void registerFrom(Supplier<BeanRegistrar> registrar) {
		registerFrom(registrar, BeanConditions.NONE);
	}

	/**
	 * Register {@link BeanRegistration} instances provided by the supplied
	 * {@link BeanRegistrar}. The given supplier will only be called if all
	 * conditions match.
	 * @param registrar the registrar supplier
	 * @param conditions the conditions that must match for the registrar to be
	 * applied
	 */
	void registerFrom(Supplier<BeanRegistrar> registrar, BeanCondition... conditions);

}
