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

package org.springframework.context.annotation;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.CollectionUtils;

/**
 * Factory used to create proxy instances for components. Typically used to create
 * concrete implementations from interfaces.
 *
 * @author Phillip Webb
 * @since 7.0
 * @see ComponentScan#proxyFactory()
 * @see ClassPathScanningCandidateComponentProvider#setProxyFactory(ScannedComponentProxyFactory)
 */
@FunctionalInterface
public interface ComponentProxyFactory {

	/**
	 * A {@link ComponentProxyFactory} that always returns {@code null}.
	 */
	static ComponentProxyFactory NONE = new None();

	/**
	 * Return an {@link InstanceSupplier} that will create the proxy instance or
	 * {@code null} if the given metadata is not supported by this factory.
	 * @param componentMetadata the metadata of the component
	 * @return an {@link InstanceSupplier} or {@code null}
	 */
	@Nullable InstanceSupplier<?> createProxyInstanceSupplier(AnnotationMetadata componentMetadata);

	/**
	 * Create a new {@link ComponentProxyFactory} composed of the given {@code factories}.
	 * @param factories the source factories
	 * @return a composite factory
	 */
	static ComponentProxyFactory of(ComponentProxyFactory... factories) {
		return of(List.of(factories));
	}

	/**
	 * Create a new {@link ComponentProxyFactory} composed of the given {@code factories}.
	 * @param factories the source factories
	 * @return a composite factory
	 */
	static ComponentProxyFactory of(@Nullable Collection<? extends ComponentProxyFactory> factories) {
		if (CollectionUtils.isEmpty(factories)) {
			return NONE;
		}
		if (factories.size() == 1) {
			return factories.iterator().next();
		}
		return componentMetadata -> {
			InstanceSupplier<?> result = null;
			ComponentProxyFactory resultFactory = NONE;
			for (ComponentProxyFactory factory : factories) {
				InstanceSupplier<?> supplier = factory.createProxyInstanceSupplier(componentMetadata);
				if (supplier == null) {
					continue;
				}
				if (result != null) {
					throw new IllegalStateException(
							"Multiple ComponentProxyFactories [%s, %s] accept %s".formatted(
									resultFactory.getClass().getName(), factories.getClass().getName(),
									componentMetadata.getClassName()));
				}
				result = supplier;
				resultFactory = factory;
			}
			return result;
		};
	}

	/**
	 * {@link ComponentProxyFactory} that always returns no {@link InstanceSupplier}. This
	 * class is public so that it can be used with annotation attributes.
	 *
	 * @see ComponentProxyFactory#NONE
	 */
	public static class None implements ComponentProxyFactory {

		@Override
		public @Nullable InstanceSupplier<?> createProxyInstanceSupplier(AnnotationMetadata componentMetadata) {
			return null;
		}
	}
}
