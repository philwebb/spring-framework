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

package org.springframework.beans.factory.generator;

import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.javapoet.CodeBlock;

/**
 * Generate code that registers a particular {@link BeanDefinition}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
@FunctionalInterface
public interface BeanRegistrationGenerator {

	/**
	 * Generate the necessary {@code statements} to register a bean in the bean
	 * factory.
	 * @param context the {@link GeneratedTypeContext}
	 * @return the code that register a bean in the bean factory
	 */
	CodeBlock generateBeanRegistration(GeneratedTypeContext context);

}
