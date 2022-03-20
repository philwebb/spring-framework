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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;

import org.springframework.aot.context.AotContext;
import org.springframework.aot.context.AotContribution;
import org.springframework.aot.context.AotProcessors.Subset;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.GeneratedClassName;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethodName;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodNameGenerator;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.AotBeanClassProcessor;
import org.springframework.beans.factory.aot.AotDefinedBeanProcessor;
import org.springframework.beans.factory.aot.DefinedBean;
import org.springframework.beans.factory.aot.UniqueBeanFactoryName;
import org.springframework.beans.factory.aot.UniqueBeanName;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactoryInitializer;
import org.springframework.beans.factory.support.generate.BeanRegistrationMethodCodeGenerator;
import org.springframework.beans.factory.support.generate.BeanRegistrationsJavaFileGenerator;
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

	private final Map<DefinedBean, DefinedBeanRegistrationHandler> handlers;

	BeanRegistrationsContribution(UniqueBeanFactoryName beanFactoryName, ConfigurableListableBeanFactory beanFactory,
			Map<DefinedBean, DefinedBeanRegistrationHandler> handlers) {
		this.beanFactoryName = beanFactoryName;
		this.handlers = handlers;
		// FIXME grab processor beans to apply
	}

	@Override
	public void applyTo(AotContext aotContext) {
		ClassNameGenerator classNameGenerator = aotContext.getClassNameGenerator();
		GeneratedClassName className = classNameGenerator.generateClassName(this.beanFactoryName, "Registrations");
		generateJavaFile(aotContext, className);
		aotContext.getGeneratedSpringFactories().forNamedItem(BeanFactory.class, this.beanFactoryName)
				.add(DefaultListableBeanFactoryInitializer.class, className);
		applyBeanDefinitionProcessors(aotContext);
		applyBeanClassProcessors(aotContext);
	}

	private JavaFile generateJavaFile(AotContext aotContext, GeneratedClassName className) {
		Map<String, BeanRegistrationMethodCodeGenerator> methodCodeGenerators = new LinkedHashMap<>();
		this.handlers.forEach((definedBean, handler) -> {
			methodCodeGenerators.put(definedBean.getBeanName(),
					handler.getBeanRegistrationMethodCodeGenerator(null, definedBean));
		});
		return new BeanRegistrationsJavaFileGenerator(methodCodeGenerators).generateJavaFile(aotContext,
				beanFactoryName, className);
	}

	private void applyBeanDefinitionProcessors(AotContext aotContext) {
		Subset<AotDefinedBeanProcessor, UniqueBeanName, DefinedBean> processors = aotContext.getProcessors()
				.allOfType(AotDefinedBeanProcessor.class);
		for (DefinedBean definedBean : this.handlers.keySet()) {
			processors.processAndApplyContributions(definedBean.getUniqueBeanName(), definedBean);
		}
	}

	private void applyBeanClassProcessors(AotContext aotContext) {
		Subset<AotBeanClassProcessor, String, Class<?>> processors = aotContext.getProcessors()
				.allOfType(AotBeanClassProcessor.class);
		for (DefinedBean definedBean : this.handlers.keySet()) {
			Class<?> beanClass = definedBean.getResolvedBeanClass();
			processors.processAndApplyContributions(beanClass.getName(), beanClass);
		}
	}

}
