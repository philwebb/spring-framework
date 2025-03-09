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
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanRegistry;
import org.springframework.beans.factory.BeanRegistry.Spec;
import org.springframework.beans.factory.BeanRegistry.SupplierContext;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyBeanRegistrar;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.invoker.HttpServiceProxyFactory.Builder;

/**
 * {@link HttpServiceProxyBeanRegistrar} that can be used to add HTTP proxy beans backed
 * by a {@link RestClient}.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 7.0
 */
public final class RestClientHttpServiceProxies extends HttpServiceProxyBeanRegistrar<RestClientHttpServiceProxies> {

	private static final RestClientHttpServiceProxies EMPTY = new RestClientHttpServiceProxies();

	private final List<BiConsumer<RestClient.Builder, SupplierContext>> restClientCustomizers;

	private RestClientHttpServiceProxies() {
		this.restClientCustomizers = Collections.emptyList();
	}

	private RestClientHttpServiceProxies(List<Class<?>> types, List<BasePackage> basePackages, @Nullable String using,
			List<BiConsumer<HttpServiceProxyFactory.Builder, SupplierContext>> httpServiceProxyFactoryCustomizers,
			List<BiConsumer<BeanRegistry.Spec<?>, Class<?>>> beanRegistrations,
			List<BiConsumer<RestClient.Builder, SupplierContext>> restClientCustomizers) {
		super(types, basePackages, using, httpServiceProxyFactoryCustomizers, beanRegistrations);
		this.restClientCustomizers = restClientCustomizers;
	}

	/**
	 * Return a new instance of this registrar that additionally sets the base URL of the
	 * {@link RestClient}.
	 * @param baseUrl the base URL to use
	 * @return a new registrar instance
	 */
	public RestClientHttpServiceProxies baseUrl(String baseUrl) {
		return restClient((restClient) -> restClient.baseUrl(baseUrl));
	}

	/**
	 * Return a new instance of this registrar that additionally sets the base URL of the
	 * {@link RestClient}.
	 * @param baseUrl the base URL to use
	 * @return a new registrar instance
	 */
	public RestClientHttpServiceProxies baseUrl(URI baseUrl) {
		return restClient((restClient) -> restClient.baseUrl(baseUrl));
	}

	/**
	 * Return a new instance of this registrar that additionally applies the given
	 * customizer to the {@link RestClient} builder.
	 * @param customizer the customizer to add
	 * @return a new registrar instance
	 */
	public RestClientHttpServiceProxies restClient(Consumer<RestClient.Builder> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return restClient(asBiConsumer(customizer));
	}

	/**
	 * Return a new instance of this registrar that additionally applies the given
	 * customizer to the {@link RestClient} builder.
	 * @param customizer the customizer to add
	 * @return a new registrar instance
	 */
	public RestClientHttpServiceProxies restClient(BiConsumer<RestClient.Builder, SupplierContext> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return copy(merge(restClientCustomizers, customizer));
	}

	@Override
	protected HttpExchangeAdapter supplyHttpExchangeAdapter(SupplierContext supplierContext) {
		RestClient restClient = supplyRestClient(supplierContext);
		return RestClientAdapter.create(restClient);
	}

	private RestClient supplyRestClient(SupplierContext supplierContext) {
		RestClient.Builder builder = RestClient.builder();
		this.restClientCustomizers.forEach((customizer) -> customizer.accept(builder, supplierContext));
		return builder.build();
	}

	@Override
	protected RestClientHttpServiceProxies copy(List<Class<?>> types, List<BasePackage> basePackages,
			@Nullable String using, List<BiConsumer<Builder, SupplierContext>> httpServiceProxyFactoryCustomizers,
			List<BiConsumer<Spec<?>, Class<?>>> beanRegistrations) {
		return new RestClientHttpServiceProxies(types, basePackages, using, httpServiceProxyFactoryCustomizers,
				beanRegistrations, this.restClientCustomizers);
	}

	private RestClientHttpServiceProxies copy(
			List<BiConsumer<RestClient.Builder, SupplierContext>> restClientCustomizers) {
		return new RestClientHttpServiceProxies(this.types, this.basePackages, this.using,
				this.httpServiceProxyFactoryCustomizers, this.beanRegistrationCustomizers, restClientCustomizers);
	}

	/**
	 * Factory method that creates a new {@link RestClientHttpServiceProxies} and
	 * {@link #add(Class...) adds} the given HTTP service types
	 * @param types the types to add
	 * @return a new registrar instance
	 */
	public static RestClientHttpServiceProxies of(Class<?>... types) {
		return of().add(types);
	}

	/**
	 * Factory method that creates a new {@link RestClientHttpServiceProxies} and
	 * {@link #scan(Class...) scans} the given base packages.
	 * @param basePackages the base packages to scan
	 * @return a new registrar instance
	 */
	public static RestClientHttpServiceProxies ofScan(String... basePackages) {
		return of().scan(basePackages);
	}

	/**
	 * Factory method that creates a new {@link RestClientHttpServiceProxies} and
	 * {@link #scan(Class...) scans} the given base packages.
	 * @param basePackages the base packages to scan
	 * @return a new registrar instance
	 */
	public static RestClientHttpServiceProxies ofScan(Class<?>... basePackages) {
		return of().scan(basePackages);
	}

	/**
	 * Factory method that creates a new {@link RestClientHttpServiceProxies} and
	 * {@link #scan(Class...) scans} the given base packages.
	 * @param basePackages the base packages to scan
	 * @return a new registrar instance
	 */
	public static RestClientHttpServiceProxies ofScan(BasePackage... basePackages) {
		return of().scan(basePackages);
	}

	/**
	 * Factory method that returns a empty {@link RestClientHttpServiceProxies}.
	 * @return an empty {@link RestClientHttpServiceProxies}
	 */
	public static RestClientHttpServiceProxies of() {
		return EMPTY;
	}

}
