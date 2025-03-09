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

package org.springframework.web.reactive.function.client.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.beans.factory.BeanRegistry;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceProxyRegistry.Spec;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.registry.AbstractHttpServiceProxyRegistry;

/**
 * Programmatic HTTP service proxy bean registration capabilities using
 * {@link WebClient.Builder} beans.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 7.0
 */
public class WebClientHttpServiceProxyRegistry extends AbstractHttpServiceProxyRegistry<Spec, WebClient.Builder> {

	/**
	 * Create a new {@link WebClientHttpServiceProxyRegistry} instance for the given
	 * {@link BeanRegistry} without any group support.
	 * @param beanRegistry the bean registry used to register beans
	 */
	public <B> WebClientHttpServiceProxyRegistry(BeanRegistry beanRegistry) {
		super(beanRegistry);
	}

	/**
	 * Create a new {@link WebClientHttpServiceProxyRegistry} instance with group support
	 * provided by the given type and lookup function.
	 * @param <B>
	 * @param beanRegistry the bean registry used to register beans
	 * @param groupLookup a function that given the group lookup bean and a group ID will
	 * return the client
	 */
	public <B> WebClientHttpServiceProxyRegistry(BeanRegistry beanRegistry, Class<B> beanType,
			BiFunction<B, String, WebClient.Builder> lookup) {
		super(beanRegistry, beanType, lookup);
	}

	/**
	 * Create a new {@link WebClientHttpServiceProxyRegistry} instance with group support
	 * provided by the given client supplier.
	 * @param beanRegistry the bean registry used to register beans
	 * @param clientSupplier the supplier that will return clients
	 */
	public WebClientHttpServiceProxyRegistry(BeanRegistry beanRegistry,
			ClientSupplier<WebClient.Builder> clientSupplier) {
		super(beanRegistry, clientSupplier);
	}

	@Override
	protected Spec createSpec(ClientSupplier<WebClient.Builder> clientSupplier) {
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
			return webClient((builder) -> builder.baseUrl(baseUrl));
		}

		/**
		 * Add a customizer that will be applied to the {@link WebClient.Builder} before
		 * the proxy is created.
		 * @param customizer the customizer to apply.
		 */
		Spec webClient(Consumer<WebClient.Builder> customizer);

	}

	/**
	 * A {@link ProxySupplier} based around a configured {@link Spec}.
	 */
	static class SpecifiedProxySupplier
			extends AbstractHttpServiceProxyRegistry.SpecifiedProxySupplier<Spec, WebClient.Builder> implements Spec {

		private final List<Consumer<WebClient.Builder>> webClientCustomizers = new ArrayList<>();

		SpecifiedProxySupplier(ClientSupplier<WebClient.Builder> clientSupplier) {
			super(clientSupplier, WebClient.Builder.class);
		}

		@Override
		public Spec webClient(Consumer<WebClient.Builder> customizer) {
			this.webClientCustomizers.add(customizer);
			return this;
		}

		@Override
		protected HttpExchangeAdapter getHttpExchangeAdapter(Supplier<WebClient.Builder> clientSupplier) {
			WebClient.Builder builder = clientSupplier.get();
			this.webClientCustomizers.forEach((customizer) -> customizer.accept(builder));
			return WebClientAdapter.create(builder.build());
		}

	}

}
