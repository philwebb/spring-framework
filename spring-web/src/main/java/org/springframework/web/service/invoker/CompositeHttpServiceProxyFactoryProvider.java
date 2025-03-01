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

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Composite {@link CompositeHttpServiceProxyFactoryProvider}.
 *
 * @author Phillip Webb
 * @since 7.0
 */
class CompositeHttpServiceProxyFactoryProvider implements HttpServiceProxyFactoryProvider {

	private final Collection<? extends HttpServiceProxyFactoryProvider> providers;

	CompositeHttpServiceProxyFactoryProvider(Collection<? extends HttpServiceProxyFactoryProvider> providers) {
		this.providers = providers;
	}

	@Override
	public @Nullable HttpServiceProxyFactory getHttpServiceProxyFactory(String id) {
		HttpServiceProxyFactory proxyFactory = null;
		for (HttpServiceProxyFactoryProvider provider : this.providers) {
			HttpServiceProxyFactory candidate = (id != null) ? provider.getHttpServiceProxyFactory(id) : null;
			if (candidate != null) {
				Assert.state(proxyFactory == null,
						() -> "Multipe HttpServiceProxyFactories provided for ID '%s'".formatted(id));
				proxyFactory = candidate;
			}
		}
		return proxyFactory;
	}

}
