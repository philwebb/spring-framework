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

package org.springframework.context.annotation.configuration;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileAlreadyExistsException;
import java.util.zip.ZipException;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PreProcessedConfiguration;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Miscellaneous system tests covering {@code @Configuration} classes with code generated
 * in the same style as the config annotation processor.
 *
 * @author Phillip Webb
 * @since 5.0
 */
public class ConfigurationClassAnnotationProcessedTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void simplestPossibleConfig() {
		this.context = new AnnotationConfigApplicationContext(SimplestPossibleConfig.class);
		String stringBean = this.context.getBean("stringBean", String.class);
		assertThat(stringBean, startsWith("foo"));
		assertEquals(stringBean, this.context.getBean("stringBean", String.class));
		assertNotEnhanced(this.context, SimplestPossibleConfig.class);
	}

	@Test
	public void directBeanCallsConfig() throws Exception {
		this.context = new AnnotationConfigApplicationContext(DirectBeanMethodCallsConfig.class);
		String stringBean = this.context.getBean("stringBean", String.class);
		String[] parts = stringBean.split("-");
		assertEquals(parts[0], parts[1]);
		assertEquals(parts[0], parts[2]);
		assertNotEnhanced(this.context, DirectBeanMethodCallsConfig.class);
	}

	@Test
	public void innerConfig() throws Exception {
		this.context = new AnnotationConfigApplicationContext(InnerConfig.class);
		assertThat(this.context.getBean("stringBean", String.class), equalTo("foo"));
		assertThat(this.context.getBean("anotherStringBean", String.class), equalTo("bar"));
		assertThat(this.context.getBean("yetAnotherStringBean", String.class), equalTo("baz"));
		assertNotEnhanced(this.context, InnerConfig.class);
		assertNotEnhanced(this.context, InnerConfig.NestedConfig.class);
		assertNotEnhanced(this.context, InnerConfig.NestedStaticConfig.class);
	}

	private void assertNotEnhanced(BeanFactory factory, Class<?> configClass) {
		Object config = factory.getBean(configClass);
		assertFalse(config.getClass().getName().contains("Enhancer"));
	}

	@Configuration
	static class SimplestPossibleConfig {

		@Bean
		public String stringBean() {
			return "foo" + System.nanoTime();
		}
	}

	static class SimplestPossibleConfig$$ConfigurationProxy extends SimplestPossibleConfig implements BeanFactoryAware {

		private PreProcessedConfiguration configuration;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.configuration = new PreProcessedConfiguration(beanFactory, this, MethodHandles.lookup());
		}

		@Override
		public String stringBean() {
			return this.configuration.beanMethod("stringBean").invoke();
		}
	}

	static class DirectBeanMethodCallsConfig {

		@Bean
		public String stringBean() throws IOException {
			return nanoTimeBean() + "-" + nanoTimeBean() + "-" + nanoTimeBean();
		}

		@Bean
		public Long nanoTimeBean() {
			return System.nanoTime();
		}

	}

	static class DirectBeanMethodCallsConfig$$ConfigurationProxy extends DirectBeanMethodCallsConfig implements BeanFactoryAware {

		private PreProcessedConfiguration configuration;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.configuration = new PreProcessedConfiguration(beanFactory, this, MethodHandles.lookup());
		}

		@Override
		public String stringBean() throws IOException {
			return this.configuration.beanMethod("stringBean").invoke();
		}

		@Override
		public Long nanoTimeBean() {
			return this.configuration.beanMethod("nanoTimeBean").invoke();
		}

	}

	@Configuration
	static class InnerConfig {

		@Bean
		public String stringBean() {
			return "foo";
		}

		@Configuration
		class NestedConfig {

			@Bean
			public String anotherStringBean() {
				return "bar";
			}
		}

		@Configuration
		static class NestedStaticConfig {

			@Bean
			public String yetAnotherStringBean() throws ZipException, FileAlreadyExistsException {
				return "baz";
			}
		}
	}

	static class InnerConfig$$ConfigurationProxy extends InnerConfig implements BeanFactoryAware {

		private PreProcessedConfiguration beanMethods;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanMethods = new PreProcessedConfiguration(beanFactory, this,
					MethodHandles.lookup());
		}

		@Override
		public String stringBean() {
			return this.beanMethods.beanMethod("stringBean").invoke();
		}

		class NestedConfig$$ConfigurationProxy extends NestedConfig implements BeanFactoryAware {

			private PreProcessedConfiguration configuration;

			@Override
			public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
				this.configuration = new PreProcessedConfiguration(beanFactory, this,
						MethodHandles.lookup());
			}

			@Override
			public String anotherStringBean() {
				return this.configuration.beanMethod("anotherStringBean").invoke();
			}
		}

		static class NestedStaticConfig$$ConfigurationProxy extends NestedStaticConfig implements BeanFactoryAware {

			private PreProcessedConfiguration configuration;

			@Override
			public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
				this.configuration = new PreProcessedConfiguration(beanFactory, this,
						MethodHandles.lookup());
			}

			@Override
			public String yetAnotherStringBean() throws ZipException, FileAlreadyExistsException {
				return this.configuration.beanMethod("yetAnotherStringBean").invoke();
			}
		}
	}
}
