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

package org.springframework.beans.factory.support.aot;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;

import org.springframework.aot.context.AotContext;
import org.springframework.aot.context.AotContribution;
import org.springframework.aot.context.AotProcessors.Subset;
import org.springframework.aot.generate.GeneratedClassName;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethodName;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodNameGenerator;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.AotBeanClassProcessor;
import org.springframework.beans.factory.aot.AotBeanDefinitionProcessor;
import org.springframework.beans.factory.aot.DefinedBean;
import org.springframework.beans.factory.aot.UniqueBeanFactoryName;
import org.springframework.beans.factory.aot.UniqueBeanName;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactoryInitializer;
import org.springframework.beans.factory.support.generate.BeanRegistrationMethodCodeGenerator;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;

/**
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class BeanRegistrationsContribution implements AotContribution {

	private final UniqueBeanFactoryName beanFactoryName;

	private final Map<DefinedBean, BeanRegistrationMethodCodeGenerator> registrations;

	BeanRegistrationsContribution(UniqueBeanFactoryName beanFactoryName, ConfigurableListableBeanFactory beanFactory,
			Map<DefinedBean, BeanRegistrationMethodCodeGenerator> registrations) {
		this.beanFactoryName = beanFactoryName;
		this.registrations = registrations;
		// FIXME grab processor beans to apply
	}

	@Override
	public void applyTo(AotContext aotContext) {
		GeneratedClassName className = aotContext.getClassNameGenerator().generateClassName(this.beanFactoryName,
				"BeanDefinitionRegistrations");
		aotContext.getGeneratedFiles().addSourceFile(generateJavaFile(aotContext, className));
		aotContext.getGeneratedSpringFactories().forNamedItem(BeanFactory.class, this.beanFactoryName)
				.add(DefaultListableBeanFactoryInitializer.class, className);
		applyBeanDefinitionProcessors(aotContext);
		applyBeanClassProcessors(aotContext);
	}

	private JavaFile generateJavaFile(AotContext aotContext, GeneratedClassName className) {
		return className.javaFileBuilder(generateType(aotContext, className)).build();
	}

	private TypeSpec generateType(GenerationContext generationContext, GeneratedClassName className) {
		GeneratedMethods methods = new GeneratedMethods(new MethodNameGenerator("initialize"));
		TypeSpec.Builder builder = className.classBuilder();
		builder.addJavadoc("BeanDefinitionRegistryInitializer for $S",
				BeanRegistrationsContribution.this.beanFactoryName);
		builder.addSuperinterface(DefaultListableBeanFactoryInitializer.class);
		Set<GeneratedMethodName> registrationMethodsToCall = addRegistrationMethods(generationContext, methods);
		builder.addMethod(generateInitializeMethod(registrationMethodsToCall));
		methods.doWithMethodSpecs(builder::addMethod);
		return builder.build();
	}

	private Set<GeneratedMethodName> addRegistrationMethods(GenerationContext generationContext,
			GeneratedMethods methods) {
		Set<GeneratedMethodName> registrationMethodsToCall = new LinkedHashSet<>();
		this.registrations.forEach((definedBean, code) -> {
			String beanName = definedBean.getBeanName();
			GeneratedMethod method = methods.add("register", beanName);
			registrationMethodsToCall.add(method.getName());
			method.generateBy((builder) -> {
				builder.addJavadoc("Register the bean definition for $S", beanName);
				builder.addModifiers(Modifier.PRIVATE);
				builder.addParameter(BeanDefinitionRegistry.class, BeanRegistrationMethodCodeGenerator.BEAN_FACTORY_VARIABLE);
				builder.addCode(code.generateRegistrationMethod(generationContext, methods));
			});
		});
		return registrationMethodsToCall;
	}

	private MethodSpec generateInitializeMethod(Set<GeneratedMethodName> registrationMethodsToCalls) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder("initialize");
		builder.addAnnotation(Override.class);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addParameter(BeanDefinitionRegistry.class, BeanRegistrationMethodCodeGenerator.BEAN_FACTORY_VARIABLE);
		for (GeneratedMethodName registrationMethodToCall : registrationMethodsToCalls) {
			builder.addStatement("$N($L)", registrationMethodToCall, BeanRegistrationMethodCodeGenerator.BEAN_FACTORY_VARIABLE);
		}
		return builder.build();
	}

	private void applyBeanDefinitionProcessors(AotContext aotContext) {
		Subset<AotBeanDefinitionProcessor, UniqueBeanName, DefinedBean> processors = aotContext.getProcessors()
				.allOfType(AotBeanDefinitionProcessor.class);
		for (DefinedBean definedBean : this.registrations.keySet()) {
			processors.processAndApplyContributions(definedBean.getUniqueBeanName(), definedBean);
		}
	}

	private void applyBeanClassProcessors(AotContext aotContext) {
		Subset<AotBeanClassProcessor, String, Class<?>> processors = aotContext.getProcessors()
				.allOfType(AotBeanClassProcessor.class);
		for (DefinedBean definedBean : this.registrations.keySet()) {
			Class<?> beanClass = definedBean.getResolvedBeanClass();
			processors.processAndApplyContributions(beanClass.getName(), beanClass);
		}
	}

}
