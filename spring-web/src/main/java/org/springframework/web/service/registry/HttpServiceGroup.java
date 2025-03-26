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

import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A group of HTTP Service interfaces that share the same
 * {@link org.springframework.web.service.invoker.HttpServiceProxyFactory} and
 * HTTP client setup.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface HttpServiceGroup {

	/**
	 * The name of the group to add HTTP Services to when a group isn't specified.
	 */
	String DEFAULT_GROUP_NAME = "default";


	/**
	 * The name of the HTTP Service group.
	 */
	String name();

	/**
	 * The HTTP Services in the group.
	 */
	Set<Class<?>> httpServiceTypes();

	/**
	 * The client type to use for the group.
	 * <p>By default, {@link ClientType#REST_CLIENT} remains unspecified.
	 */
	ClientType clientType();


	/**
	 * Enum to specify the client type to use for an HTTP Service group.
	 */
	enum ClientType {

		/**
		 * A group backed by {@link org.springframework.web.client.RestClient}.
		 */
		REST_CLIENT("org.springframework.web.client.support.RestClientHttpServiceGroupAdapter"),

		/**
		 * A group backed by {@link org.springframework.web.reactive.function.client.WebClient}.
		 */
		WEB_CLIENT("org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupAdapter"),

		/**
		 * Not specified, falling back on a default.
		 * @see ImportHttpServices#clientType()
		 * @see HttpServiceGroups#clientType()
		 * @see AbstractHttpServiceRegistrar#setDefaultClientType
		 */
		UNSPECIFIED(null);


		private final @Nullable String groupAdapterClassName;

		private final @Nullable Class<? extends HttpServiceGroupAdapter<?>> groupAdapterType;


		private ClientType(@Nullable String groupAdapterClassName) {
			this.groupAdapterClassName = groupAdapterClassName;
			this.groupAdapterType = resolveIfPresent(groupAdapterClassName);
		}

		@SuppressWarnings("unchecked")
		static @Nullable Class<? extends HttpServiceGroupAdapter<?>> resolveIfPresent(@Nullable String type) {
			try {
				if (type != null) {
					return (Class<? extends HttpServiceGroupAdapter<?>>) ClassUtils.forName(type,
							HttpServiceGroup.class.getClassLoader());
				}
			}
			catch (ClassNotFoundException ex) {
			}
			return null;
		}

		ClientType orElse(ClientType clientType) {
			return (this != UNSPECIFIED) ? this : clientType;
		}

		Class<? extends HttpServiceGroupAdapter<?>> getGroupAdapterType() {
			if (this == UNSPECIFIED) {
				return REST_CLIENT.getGroupAdapterType();
			}
			Assert.state(this.groupAdapterType != null,
					() -> "HttpServiceGroup client type %s could not resolve class %s".formatted(name(),
							this.groupAdapterClassName));
			return this.groupAdapterType;
		}

	}

}
