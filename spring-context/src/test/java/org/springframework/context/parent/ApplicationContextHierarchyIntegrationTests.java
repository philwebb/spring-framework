/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.parent;

import java.applet.AppletContext;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.testfixture.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link AppletContext} hierarchies.
 *
 * @author Phillip Webb
 */
public class ApplicationContextHierarchyIntegrationTests {

	@Test
	void noHierarchy() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				NoHierarchyConfiguration.class)) {
			ObjectProvider<TestBean> provider = context.getBeanProvider(TestBean.class);
			assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(
					() -> provider.getObject());
			assertThat(provider.getIfUnique()).isNull();
			assertThat(provider.stream().toArray()).hasSize(2);
		}
	}

	@Test
	void noHierarchyInject() {
		assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(() -> {
			try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
					NoHierarchyConfiguration.class, TestComponent.class)) {
			}
		}).withCauseInstanceOf(NoUniqueBeanDefinitionException.class);
	}

	@Test
	void evenSplit() {
		try (AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext(
				EvenSplitParentConfiguration.class)) {
			try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
					EvenSplitChildConfiguration.class)) {
				context.setParent(parent);
				ObjectProvider<TestBean> provider = context.getBeanProvider(
						TestBean.class);
				assertThat(provider.getObject()).hasToString("testBean2");
				assertThat(provider.getIfUnique()).hasToString("testBean2");
				assertThat(provider.stream().toArray()).hasSize(2);
			}
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class NoHierarchyConfiguration {

		@Bean
		TestBean testBean1() {
			return new TestBean("testBean1");
		}

		@Bean
		TestBean testBean2() {
			return new TestBean("testBean2");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EvenSplitParentConfiguration {

		@Bean
		TestBean testBean1() {
			return new TestBean("testBean1");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EvenSplitChildConfiguration {

		@Bean
		TestBean testBean2() {
			return new TestBean("testBean2");
		}

	}

	static class TestBean {

		private final String name;

		TestBean(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

	@Component
	static class TestComponent {

		private final TestBean testBean;

		TestComponent(TestBean testBean) {
			this.testBean = testBean;
		}

		TestBean getTestBean() {
			return testBean;
		}

	}

}
