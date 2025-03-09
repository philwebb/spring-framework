/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.service.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanRegistry;
import org.springframework.beans.factory.BeanRegistry.SupplierContext;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.invoker.HttpServiceProxyFactory.Builder;
import org.springframework.web.service.registry.AbstractHttpServiceProxyRegistry.Spec;

/**
 * Base class for programmatic HTTP service proxy bean registration capabilities.
 *
 * To create an instance, see {@link HttpServiceProxyRegistry}, or
 * {@link org.springframework.web.client.support.RestClientHttpServiceProxyRegistry}, or
 * {@link org.springframework.web.reactive.function.client.support.WebClientHttpServiceProxyRegistry}.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 7.0
 * @param <S> the spec type used when customizing the proxy
 * @param <C> the client used when creating the proxy (usually a builder)
 */
public abstract class AbstractHttpServiceProxyRegistry<S extends Spec<S>, C> {

	private final BeanRegistry beanRegistry;

	private final ClientSupplier<C> clientSupplier;

	/**
	 * Create a new {@link AbstractHttpServiceProxyRegistry} instance for the given
	 * {@link BeanRegistry} without any group support.
	 * @param beanRegistry the bean registry used to register beans
	 */
	protected AbstractHttpServiceProxyRegistry(BeanRegistry beanRegistry) {
		this(beanRegistry, ClientSupplier.usingBean());
	}

	/**
	 * Create a new {@link AbstractHttpServiceProxyRegistry} instance with group support
	 * provided by the given type and lookup function.
	 * @param <B>
	 * @param beanRegistry the bean registry used to register beans
	 * @param groupLookupBeanType the type of bean used to support group lookup
	 * @param groupLookup a function that given the group lookup bean and a group ID will
	 * return the client
	 */
	protected <B> AbstractHttpServiceProxyRegistry(BeanRegistry beanRegistry, Class<B> groupLookupBeanType,
			BiFunction<B, String, C> groupLookup) {
		this(beanRegistry, ClientSupplier.usingBean(groupLookupBeanType, groupLookup));
	}

	/**
	 * Create a new {@link AbstractHttpServiceProxyRegistry} instance with group support
	 * provided by the given client supplier.
	 * @param beanRegistry the bean registry used to register beans
	 * @param clientSupplier the supplier that will return clients
	 */
	protected AbstractHttpServiceProxyRegistry(BeanRegistry beanRegistry, ClientSupplier<C> clientSupplier) {
		Assert.notNull(beanRegistry, "'beanRegistry' must not be null");
		Assert.notNull(clientSupplier, "'clientSupplier' must not be null");
		this.beanRegistry = beanRegistry;
		this.clientSupplier = clientSupplier;
	}

	/**
	 * Register the given service types as beans.
	 * @param serviceTypes the service types to register
	 * @return a set containing the generated bean names
	 */
	public Set<String> registerBeans(Class<?>... serviceTypes) {
		return doRegisterBeans(null, Set.of(serviceTypes), null, null);
	}

	/**
	 * Register the given service types as beans using a specific group.
	 * @param serviceTypes the service types to register
	 * @return a set containing the generated bean names
	 */
	public Set<String> registerBeans(String groupId, Class<?>... serviceTypes) {
		Assert.hasText(groupId, "'groupId' must not be empty");
		return doRegisterBeans(groupId, Set.of(serviceTypes), null, null);
	}

	/**
	 * Register the given service types as beans.
	 * @param serviceTypes the service types to register
	 * @return a set containing the generated bean names
	 * @see HttpServiceTypes
	 */
	public Set<String> registerBeans(Iterable<Class<?>> serviceTypes) {
		Assert.notNull(serviceTypes, "'serviceTypes' must not be null");
		return doRegisterBeans(null, serviceTypes, null, null);
	}

