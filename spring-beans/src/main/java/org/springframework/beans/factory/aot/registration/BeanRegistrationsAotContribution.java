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

import java.util.Map;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedClassName;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.Assert;

/**
 * AOT contribution from a {@link BeanRegistrationsAotProcessor} used to register bean
 * definitions.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationsAotProcessor
 */
class BeanRegistrationsAotContribution implements BeanFactoryInitializationAotContribution {

	private static final String BEAN_FACTORY_PARAMETER_NAME = "beanFactory";

	private final BeanDefinitionMethodGeneratorFactory beanDefinitionMethodGeneratorFactory;

	private final Map<String, BeanDefinitionMethodGenerator> registrations;

	BeanRegistrationsAotContribution(BeanDefinitionMethodGeneratorFactory beanDefinitionMethodGeneratorFactory,
			Map<String, BeanDefinitionMethodGenerator> registrations) {
		this.registrations = registrations;
		this.beanDefinitionMethodGeneratorFactory = beanDefinitionMethodGeneratorFactory;
	}

	@Override
	public void applyTo(GenerationContext generationContext,
			BeanFactoryInitializationCode beanFactoryInitializationCode) {
		String beanFactoryName = beanFactoryInitializationCode.getBeanFactoryName();
		GeneratedClassName generatedClassName = generationContext.getClassNameGenerator()
				.generateClassName(beanFactoryName, "Registrations");
		BeanRegistrationsCodeGenerator codeGenerator = new BeanRegistrationsCodeGenerator(beanFactoryInitializationCode,
				this.beanDefinitionMethodGeneratorFactory);
		GeneratedMethod registerMethod = codeGenerator.getMethodGenerator().generateMethod("registerBeanDefinitions")
				.using(builder -> generateRegisterMethod(builder, generationContext, codeGenerator));
		beanFactoryInitializationCode.addInitializer(MethodReference.of(generatedClassName, registerMethod.getName()));
	}

	private void generateRegisterMethod(MethodSpec.Builder builder, GenerationContext generationContext,
			BeanRegistrationsCode beanRegistrationsCode) {
		builder.addJavadoc("Register the bean definitions.");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addParameter(DefaultListableBeanFactory.class, BEAN_FACTORY_PARAMETER_NAME);
		CodeBlock.Builder code = CodeBlock.builder();
		this.registrations.forEach((beanName, beanDefinitionMethodGenerator) -> {
			MethodReference beanDefinitionMethod = beanDefinitionMethodGenerator
					.generateBeanDefinitionMethod(generationContext, beanRegistrationsCode);
			code.addStatement("$L.registerBeanDefinition($S, $L)", BEAN_FACTORY_PARAMETER_NAME, beanName,
					beanDefinitionMethod.toCodeBlock());
		});
		builder.addCode(code.build());
	}

	private static class BeanRegistrationsCodeGenerator implements BeanRegistrationsCode {

		private final BeanFactoryInitializationCode beanFactoryInitializationCode;

		private final InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator;

		private final GeneratedMethods generatedMethods = new GeneratedMethods();

		public BeanRegistrationsCodeGenerator(BeanFactoryInitializationCode beanFactoryInitializationCode,
				BeanDefinitionMethodGeneratorFactory methodGeneratorFactory) {
			this.innerBeanDefinitionMethodGenerator = (generationContext, innerRegisteredBean,
					innerBeanPropertyName) -> {
				BeanDefinitionMethodGenerator methodGenerator = methodGeneratorFactory
						.getBeanDefinitionMethodGenerator(innerRegisteredBean, innerBeanPropertyName);
				Assert.state(methodGenerator != null, "Unexpected filtering of inner-bean");
				return methodGenerator.generateBeanDefinitionMethod(generationContext, this);
			};
			this.beanFactoryInitializationCode = beanFactoryInitializationCode;
		}

		@Override
		public String getBeanFactoryName() {
			return beanFactoryInitializationCode.getBeanFactoryName();
		}

		@Override
		public MethodGenerator getMethodGenerator() {
			return this.generatedMethods;
		}

		@Override
		public InnerBeanDefinitionMethodGenerator getInnerBeanDefinitionMethodGenerator() {
			return this.innerBeanDefinitionMethodGenerator;
		}

		JavaFile generatedJavaFile(GeneratedClassName generatedClassName) {
			TypeSpec.Builder classBuilder = generatedClassName.classBuilder();
			classBuilder.addJavadoc("Register bean defintions for the '$L' bean factory.", beanFactoryName);
			classBuilder.addModifiers(Modifier.PUBLIC);
			this.generatedMethods.doWithMethodSpecs(classBuilder::addMethod);
			return generatedClassName.toJavaFile(classBuilder);
		}

	}

}
