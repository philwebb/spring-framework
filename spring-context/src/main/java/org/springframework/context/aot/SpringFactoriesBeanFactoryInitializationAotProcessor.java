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

package org.springframework.context.aot;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.io.support.StaticSpringFactoriesGenerator;
import org.springframework.javapoet.CodeBlock;

/**
 * AOT {@code BeanFactoryPostProcessor} that processes {@code spring.factories}
 * and generates a programmatic version with the {@link StaticSpringFactoriesGenerator}.
 * <p>
 * This processor is registered by default in the {@link ApplicationContextAotGenerator},
 * as the first in the list since other initialization code might rely on {@code spring.factories}
 * entries.
 *
 * @author Brian Clozel
 * @see ApplicationContextAotGenerator
 */
class SpringFactoriesBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		return new SpringFactoriesContribution(beanFactory.getBeanClassLoader());
	}

	static class SpringFactoriesContribution implements BeanFactoryInitializationAotContribution {

		private final StaticSpringFactoriesGenerator generator;

		SpringFactoriesContribution(ClassLoader classLoader) {
			this.generator = new StaticSpringFactoriesGenerator(classLoader);
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
			CodeBlock invokeSpringFactories = this.generator.generateStaticSpringFactories(generationContext)
					.toInvokeCodeBlock(CodeBlock.of("$L.getBeanClassLoader()", "beanFactory"));
			GeneratedMethod initSpringFactories = beanFactoryInitializationCode.getMethodGenerator()
					.generateMethod("init", "springFactories")
					.using(builder -> builder.addParameter(ConfigurableBeanFactory.class, "beanFactory")
							.addCode(CodeBlock.builder().addStatement(invokeSpringFactories).build()));
			beanFactoryInitializationCode.addInitializer(MethodReference.of(initSpringFactories.getName()));
		}

	}
}
