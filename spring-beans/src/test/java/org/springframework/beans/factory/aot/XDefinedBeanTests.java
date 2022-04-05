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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link XDefinedBean}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class XDefinedBeanTests {

	private DefaultListableBeanFactory beanFactory;

	private RootBeanDefinition beanDefinition;

	private UniqueBeanFactoryName beanFactoryName;

	private XDefinedBean definedBean;

	@BeforeEach
	void setup() {
		this.beanFactory = new DefaultListableBeanFactory();
		this.beanDefinition = new RootBeanDefinition(TestBean.class);
		this.beanDefinition.setTargetType(ResolvableType.forClassWithGenerics(TestBean.class, String.class));
		this.beanFactory.registerBeanDefinition("testBean", beanDefinition);
		this.beanFactoryName = new UniqueBeanFactoryName("default");
		this.definedBean = new XDefinedBean(beanFactory, this.beanFactoryName, "testBean");
	}

	@Test
	void createWhenBeanFactoryIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new XDefinedBean(null, this.beanFactoryName, "testBean"))
				.withMessage("'beanFactory' must not be null");
	}

	@Test
	void createWhenBeanFactoryNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new XDefinedBean(this.beanFactory, null, "testBean"))
				.withMessage("'beanFactoryName' must not be null");
	}

	@Test
	void createWhenBeanNameIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new XDefinedBean(this.beanFactory, this.beanFactoryName, null))
				.withMessage("'beanName' must not be empty");
	}

	@Test
	void createWhenBeanNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new XDefinedBean(this.beanFactory, this.beanFactoryName, ""))
				.withMessage("'beanName' must not be empty");
	}

	@Test
	void getBeanFactoryReturnsBeanFactory() {
		assertThat(this.definedBean.getBeanFactory()).isEqualTo(this.beanFactory);
	}

	@Test
	void getBeanNameReturnsBeanName() {
		assertThat(this.definedBean.getBeanName()).isEqualTo("testBean");
	}

	@Test
	void getUniqueBeanNameReturnsUniqueBeanName() {
		assertThat(this.definedBean.getUniqueBeanName()).hasToString("default:testBean");
	}

	@Test
	void getBeanDefinitionReturnsBeanDefinition() {
		assertThat(this.definedBean.getBeanDefinition()).isSameAs(this.beanDefinition);
	}

	@Test
	void getMergedBeanDefinitionReturnsMergedBeanDefinition() {
		assertThat(this.definedBean.getMergedBeanDefinition()).isNotSameAs(this.beanDefinition)
				.isEqualTo(this.beanFactory.getMergedBeanDefinition("testBean"));
	}

	@Test
	void getResolvedBeanTypeReturnsResolvedBeanType() {
		assertThat(this.definedBean.getResolvedBeanType())
				.isEqualTo(ResolvableType.forClassWithGenerics(TestBean.class, String.class));
	}

	@Test
	void getResolvedBeanClassReturnsResolvedBeanClass() {
		assertThat(this.definedBean.getResolvedBeanClass()).isEqualTo(TestBean.class);
	}

	static class TestBean<T> {

	}

}
