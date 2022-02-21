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

import java.util.List;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;

/**
 * Process an {@link ApplicationContext} and its {@link BeanFactory} to generate
 * code that represents the state of the bean factory, as well as the necessary
 * hints that can be used at runtime in a constrained environment.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class ApplicationContextAotGenerator {

	private final List<ApplicationContextInitializationContributor> contributors;

	ApplicationContextAotGenerator(List<ApplicationContextInitializationContributor> contributors) {
		this.contributors = contributors;
	}

	/**
	 * Create an instance using the specified {@link ClassLoader}.
	 * @param classLoader the class loader to use
	 */
	public ApplicationContextAotGenerator(ClassLoader classLoader) {
		this(SpringFactoriesLoader.loadFactories(ApplicationContextInitializationContributor.class, classLoader));
	}

	/**
	 * Refresh the specified {@link GenericApplicationContext} and generate the
	 * necessary code to restore the state of its {@link BeanFactory}, using the
	 * specified {@link GeneratedTypeContext}.
	 * @param applicationContext the application context to handle
	 * @param generationContext the generation context to use
	 */
	public void generateApplicationContext(GenericApplicationContext applicationContext,
			GeneratedTypeContext generationContext) {
		applicationContext.refreshForAotProcessing();

		GeneratedType mainGeneratedType = generationContext.getMainGeneratedType();
		mainGeneratedType.customizeType(type -> type.addSuperinterface(ApplicationContextInitializer.class));
		mainGeneratedType.addMethod(initializeMethod(generationContext));
	}

	private MethodSpec.Builder initializeMethod(GeneratedTypeContext generationContext) {
		MethodSpec.Builder method = MethodSpec.methodBuilder("initialize").addModifiers(Modifier.PUBLIC)
				.addParameter(GenericApplicationContext.class, "context").addAnnotation(Override.class);
		CodeBlock.Builder code = CodeBlock.builder();
		code.addStatement("$T beanFactory = context.getDefaultListableBeanFactory()", DefaultListableBeanFactory.class);
		invokeContributors(code, generationContext);
		method.addCode(code.build());
		return method;
	}

	private void invokeContributors(CodeBlock.Builder code, GeneratedTypeContext generationContext) {
		for (ApplicationContextInitializationContributor contributor : this.contributors) {
			CodeBlock contribution = contributor.contribute(generationContext);
			code.add(contribution);
			if (!contribution.isEmpty()) {
				code.add("\n");
			}
		}
	}

}