	/**
	 * Register the given service types as beans using a specific group.
	 * @param groupId the group ID used to lookup the client
	 * @param serviceTypes the service types to register
	 * @return a set containing the generated bean names
	 * @see HttpServiceTypes
	 */
	public Set<String> registerBeans(String groupId, Iterable<Class<?>> serviceTypes) {
		Assert.hasText(groupId, "'groupId' must not be empty");
		Assert.notNull(serviceTypes, "'serviceTypes' must not be null");
		return doRegisterBeans(groupId, serviceTypes, null, null);
	}

	/**
	 * Register the given service types as beans.
	 * @param serviceTypes the service types to register
	 * @param proxyCustomizer callback to customize the generated proxy
	 * @return a set containing the generated bean names
	 * @see HttpServiceTypes
	 */
	public Set<String> registerBeans(Iterable<Class<?>> serviceTypes, BiConsumer<Class<?>, S> proxyCustomizer) {
		Assert.notNull(serviceTypes, "'serviceTypes' must not be null");
		Assert.notNull(proxyCustomizer, "'proxyCustomizer' must not be null");
		return doRegisterBeans(null, serviceTypes, proxyCustomizer, null);
	}

	/**
	 * Register the given service types as beans using a specific group.
	 * @param groupId the group ID used to lookup the client
	 * @param serviceTypes the service types to register
	 * @param proxyCustomizer callback to customize the generated proxy
	 * @return a set containing the generated bean names
	 * @see HttpServiceTypes
	 */
	public Set<String> registerBeans(String groupId, Iterable<Class<?>> serviceTypes,
			BiConsumer<Class<?>, S> proxyCustomizer) {
		Assert.hasText(groupId, "'groupId' must not be empty");
		Assert.notNull(serviceTypes, "'serviceTypes' must not be null");
		Assert.notNull(proxyCustomizer, "'proxyCustomizer' must not be null");
		return doRegisterBeans(groupId, serviceTypes, proxyCustomizer, null);
	}

	/**
	 * Register the given service types as beans.
	 * @param serviceTypes the service types to register
	 * @param proxyCustomizer callback to customize the generated proxy
	 * @return a set containing the generated bean names
	 * @see HttpServiceTypes
	 */
	public Set<String> registerBeans(Iterable<Class<?>> serviceTypes, BiConsumer<Class<?>, S> proxyCustomizer,
			BiConsumer<Class<?>, BeanRegistry.Spec<?>> beanCustomizer) {
		Assert.notNull(serviceTypes, "'serviceTypes' must not be null");
		Assert.notNull(proxyCustomizer, "'proxyCustomizer' must not be null");
		Assert.notNull(beanCustomizer, "'beanCustomizer' must not be null");
		return doRegisterBeans(null, serviceTypes, proxyCustomizer, beanCustomizer);
	}

	/**
	 * Register the given service types as beans using a specific group.
	 * @param groupId the group ID used to lookup the client
	 * @param serviceTypes the service types to register
	 * @param proxyCustomizer callback to customize the generated proxy
	 * @param beanCustomizer callback to customize bean properties than the name and
	 * supplier
	 * @return a set containing the generated bean names
	 * @see HttpServiceTypes
	 */
	public Set<String> registerBeans(String groupId, Iterable<Class<?>> serviceTypes,
			BiConsumer<Class<?>, S> proxyCustomizer, BiConsumer<Class<?>, BeanRegistry.Spec<?>> beanCustomizer) {
		Assert.hasText(groupId, "'groupId' must not be empty");
		Assert.notNull(serviceTypes, "'serviceTypes' must not be null");
		Assert.notNull(proxyCustomizer, "'proxyCustomizer' must not be null");
		Assert.notNull(beanCustomizer, "'beanCustomizer' must not be null");
		return doRegisterBeans(groupId, serviceTypes, proxyCustomizer, beanCustomizer);
	}

	/**
	 * Register the given service type as a bean.
	 * @param serviceType the service type to register
	 * @return the generated bean name
	 */
	public <T> String registerBean(Class<T> serviceType) {
		Assert.notNull(serviceType, "'serviceType' must not be null");
		return doRegisterBean(null, serviceType, null, null);
	}

