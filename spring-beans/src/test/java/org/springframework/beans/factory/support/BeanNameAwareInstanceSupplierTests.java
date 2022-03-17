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

package org.springframework.beans.factory.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanNameAwareInstanceSupplier}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class BeanNameAwareInstanceSupplierTests {

	@Test
	void getCallsWithNullBeanName() {
		TestSupplier supplier = new TestSupplier();
		assertThat(supplier.get()).isEqualTo("I am bean null");
	}

	@Test
	void callFromBeanFactoryProvidesBeanName() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition();
		beanDefinition.setInstanceSupplier(new TestSupplier());
		beanFactory.registerBeanDefinition("test", beanDefinition);
		assertThat(beanFactory.getBean("test")).isEqualTo("I am bean test");

	}

	static class TestSupplier implements BeanNameAwareInstanceSupplier<String> {

		@Override
		public String get(String beanName) {
			return "I am bean " + beanName;
		}

	}

}
