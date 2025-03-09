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

package org.springframework.web.client.support;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.beans.factory.BeanRegistry;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceProxyRegistry.Spec;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.registry.AbstractHttpServiceProxyRegistry;

/**
 * Programmatic HTTP service proxy bean registration capabilities using
 * {@link RestClient.Builder} beans.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 7.0
 */
public class RestClientHttpServiceProxyRegistry extends AbstractHttpServiceProxyRegistry<Spec, RestClient.Builder> {

	/**
	 * Create a new {@link RestClientHttpServiceProxyRegistry} instance for the given
	 * {@link BeanRegistry} without any group support.
	 * @param beanRegistry the bean registry used to register beans
	 */
	public <B> RestClientHttpServiceProxyRegistry(BeanRegistry beanRegistry) {
		super(beanRegistry);
	}

	/**
	 * Create a new {@link RestClientHttpServiceProxyRegistry} instance with group support
	 * provided by the given type and lookup function.
	 * @param <B>
	 * @param beanRegistry the bean registry used to register beans
	 * @param groupLookup a function that given the group lookup bean and a group ID will
	 * return the client
	 */
	public <B> RestClientHttpServiceProxyRegistry(BeanRegistry beanRegistry, Class<B> beanType,
			BiFunction<B, String, RestClient.Builder> lookup) {
		super(beanRegistry, beanType, lookup);
	}

	/**
	 * Create a new {@link RestClientHttpServiceProxyRegistry} instance with group support
	 * provided by the given client supplier.
	 * @param beanRegistry the bean registry used to register beans
	 * @param clientSupplier the supplier that will return clients
	 */
	public RestClientHttpServiceProxyRegistry(BeanRegistry beanRegistry, ClientSupplier<RestClient.Builder> clientSupplier) {
		super(beanRegistry, clientSupplier);
	}

	@Override
	protected Spec createSpec(ClientSupplier<RestClient.Builder> clientSupplier) {
		return new SpecifiedProxySupplier(clientSupplier);
	}

	/**
	 * Specification for customizing the way that proxies are created.
	 */
	public interface Spec extends AbstractHttpServiceProxyRegistry.Spec<Spec> {

		/**
		 * Set the base URL to use
		 * @param baseUrl the base URL to use
		 */
		default Spec baseUrl(String baseUrl) {
			return restClient((builder) -> builder.baseUrl(baseUrl));
		}

		/**
		 * Set the base URL to use
		 * @param baseUrl the base URL to use
		 */
		default Spec baseUrl(URI baseUrl) {
			return restClient((builder) -> builder.baseUrl(baseUrl));
		}

		/**
		 * Add a customizer that will be applied to the {@link RestClient.Builder} before
		 * the proxy is created.
		 * @param customizer the customizer to apply.
		 */
		Spec restClient(Consumer<RestClient.Builder> customizer);

	}

	/**
	 * A {@link ProxySupplier} based around a configured {@link Spec}.
	 */
	static class SpecifiedProxySupplier extends AbstractHttpServiceProxyRegistry.SpecifiedProxySupplier<Spec, RestClient.Builder>
			implements Spec {

		private final List<Consumer<RestClient.Builder>> restClientCustomizers = new ArrayList<>();

		SpecifiedProxySupplier(ClientSupplier<RestClient.Builder> clientSupplier) {
			super(clientSupplier, RestClient.Builder.class);
		}

		@Override
		public Spec restClient(Consumer<RestClient.Builder> customizer) {
			this.restClientCustomizers.add(customizer);
			return this;
		}

		@Override
		protected HttpExchangeAdapter getHttpExchangeAdapter(Supplier<RestClient.Builder> clientSupplier) {
			RestClient.Builder builder = clientSupplier.get();
			this.restClientCustomizers.forEach((customizer) -> customizer.accept(builder));
			return RestClientAdapter.create(builder.build());
		}

	}

}
