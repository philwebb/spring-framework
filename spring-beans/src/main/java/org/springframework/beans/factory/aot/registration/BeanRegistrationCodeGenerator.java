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

package org.springframework.beans.factory.aot.registration;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.javapoet.CodeBlock;

/**
 * {@link BeanRegistrationCode} with generation support. Instances of this interface can
 * be supplied by a {@link BeanRegistrationCodeGeneratorFactory} if custom code generation
 * is required.
 * <p>
 * Implementations can assume that they will be included in the body of a new method must
 * generate code that returns a fully configured {@link BeanDefinition}. For
 * example:<blockquote><pre class="code">
 * RootBeanDefinition beanDefinition = new RootBeanDefinition(MyBean.class);
 * beanDefinition.setPrimary(true);
 * beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
 * ...
 * return beanDefinition;
 * </pre></blockquote>
 * <p>
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationCodeGeneratorFactory
 */
public interface BeanRegistrationCodeGenerator extends BeanRegistrationCode {

	/**
	 * The recommended variable name to used when creating the bean definition.
	 */
	static final String BEAN_DEFINITION_VARIABLE = "beanDefinition";

	/**
	 * Generate a code block containing the method body to use for bean registration.
	 * @param generationContext the generation context
	 * @return a code block containing the method body
	 */
	CodeBlock generateCode(GenerationContext generationContext);

}
