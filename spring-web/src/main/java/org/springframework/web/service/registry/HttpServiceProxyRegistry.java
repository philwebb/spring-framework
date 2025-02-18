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

import org.jspecify.annotations.Nullable;

/**
 * Registry for access to HTTP Service proxies grouped by target URL.
 *
 * <p>To create an instance, see
 * {@link org.springframework.web.client.support.RestClientHttpServiceProxyRegistry}, or
 * {@link org.springframework.web.reactive.function.client.support.WebClientHttpServiceProxyRegistry}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <G> the type of HttpServiceGroup supported by the registry
 */
public interface HttpServiceProxyRegistry<G extends HttpServiceGroup<G, ?>> {

	/**
	 * Return a client proxy of the given type from any HTTP Service group as
	 * long as there is only one proxy of the given type across all groups.
	 * @param httpServiceType the type of HTTP Service to return
	 * @return the proxy instance or {@code null} if not found
	 * @param <P> the proxy type
	 * @throws IllegalArgumentException if there is more than one proxy of
	 * the given type
	 */
	<P> @Nullable P getClientProxy(Class<P> httpServiceType);

	/**
	 * Return a client proxy from the identified HTTP Service group.
	 * @param groupId identifier of the group
	 * @param httpServiceType the type of HTTP Service to return
	 * @return the proxy instance or {@code null} if not found
	 * @param <P> the proxy type
	 */
	<P> @Nullable P getClientProxy(String groupId, Class<P> httpServiceType);

	/**
	 * Get all registered HTTP Service groups.
	 */
	Map<String, G> getGroups();

}
