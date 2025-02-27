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

import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * Factory used to create proxy instances for scanned components. Typically used to
 * dynamically create an concrete implementation from an interface.
 * <p>
 * Implementations may also be registered in {@code spring.factories} if they can apply to
 * standard {@link ComponentScan @ComponentScan} calls. In such cases, there must be a
 * unique way to identify when a proxy should be created (for example, by checking for a
 * specific annotation).
 * <p>
 * Implementations of this interface may also return an {@link InstanceSupplier} based on
 * a proxy factory bean that the user has specified.
 *
 * @author Phillip Webb
 * @since 7.0
 * @see ComponentScan#proxyFactory()
 * @see ClassPathScanningCandidateComponentProvider#setProxyFactory(ScannedComponentProxyFactory)
 */
@FunctionalInterface
public interface ScannedComponentProxyFactory {

	public static final ScannedComponentProxyFactory.None NONE = new None();

	/**
	 * Return an {@link InstanceSupplier} that will create the proxy instance or
	 * {@code null} if the given metadata is not supported by this factory.
	 * @param scannedComponentMetadata the metadata of the scanned component
	 * @param bean the bean that should be used to create the proxy or {@code null}
	 * @param beanName the name bean that being passed in {@code bean} or {@code null}
	 * @return an {@link InstanceSupplier} or {@code null}
	 */
	@Nullable
	InstanceSupplier<?> createProxyInstanceSupplier(AnnotationMetadata scannedComponentMetadata, @Nullable Object bean,
			@Nullable String beanName);

	/**
	 * Create a composite {@link ScannedComponentProxyFactory} based on the provided
	 * {@code factories}.
	 * @param factories the source factories
	 * @return a composite factory
	 */
	static ScannedComponentProxyFactory composite(
			@Nullable Collection<? extends ScannedComponentProxyFactory> factories) {
		if (CollectionUtils.isEmpty(factories)) {
			return NONE;
		}
		if (factories.size() == 1) {
			return factories.iterator().next();
		}
		return (scannedComponentMetadata, proxyFactoryBean, proxyFactoryBeanName) -> {
			InstanceSupplier<?> result = null;
			ScannedComponentProxyFactory resultFactory = null;
			for (ScannedComponentProxyFactory factory : factories) {
				InstanceSupplier<?> supplier = factory.createProxyInstanceSupplier(scannedComponentMetadata,
						proxyFactoryBean, proxyFactoryBeanName);
				if (supplier == null) {
					continue;
				}
				if (result != null) {
					throw new IllegalStateException(
							"Multiple ProxyInstanceSupplierFactory [%s, %s] instances accept %s%s".formatted(
									resultFactory.getClass().getName(), factories.getClass().getName(),
									scannedComponentMetadata.getClassName(),
									(proxyFactoryBeanName != null) ? " (" + proxyFactoryBeanName + ")" : ""));
				}
				result = supplier;
				resultFactory = factory;
			}
			return result;
		};
	}

	/**
	 * {@link ScannedComponentProxyFactory} that always returns no
	 * {@link InstanceSupplier}.
	 */
	public static class None implements ScannedComponentProxyFactory {

		@Override
		public InstanceSupplier<?> createProxyInstanceSupplier(AnnotationMetadata scannedComponentMetadata,
				@Nullable Object bean, @Nullable String beanName) {
			return null;
		}

	}

}
