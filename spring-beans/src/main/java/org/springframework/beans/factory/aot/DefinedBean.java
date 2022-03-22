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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * Details that can be used when AOT processing a bean that has been defined in a
 * {@link BeanFactory}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see AotDefinedBeanProcessor
 * @see DefinedBeanExcludeFilter
 */
public final class DefinedBean {

	private final ConfigurableListableBeanFactory beanFactory;

	private final String beanName;

	private final UniqueBeanName uniqueBeanName;

	private final BeanDefinition beanDefinition;

	private final RootBeanDefinition mergedBeanDefinition;

	private final ResolvableType resolvedBeanType;

	public DefinedBean(ConfigurableListableBeanFactory beanFactory, UniqueBeanFactoryName beanFactoryName,
			String beanName) {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		Assert.notNull(beanFactoryName, "'beanFactoryName' must not be null");
		Assert.hasLength(beanName, "'beanName' must not be empty");
		this.beanFactory = beanFactory;
		this.beanName = beanName;
		this.uniqueBeanName = new UniqueBeanName(beanFactoryName, beanName);
		this.beanDefinition = beanFactory.getBeanDefinition(beanName);
		this.mergedBeanDefinition = (RootBeanDefinition) beanFactory.getMergedBeanDefinition(beanName);
		this.resolvedBeanType = this.mergedBeanDefinition.getResolvableType();
	}

	/**
	 * Return the {@link ConfigurableListableBeanFactory} that defined the bean.
	 * @return the bean factory
	 */
	public ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * Return the name of the bean as defined in the bean factory.
	 * @return the bean name
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return the unique bean name composed from the {@link UniqueBeanFactoryName bean
	 * factory name} and the {@link #getBeanName() bean name}.
	 * @return the unique bean name
	 */
	public UniqueBeanName getUniqueBeanName() {
		return this.uniqueBeanName;
	}

	/**
	 * Return the {@link BeanDefinition} as registered.
	 * @return the bean definition
	 * @see #getMergedBeanDefinition()
	 */
	public BeanDefinition getBeanDefinition() {
		return this.beanDefinition;
	}

	/**
	 * Return the full merged bean definition.
	 * @return the merged bean definition
	 * @see #getBeanDefinition()
	 * @see ConfigurableBeanFactory#getMergedBeanDefinition(String)
	 */
	public RootBeanDefinition getMergedBeanDefinition() {
		return this.mergedBeanDefinition;
	}

	/**
	 * Return the bean class as resolved by the bean factory.
	 * @return the resolved bean class
	 */
	public ResolvableType getResolvedBeanType() {
		return this.resolvedBeanType;
	}

	/**
	 * Return the bean class as resolved by the bean factory.
	 * @return the resolved bean class
	 */
	public Class<?> getResolvedBeanClass() {
		return this.resolvedBeanType.toClass();
	}

}