	/**
	 * Register the given service type as a bean using a specific group.
	 * @param groupId the group ID used to lookup the client
	 * @param serviceType the service type to register
	 * @return the generated bean name
	 */
	public <T> String registerBean(String groupId, Class<T> serviceType) {
		Assert.hasText(groupId, "'groupId' must not be empty");
		Assert.notNull(serviceType, "'serviceType' must not be null");
		return doRegisterBean(groupId, serviceType, null, null);
	}

	/**
	 * Register the given service type as a bean.
	 * @param serviceType the service type to register
	 * @param proxyCustomizer callback to customize the generated proxy
	 * @return the generated bean name
	 */
	public <T> String registerBean(Class<T> serviceType, Consumer<S> proxyCustomizer) {
		Assert.notNull(serviceType, "'serviceType' must not be null");
		Assert.notNull(proxyCustomizer, "'proxyCustomizer' must not be null");
		return doRegisterBean(null, serviceType, proxyCustomizer, null);
	}

	/**
	 * Register the given service type as a bean using a specific group.
	 * @param groupId the group ID used to lookup the client
	 * @param serviceType the service type to register
	 * @param proxyCustomizer callback to customize the generated proxy
	 * @return the generated bean name
	 */
	public <T> String registerBean(String groupId, Class<T> serviceType, Consumer<S> proxyCustomizer) {
		Assert.hasText(groupId, "'groupId' must not be empty");
		Assert.notNull(serviceType, "'serviceType' must not be null");
		Assert.notNull(proxyCustomizer, "'proxyCustomizer' must not be null");
		return doRegisterBean(groupId, serviceType, proxyCustomizer, null);
	}

	/**
	 * Register the given service type as a bean.
	 * @param serviceType the service type to register
	 * @param proxyCustomizer callback to customize the generated proxy
	 * @param beanCustomizer callback to customize bean properties than the name and
	 * supplier
	 * @return the generated bean name
	 */
	public <T> String registerBean(Class<T> serviceType, Consumer<S> proxyCustomizer,
			Consumer<BeanRegistry.Spec<T>> beanCustomizer) {
		Assert.notNull(serviceType, "'serviceType' must not be null");
		Assert.notNull(proxyCustomizer, "'proxyCustomizer' must not be null");
		Assert.notNull(beanCustomizer, "'beanCustomizer' must not be null");
		return doRegisterBean(null, serviceType, proxyCustomizer, beanCustomizer);
	}

