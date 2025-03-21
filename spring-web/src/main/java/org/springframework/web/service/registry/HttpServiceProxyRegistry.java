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

import org.jspecify.annotations.Nullable;

/**
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface HttpServiceProxyRegistry {

	/**
	 * Return an HTTP service client from any group as long as there is only one
	 * client of this type across all groups.
	 * @param httpServiceType the type of client to return
	 * @return the proxy instance or {@code null} if not found
	 * @param <P> the HTTP interface type for teh client
	 * @throws IllegalArgumentException if there is more than one client across
	 * all groups
	 */
	<P> @Nullable P getClient(Class<P> httpServiceType);

	/**
	 * Return an HTTP service client from the specified group.
	 * @param groupName the group of the client
	 * @param httpServiceType the type of client to return
	 * @return the proxy instance or {@code null} if not found
	 * @param <P> the HTTP interface type for teh client
	 */
	<P> @Nullable P getClient(String groupName, Class<P> httpServiceType);

}
