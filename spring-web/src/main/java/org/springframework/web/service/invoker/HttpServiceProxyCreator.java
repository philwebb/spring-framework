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

package org.springframework.web.service.invoker;

import java.util.function.Consumer;

/**
 * Interface implemented to support the create of HTTP service proxies or
 * {@link HttpServiceProxyFactory HTTP service proxy factories}.
 *
 * @author Phillip Webb
 * @since 7.0
 */
public interface HttpServiceProxyCreator {

	/**
	 * Return a proxy that implements the given HTTP service interface to perform HTTP
	 * requests and retrieve responses through an HTTP client.
	 * @param type the HTTP service to create a proxy for
	 * @param <S> the HTTP service type
	 * @return the created proxy
	 * @see HttpServiceProxyFactory#serviceProxy(Class)
	 * @see #serviceProxyFactory()
	 */
	default <T> T serviceProxy(Class<T> type) {
		return serviceProxyFactory().serviceProxy(type);
	}

	/**
	 * Return a {@link HttpServiceProxyFactory} that can be used to create HTTP service
	 * proxies.
	 * @return a {@link HttpServiceProxyFactory} instance
	 * @see HttpServiceProxyFactory#of(HttpExchangeAdapter)
	 */
	default HttpServiceProxyFactory serviceProxyFactory() {
		return serviceProxyFactory((builderCustomizer) -> {
		});
	}

	/**
	 * Return a {@link HttpServiceProxyFactory} that can be used to create HTTP service
	 * proxies.
	 * @param builderCustomizer callback that can be used to customize the {@link HttpServiceProxyFactory} builder.
	 * @return a {@link HttpServiceProxyFactory} instance
	 * @see HttpServiceProxyFactory#of(HttpExchangeAdapter, Consumer)
	 */
	HttpServiceProxyFactory serviceProxyFactory(Consumer<HttpServiceProxyFactory.Builder> builderCustomizer);

}