	/**
	 * Register the given service type as a bean using a specific group.
	 * @param groupId the group ID used to lookup the client
	 * @param serviceType the service type to register
	 * @param proxyCustomizer callback to customize the generated proxy
	 * @param beanCustomizer callback to customize bean properties than the name and
	 * supplier
	 * @return the generated bean name
	 */
	public <T> String registerBean(String groupId, Class<T> serviceType, Consumer<S> proxyCustomizer,
			Consumer<BeanRegistry.Spec<T>> beanCustomizer) {
		Assert.hasText(groupId, "'groupId' must not be empty");
		Assert.notNull(serviceType, "'serviceType' must not be null");
		Assert.notNull(proxyCustomizer, "'proxyCustomizer' must not be null");
		Assert.notNull(beanCustomizer, "'beanCustomizer' must not be null");
		return doRegisterBean(groupId, serviceType, proxyCustomizer, beanCustomizer);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Set<String> doRegisterBeans(@Nullable String groupId, Iterable<Class<?>> serviceTypes,
			@Nullable BiConsumer<Class<?>, S> proxyCustomizer,
			@Nullable BiConsumer<Class<?>, BeanRegistry.Spec<?>> beanCustomizer) {
		Set<String> beanNames = new LinkedHashSet<>();
		for (Class<?> serviceType : serviceTypes) {
			String beanName = doRegisterBean(groupId, serviceType, asBiConsumer(serviceType, proxyCustomizer),
					(Consumer) asBiConsumer(serviceType, beanCustomizer));
			beanNames.add(beanName);
		}
		return Collections.unmodifiableSet(beanNames);
	}

	private <T> String doRegisterBean(@Nullable String groupId, Class<T> serviceType,
			@Nullable Consumer<S> proxyCustomizer, @Nullable Consumer<BeanRegistry.Spec<T>> beanCustomizer) {
		return this.beanRegistry.registerBean(serviceType, (beanSpec) -> {
			S spec = createSpec(this.clientSupplier);
			customize(proxyCustomizer, spec);
			customize(beanCustomizer, beanSpec);
			ProxySupplier proxySupplier = getProxySupplier(spec);
			beanSpec.supplier((supplierContext) -> proxySupplier.createProxy(supplierContext, groupId, serviceType));
		});
	}

	private <T> void customize(@Nullable Consumer<T> customizer, T t) {
		if (customizer != null) {
			customizer.accept(t);
		}
	}

	private <T> @Nullable Consumer<T> asBiConsumer(Class<?> serviceType, @Nullable BiConsumer<Class<?>, T> customizer) {
		return (customizer != null) ? (t) -> customizer.accept(serviceType, t) : null;
	}

	/**
	 * Factory method used to create the spec. Usually returns a
	 * {@link SpecifiedProxySupplier} instance.
	 * @param clientSupplier the client supplier
	 * @return a new spec instance for further customization
	 */
	protected abstract S createSpec(ClientSupplier<C> clientSupplier);

	/**
	 * Return the {@link ProxySupplier} to use for the given spec. By default this method
	 * assumes that the spec can be cast to a {@link ProxySupplier}.
	 * @param spec the spec
	 * @return a proxy supplier
	 */
	protected ProxySupplier getProxySupplier(S spec) {
		return ((ProxySupplier) spec);
	}

	/**
	 * Specification for customizing the way that proxies are created.
	 */
	public interface Spec<S extends Spec<S>> {

		/**
		 * Add a customizer that will be applied to the
		 * {@link HttpServiceProxyFactory.Builder} before the proxy is created.
		 * @param customizer the customizer to apply.
		 */
		S proxyFactory(Consumer<HttpServiceProxyFactory.Builder> customizer);

	}

	/**
	 * Supplies a fully created proxy.
	 */
	@FunctionalInterface
	protected interface ProxySupplier {

		/**
		 * Return a new proxy instance.
		 * @param <T> the service type
		 * @param supplierContext the supplier context which may be used to obtain
		 * depenencies
		 * @param groupId the group ID requested or {@code null} if no group is being used
		 * @param serviceType the service type to create
		 * @return a new proxy instance
		 */
		<T> T createProxy(SupplierContext supplierContext, @Nullable String groupId, Class<T> serviceType);

	}

	/**
	 * Interface used to supply the client, usually from the {@link SupplierContext}.
	 *
	 * @param <C> the client type
	 */
	@FunctionalInterface
	public interface ClientSupplier<C> {

		/**
		 * Return the client from the given context.
		 * @param supplierContext the supplier context
		 * @param clientType the client type request
		 * @param groupId the group ID requested or {@code null} if no group is being used
		 * @return a client instance
		 * @throws NoSuchBeanDefinitionException if no client bean can be found
		 * @throws NoSuchElementException if no group lookup element can be found
		 */
		C getClient(SupplierContext supplierContext, Class<C> clientType, @Nullable String groupId)
				throws NoSuchBeanDefinitionException, NoSuchElementException;

		/**
		 * Factory method that returns a {@link ClientSupplier} that looks up the client
		 * directly by type from the {@link SupplierContext}. This supplier does not
		 * support groups.
		 * @param <C> the client type
		 * @return a new {@link ClientSupplier} instance.
		 */
		static <C> ClientSupplier<C> usingBean() {
			return (supplierContext, clientType, groupId) -> {
				Assert.state(!StringUtils.hasText(groupId),
						"Group lookups cannot be performed by this client supplier");
				return supplierContext.bean(clientType);
			};
		}

		/**
		 * Factory method that returns a {@link ClientSupplier} that looks up the client
		 * either directly by type, or by using a group lookup bean.
		 * @param <C>
		 * @param <B>
		 * @param groupLookupBeanType the bean that will be used to lookup the client for
		 * a group
		 * @param groupLookup the function used to perform the lookup (usually a method
		 * reference)
		 * @return
		 */
		static <C, B> ClientSupplier<C> usingBean(Class<B> groupLookupBeanType, BiFunction<B, String, C> groupLookup) {
			Assert.notNull(groupLookupBeanType, "'groupLookupBeanType' must not be null");
			Assert.notNull(groupLookup, "'groupLookup' must not be null");
			return (supplierContext, clientType, groupId) -> {
				if (!StringUtils.hasText(groupId)) {
					return supplierContext.bean(clientType);
				}
				B lookupBean = supplierContext.bean(groupLookupBeanType);
				C client = groupLookup.apply(lookupBean, groupId);
				if (client == null) {
					throw new NoSuchElementException("Group '%s' is not available".formatted(groupId));
				}
				Assert.isInstanceOf(clientType, client);
				return client;
			};
		}

	}

	/**
	 * A {@link ProxySupplier} based around a configured {@link Spec}.
	 *
	 * @param <C> the client type
	 * @see AbstractHttpServiceProxyRegistry#createSpec(ClientSupplier)
	 */
	protected static abstract class SpecifiedProxySupplier<S extends Spec<S>, C> implements Spec<S>, ProxySupplier {

		private final ClientSupplier<C> clientSupplier;

		private final Class<C> clientType;

		private final List<Consumer<HttpServiceProxyFactory.Builder>> proxyFactoryCustomizers = new ArrayList<>();

		/**
		 * Create a new {@link SpecifiedProxySupplier} instance.
		 * @param clientSupplier the client supplier
		 * @param clientType the client type
		 */
		protected SpecifiedProxySupplier(ClientSupplier<C> clientSupplier, Class<C> clientType) {
			this.clientSupplier = clientSupplier;
			this.clientType = clientType;
		}

		@Override
		@SuppressWarnings("unchecked")
		public S proxyFactory(Consumer<Builder> customizer) {
			this.proxyFactoryCustomizers.add(customizer);
			return (S) this;
		}

		@Override
		public <T> T createProxy(SupplierContext supplierContext, @Nullable String groupId, Class<T> serviceType) {
			Supplier<C> clientSupplier = () -> this.clientSupplier.getClient(supplierContext, this.clientType, groupId);
			HttpServiceProxyFactory.Builder builder = getProxyFactoryBuilder(clientSupplier);
			this.proxyFactoryCustomizers.forEach((customizer) -> customizer.accept(builder));
			HttpServiceProxyFactory factory = builder.build();
			return factory.createClient(serviceType);
		}

		/**
		 * Strategy used to return the fully-configured
		 * {@link HttpServiceProxyFactory.Builder}. By default this method will create the
		 * builder using the result of {@link #getHttpExchangeAdapter(Supplier)}.
		 */
		protected HttpServiceProxyFactory.Builder getProxyFactoryBuilder(Supplier<C> clientSupplier) {
			HttpExchangeAdapter adapter = getHttpExchangeAdapter(clientSupplier);
			return HttpServiceProxyFactory.builderFor(adapter);
		}

		/**
		 * Strategy used to return a fully-configured {@link HttpExchangeAdapter} used to
		 * create the {@link HttpServiceProxyFactory.Builder}.
		 * @param clientSupplier the client supplier
		 * @return a {@link HttpExchangeAdapter}
		 */
		protected abstract HttpExchangeAdapter getHttpExchangeAdapter(Supplier<C> clientSupplier);

	}

}
