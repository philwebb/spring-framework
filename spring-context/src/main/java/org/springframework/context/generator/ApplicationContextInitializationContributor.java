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

package org.springframework.context.generator;

import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.javapoet.CodeBlock;

/**
 * Strategy interface to be implemented by components that participates in the
 * setup of an {@link ApplicationContext}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 * @see ApplicationContextInitializer
 */
@FunctionalInterface
public interface ApplicationContextInitializationContributor {

	/**
	 * Contribute code that participates in the setup of an {@link ApplicationContext}.
	 * <p>Implementation of this interface can assume the following variables
	 * to be accessible:
	 * <ul>
	 * <li>{@code context}: the {@code GenericApplicationContext}</li>
	 * <li>{@code beanFactory}: the {@code DefaultListableBeanFactory}</li>
	 * </ul>
	 * <p>Additional method can be added using the specified
	 * {@link GeneratedTypeContext}.
	 * @param generationContext the {@link GeneratedTypeContext}
	 * @return the code that this instance contributed to the setup of the
	 * application context
	 */
	CodeBlock contribute(GeneratedTypeContext generationContext);

}
