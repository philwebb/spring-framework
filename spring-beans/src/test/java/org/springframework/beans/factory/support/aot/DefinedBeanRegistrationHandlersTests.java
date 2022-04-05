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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.aot.XDefinedBean;
import org.springframework.beans.factory.aot.UniqueBeanFactoryName;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.aot.DefinedBeanRegistrationHandlers.DefaultDefinedBeanRegistrationHandler;
import org.springframework.beans.factory.support.generate.BeanRegistrationMethodCodeGenerator;
import org.springframework.core.Ordered;
import org.springframework.core.mock.MockSpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefinedBeanRegistrationHandlers}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class DefinedBeanRegistrationHandlersTests {

	@Test
	void getHandlerWhenNoHandlerReturnsDefault() {
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		DefinedBeanRegistrationHandlers handlers = new DefinedBeanRegistrationHandlers(springFactoriesLoader,
				beanFactory);
		beanFactory.registerBeanDefinition("test",
				BeanDefinitionBuilder.rootBeanDefinition(TestBean.class).getBeanDefinition());
		XDefinedBean definedBean = new XDefinedBean(beanFactory, new UniqueBeanFactoryName("test"), "test");
		assertThat(handlers.getHandler(definedBean)).isSameAs(DefaultDefinedBeanRegistrationHandler.INSTANCE);
	}

	@Test
	void getHandlerConsidersFactoryLoadedInstancesAndBeansInOrderedOrder() {
		MockDefinedBeanRegistrationHandler handler1 = new MockDefinedBeanRegistrationHandler(false, 1);
		MockDefinedBeanRegistrationHandler handler2 = new MockDefinedBeanRegistrationHandler(false, 2);
		MockDefinedBeanRegistrationHandler handler3 = new MockDefinedBeanRegistrationHandler(false, 3);
		MockDefinedBeanRegistrationHandler handler4 = new MockDefinedBeanRegistrationHandler(true, 4);
		MockDefinedBeanRegistrationHandler handler5 = new MockDefinedBeanRegistrationHandler(true, 5);
		MockDefinedBeanRegistrationHandler handler6 = new MockDefinedBeanRegistrationHandler(true, 6);
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		springFactoriesLoader.addInstance(DefinedBeanRegistrationHandler.class, handler3, handler1, handler5);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("handler4", handler4);
		beanFactory.registerSingleton("handler2", handler2);
		beanFactory.registerSingleton("handler6", handler6);
		DefinedBeanRegistrationHandlers handlers = new DefinedBeanRegistrationHandlers(springFactoriesLoader,
				beanFactory);
		beanFactory.registerBeanDefinition("test",
				BeanDefinitionBuilder.rootBeanDefinition(TestBean.class).getBeanDefinition());
		XDefinedBean definedBean = new XDefinedBean(beanFactory, new UniqueBeanFactoryName("test"), "test");
		assertThat(handlers.getHandler(definedBean)).isSameAs(handler4);
		assertThat(handler1.wasCalled()).isTrue();
		assertThat(handler2.wasCalled()).isTrue();
		assertThat(handler3.wasCalled()).isTrue();
		assertThat(handler4.wasCalled()).isTrue();
		assertThat(handler5.wasCalled()).isFalse();
		assertThat(handler6.wasCalled()).isFalse();
	}

	static class MockDefinedBeanRegistrationHandler implements DefinedBeanRegistrationHandler, Ordered {

		private final boolean canHandle;

		private final int order;

		private boolean called;

		MockDefinedBeanRegistrationHandler(boolean canHandle, int order) {
			this.canHandle = canHandle;
			this.order = order;
		}

		@Override
		public boolean canHandle(XDefinedBean definedBean) {
			this.called = true;
			return this.canHandle;
		}

		@Override
		public BeanRegistrationMethodCodeGenerator getBeanRegistrationMethodCodeGenerator(XDefinedBean definedBean) {
			return null;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		boolean wasCalled() {
			return this.called;
		}

	}

	static class TestBean {

	}

}
