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

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Strategy used to provide {@link HttpServiceProxyFactory} instances.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 7.0
 */
@FunctionalInterface
public interface HttpServiceProxyFactoryProvider {

	/**
	 * Return the {@link HttpServiceProxyFactory} with the given name or {@code null}.
	 * @param id the ID of the HTTP service proxy factory to provide
	 * @return a {@link HttpServiceProxyFactory} instance or {@code null}
	 */
	@Nullable
	HttpServiceProxyFactory getHttpServiceProxyFactory(String id);

	/**
	 * Return the {@link HttpServiceProxyFactory} with the given name or throw a
	 * {@link NoSuchElementException}.
	 * @param id the ID of the HTTP service proxy factory to provide
	 * @return a {@link HttpServiceProxyFactory} instance (never {@code null})
	 */
	default HttpServiceProxyFactory getRequiredHttpServiceProxyFactory(String id) throws NoSuchElementException {
		HttpServiceProxyFactory factory = getHttpServiceProxyFactory(id);
		if (factory == null) {
			throw new NoSuchElementException("No HttpServiceProxyFactory provided for ID '%s'".formatted(id));
		}
		return factory;
	}

	/**
	 * Factory method to create a {@link HttpServiceProxyFactoryProvider} instance backed
	 * by the given {@link Map}.
	 * @param factories a {@link Map} containing the factories
	 * @return a new {@link HttpServiceProxyFactoryProvider} instance
	 */
	static HttpServiceProxyFactoryProvider of(Map<String, ? extends HttpServiceProxyFactory> factories) {
		Assert.notNull(factories, "'factories' must not be null");
		return factories::get;
	}

	/**
	 * Factory method to create a new composte {@link HttpServiceProxyFactoryProvider}.
	 * @param providers the providers to use in the composite
	 */
	static HttpServiceProxyFactoryProvider composite(Collection<? extends HttpServiceProxyFactoryProvider> providers) {
		return new CompositeHttpServiceProxyFactoryProvider(providers);
	}

}
