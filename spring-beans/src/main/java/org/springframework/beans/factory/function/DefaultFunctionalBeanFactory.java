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

package org.springframework.beans.factory.function;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ResolvableType;

/**
 * Default {@link FunctionalBeanFactory} implementation.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class DefaultFunctionalBeanFactory extends AbstractFunctionalBeanFactory implements
		HierarchicalBeanFactory, FunctionalBeanFactory, FunctionalBeanRegistry {

	private final FunctionalBeanFactory parent = null;

	private final AtomicInteger sequenceGenerator = new AtomicInteger();

	private final FunctionalBeanRegistrations registrations = new FunctionalBeanRegistrations();

	public DefaultFunctionalBeanFactory() {
	}

	@Override
	public <T> void register(FunctionBeanDefinition<T> definition) {
		FunctionalBeanRegistration<T> registration = new FunctionalBeanRegistration<>(
				this.sequenceGenerator.getAndIncrement(), definition);
		this.registrations.add(registration);
	}

	@Override
	public FunctionalBeanFactory getParentBeanFactory() {
		return parent;
	}

	@Override
	public boolean containsLocalBean(String name) {
		return false;
	}

	@Override
	public <T> T getBean(BeanSelector<T> selector, Object... args) throws BeansException {
		return null;
		// FIXME checksParent

	}

	@Override
	public <S, T> T getBean(BeanSelector<S> selector, Class<T> requiredType)
			throws BeansException {
		return null;
		// FIXME checksParent
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(BeanSelector<T> selector,
			boolean allowEagerInit) throws BeansException {
		return null;
	}

	@Override
	public <T> boolean containsBean(BeanSelector<T> selector) {
		// FIXME
		FunctionalBeanFactory parentBeanFactory = getParentBeanFactory();
		return (parentBeanFactory != null && parentBeanFactory.containsBean(selector));
	}

	@Override
	public <T> boolean isSingleton(BeanSelector<T> selector) {
		// FIXME checksParent
		return false;
	}

	@Override
	public <T> boolean isPrototype(BeanSelector<T> selector) {
		// FIXME checksParent
		return false;
	}

	@Override
	public <T> boolean isTypeMatch(BeanSelector<T> selector, Class<?> typeToMatch) {
		// FIXME checksParent
		return false;
	}

	@Override
	public <T> boolean isTypeMatch(BeanSelector<T> selector, ResolvableType typeToMatch) {
		// FIXME checksParent
		return false;
	}

	@Override
	public <T> Class<?> getType(BeanSelector<T> selector, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {
		// FIXME checksParent
		return null;
	}

	@Override
	public String[] getAliases(String name) {
		return null;// FIXME
		// FIXME checksParent
	}

	@Override
	public <T> boolean containsBeanDefinition(BeanSelector<T> selector) {
		return false;
	}

	@Override
	public int getBeanDefinitionCount() {
		return 0;// FIXME
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return null;// FIXME
	}

	@Override
	public <T> String[] getBeanNames(BeanSelector<T> selector,
			boolean includeNonSingletons, boolean allowEagerInit) {
		return null;
	}

	@Override
	public <T> Map<String, T> getBeans(BeanSelector<T> selector,
			boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
		return null;
	}



	static DefaultFunctionalBeanFactory of(Consumer<Builder> initializer) {
		return null;
	}

	static class Builder {

		// setParent
		// registerScope
		// addEmbeddedValueResolver

	}

}
