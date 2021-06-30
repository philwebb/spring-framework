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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.OrderComparator;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default {@link FunctionalBeanFactory} implementation.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class DefaultFunctionalBeanFactory extends AbstractFunctionalBeanFactory
		implements HierarchicalBeanFactory, FunctionalBeanFactory, FunctionalBeanRegistry {

	private static final String[] EMPTY_STRING_ARRAY = {};

	private final FunctionalBeanFactory parent = null;

	private final Comparator<Object> dependencyComparator = OrderComparator.INSTANCE;

	private final AtomicInteger sequenceGenerator = new AtomicInteger();

	private final FunctionalBeanRegistrations registrations = new FunctionalBeanRegistrations();

	public DefaultFunctionalBeanFactory() {
	}

	public DefaultFunctionalBeanFactory(FunctionalBeanRegistrar... registrars) {
		registerFrom(registrars);
	}

	public DefaultFunctionalBeanFactory(Consumer<Builder> builder) {
	}

	@Override
	public <T> void register(FunctionalBeanDefinition<T> definition) {
		FunctionalBeanRegistration<T> registration = new FunctionalBeanRegistration<>(
				this.sequenceGenerator.getAndIncrement(), definition);
		this.registrations.add(registration);
	}

	@Override
	public void registerFrom(FunctionalBeanRegistrar... registrars) {
		for (FunctionalBeanRegistrar registrar : registrars) {
			registrar.apply(this);
		}
	}

	@Override
	public FunctionalBeanFactory getParentBeanFactory() {
		return this.parent;
	}

	@Override
	public boolean containsLocalBean(String name) {
		return this.registrations.find(BeanSelector.byName(name), true) != null;
	}

	@Override
	public <T> T getBean(BeanSelector<T> selector) throws BeansException {
		return resolveBean(null, selector, null, null);
	}

	@Override
	public <T> T getBean(BeanSelector<T> selector, Object... args) throws BeansException {
		return resolveBean(null, selector, args, null);
	}

	@Override
	public <T, R> R getBean(BeanSelector<T> selector, Class<R> requiredType) throws BeansException {
		return resolveBean(null, selector, null, requiredType);
	}

	private <T, R> R resolveBean(@Nullable FunctionalInjectionContext parentInjectionContext, BeanSelector<T> selector,
			@Nullable Object[] args, @Nullable Class<R> requiredType) throws BeansException {
		R bean = resolveBean(parentInjectionContext, selector, args, requiredType, false);
		if (bean != null) {
			return bean;
		}
		throw new NoSelectableBeanDefinitionException(selector);
	}

	private <T, R> R resolveBeanIfAvailable(@Nullable FunctionalInjectionContext parentInjectionContext,
			BeanSelector<T> selector, @Nullable Object[] args, @Nullable Class<R> requiredType) throws BeansException {
		return resolveBean(parentInjectionContext, selector, args, requiredType, false);
	}

	private <T, R> R resolveBeanIfUnique(@Nullable FunctionalInjectionContext parentInjectionContext,
			BeanSelector<T> selector, @Nullable Object[] args, @Nullable Class<R> requiredType) throws BeansException {
		return resolveBean(parentInjectionContext, selector, args, requiredType, true);
	}

	@SuppressWarnings("unchecked")
	private <T, R> R resolveBean(@Nullable FunctionalInjectionContext parentInjectionContext, BeanSelector<T> selector,
			@Nullable Object[] args, @Nullable Class<R> requiredType, boolean nonUniqueAsNull) throws BeansException {
		FunctionalBeanRegistration<T> registration = this.registrations.find(selector, nonUniqueAsNull);
		if (registration != null) {
			if (requiredType != null && !registration.getDefinition().hasType(requiredType)) {
				throw new NoSelectableBeanDefinitionException(selector, requiredType);
			}
			T beanInstance = getBeanInstance(parentInjectionContext, registration, args);
			return (R) beanInstance;
		}
		if (this.parent != null) {
			if (this.parent instanceof DefaultFunctionalBeanFactory) {
				return ((DefaultFunctionalBeanFactory) this.parent).resolveBean(null, selector, args, requiredType,
						nonUniqueAsNull);
			}
			try {
				if (args != null) {
					return (R) this.parent.getBean(selector, args);
				}
				return this.parent.getBean(selector, requiredType);
			}
			catch (NoUniqueBeanDefinitionException ex) {
				if (nonUniqueAsNull) {
					return null;
				}
				throw ex;
			}
		}
		return null;
	}

	private <T> Stream<T> resolveStream(@Nullable FunctionalInjectionContext parentInjectionContext,
			BeanSelector<T> selector, boolean allowEagerInit) {
		Stream<T> stream = this.registrations.stream(selector).map(
				registration -> getBeanInstance(parentInjectionContext, registration, null));
		if (this.parent != null) {
			Stream<T> parentStream = (this.parent instanceof DefaultFunctionalBeanFactory)
					? ((DefaultFunctionalBeanFactory) this.parent).resolveStream(null, selector, allowEagerInit)
					: this.parent.getBeanProvider(selector, allowEagerInit).stream();
			stream = Stream.concat(stream, parentStream);
		}
		return stream;
	}

	private <T> Stream<T> resolveOrderedStream(@Nullable FunctionalInjectionContext parentInjectionContext,
			BeanSelector<T> selector, boolean allowEagerInit) {
		List<T> beans = new ArrayList<>();
		Map<Object, FunctionalBeanDefinition<?>> instancesToDefinitions = new IdentityHashMap<>();
		this.registrations.stream(selector).forEach(registration -> {
			T instance = getBeanInstance(parentInjectionContext, registration, null);
			beans.add(instance);
			instancesToDefinitions.put(instance, registration.getDefinition());
		});
		FunctionalBeanFactory beanFactory = this.parent;
		while (beanFactory != null) {
			Map<String, T> localBeans = beanFactory.getBeans(selector);
			beans.addAll(localBeans.values());
			instancesToDefinitions.putAll(getInstancesToDefinitionsMap(beanFactory, localBeans));
			beanFactory = getParentBeanFactory(beanFactory);
		}
		beans.sort(adaptOrderComparator(instancesToDefinitions));
		return beans.stream();
	}

	private <T> Map<Object, FunctionalBeanDefinition<?>> getInstancesToDefinitionsMap(FunctionalBeanFactory beanFactory,
			Map<String, ?> beans) {
		IdentityHashMap<Object, FunctionalBeanDefinition<?>> instancesToDefinitions = new IdentityHashMap<>();
		beans.forEach((name, instance) -> instancesToDefinitions.put(instance, beanFactory.getBeanDefinition(name)));
		return instancesToDefinitions;
	}

	private Comparator<Object> adaptOrderComparator(Map<?, FunctionalBeanDefinition<?>> instancesToDefinitions) {
		Comparator<Object> dependencyComparator = this.dependencyComparator;
		OrderComparator comparator = (dependencyComparator instanceof OrderComparator
				? (OrderComparator) dependencyComparator
				: OrderComparator.INSTANCE);
		return comparator.withSourceProvider(
				new FunctionalBeanDefinitionAwareOrderSourceProvider(instancesToDefinitions));
	}

	private FunctionalBeanFactory getParentBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof HierarchicalBeanFactory) {
			BeanFactory parentBeanFactory = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
			Assert.isInstanceOf(FunctionalBeanFactory.class, parentBeanFactory);
			return (FunctionalBeanFactory) parentBeanFactory;
		}
		return null;
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(BeanSelector<T> selector, boolean allowEagerInit)
			throws BeansException {
		return getBeanProvider(null, selector, allowEagerInit);
	}

	private <T> ObjectProvider<T> getBeanProvider(@Nullable FunctionalInjectionContext parentInjectionContext,
			BeanSelector<T> selector, boolean allowEagerInit) {
		return new FunctionalObjectProvider<>(parentInjectionContext, selector, allowEagerInit);
	}

	@Override
	public <T> boolean containsBean(BeanSelector<T> selector) {
		if (containsBeanDefinition(selector)) {
			return true;
		}
		return (this.parent != null && this.parent.containsBean(selector));
	}

	@Override
	public <T> boolean isSingleton(BeanSelector<T> selector) {
		FunctionalBeanRegistration<T> registration = this.registrations.find(selector, false);
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
		FunctionalBeanRegistration<T> registration = this.registrations.find(selector, false);
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
		FunctionalBeanRegistration<T> registration = this.registrations.find(selector, false);
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
		FunctionalBeanRegistration<T> registration = this.registrations.find(selector, false);
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
		FunctionalBeanRegistration<T> registration = this.registrations.find(selector, false);
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
		return this.registrations.anyMatch(selector);
	}

	@Override
	public int getBeanDefinitionCount() {
		return this.registrations.size();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return this.registrations.getNames();
	}

	@Override
	public FunctionalBeanDefinition<?> getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		FunctionalBeanRegistration<Object> registration = this.registrations.find(BeanSelector.byName(beanName), false);
		if (registration != null) {
			return registration.getDefinition();
		}
		throw new NoSuchBeanDefinitionException(beanName);
	}

	@Override
	public <T> String[] getBeanNames(BeanSelector<T> selector, boolean allowEagerInit) {
		return this.registrations.stream(selector).map(this::getName).toArray(String[]::new);
	}

	@Override
	public <T> Map<String, T> getBeans(BeanSelector<T> selector, boolean allowEagerInit) throws BeansException {
		Map<String, T> beans = new LinkedHashMap<>();
		this.registrations.stream(selector).forEach(registration -> {
			String name = getName(registration);
			T instance = getBeanInstance(null, registration, null);
			beans.put(name, instance);
		});
		return Collections.unmodifiableMap(beans);
	}

	private <T> String getName(FunctionalBeanRegistration<T> registration) {
		return registration.getDefinition().getName();
	}

	private <T> T getBeanInstance(FunctionalInjectionContext parentInjectionContext,
			FunctionalBeanRegistration<T> registration, Object[] args) {
		return registration.getBeanInstance(new FunctionalInjectionContext(parentInjectionContext, registration, args));
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

	public static class Builder {

		// setParent
		// registerScope
		// addEmbeddedValueResolver

	}

	private class FunctionalInjectionContext implements InjectionContext {

		private final FunctionalInjectionContext parent;

		private final FunctionalBeanRegistration<?> registration;

		@Nullable
		private Object[] args;

		FunctionalInjectionContext(FunctionalInjectionContext parent, FunctionalBeanRegistration<?> registration,
				@Nullable Object[] args) {
			assertNotCurrentlyInCreation(parent, registration);
			this.parent = parent;
			this.registration = registration;
			this.args = args;
		}

		private void assertNotCurrentlyInCreation(FunctionalInjectionContext injectionContext,
				FunctionalBeanRegistration<?> registration) {
			while (injectionContext != null) {
				if (injectionContext.registration.equals(registration)) {
					throw new BeanCurrentlyInCreationException(registration.getDefinition().getName());
				}
				injectionContext = injectionContext.parent;
			}
		}

		public <T> T getBean(BeanSelector<T> selector) {
			return DefaultFunctionalBeanFactory.this.resolveBean(this, selector, null, null, false);
		}

		@Override
		public <S, T> T getBean(BeanSelector<S> selector, Class<T> requiredType) throws BeansException {
			return DefaultFunctionalBeanFactory.this.resolveBean(this, selector, null, requiredType, false);
		}

		@Override
		public <T> T getBean(BeanSelector<T> selector, Object... args) throws BeansException {
			return DefaultFunctionalBeanFactory.this.resolveBean(this, selector, args, null, false);
		}

		@Override
		public <T> ObjectProvider<T> getBeanProvider(BeanSelector<T> selector) {
			return DefaultFunctionalBeanFactory.this.getBeanProvider(this, selector, true);
		}

		@Override
		public String resolveEmbeddedValue(String value) {
			return DefaultFunctionalBeanFactory.this.resolveEmbeddedValue(value);
		}

		@Override
		public Object[] getArgs() {
			return this.args;
		}

	}

	private class FunctionalObjectProvider<T> implements ObjectProvider<T> {

		private final FunctionalInjectionContext parentInjectionContext;

		private final BeanSelector<T> selector;

		private final boolean allowEagerInit;

		FunctionalObjectProvider(FunctionalInjectionContext parentInjectionContext, BeanSelector<T> selector,
				boolean allowEagerInit) {
			this.parentInjectionContext = parentInjectionContext;
			this.selector = selector;
			this.allowEagerInit = allowEagerInit;
		}

		@Override
		public T getObject() throws BeansException {
			return resolveBean(this.parentInjectionContext, this.selector, null, null);
		}

		@Override
		public T getObject(Object... args) throws BeansException {
			return resolveBean(this.parentInjectionContext, this.selector, args, null);
		}

		@Override
		public T getIfAvailable() throws BeansException {
			return resolveBeanIfAvailable(this.parentInjectionContext, this.selector, null, null);
		}

		@Override
		public T getIfUnique() throws BeansException {
			return resolveBeanIfUnique(this.parentInjectionContext, this.selector, null, null);
		}

		@Override
		public Stream<T> stream() {
			return resolveStream(this.parentInjectionContext, this.selector, this.allowEagerInit);
		}

		@Override
		public Stream<T> orderedStream() {
			return resolveOrderedStream(this.parentInjectionContext, this.selector, this.allowEagerInit);
		}

	}

	private static class FunctionalBeanDefinitionAwareOrderSourceProvider
			implements OrderComparator.OrderSourceProvider {

		private final Map<?, FunctionalBeanDefinition<?>> instancesToDefinitions;

		FunctionalBeanDefinitionAwareOrderSourceProvider(Map<?, FunctionalBeanDefinition<?>> instancesToDefinitions) {
			this.instancesToDefinitions = instancesToDefinitions;
		}

		@Override
		public Object getOrderSource(Object obj) {
			FunctionalBeanDefinition<?> definition = this.instancesToDefinitions.get(obj);
			return (definition != null) ? definition.getOrderSource() : null;
		}

	}

}
