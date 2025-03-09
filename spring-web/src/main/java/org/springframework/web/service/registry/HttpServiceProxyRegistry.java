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

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.springframework.beans.factory.BeanRegistry;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.HttpServiceProxyRegistry.Spec;

/**
 * Programmatic HTTP service proxy bean registration capabilities using
 * {@link HttpServiceProxyFactory.Builder} beans.
 *
 * @author Phillip Webb
 * @since 7.0
 */
public class HttpServiceProxyRegistry extends AbstractHttpServiceProxyRegistry<Spec, HttpServiceProxyFactory.Builder> {

	/**
	 * Create a new {@link HttpServiceProxyRegistry} instance for the given
	 * {@link BeanRegistry} without any group support.
	 * @param beanRegistry the bean registry used to register beans
	 */
	public <B> HttpServiceProxyRegistry(BeanRegistry beanRegistry) {
		super(beanRegistry, ClientSupplier.usingBean());
	}

	/**
	 * Create a new {@link HttpServiceProxyRegistry} instance with group support provided
	 * by the given type and lookup function.
	 * @param <B>
	 * @param beanRegistry the bean registry used to register beans
	 * @param groupLookup a function that given the group lookup bean and a group ID will
	 * return the client
	 */
	public <B> HttpServiceProxyRegistry(BeanRegistry beanRegistry, Class<B> beanType,
			BiFunction<B, String, HttpServiceProxyFactory.Builder> lookup) {
		super(beanRegistry, beanType, lookup);
	}

	/**
	 * Create a new {@link HttpServiceProxyRegistry} instance with group support provided
	 * by the given client supplier.
	 * @param beanRegistry the bean registry used to register beans
	 * @param clientSupplier the supplier that will return clients
	 */
	public HttpServiceProxyRegistry(BeanRegistry beanRegistry,
			ClientSupplier<HttpServiceProxyFactory.Builder> clientSupplier) {
		super(beanRegistry, clientSupplier);
	}

	@Override
	protected Spec createSpec(ClientSupplier<HttpServiceProxyFactory.Builder> clientSupplier) {
		return new SpecifiedProxySupplier(clientSupplier);
	}

	/**
	 * Specification for customizing the way that proxies are created.
	 */
	public interface Spec extends AbstractHttpServiceProxyRegistry.Spec<Spec> {

	}

	/**
	 * A {@link ProxySupplier} based around a configured {@link Spec}.
	 */
	static class SpecifiedProxySupplier
			extends AbstractHttpServiceProxyRegistry.SpecifiedProxySupplier<Spec, HttpServiceProxyFactory.Builder>
			implements Spec {

		SpecifiedProxySupplier(ClientSupplier<HttpServiceProxyFactory.Builder> clientSupplier) {
			super(clientSupplier, HttpServiceProxyFactory.Builder.class);
		}

		@Override
		protected HttpServiceProxyFactory.Builder getProxyFactoryBuilder(
				Supplier<HttpServiceProxyFactory.Builder> clientSupplier) {
			return clientSupplier.get();
		}

		@Override
		protected HttpExchangeAdapter getHttpExchangeAdapter(Supplier<HttpServiceProxyFactory.Builder> clientSupplier) {
			throw new IllegalStateException("HttpExchangeAdapter is not available for HttpServiceProxyFactory");
		}

	}

}
