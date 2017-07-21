/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.lang.invoke.MethodHandles;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link PreProcessedConfiguration}.
 *
 * @author Phillip Webb
 */
public class PreProcessedConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenBeanFactoryIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("BeanFactory must not be null");
		new PreProcessedConfiguration(null, new Object(), MethodHandles.lookup());
	}

	@Test
	public void createWhenBeanFactoryIsNotConfigurableBeanFactoryShouldThrowException()
			throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("must be an instance of");
		new PreProcessedConfiguration(mock(BeanFactory.class), new Object(),
				MethodHandles.lookup());
	}

	@Test
	public void createWhenInstanceIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Instance must not be null");
		new PreProcessedConfiguration(mock(ConfigurableBeanFactory.class),
				null, MethodHandles.lookup());
	}

	@Test
	public void createWhenLookupIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Lookup must not be null");
		new PreProcessedConfiguration(mock(ConfigurableBeanFactory.class), new Object(), null);
	}

//	@Test
//	public void getShouldReturnConfigurationClassBeanMethod() throws Throwable {
//		TestConfiguration$$Beans instance = new TestConfiguration$$Beans();
//		instance.setBeanFactory(mock(ConfigurableBeanFactory.class));
//		BeanMethodInvoker beanMethod = instance.testBeanBeanMethod();
//		TestBean result = (TestBean) beanMethod.getInvoker().invoke("foo");
//		assertEquals("test-foo", result.toString());
//		instance.testBean("bar");
//	}


	@Configuration
	static class TestConfiguration {

		@Bean
		public TestBean testBean(String otherBean) {
			return new TestBean(otherBean);
		}
	}

	static class TestConfiguration$$ConfigurationProxy extends TestConfiguration implements BeanFactoryAware {

		private PreProcessedConfiguration configuration;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.configuration = new PreProcessedConfiguration(beanFactory, this, MethodHandles.lookup());
		}

		@Override
		public TestBean testBean(String otherBean) {
			return this.configuration.beanMethod("testBean", String.class).invoke();
		}
	}

	static class TestBean {

		private final String value;

		public TestBean(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "test-" + this.value;
		}
	}

}
