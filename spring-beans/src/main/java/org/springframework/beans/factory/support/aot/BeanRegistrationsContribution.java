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

import java.util.Collection;
import java.util.Set;

import org.springframework.aot.context.XAotContext;
import org.springframework.aot.context.XAotContribution;
import org.springframework.aot.context.XAotProcessors.Subset;
import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.GeneratedClassName;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.AotBeanClassProcessor;
import org.springframework.beans.factory.aot.XAotDefinedBeanProcessor;
import org.springframework.beans.factory.aot.XDefinedBean;
import org.springframework.beans.factory.aot.UniqueBeanFactoryName;
import org.springframework.beans.factory.aot.UniqueBeanName;
import org.springframework.beans.factory.support.DefaultListableBeanFactoryInitializer;
import org.springframework.beans.factory.support.generate.BeanRegistrationsJavaFileGenerator;
import org.springframework.javapoet.JavaFile;

/**
 * {@link XAotContribution} provided by {@link BeanRegistrationsAotBeanFactoryProcessor} to
 * contribute bean registration code.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class BeanRegistrationsContribution implements XAotContribution {

	private final UniqueBeanFactoryName beanFactoryName;

	private final Set<XDefinedBean> definedBeans;

	private final BeanRegistrationsJavaFileGenerator javaFileGenerator;

	private final Collection<XAotDefinedBeanProcessor> aotDefinedBeanProcessors;

	private final Collection<AotBeanClassProcessor> aotBeanClassProcessors;

	BeanRegistrationsContribution(UniqueBeanFactoryName beanFactoryName, Set<XDefinedBean> definedBeans,
			BeanRegistrationsJavaFileGenerator javaFileGenerator,
			Collection<XAotDefinedBeanProcessor> aotDefinedBeanProcessors,
			Collection<AotBeanClassProcessor> aotBeanClassProcessors) {
		this.beanFactoryName = beanFactoryName;
		this.definedBeans = definedBeans;
		this.javaFileGenerator = javaFileGenerator;
		this.aotDefinedBeanProcessors = aotDefinedBeanProcessors;
		this.aotBeanClassProcessors = aotBeanClassProcessors;
	}

	BeanRegistrationsJavaFileGenerator getJavaFileGenerator() {
		return this.javaFileGenerator;
	}

	Collection<XAotDefinedBeanProcessor> getAotDefinedBeanProcessors() {
		return this.aotDefinedBeanProcessors;
	}

	Collection<AotBeanClassProcessor> getAotBeanClassProcessors() {
		return this.aotBeanClassProcessors;
	}

	@Override
	public void applyTo(XAotContext aotContext) {
		ClassNameGenerator classNameGenerator = aotContext.getClassNameGenerator();
		GeneratedClassName className = classNameGenerator.generateClassName(this.beanFactoryName, "Registrations");
		JavaFile generatedJavaFile = this.javaFileGenerator.generateJavaFile(aotContext, this.beanFactoryName,
				className);
		aotContext.getGeneratedFiles().addSourceFile(generatedJavaFile);
		aotContext.getGeneratedSpringFactories().forNamedItem(BeanFactory.class, this.beanFactoryName)
				.add(DefaultListableBeanFactoryInitializer.class, className);
		applyBeanDefinitionProcessors(aotContext);
		applyBeanClassProcessors(aotContext);
	}

	private void applyBeanDefinitionProcessors(XAotContext aotContext) {
		Subset<XAotDefinedBeanProcessor, UniqueBeanName, XDefinedBean> processors = aotContext.getProcessors()
				.allOfType(XAotDefinedBeanProcessor.class).and(this.aotDefinedBeanProcessors);
		for (XDefinedBean definedBean : this.definedBeans) {
			UniqueBeanName uniqueBeanName = definedBean.getUniqueBeanName();
			processors.processAndApplyContributions(uniqueBeanName, definedBean);
		}
	}

	private void applyBeanClassProcessors(XAotContext aotContext) {
		Subset<AotBeanClassProcessor, String, Class<?>> processors = aotContext.getProcessors()
				.allOfType(AotBeanClassProcessor.class).and(this.aotBeanClassProcessors);
		for (XDefinedBean definedBean : this.definedBeans) {
			Class<?> beanClass = definedBean.getResolvedBeanClass();
			processors.processAndApplyContributions(beanClass.getName(), beanClass);
		}
	}

}