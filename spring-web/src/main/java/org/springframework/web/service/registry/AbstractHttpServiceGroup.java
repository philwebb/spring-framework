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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Base class for {@link HttpServiceGroup} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <G> the group type
 * @param <CB> the client builder type
 */
public abstract class AbstractHttpServiceGroup<G extends AbstractHttpServiceGroup<G, CB>, CB>
		implements HttpServiceGroup<G, CB> {

	private final String id;

	private final CB baseClientBuilder;

	private final ClassPathScanningCandidateComponentProvider componentProvider;

	private Consumer<HttpServiceProxyFactory.Builder> proxyFactoryConfigurer = builder -> {};

	private final Set<Class<?>> httpServiceTypes = new LinkedHashSet<>();

	private @Nullable Map<Class<?>, Object> proxyMap;


	protected AbstractHttpServiceGroup(
			String id, CB baseClientBuilder, ClassPathScanningCandidateComponentProvider componentProvider) {

		this.id = id;
		this.baseClientBuilder = baseClientBuilder;
		this.componentProvider = componentProvider;
	}


	@Override
	public String id() {
		return this.id;
	}

	@Override
	public Set<Class<?>> httpServiceTypes() {
		return this.httpServiceTypes;
	}

	@Override
	public G addHttpServiceTypes(Class<?>... httpServiceTypes) {
		this.httpServiceTypes.addAll(Arrays.asList(httpServiceTypes));
		return self();
	}

	@Override
	public G detectHttpServiceTypes(String... basePackages) {
		for (String basePackage : basePackages) {
			for (BeanDefinition definition : this.componentProvider.findCandidateComponents(basePackage)) {
				String className = definition.getBeanClassName();
				if (className == null) {
					continue;
				}
				try {
					Class<?> clazz = ClassUtils.forName(className, getClass().getClassLoader());
					this.httpServiceTypes.add(clazz);
				}
				catch (ClassNotFoundException ex) {
					throw new RuntimeException("Failed to find '" + className + "'", ex);
				}
			}
		}
		return self();
	}

	@Override
	public G detectHttpServiceTypes(Class<?>... basePackages) {
		return detectHttpServiceTypes(
				Arrays.stream(basePackages).map(Class::getPackageName).toArray(String[]::new));
	}

	@Override
	public G configureClient(Consumer<CB> configurer) {
		configurer.accept(this.baseClientBuilder);
		return self();
	}

	@Override
	public G configureProxyFactory(Consumer<HttpServiceProxyFactory.Builder> configurer) {
		this.proxyFactoryConfigurer = this.proxyFactoryConfigurer.andThen(configurer);
		return self();
	}

	@SuppressWarnings("unchecked")
	protected <S extends G> S self() {
		return (S) this;
	}

	@Override
	public void initClientProxies() {
		if (this.proxyMap != null) {
			return;
		}
		this.proxyMap = new HashMap<>(this.httpServiceTypes.size());
		HttpServiceProxyFactory factory = initProxyFactory();
		for (Class<?> type : this.httpServiceTypes) {
			this.proxyMap.put(type, factory.createClient(type));
		}
	}

	private HttpServiceProxyFactory initProxyFactory() {
		HttpExchangeAdapter adapter = createExchangeAdapter(this.baseClientBuilder);
		HttpServiceProxyFactory.Builder proxyFactoryBuilder = HttpServiceProxyFactory.builderFor(adapter);
		this.proxyFactoryConfigurer.accept(proxyFactoryBuilder);
		return proxyFactoryBuilder.build();
	}

	protected abstract HttpExchangeAdapter createExchangeAdapter(CB baseClientBuilder);

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T getClientProxy(Class<T> httpServiceType) {
		return (T) getClientProxyMap().get(httpServiceType);
	}

	@Override
	public Map<Class<?>, Object> getClientProxyMap() {
		Assert.state(this.proxyMap != null, "Proxies not initialized yet");
		return this.proxyMap;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() +
				"{id='" + this.id + "', httpServiceTypes=" + this.httpServiceTypes + "}";
	}

}
