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

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanRegistry;
import org.springframework.beans.factory.BeanRegistry.Spec;
import org.springframework.beans.factory.BeanRegistry.SupplierContext;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyBeanRegistrar;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.invoker.HttpServiceProxyFactory.Builder;

/**
 * {@link HttpServiceProxyBeanRegistrar} that can be used to add HTTP proxy beans backed
 * by a {@link WebClient}.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 7.0
 */
public final class WebClientHttpServiceProxies extends HttpServiceProxyBeanRegistrar<WebClientHttpServiceProxies> {

	private static final WebClientHttpServiceProxies EMPTY = new WebClientHttpServiceProxies();

	private final List<BiConsumer<WebClient.Builder, SupplierContext>> webClientCustomizers;

	private WebClientHttpServiceProxies() {
		this.webClientCustomizers = Collections.emptyList();
	}

	private WebClientHttpServiceProxies(List<Class<?>> types, List<BasePackage> basePackages, @Nullable String using,
			List<BiConsumer<HttpServiceProxyFactory.Builder, SupplierContext>> httpServiceProxyFactoryCustomizers,
			List<BiConsumer<BeanRegistry.Spec<?>, Class<?>>> beanRegistrations,
			List<BiConsumer<WebClient.Builder, SupplierContext>> webClientCustomizers) {
		super(types, basePackages, using, httpServiceProxyFactoryCustomizers, beanRegistrations);
		this.webClientCustomizers = webClientCustomizers;
	}

	/**
	 * Return a new instance of this registrar that additionally sets the base URL of the
	 * {@link WebClient}.
	 * @param baseUrl the base URL to use
	 * @return a new registrar instance
	 */
	public WebClientHttpServiceProxies baseUrl(String baseUrl) {
		return webClient((restClient) -> restClient.baseUrl(baseUrl));
	}

	/**
	 * Return a new instance of this registrar that additionally applies the given
	 * customizer to the {@link WebClient} builder.
	 * @param customizer the customizer to add
	 * @return a new registrar instance
	 */
	public WebClientHttpServiceProxies webClient(Consumer<WebClient.Builder> customizer) {
		return webClient(asBiConsumer(customizer));
	}

	/**
	 * Return a new instance of this registrar that additionally applies the given
	 * customizer to the {@link WebClient} builder.
	 * @param customizer the customizer to add
	 * @return a new registrar instance
	 */
	public WebClientHttpServiceProxies webClient(BiConsumer<WebClient.Builder, SupplierContext> customizer) {
		return copy(merge(webClientCustomizers, customizer));
	}

	@Override
	protected HttpExchangeAdapter supplyHttpExchangeAdapter(SupplierContext supplierContext) {
		WebClient restClient = supplyWebClient(supplierContext);
		return WebClientAdapter.create(restClient);
	}

	private WebClient supplyWebClient(SupplierContext supplierContext) {
		WebClient.Builder builder = WebClient.builder();
		this.webClientCustomizers.forEach((customizer) -> customizer.accept(builder, supplierContext));
		return builder.build();
	}

	@Override
	protected WebClientHttpServiceProxies copy(List<Class<?>> types, List<BasePackage> basePackages,
			@Nullable String using, List<BiConsumer<Builder, SupplierContext>> httpServiceProxyFactoryCustomizers,
			List<BiConsumer<Spec<?>, Class<?>>> beanRegistrations) {
		return new WebClientHttpServiceProxies(types, basePackages, using, httpServiceProxyFactoryCustomizers,
				beanRegistrations, this.webClientCustomizers);
	}

	private WebClientHttpServiceProxies copy(
			List<BiConsumer<WebClient.Builder, SupplierContext>> restClientCustomizers) {
		return new WebClientHttpServiceProxies(this.types, this.basePackages, this.using,
				this.httpServiceProxyFactoryCustomizers, this.beanRegistrationCustomizers, webClientCustomizers);
	}

	/**
	 * Factory method that creates a new {@link WebClientHttpServiceProxies} and
	 * {@link #add(Class...) adds} the given HTTP service types
	 * @param types the types to add
	 * @return a new registrar instance
	 */
	public static WebClientHttpServiceProxies of(Class<?>... types) {
		return of().add(types);
	}

	/**
	 * Factory method that creates a new {@link WebClientHttpServiceProxies} and
	 * {@link #scan(Class...) scans} the given base packages.
	 * @param basePackages the base packages to scan
	 * @return a new registrar instance
	 */
	public static WebClientHttpServiceProxies ofScan(String... basePackages) {
		return of().scan(basePackages);
	}

	/**
	 * Factory method that creates a new {@link WebClientHttpServiceProxies} and
	 * {@link #scan(Class...) scans} the given base packages.
	 * @param basePackages the base packages to scan
	 * @return a new registrar instance
	 */
	public static WebClientHttpServiceProxies ofScan(Class<?>... basePackages) {
		return of().scan(basePackages);
	}

	/**
	 * Factory method that creates a new {@link WebClientHttpServiceProxies} and
	 * {@link #scan(Class...) scans} the given base packages.
	 * @param basePackages the base packages to scan
	 * @return a new registrar instance
	 */
	public static WebClientHttpServiceProxies ofScan(BasePackage... basePackages) {
		return of().scan(basePackages);
	}

	/**
	 * Factory method that returns a empty {@link WebClientHttpServiceProxies}.
	 * @return an empty {@link WebClientHttpServiceProxies}
	 */
	public static WebClientHttpServiceProxies of() {
		return EMPTY;
	}

}
