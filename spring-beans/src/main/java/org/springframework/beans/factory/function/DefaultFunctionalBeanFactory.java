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
import org.springframework.lang.Nullable;

/**
 * Default {@link FunctionalBeanFactory} implementation.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class DefaultFunctionalBeanFactory extends AbstractFunctionalBeanFactory implements
		HierarchicalBeanFactory, FunctionalBeanFactory, FunctionalBeanRegistry {

	private static final String[] EMPTY_STRING_ARRAY = {};

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
		return this.parent;
	}

	@Override
	public boolean containsLocalBean(String name) {
		return this.registrations.find(BeanSelector.byName(name)) != null;
	}

	@Override
	public <T> T getBean(BeanSelector<T> selector) throws BeansException {
		return getBean(selector, null, null);
	}

	@Override
	public <T> T getBean(BeanSelector<T> selector, Object... args) throws BeansException {
		return getBean(selector, args, null);
	}

	@Override
	public <T, R> R getBean(BeanSelector<T> selector, Class<R> requiredType)
			throws BeansException {
		return getBean(selector, null, requiredType);
	}

	@SuppressWarnings("unchecked")
	private <T, R> R getBean(BeanSelector<T> selector, @Nullable Object[] args,
			@Nullable Class<R> requiredType) throws BeansException {
		FunctionalBeanRegistration<T> registration = this.registrations.find(selector);
		if (registration != null) {
			InjectionContext injectionContext = new FunctionalBeanInjectionContext(
					registration, args);
			Object bean = registration.getBeanInstance(injectionContext, args);
			return (R) bean;
		}
		if (this.parent != null) {
			return (args != null) ? (R) this.parent.getBean(selector, args)
					: this.parent.getBean(selector, requiredType);
		}
		throw new NoSelectableBeanDefinitionException(selector);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(BeanSelector<T> selector,
			boolean allowEagerInit) throws BeansException {
		return getBeanProvider(selector, null);
	}

	private <T> ObjectProvider<T> getBeanProvider(BeanSelector<T> selector,
			FunctionalBeanInjectionContext injectionContext) {


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
		FunctionalBeanRegistration<T> registration = this.registrations.find(selector);
		if (registration != null) {
			return registration.isSingleton();
		}
		if (this.parent != null) {
			return this.parent.isSingleton(selector);
		}
		throw new NoSelectableBeanDefinitionException(selector);
	}

	@Override
	public <T> boolean isPrototype(BeanSelector<T> selector) {
		FunctionalBeanRegistration<T> registration = this.registrations.find(selector);
		if (registration != null) {
			return registration.isPrototype();
		}
		if (this.parent != null) {
			return this.parent.isPrototype(selector);
		}
		throw new NoSelectableBeanDefinitionException(selector);
	}

	@Override
	public <T> boolean isTypeMatch(BeanSelector<T> selector, Class<?> typeToMatch) {
		FunctionalBeanRegistration<T> registration = this.registrations.find(selector);
		if (registration != null) {
			return registration.getDefinition().hasType(typeToMatch);
		}
		if (this.parent != null) {
			return this.parent.isTypeMatch(selector, typeToMatch);
		}
		throw new NoSelectableBeanDefinitionException(selector);
	}

	@Override
	public <T> boolean isTypeMatch(BeanSelector<T> selector, ResolvableType typeToMatch) {
		FunctionalBeanRegistration<T> registration = this.registrations.find(selector);
		if (registration != null) {
			return registration.getDefinition().hasType(typeToMatch);
		}
		if (this.parent != null) {
			return this.parent.isTypeMatch(selector, typeToMatch);
		}
		throw new NoSelectableBeanDefinitionException(selector);
	}

	@Override
	public <T> Class<?> getType(BeanSelector<T> selector, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {
		FunctionalBeanRegistration<T> registration = this.registrations.find(selector);
		if (registration != null) {
			return registration.getDefinition().getType();
		}
		if (this.parent != null) {
			return this.parent.getType(selector, allowFactoryBeanInit);
		}
		throw new NoSelectableBeanDefinitionException(selector);
	}

	@Override
	public String[] getAliases(String name) {
		return EMPTY_STRING_ARRAY;
	}

	@Override
	public <T> boolean containsBeanDefinition(BeanSelector<T> selector) {
		return false; // FIXME
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

	/**
	 * Resolve the given embedded value, e.g. an annotation attribute.
	 * @param value the value to resolve
	 * @return the resolved value (may be the original value as-is)
	 */
	@Nullable
	public String resolveEmbeddedValue(String value) {
		return value;
	}

	public static DefaultFunctionalBeanFactory of(Consumer<Builder> initializer) {
		return null;
	}

	public static class Builder {

		// setParent
		// registerScope
		// addEmbeddedValueResolver

	}

	private class FunctionalBeanInjectionContext implements InjectionContext {

		private final InjectionContext parent;

		private final FunctionalBeanRegistration<?> registration;

		@Nullable
		private Object[] args;

		FunctionalBeanInjectionContext(FunctionalBeanRegistration<?> registration,
				@Nullable Object[] args) {
			this.parent = null;
			this.args = args;
			this.registration = registration;
		}

		@Override
		public Object[] getArgs() {
			return this.args;
		}

		public <T> T getBean(BeanSelector<T> selector){
			// FIXME
			return null;
		}

		@Override
		public <T> ObjectProvider<T> getBeanProvider(BeanSelector<T> selector) {
			return DefaultFunctionalBeanFactory.this.getBeanProvider(selector, this);
		}

		@Override
		public String resolveEmbeddedValue(String value) {
			return DefaultFunctionalBeanFactory.this.resolveEmbeddedValue(value);
		}

	}

}
