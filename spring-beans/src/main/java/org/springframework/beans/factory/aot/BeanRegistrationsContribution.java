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

package org.springframework.beans.factory.aot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

import org.springframework.aot.context.AotContext;
import org.springframework.aot.context.AotContribution;
import org.springframework.aot.context.AotProcessors.Subset;
import org.springframework.aot.generate.GeneratedClassName;
import org.springframework.aot.generate.MethodNameGenerator;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryInitializer;
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

	private final Map<DefinedBean, BeanRegistrationCode> registrations;

	BeanRegistrationsContribution(UniqueBeanFactoryName beanFactoryName, ConfigurableListableBeanFactory beanFactory,
			Map<DefinedBean, BeanRegistrationCode> registrations) {
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
				.add(BeanDefinitionRegistryInitializer.class, className);
		applyBeanDefinitionProcessors(aotContext);
		applyBeanClassProcessors(aotContext);
	}

	private JavaFile generateJavaFile(AotContext aotContext, GeneratedClassName className) {
		return JavaFile.builder(className.getPackageName(), generateType(aotContext, className)).build();
	}

	private TypeSpec generateType(AotContext aotContext, GeneratedClassName className) {
		TypeSpec.Builder builder = TypeSpec.classBuilder(className.toString());
		builder.addJavadoc("BeanDefinitionRegistryInitializer for $S", this.beanFactoryName);
		builder.addSuperinterface(BeanDefinitionRegistryInitializer.class);
		List<MethodSpec> registrationMethods = generateRegistartionMethods(aotContext);
		builder.addMethod(generateInitializeMethod(registrationMethods));
		registrationMethods.forEach(builder::addMethod);
		return builder.build();
	}

	private MethodSpec generateInitializeMethod(List<MethodSpec> registrationMethods) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder("initialize");
		builder.addAnnotation(Override.class);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addParameter(BeanDefinitionRegistry.class, BeanRegistrationCode.REGISTRY);
		for (MethodSpec registrationMethod : registrationMethods) {
			builder.addStatement("$N($L)", registrationMethod, BeanRegistrationCode.REGISTRY);
		}
		return builder.build();
	}

	private List<MethodSpec> generateRegistartionMethods(AotContext aotContext) {
		MethodNameGenerator nameGenerator = new MethodNameGenerator();
		List<MethodSpec> generatedMethods = new ArrayList<>(this.registrations.size());
		this.registrations.forEach((definedBean, registration) -> {
			generatedMethods.add(generateRegistrationMethod(aotContext, nameGenerator, definedBean, registration));
		});
		return generatedMethods;
	}

	private MethodSpec generateRegistrationMethod(AotContext aotContext, MethodNameGenerator nameGenerator,
			DefinedBean definedBean, BeanRegistrationCode code) {
		String beanName = definedBean.getBeanName();
		MethodSpec.Builder builder = MethodSpec.methodBuilder(nameGenerator.generatedMethodName("register", beanName));
		builder.addJavadoc("Register the bean definition for $S", beanName);
		builder.addModifiers(Modifier.PRIVATE);
		builder.addParameter(BeanDefinitionRegistry.class, BeanRegistrationCode.REGISTRY);
		builder.addCode(code.getRegistrationMethodBody(aotContext));
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
