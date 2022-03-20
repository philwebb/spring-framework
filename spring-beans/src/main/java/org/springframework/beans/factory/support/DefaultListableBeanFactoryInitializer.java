/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.support;

/**
 * Callback interface for initializing a {@link DefaultListableBeanFactory} prior to it
 * being used to obtain beans.
 * <p>
 * Typically used with AOT optimized applications that require some programmatic
 * initialization of the bean factory, for example, registering bean definitions.
 * <p>
 * {@code DefaultListableBeanFactoryInitializer} processors are encouraged to detect
 * whether Spring's {@link org.springframework.core.Ordered Ordered} interface has been
 * implemented or if the {@link org.springframework.core.annotation.Order @Order}
 * annotation is present and to sort instances accordingly if so prior to invocation.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
@FunctionalInterface
public interface DefaultListableBeanFactoryInitializer {

	/**
	 * Initialize the given default listable bean factory.
	 * @param beanDefinitionRegistry the registry to initialize
	 */
	void initialize(DefaultListableBeanFactory beanFactory);

}
