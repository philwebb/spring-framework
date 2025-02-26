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

package org.springframework.beans.factory.support;

import java.util.List;

import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.lang.Nullable;

/**
 * Factory that can be registered in {@code spring.factories} in order to support creation
 * of a proxy {@link InstanceSupplier} for a given {@link Class}.
 *
 * @author Phillip Webb
 * @since 7.0
 */
@FunctionalInterface
public interface ProxyInstanceSupplierFactory {

	/**
	 * Return an {@link InstanceSupplier} that will create the proxy instance or
	 * {@code null} if the given class is not supported by this factory.
	 * @param <T> the type of instance ultimately supplied
	 * @param type the type of instance requested
	 * @return an {@link InstanceSupplier} or {@code null}
	 */
	@Nullable
	<T> InstanceSupplier<T> createProxyInstanceSupplier(Class<T> type);


	static ProxyInstanceSupplierFactory fromFactories() {
		return fromFactories(SpringFactoriesLoader.forDefaultResourceLocation());
	}

	static ProxyInstanceSupplierFactory fromFactories(SpringFactoriesLoader factoriesLoader) {
		return fromFactories(factoriesLoader.load(ProxyInstanceSupplierFactory.class));
	}

	static ProxyInstanceSupplierFactory fromFactories(List<? extends ProxyInstanceSupplierFactory> factories) {
		return new ProxyInstanceSupplierFactory() {

			@Override
			public <T> InstanceSupplier<T> createProxyInstanceSupplier(Class<T> type) {
				InstanceSupplier<T> result = null;
				ProxyInstanceSupplierFactory resultFactory = null;
				for (ProxyInstanceSupplierFactory factory : factories) {
					InstanceSupplier<T> supplier = factory.createProxyInstanceSupplier(type);
					if (supplier == null) {
						continue;
					}
					if (result != null) {
						throw new IllegalStateException("Multiple ProxyInstanceSupplierFactory [%s, %s] instances accept %s"
							.formatted(resultFactory.getClass().getName(), factories.getClass().getName(), type));
					}
					result = supplier;
					resultFactory = factory;
				}
				return result;
			}

		};
	}

}
