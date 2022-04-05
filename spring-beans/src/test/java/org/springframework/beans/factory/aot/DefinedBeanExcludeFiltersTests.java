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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.mock.MockSpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DefinedBeanExcludeFilters}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class DefinedBeanExcludeFiltersTests {

	@Test
	void createWhenSpringFactoriesLoaderIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DefinedBeanExcludeFilters(null, new DefaultListableBeanFactory()))
				.withMessage("'springFactoriesLoader' must not be null");
	}

	@Test
	void createWhenBeanFactoryIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> new DefinedBeanExcludeFilters(SpringFactoriesLoader.forDefaultResourceLocation(), null))
				.withMessage("'beanFactory' must not be null");
	}

	@Test
	void isExcludedWhenDefinedBeanIsNullThrowsException() {
		DefinedBeanExcludeFilters filters = new DefinedBeanExcludeFilters(new DefaultListableBeanFactory());
		assertThatIllegalArgumentException().isThrownBy(() -> filters.isExcluded(null))
				.withMessage("'definedBean' must not be null");
	}

	@Test
	void isExcludedConsidersFactoryLoadedInstancesAndBeansInOrderedOrder() {
		MockDefinedBeanExcludeFilter filter1 = new MockDefinedBeanExcludeFilter(false, 1);
		MockDefinedBeanExcludeFilter filter2 = new MockDefinedBeanExcludeFilter(false, 2);
		MockDefinedBeanExcludeFilter filter3 = new MockDefinedBeanExcludeFilter(false, 3);
		MockDefinedBeanExcludeFilter filter4 = new MockDefinedBeanExcludeFilter(true, 4);
		MockDefinedBeanExcludeFilter filter5 = new MockDefinedBeanExcludeFilter(true, 5);
		MockDefinedBeanExcludeFilter filter6 = new MockDefinedBeanExcludeFilter(true, 6);
		MockSpringFactoriesLoader springFactoriesLoader = new MockSpringFactoriesLoader();
		springFactoriesLoader.addInstance(DefinedBeanExcludeFilter.class, filter3, filter1, filter5);
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("filter4", filter4);
		beanFactory.registerSingleton("filter2", filter2);
		beanFactory.registerSingleton("filter6", filter6);
		DefinedBeanExcludeFilters filters = new DefinedBeanExcludeFilters(springFactoriesLoader, beanFactory);
		beanFactory.registerBeanDefinition("test",
				BeanDefinitionBuilder.rootBeanDefinition(TestBean.class).getBeanDefinition());
		XDefinedBean definedBean = new XDefinedBean(beanFactory, new UniqueBeanFactoryName("test"), "test");
		assertThat(filters.isExcluded(definedBean)).isTrue();
		assertThat(filter1.wasCalled()).isTrue();
		assertThat(filter2.wasCalled()).isTrue();
		assertThat(filter3.wasCalled()).isTrue();
		assertThat(filter4.wasCalled()).isTrue();
		assertThat(filter5.wasCalled()).isFalse();
		assertThat(filter6.wasCalled()).isFalse();
	}

	static class MockDefinedBeanExcludeFilter implements DefinedBeanExcludeFilter, Ordered {

		private final boolean excluded;

		private final int order;

		private XDefinedBean definedBean;

		MockDefinedBeanExcludeFilter(boolean excluded, int order) {
			this.excluded = excluded;
			this.order = order;
		}

		@Override
		public boolean isExcluded(XDefinedBean definedBean) {
			this.definedBean = definedBean;
			return this.excluded;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		boolean wasCalled() {
			return this.definedBean != null;
		}

	}

	static class TestBean {

	}

}
