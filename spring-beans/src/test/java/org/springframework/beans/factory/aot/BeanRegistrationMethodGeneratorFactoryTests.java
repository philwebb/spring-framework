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

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.mock.MockSpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BeanRegistrationMethodGeneratorFactory}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class BeanRegistrationMethodGeneratorFactoryTests {

	@Test
	void getContributedBeanRegistrationWhenExcludedByBeanRegistrationExcludeFilterBeanReturnsNull() {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RegisteredBean registeredBean = registerTestBean(beanFactory);
		beanFactory.registerSingleton("filter", new MockBeanRegistrationExcludeFilter(true, 0));
		BeanRegistrationMethodGeneratorFactory manager = new BeanRegistrationMethodGeneratorFactory(beanFactory,
				springFactoriesLoader);
		assertThat(manager.getBeanRegistrationMethodGenerator(registeredBean)).isNull();
	}

	@Test
	void getContributedBeanRegistrationWhenExcludedByBeanRegistrationLoadedExcludeFilterReturnsNull() {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		springFactoriesLoader.addInstance(BeanRegistrationExcludeFilter.class,
				new MockBeanRegistrationExcludeFilter(true, 0));
		RegisteredBean registeredBean = registerTestBean(beanFactory);
		BeanRegistrationMethodGeneratorFactory manager = new BeanRegistrationMethodGeneratorFactory(beanFactory,
				springFactoriesLoader);
		assertThat(manager.getBeanRegistrationMethodGenerator(registeredBean)).isNull();
	}

	@Test
	void getContributedBeanRegistrationConsidersFactoryLoadedExcludeFiltersAndBeansInOrderedOrder() {
		MockBeanRegistrationExcludeFilter filter1 = new MockBeanRegistrationExcludeFilter(false, 1);
		MockBeanRegistrationExcludeFilter filter2 = new MockBeanRegistrationExcludeFilter(false, 2);
		MockBeanRegistrationExcludeFilter filter3 = new MockBeanRegistrationExcludeFilter(false, 3);
		MockBeanRegistrationExcludeFilter filter4 = new MockBeanRegistrationExcludeFilter(true, 4);
		MockBeanRegistrationExcludeFilter filter5 = new MockBeanRegistrationExcludeFilter(true, 5);
		MockBeanRegistrationExcludeFilter filter6 = new MockBeanRegistrationExcludeFilter(true, 6);
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		springFactoriesLoader.addInstance(BeanRegistrationExcludeFilter.class, filter3, filter1, filter5);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("filter4", filter4);
		beanFactory.registerSingleton("filter2", filter2);
		beanFactory.registerSingleton("filter6", filter6);
		RegisteredBean registeredBean = registerTestBean(beanFactory);
		BeanRegistrationMethodGeneratorFactory manager = new BeanRegistrationMethodGeneratorFactory(beanFactory,
				springFactoriesLoader);
		assertThat(manager.getBeanRegistrationMethodGenerator(registeredBean)).isNull();
		assertThat(filter1.wasCalled()).isTrue();
		assertThat(filter2.wasCalled()).isTrue();
		assertThat(filter3.wasCalled()).isTrue();
		assertThat(filter4.wasCalled()).isTrue();
		assertThat(filter5.wasCalled()).isFalse();
		assertThat(filter6.wasCalled()).isFalse();
	}

	@Test
	void getContributedBeanRegistrationAddsContributionsFromProcessors() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanRegistrationAotContribution beanContribution = mock(BeanRegistrationAotContribution.class);
		BeanRegistrationAotProcessor processorBean = registeredBean -> beanContribution;
		beanFactory.registerSingleton("processorBean", processorBean);
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		BeanRegistrationAotContribution loaderContribution = mock(BeanRegistrationAotContribution.class);
		BeanRegistrationAotProcessor loaderProcessor = registeredBean -> loaderContribution;
		springFactoriesLoader.addInstance(BeanRegistrationAotProcessor.class, loaderProcessor);
		RegisteredBean registeredBean = registerTestBean(beanFactory);
		BeanRegistrationMethodGeneratorFactory manager = new BeanRegistrationMethodGeneratorFactory(beanFactory,
				springFactoriesLoader);
		BeanRegistrationMethodGenerator contributedBeanRegistration = manager
				.getBeanRegistrationMethodGenerator(registeredBean);
		assertThat(contributedBeanRegistration).extracting("aotContributions").asList()
				.containsExactly(beanContribution, loaderContribution);
	}

	@Test
	void getContributedBeanRegistrationWhenCodeGeneratorFactoryReturnsNullUsesDefaultCodeGenerator() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanRegistrationCodeGeneratorFactory codeGeneratorFactory = (registeredBean,
				innerBeanRegistrationMethodGenerator) -> null;
		beanFactory.registerSingleton("codeGeneratorFactory", codeGeneratorFactory);
		RegisteredBean registeredBean = registerTestBean(beanFactory);
		BeanRegistrationMethodGeneratorFactory manager = new BeanRegistrationMethodGeneratorFactory(beanFactory,
				new MockSpringFactoriesLoader());
		BeanRegistrationMethodGenerator contributedBeanRegistration = manager
				.getBeanRegistrationMethodGenerator(registeredBean);
		assertThat(contributedBeanRegistration).extracting("codeGenerator")
				.isInstanceOf(DefaultBeanRegistrationCodeGenerator.class);
	}

	@Test
	void getContributedBeanRegistrationWhenCodeGeneratorFactoryReturnsCustomGeneratorUsesCustomGenerator() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanRegistrationCodeGenerator codeGenerator = mock(BeanRegistrationCodeGenerator.class);
		BeanRegistrationCodeGeneratorFactory codeGeneratorFactory = (registeredBean,
				innerBeanRegistrationMethodGenerator) -> codeGenerator;
		beanFactory.registerSingleton("codeGeneratorFactory", codeGeneratorFactory);
		RegisteredBean registeredBean = registerTestBean(beanFactory);
		BeanRegistrationMethodGeneratorFactory manager = new BeanRegistrationMethodGeneratorFactory(beanFactory,
				new MockSpringFactoriesLoader());
		BeanRegistrationMethodGenerator contributedBeanRegistration = manager
				.getBeanRegistrationMethodGenerator(registeredBean);
		assertThat(contributedBeanRegistration).extracting("codeGenerator").isSameAs(codeGenerator);
	}

	@Test
	void getContributedBeanRegistrationProvidesInnerBeanRegistrationMethodGeneratorToFactory() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		BeanRegistrationCodeGenerator codeGenerator = mock(BeanRegistrationCodeGenerator.class);
		BeanRegistrationCodeGeneratorFactory codeGeneratorFactory = (registeredBean,
				innerBeanRegistrationMethodGenerator) -> {
			if (!"test".equals(registeredBean.getBeanName())) {
				return null;
			}
			assertThat(innerBeanRegistrationMethodGenerator).isNotNull();
			RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean,
					new RootBeanDefinition(InnerTestBean.class));
			MethodReference methodReference = innerBeanRegistrationMethodGenerator
					.generateInnerBeanDefinitionMethod(mock(GenerationContext.class), innerBean);
			assertThat(methodReference).isNotNull();
			return codeGenerator;
		};
		beanFactory.registerSingleton("codeGeneratorFactory", codeGeneratorFactory);
		RegisteredBean registeredBean = registerTestBean(beanFactory);
		BeanRegistrationMethodGeneratorFactory manager = new BeanRegistrationMethodGeneratorFactory(beanFactory,
				new MockSpringFactoriesLoader());
		BeanRegistrationMethodGenerator contributedBeanRegistration = manager
				.getBeanRegistrationMethodGenerator(registeredBean);
		assertThat(contributedBeanRegistration).extracting("codeGenerator").isSameAs(codeGenerator);

	}

	private RegisteredBean registerTestBean(DefaultListableBeanFactory beanFactory) {
		beanFactory.registerBeanDefinition("test",
				BeanDefinitionBuilder.rootBeanDefinition(TestBean.class).getBeanDefinition());
		return RegisteredBean.of(beanFactory, "test");
	}

	static class MockBeanRegistrationExcludeFilter implements BeanRegistrationExcludeFilter, Ordered {

		private final boolean excluded;

		private final int order;

		private RegisteredBean registeredBean;

		MockBeanRegistrationExcludeFilter(boolean excluded, int order) {
			this.excluded = excluded;
			this.order = order;
		}

		@Override
		public boolean isExcluded(RegisteredBean registeredBean) {
			this.registeredBean = registeredBean;
			return this.excluded;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		boolean wasCalled() {
			return this.registeredBean != null;
		}

	}

	static class TestBean {

	}

	static class InnerTestBean {

	}

}
