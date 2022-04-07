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

import java.util.function.BiFunction;
import java.util.function.Supplier;

import groovyjarjarantlr4.v4.runtime.misc.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * A {@code RegisteredBean} represents a bean that has been registered with a
 * {@link BeanFactory}, but has not necessarily been instantiated. It provides access to
 * the bean factory that contains the bean as well as the bean name. In the case of
 * inner-beans, the bean name may have been generated.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public final class RegisteredBean {

	private final ConfigurableBeanFactory beanFactory;

	private final Supplier<String> beanName;

	private final Supplier<RootBeanDefinition> mergedBeanDefinition;
	
	// FIXME is inner

	private RegisteredBean(ConfigurableBeanFactory beanFactory, Supplier<String> beanName,
			Supplier<RootBeanDefinition> mergedBeanDefinition) {
		this.beanFactory = beanFactory;
		this.beanName = beanName;
		this.mergedBeanDefinition = mergedBeanDefinition;
	}

	public static RegisteredBean of(ConfigurableBeanFactory beanFactory, String beanName) {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		Assert.hasLength(beanName, "'beanName' must not be empty");
		return new RegisteredBean(beanFactory, () -> beanName,
				() -> (RootBeanDefinition) beanFactory.getMergedBeanDefinition(beanName));
	}

	public static RegisteredBean ofInnerBean(RegisteredBean parent, BeanDefinitionHolder innerBean) {
		Assert.notNull(innerBean, "'innerBean' must not be null");
		return ofInnerBean(parent, innerBean.getBeanName(), innerBean.getBeanDefinition());
	}

	public static RegisteredBean ofInnerBean(RegisteredBean parent, BeanDefinition innerBeanDefinition) {
		return ofInnerBean(parent, null, innerBeanDefinition);
	}

	public static RegisteredBean ofInnerBean(RegisteredBean parent, @Nullable String innerBeanName,
			BeanDefinition innerBeanDefinition) {
		Assert.notNull(parent, "'parent' must not be null");
		Assert.notNull(innerBeanDefinition, "'innerBeanDefinition' must not be null");
		InnerBeanResolver resolver = new InnerBeanResolver(parent, innerBeanName, innerBeanDefinition);
		Supplier<String> beanName = StringUtils.hasLength(innerBeanName) ? () -> innerBeanName
				: SingletonSupplier.of(resolver::resolveBeanName);
		return new RegisteredBean(parent.getBeanFactory(), beanName, resolver::resolveMergedBeanDefinition);
	}

	/**
	 * Return the name of the bean.
	 * @return the beanName the bean name
	 */
	public String getBeanName() {
		return this.beanName.get();
	}

	/**
	 * Return the bean factory containing the bean.
	 * @return the bean factory
	 */
	public ConfigurableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	public Class<?> getBeanClass() {
		if (this.beanFactory.containsSingleton(getBeanName())) {
			return this.beanFactory.getSingleton(getBeanName()).getClass();
		}
		return getBeanType().resolve();
	}

	public ResolvableType getBeanType() {
		if (this.beanFactory.containsSingleton(getBeanName())) {
			return ResolvableType.forInstance(this.beanFactory.getSingleton(getBeanName()));
		}
		return getMergedBeanDefinition().getResolvableType();
	}

	public RootBeanDefinition getMergedBeanDefinition() {
		return this.mergedBeanDefinition.get();
	}

	/**
	 * Resolver used to obtain inner-bean details.
	 */
	private static class InnerBeanResolver {

		private final RegisteredBean parent;

		@Nullable
		private final String innerBeanName;

		private final BeanDefinition innerBeanDefinition;

		InnerBeanResolver(RegisteredBean parent, @Nullable String innerBeanName, BeanDefinition innerBeanDefinition) {
			Assert.isInstanceOf(AbstractAutowireCapableBeanFactory.class, parent.getBeanFactory());
			this.parent = parent;
			this.innerBeanName = innerBeanName;
			this.innerBeanDefinition = innerBeanDefinition;
		}

		String resolveBeanName() {
			return resolveInnerBean((beanName, mergedBeanDefinition) -> beanName);
		}

		RootBeanDefinition resolveMergedBeanDefinition() {
			return resolveInnerBean((beanName, mergedBeanDefinition) -> mergedBeanDefinition);
		}

		private <T> T resolveInnerBean(BiFunction<String, RootBeanDefinition, T> resolver) {
			// Always use a fresh BeanDefinitionValueResolver in case the parent merged
			// bean definition has changed.
			BeanDefinitionValueResolver beanDefinitionValueResolver = new BeanDefinitionValueResolver(
					(AbstractAutowireCapableBeanFactory) parent.getBeanFactory(), parent.getBeanName(),
					parent.getMergedBeanDefinition());
			return beanDefinitionValueResolver.resolveInnerBean(innerBeanName, innerBeanDefinition, resolver);
		}

	}

}
