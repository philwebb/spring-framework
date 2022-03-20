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

package org.springframework.beans.factory.support.generate;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.javapoet.CodeBlock;

/**
 * Strategy used to generator the code required to register a {@link BeanDefinition} to
 * {@link DefaultListableBeanFactory} programmatically. Most bean definitions won't need a
 * custom generator and can rely on default code generation.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationCodeGeneratorProvider
 * @see BeanRegistrationsContribution
 * @see DefaultBeanRegistrationMethodCodeGenerator
 */
@FunctionalInterface
public interface BeanRegistrationMethodCodeGenerator {

	/**
	 * The name of the parameter passed to the generated method that contains the
	 * {@link BeanDefinitionRegistry} to use.
	 */
	static final String BEAN_FACTORY_VARIABLE = "beanFactory";

	/**
	 * Return a {@link CodeBlock} containing source code to register a
	 * {@link BeanDefinition} to {@link BeanDefinitionRegistry}. The resulting code will
	 * be included as the body of a generated method and can assume that the
	 * {@link #BEAN_FACTORY_VARIABLE} parameter is available.
	 * @param generationContext the generation context
	 * @param generatedMethods the generated methods
	 * @return the generated code
	 */
	CodeBlock generateBeanRegistrationMethodCode(GenerationContext generationContext,
			GeneratedMethods generatedMethods);

}
