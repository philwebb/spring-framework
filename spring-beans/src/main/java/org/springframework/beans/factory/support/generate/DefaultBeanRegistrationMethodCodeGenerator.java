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
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;

/**
 * Default {@link BeanRegistrationMethodCodeGenerator} providing registration code
 * suitable for most beans.
 * <p>
 * For example:
 * <p>
 * <pre class="code">
 * RootBeanDefinition beanDefinition = RootBeanDefinition.supply(MyBean.class)
 * 		.usingConstructor()
 * 		.resolvedBy(MyBean::new);
 * beanDefinition.setPrimary(true);
 * beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
 * beanFactory.registerBeanDefinition("myBean", beanDefinition);
 * </pre>
 * <p>
 * The generated code expects a {@link DefaultListableBeanFactory} {@code beanFactory}
 * variable to be available.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see SuppliedInstanceBeanDefinitionCodeGenerator
 * @see BeanDefinitionPropertiesCodeGenerator
 */
class DefaultBeanRegistrationMethodCodeGenerator implements BeanRegistrationMethodCodeGenerator {

	static final String BEAN_FACTORY_VARIABLE = SuppliedInstanceBeanDefinitionCodeGenerator.BEAN_FACTORY_VARIABLE;

	private final DefaultListableBeanFactory beanFactory;

	private final String beanName;

	private final BeanDefinition beanDefinition;

	DefaultBeanRegistrationMethodCodeGenerator(DefaultListableBeanFactory beanFactory, String beanName,
			BeanDefinition beanDefinition) {
		this.beanFactory = beanFactory;
		this.beanName = beanName;
		this.beanDefinition = beanDefinition;
	}

	@Override
	public CodeBlock generateBeanRegistrationMethodCode(GenerationContext generationContext,
			GeneratedMethods generatedMethods) {
		Builder builder = CodeBlock.builder();
		CodeBlock suppliedInstanceCode = new SuppliedInstanceBeanDefinitionCodeGenerator(this.beanFactory,
				generatedMethods).generateCode(this.beanDefinition, this.beanName);
		builder.addStatement("$T $L = $L", RootBeanDefinition.class,
				BeanDefinitionPropertiesCodeGenerator.BEAN_DEFINITION_VARIABLE, suppliedInstanceCode);
		builder.add(new BeanDefinitionPropertiesCodeGenerator(this.beanFactory, generatedMethods)
				.generateCode(this.beanDefinition, this.beanName));
		builder.addStatement("$L.registerBeanDefinition($S, $L)", BEAN_FACTORY_VARIABLE, this.beanName,
				BeanDefinitionPropertiesCodeGenerator.BEAN_DEFINITION_VARIABLE);
		return builder.build();
	}

}
