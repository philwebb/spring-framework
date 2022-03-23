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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.aot.context.AotContribution;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.AotBeanClassProcessor;
import org.springframework.beans.factory.aot.AotDefinedBeanProcessor;
import org.springframework.beans.factory.aot.DefinedBean;
import org.springframework.beans.factory.aot.DefinedBeanExcludeFilter;
import org.springframework.beans.factory.aot.DefinedBeanExcludeFilters;
import org.springframework.beans.factory.aot.UniqueBeanFactoryName;
import org.springframework.beans.factory.aot.UniqueBeanName;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.generate.BeanRegistrationMethodCodeGenerator;
import org.springframework.beans.factory.support.generate.DefaultBeanRegistrationMethodCodeGenerator;
import org.springframework.beans.testfixture.beans.AnnotatedBean;
import org.springframework.core.mock.MockSpringFactoriesLoader;
import org.springframework.javapoet.CodeBlock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConstructorOrFactoryMethodResolver}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class ConstructorOrFactoryMethodResolverTests {

	@Test
	void processAheadOfTimeWhenNoBeansReturnsNull() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanRegistrationsAotBeanFactoryProcessor processor = new BeanRegistrationsAotBeanFactoryProcessor();
		assertThat(processor.processAheadOfTime(new UniqueBeanFactoryName("testBeanFactory"), beanFactory)).isNull();
	}

	@Test
	void processAheadOfTimeReturnsContribution() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("testBean", new RootBeanDefinition(AnnotatedBean.class));
		BeanRegistrationsAotBeanFactoryProcessor processor = new BeanRegistrationsAotBeanFactoryProcessor();
		BeanRegistrationsContribution contribution = (BeanRegistrationsContribution) processor
				.processAheadOfTime(new UniqueBeanFactoryName("testBeanFactory"), beanFactory);
		assertThat(contribution).isNotNull();
		assertThat(contribution.getJavaFileGenerator()).extracting("beanRegistrationMethodCodeGenerators")
				.asInstanceOf(InstanceOfAssertFactories.MAP).containsOnlyKeys("testBean");
	}

	@Test
	void processAheadOfTimeWhenHasExcludeFilterIgnoresFilteredDefinition() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("testBean", new RootBeanDefinition(AnnotatedBean.class));
		MockSpringFactoriesLoader loader = new MockSpringFactoriesLoader();
		DefinedBeanExcludeFilter filterAll = (definedBean) -> true;
		loader.addInstance(DefinedBeanExcludeFilter.class, filterAll);
		DefinedBeanExcludeFilters excludeFilters = new DefinedBeanExcludeFilters(loader, beanFactory);
		BeanRegistrationsAotBeanFactoryProcessor processor = new BeanRegistrationsAotBeanFactoryProcessor(
				bf -> excludeFilters, DefinedBeanRegistrationHandlers::new);
		BeanRegistrationsContribution contribution = (BeanRegistrationsContribution) processor
				.processAheadOfTime(new UniqueBeanFactoryName("testBeanFactory"), beanFactory);
		assertThat(contribution).isNull();
	}

	@Test
	void processAheadOfTimeWhenHasDefinedBeanRegistrationHandlerUsesHandler() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("testBean", new RootBeanDefinition(AnnotatedBean.class));
		MockSpringFactoriesLoader loader = new MockSpringFactoriesLoader();
		DefinedBeanRegistrationHandler handler = new TestDefinedBeanRegistrationHandler(true);
		loader.addInstance(DefinedBeanRegistrationHandler.class, handler);
		DefinedBeanRegistrationHandlers handlers = new DefinedBeanRegistrationHandlers(loader, beanFactory);
		BeanRegistrationsAotBeanFactoryProcessor processor = new BeanRegistrationsAotBeanFactoryProcessor(
				DefinedBeanExcludeFilters::new, (bf) -> handlers);
		BeanRegistrationsContribution contribution = (BeanRegistrationsContribution) processor
				.processAheadOfTime(new UniqueBeanFactoryName("testBeanFactory"), beanFactory);
		assertThat(contribution).isNotNull();
		assertThat(contribution.getJavaFileGenerator()).extracting("beanRegistrationMethodCodeGenerators")
				.asInstanceOf(InstanceOfAssertFactories.MAP).extractingByKey("testBean")
				.isInstanceOf(TestBeanRegistrationMethodCodeGenerator.class);
	}

	@Test
	void processAheadOfTimeWhenHandlerReturnsNoBeanRegistrationMethodCodeGeneratorUsesDefaultGenerator() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("testBean", new RootBeanDefinition(AnnotatedBean.class));
		MockSpringFactoriesLoader loader = new MockSpringFactoriesLoader();
		DefinedBeanRegistrationHandler handler = new TestDefinedBeanRegistrationHandler(false);
		loader.addInstance(DefinedBeanRegistrationHandler.class, handler);
		DefinedBeanRegistrationHandlers handlers = new DefinedBeanRegistrationHandlers(loader, beanFactory);
		BeanRegistrationsAotBeanFactoryProcessor processor = new BeanRegistrationsAotBeanFactoryProcessor(
				DefinedBeanExcludeFilters::new, (bf) -> handlers);
		BeanRegistrationsContribution contribution = (BeanRegistrationsContribution) processor
				.processAheadOfTime(new UniqueBeanFactoryName("testBeanFactory"), beanFactory);
		assertThat(contribution).isNotNull();
		assertThat(contribution.getJavaFileGenerator()).extracting("beanRegistrationMethodCodeGenerators")
				.asInstanceOf(InstanceOfAssertFactories.MAP).extractingByKey("testBean")
				.isInstanceOf(DefaultBeanRegistrationMethodCodeGenerator.class);
	}

	@Test
	void processAheadOfTimeIncludesAotDefinedBeanProcessors() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("testBean", new RootBeanDefinition(AnnotatedBean.class));
		beanFactory.registerBeanDefinition("processor", new RootBeanDefinition(TestAotDefinedBeanProcessor.class));
		BeanRegistrationsAotBeanFactoryProcessor processor = new BeanRegistrationsAotBeanFactoryProcessor();
		BeanRegistrationsContribution contribution = (BeanRegistrationsContribution) processor
				.processAheadOfTime(new UniqueBeanFactoryName("testBeanFactory"), beanFactory);
		assertThat(contribution).isNotNull();
		assertThat(contribution.getAotDefinedBeanProcessors())
				.containsExactly((AotDefinedBeanProcessor) beanFactory.getBean("processor"));
	}

	@Test
	void processAheadOfTimeIncludesAotBeanClassProcessor() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("testBean", new RootBeanDefinition(AnnotatedBean.class));
		beanFactory.registerBeanDefinition("processor", new RootBeanDefinition(TestAotBeanClassProcessor.class));
		BeanRegistrationsAotBeanFactoryProcessor processor = new BeanRegistrationsAotBeanFactoryProcessor();
		BeanRegistrationsContribution contribution = (BeanRegistrationsContribution) processor
				.processAheadOfTime(new UniqueBeanFactoryName("testBeanFactory"), beanFactory);
		assertThat(contribution).isNotNull();
		assertThat(contribution.getAotBeanClassProcessors())
				.containsExactly((TestAotBeanClassProcessor) beanFactory.getBean("processor"));
	}

	static class TestDefinedBeanRegistrationHandler implements DefinedBeanRegistrationHandler {

		private boolean returnGenerator;

		TestDefinedBeanRegistrationHandler(boolean returnGenerator) {
			this.returnGenerator = returnGenerator;
		}

		@Override
		public boolean canHandle(DefinedBean definedBean) {
			return true;
		}

		@Override
		public BeanRegistrationMethodCodeGenerator getBeanRegistrationMethodCodeGenerator(DefinedBean definedBean) {
			return (!this.returnGenerator) ? null : new TestBeanRegistrationMethodCodeGenerator();
		}

	}

	static class TestBeanRegistrationMethodCodeGenerator implements BeanRegistrationMethodCodeGenerator {

		@Override
		public CodeBlock generateBeanRegistrationMethodCode(GenerationContext generationContext,
				GeneratedMethods generatedMethods) {
			return CodeBlock.of("test");
		}

	}

	static class TestAotDefinedBeanProcessor implements AotDefinedBeanProcessor {

		@Override
		public AotContribution processAheadOfTime(UniqueBeanName beanName, DefinedBean definedBean) {
			return null;
		}

	}

	static class TestAotBeanClassProcessor implements AotBeanClassProcessor {

		@Override
		public AotContribution processAheadOfTime(String beanClassName, Class<?> beanClass) {
			return null;
		}

	}
}
