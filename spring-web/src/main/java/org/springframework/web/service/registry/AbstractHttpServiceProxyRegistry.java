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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Base class for {@link HttpServiceProxyRegistry} implementations.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <G> the type of {@link HttpServiceGroup} supported by the registry
 */
public abstract class AbstractHttpServiceProxyRegistry<G extends HttpServiceGroup<G, ?>>
		implements ConfigurableHttpServiceProxyRegistry<G> {

	private final Map<String, G> groups = new LinkedHashMap<>();

	private @Nullable Environment environment;

	private @Nullable ResourceLoader resourceLoader;

	private @Nullable ClassPathScanningCandidateComponentProvider componentProvider;


	@Override
	public <P> @Nullable P getClientProxy(Class<P> httpServiceType) {
		P result = null;
		for (G group : this.groups.values()) {
			P p = group.getClientProxy(httpServiceType);
			if (p != null) {
				if (result != null) {
					throw new IllegalArgumentException(
							"More than one client proxy of type " + httpServiceType.getName() + " found");
				}
				result = p;
			}
		}
		return result;
	}

	@Override
	public <P> @Nullable P getClientProxy(String groupId, Class<P> httpServiceType) {
		G group = this.groups.get(groupId);
		return (group != null ? group.getClientProxy(httpServiceType) : null);
	}

	@Override
	public Map<String, G> getGroups() {
		return Collections.unmodifiableMap(this.groups);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void registerGroup(String id, HttpServiceGroupConfigurer<G> configurer) {
		if (this.groups.containsKey(id)) {
			throw new IllegalArgumentException("Group with id  '" + id + "' already exists");
		}
		G group = createGroup(id, getComponentProvider());
		configurer.configure(group);
		this.groups.put(id, group);
	}

	protected abstract G createGroup(String id, ClassPathScanningCandidateComponentProvider componentProvider);

	private ClassPathScanningCandidateComponentProvider getComponentProvider() {
		if (this.componentProvider == null) {
			this.environment = (this.environment != null ? this.environment : new StandardEnvironment());
			this.componentProvider = new HttpServiceClassPathScanningCandidateComponentProvider();
			this.componentProvider.setEnvironment(this.environment);
			this.componentProvider.setResourceLoader(this.resourceLoader);
			this.componentProvider.addIncludeFilter(new HttpExchangeAnnotationTypeFilter());
		}
		return this.componentProvider;
	}

	@Override
	public void apply(HttpServiceGroupConfigurer<G> groupConfigurer) {
		for (G group : this.groups.values()) {
			groupConfigurer.configure(group);
		}
	}

	@Override
	public void afterSingletonsInstantiated() {
		for (G group : this.groups.values()) {
			group.initClientProxies();
		}
	}


	private static class HttpServiceClassPathScanningCandidateComponentProvider
			extends ClassPathScanningCandidateComponentProvider {

		@Override
		protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
			AnnotationMetadata metadata = beanDefinition.getMetadata();
			return (metadata.isIndependent() && !metadata.isAnnotation());
		}
	}


	/**
	 * {@link org.springframework.core.type.filter.TypeFilter} that looks for
	 * type or method level {@link HttpExchange} annotations.
	 */
	private static final class HttpExchangeAnnotationTypeFilter extends AnnotationTypeFilter {

		public HttpExchangeAnnotationTypeFilter() {
			super(HttpExchange.class, true, true);
		}

		@Override
		protected boolean matchSelf(MetadataReader metadataReader) {
			if (metadataReader.getClassMetadata().isInterface()) {
				for (MethodMetadata metadata : metadataReader.getAnnotationMetadata().getDeclaredMethods()) {
					if (metadata.getAnnotations().isPresent(HttpExchange.class)) {
						return true;
					}
				}
			}
			return false;
		}

	}
}
