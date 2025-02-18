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

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;

/**
 * Extends the {@link HttpServiceProxyRegistry} contract with methods to populate
 * and configure the registry.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <G> the type of HttpServiceGroup supported by the registry
 */
public interface ConfigurableHttpServiceProxyRegistry<G extends HttpServiceGroup<G, ?>>
		extends HttpServiceProxyRegistry<G>, EnvironmentAware, ResourceLoaderAware, SmartInitializingSingleton {

	/**
	 * Add a new group.
	 * @param id unique identifier for the group
	 * @param groupConfigurer a configurer to further customize the group
	 */
	void registerGroup(String id, HttpServiceGroupConfigurer<G> groupConfigurer);

	/**
	 * Apply the given configurer to all groups.
	 */
	void apply(HttpServiceGroupConfigurer<G> groupConfigurer);

}
