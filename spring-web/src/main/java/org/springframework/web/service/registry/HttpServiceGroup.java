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

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * A grouping of HTTP service types that share a client with the same setup.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <G> the concrete HttpServiceGroup subtype
 * @param <CB> the type of client builder (e.g. RestClient.Builder)
 */
public interface HttpServiceGroup<G extends HttpServiceGroup<G, CB>, CB> {

	/**
	 * Return the base URL for the HTTP Service group.
	 */
	String id();

	/**
	 * Return the configured HTTP Service types.
	 */
	Set<Class<?>> httpServiceTypes();

	/**
	 * Set the baseUrl on the underlying client builder. A shortcut for doing the
	 * same directly on the client builder via {@link #configureClient(Consumer)}.
	 */
	G baseUrl(String baseUrl);

	/**
	 * Add the given HTTP service types.
	 */
	G addHttpServiceTypes(Class<?>... httpServiceTypes);

	/**
	 * Scan the classpath for HTTP Services under one or more base packages.
	 * {@link org.springframework.web.service.annotation.HttpExchange}.
	 * @see org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
	 */
	G detectHttpServiceTypes(String... basePackages);

	/**
	 * Scan the classpath for HTTP Services under one or more base packages.
	 * {@link org.springframework.web.service.annotation.HttpExchange}.
	 * @see org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
	 */
	G detectHttpServiceTypes(Class<?>... basePackages);

	/**
	 * Callback to configure the underlying HTTP client.
	 */
	G configureClient(Consumer<CB> configurer);

	/**
	 * Callback to configure the {@link HttpServiceProxyFactory} used
	 * to create proxy instances.
	 */
	G configureProxyFactory(Consumer<HttpServiceProxyFactory.Builder> configurer);

	/**
	 * Create the client proxy instances for all configured HTTP Service types.
	 */
	void initClientProxies();

	/**
	 * Return the client proxy instance for the given HttpService type.
	 * @param httpServiceType the type of HTTP Service to return
	 * @param <T> the HTTP Service type
	 * @throws IllegalStateException if {@link #initClientProxies()} has not been called yet called
	 */
	<T> @Nullable T getClientProxy(Class<T> httpServiceType);

	/**
	 * Return a Map from HttpService types to proxy instances. The returned Map is
	 * empty before {@link #initClientProxies()} is called.
	 * @throws IllegalStateException if {@link #initClientProxies()} has not been called yet called
	 */
	Map<Class<?>, Object> getClientProxyMap();

}
