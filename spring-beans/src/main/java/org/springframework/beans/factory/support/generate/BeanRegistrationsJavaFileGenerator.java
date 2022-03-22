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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedClassName;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethodName;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodNameGenerator;
import org.springframework.beans.factory.aot.UniqueBeanFactoryName;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactoryInitializer;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;

/**
 * Java file generator to generate a {@link DefaultListableBeanFactoryInitializer} to
 * register bean definitions.
 * <p>
 * Generates code in the following form:<blockquote><pre class="code">
 * public class MyInitializer implements DefaultListableBeanFactoryInitialier {
 *
 * 	initialize(DefaultListableBeanFactory beanFactory) {
 * 		registerMyBean(beanFactory);
 * 	}
 *
 * 	registerMyBean(DefaultListableBeanFactory beanFactory) {
 * 		// ...
 * 	}
 *
 * }
 * </pre></blockquote>
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class BeanRegistrationsJavaFileGenerator {

	private final Map<String, BeanRegistrationMethodCodeGenerator> beanRegistrationMethodCodeGenerators;

	/**
	 * Create a new {@link BeanRegistrationsJavaFileGenerator} instance.
	 * @param beanRegistrationMethodCodeGenerators a map containing {@code beanName} to
	 * {@link BeanRegistrationMethodCodeGenerator} in the order that registrations should
	 * be made.
	 */
	public BeanRegistrationsJavaFileGenerator(
			Map<String, BeanRegistrationMethodCodeGenerator> beanRegistrationMethodCodeGenerators) {
		this.beanRegistrationMethodCodeGenerators = beanRegistrationMethodCodeGenerators;
	}

	/**
	 * Return a {@link JavaFile} containing the generated code.
	 * @param generationContext the generation context
	 * @param beanFactoryName the bean factory name
	 * @param className the class name of the generated file
	 * @return a {@link JavaFile} containing the generated code
	 */
	public JavaFile generateJavaFile(GenerationContext generationContext, UniqueBeanFactoryName beanFactoryName,
			GeneratedClassName className) {
		TypeSpec typeSpec = generateTypeSpecCode(generationContext, beanFactoryName, className);
		return className.javaFileBuilder(typeSpec).build();
	}

	private TypeSpec generateTypeSpecCode(GenerationContext generationContext, UniqueBeanFactoryName beanFactoryName,
			GeneratedClassName className) {
		GeneratedMethods generatedMethods = new GeneratedMethods(new MethodNameGenerator("initialize"));
		TypeSpec.Builder builder = className.classBuilder();
		builder.addModifiers(Modifier.PUBLIC);
		builder.addJavadoc("{@link $T} for bean factory '$L'.",
				DefaultListableBeanFactoryInitializer.class, beanFactoryName);
		builder.addSuperinterface(DefaultListableBeanFactoryInitializer.class);
		Set<GeneratedMethodName> registrationMethodsToCall = addRegistrationMethods(generationContext,
				generatedMethods);
		builder.addMethod(generateInitializeMethod(registrationMethodsToCall));
		generatedMethods.doWithMethodSpecs(builder::addMethod);
		return builder.build();
	}

	private Set<GeneratedMethodName> addRegistrationMethods(GenerationContext generationContext,
			GeneratedMethods generatedMethods) {
		Set<GeneratedMethodName> registrationMethodsToCall = new LinkedHashSet<>();
		this.beanRegistrationMethodCodeGenerators.forEach((beanName, code) -> {
			GeneratedMethod method = generateRegistrationMethod(generationContext, generatedMethods, beanName, code);
			registrationMethodsToCall.add(method.getName());
		});
		return registrationMethodsToCall;
	}

	private GeneratedMethod generateRegistrationMethod(GenerationContext generationContext,
			GeneratedMethods generatedMethods, String beanName, BeanRegistrationMethodCodeGenerator code) {
		return generatedMethods.add("register", beanName).generateBy(builder -> {
			builder.addJavadoc("Register the bean definition for '$L'.", beanName);
			builder.addModifiers(Modifier.PRIVATE);
			builder.addParameter(DefaultListableBeanFactory.class,
					BeanRegistrationMethodCodeGenerator.BEAN_FACTORY_VARIABLE);
			builder.addCode(code.generateBeanRegistrationMethodCode(generationContext, generatedMethods));
		});
	}

	private MethodSpec generateInitializeMethod(Set<GeneratedMethodName> registrationMethodsToCalls) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder("initialize");
		builder.addAnnotation(Override.class);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addParameter(DefaultListableBeanFactory.class,
				BeanRegistrationMethodCodeGenerator.BEAN_FACTORY_VARIABLE);
		for (GeneratedMethodName registrationMethodToCall : registrationMethodsToCalls) {
			builder.addStatement("$L($L)", registrationMethodToCall,
					BeanRegistrationMethodCodeGenerator.BEAN_FACTORY_VARIABLE);
		}
		return builder.build();
	}

}
