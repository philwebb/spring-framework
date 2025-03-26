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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;

/**
 * Abstract registrar class that imports:
 * <ul>
 * <li>Bean definitions for HTTP Service client proxies organized by
 * {@link HttpServiceGroup}.
 * <li>Bean definition for an {@link HttpServiceProxyRegistryFactoryBean} that
 * initializes the infrastructure for each group, {@code RestClient} or
 * {@code WebClient} and a proxy factory, necessary to create the proxies.
 * </ul>
 *
 * <p>Subclasses determine the HTTP Services to register by implementing
 * {@link #registerHttpServices}.
 *
 * <p>There is built-in support for declaring HTTP Services through
 * {@link ImportHttpServices} annotations. It is also possible to perform
 * registrations directly, sourced in another way, by extending this class.
 *
 * <p>It is possible to import multiple instances of this registrar type.
 * Subsequent imports update the existing registry {@code FactoryBean}
 * definition, and likewise merge HTTP Service group definitions.
 *
 * <p>An application can autowire HTTP Service proxy beans, or autowire the
 * {@link HttpServiceProxyRegistry} from which to obtain proxies.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 * @see ImportHttpServices
 * @see HttpServiceProxyRegistryFactoryBean
 */
public abstract class AbstractHttpServiceRegistrar implements
		ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware, BeanFactoryAware {


	private HttpServiceGroup.ClientType defaultClientType = HttpServiceGroup.ClientType.UNSPECIFIED;

	private @Nullable Environment environment;

	private @Nullable ResourceLoader resourceLoader;

	private @Nullable BeanFactory beanFactory;

	private final Map<String, HttpServiceGroup> groupMap = new LinkedHashMap<>();

	private @Nullable ClassPathScanningCandidateComponentProvider scanner;


	/**
	 * Set the client type to use when the client type for an HTTP Service group
	 * remains {@link HttpServiceGroup.ClientType#UNSPECIFIED}.
	 * <p>By default, when this property is not set, then {@code REST_CLIENT}
	 * is used for any HTTP Service group whose client type remains unspecified.
	 */
	public void setDefaultClientType(HttpServiceGroup.ClientType defaultClientType) {
		this.defaultClientType = defaultClientType;
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
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}


	@Override
	public final void registerBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry beanRegistry,
			BeanNameGenerator beanNameGenerator) {

		registerHttpServices(new DefaultHttpServiceRegistry(), importingClassMetadata);

		String proxyRegistryBeanName = HttpServiceProxyRegistry.class.getName();
		GenericBeanDefinition proxyRegistryBeanDef;

		if (!beanRegistry.containsBeanDefinition(proxyRegistryBeanName)) {
			proxyRegistryBeanDef = new GenericBeanDefinition();
			proxyRegistryBeanDef.setBeanClass(HttpServiceProxyRegistryFactoryBean.class);
			ConstructorArgumentValues args = proxyRegistryBeanDef.getConstructorArgumentValues();
			args.addIndexedArgumentValue(0, new LinkedHashMap<String, HttpServiceGroup>());
			args.addIndexedArgumentValue(1, ClientType.UNSPECIFIED);
			beanRegistry.registerBeanDefinition(proxyRegistryBeanName, proxyRegistryBeanDef);
		}
		else {
			proxyRegistryBeanDef = (GenericBeanDefinition) beanRegistry.getBeanDefinition(proxyRegistryBeanName);
		}

		updateGroups(getMapArg(0, proxyRegistryBeanDef));
		updateDefaultClientType(getValueHolder(1, proxyRegistryBeanDef, ClientType.class));

		this.groupMap.forEach((groupName, group) -> group.httpServiceTypes().forEach(type -> {
			GenericBeanDefinition proxyBeanDef = new GenericBeanDefinition();
			proxyBeanDef.setBeanClass(type);
			proxyBeanDef.setInstanceSupplier(() -> getProxyInstance(proxyRegistryBeanName, groupName, type));
			String beanName = (groupName + "." + beanNameGenerator.generateBeanName(proxyBeanDef, beanRegistry));
			if (!beanRegistry.containsBeanDefinition(beanName)) {
				beanRegistry.registerBeanDefinition(beanName, proxyBeanDef);
			}
		}));
	}

	@Override
	public final void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
	}

	/**
	 * This method is called before any bean definition registrations are made.
	 * Subclasses must implement it to register the HTTP Services for which bean
	 * definitions for which proxies need to be created.
	 * @param registry to perform HTTP Service registrations with
	 * @param importingClassMetadata annotation metadata of the importing class
	 */
	protected abstract void registerHttpServices(
			HttpServiceRegistry registry, AnnotationMetadata importingClassMetadata);

	private ClassPathScanningCandidateComponentProvider getScanner() {
		if (this.scanner == null) {
			Assert.state(this.environment != null, "Environment has not been set");
			Assert.state(this.resourceLoader != null, "ResourceLoader has not been set");
			this.scanner = new HttpExchangeClassPathScanningCandidateComponentProvider();
			this.scanner.setEnvironment(this.environment);
			this.scanner.setResourceLoader(this.resourceLoader);
		}
		return this.scanner;
	}

	@SuppressWarnings("unchecked")
	private static <K, V> Map<K, V> getMapArg(int index, GenericBeanDefinition registryBeanDef) {
		ConstructorArgumentValues.ValueHolder valueHolder = getValueHolder(index, registryBeanDef, Map.class);
		Map<K, V> map = (Map<K, V>) valueHolder.getValue();
		Assert.state(map!= null, "No constructor argument value");
		return map;
	}

	private static <T> ConstructorArgumentValues.ValueHolder getValueHolder(int index,
			GenericBeanDefinition registryBeanDef, Class<T> type) {

		ConstructorArgumentValues args = registryBeanDef.getConstructorArgumentValues();
		ConstructorArgumentValues.ValueHolder valueHolder = args.getArgumentValue(index, type);
		Assert.state(valueHolder != null, () -> "Expected %s constructor argument at index %s".formatted(type, index));
		return valueHolder;
	}

	private void updateGroups(Map<String, HttpServiceGroup> target) {
		this.groupMap.forEach((name, group) -> {
			HttpServiceGroup previousGroup = target.putIfAbsent(name, group);
			if (previousGroup != null) {
				if (!compatibleClientTypes(group.clientType(), previousGroup.clientType())) {
					throw new IllegalArgumentException("ClientType conflict for group '" + name + "'");
				}
				previousGroup.httpServiceTypes().addAll(group.httpServiceTypes());
			}
		});
	}

	private static boolean compatibleClientTypes(
			HttpServiceGroup.ClientType clientTypeA, HttpServiceGroup.ClientType clientTypeB) {

		return (clientTypeA == clientTypeB ||
				clientTypeA == HttpServiceGroup.ClientType.UNSPECIFIED ||
				clientTypeB == HttpServiceGroup.ClientType.UNSPECIFIED);
	}

	private void updateDefaultClientType(ConstructorArgumentValues.ValueHolder target) {
		HttpServiceGroup.ClientType current = (ClientType) target.getValue();
		Assert.state(current == ClientType.UNSPECIFIED || current == this.defaultClientType,
				"Default ClientType conflict");
		target.setValue(this.defaultClientType);
	}

	private Object getProxyInstance(String registryBeanName, String groupName, Class<?> type) {
		Assert.state(this.beanFactory != null, "BeanFactory has not been set");
		HttpServiceProxyRegistry registry = this.beanFactory.getBean(registryBeanName, HttpServiceProxyRegistry.class);
		Object proxy = registry.getClient(groupName, type);
		Assert.notNull(proxy, "No proxy for HTTP Service [" + type.getName() + "]");
		return proxy;
	}

	private static @Nullable HttpServiceGroupAdapter<?> instantiateAdapter(String className) {
		try {
			ClassLoader classLoader = HttpServiceProxyRegistryFactoryBean.class.getClassLoader();
			Class<?> clazz = ClassUtils.forName(className, classLoader);
			return (HttpServiceGroupAdapter<?>) BeanUtils.instantiateClass(clazz);
		}
		catch (ClassNotFoundException ex) {
			// ignore
		}
		return null;
	}


	/**
	 * Registry API to allow subclasses to register HTTP Services.
	 */
	protected interface HttpServiceRegistry {

		/**
		 * Perform HTTP Service registrations for the given group.
		 */
		GroupSpec forGroup(String name);

		/**
		 * Variant of {@link #forGroup(String)} with a client type.
		 */
		GroupSpec forGroup(String name, HttpServiceGroup.ClientType clientType);

		/**
		 * Perform HTTP Service registrations for the
		 * {@link HttpServiceGroup#DEFAULT_GROUP_NAME} group.
		 */
		default GroupSpec forDefaultGroup() {
			return forGroup(HttpServiceGroup.DEFAULT_GROUP_NAME);
		}

		/**
		 * Spec to list or scan for HTTP Service types.
		 */
		interface GroupSpec {

			/**
			 * List HTTP Service types to create proxies for.
			 */
			GroupSpec register(Class<?>... serviceTypes);

			/**
			 * Detect HTTP Service types in the given packages, looking for
			 * interfaces with a type and/or method {@link HttpExchange} annotation.
			 */
			GroupSpec detectInBasePackages(Class<?>... packageClasses);

			/**
			 * Variant of {@link #detectInBasePackages(Class[])} with a String package name.
			 */
			GroupSpec detectInBasePackages(String... packageNames);

		}
	}


	/**
	 * Default implementation of {@link HttpServiceRegistry}.
	 */
	private class DefaultHttpServiceRegistry implements HttpServiceRegistry {

		@Override
		public GroupSpec forGroup(String name) {
			return forGroup(name, HttpServiceGroup.ClientType.UNSPECIFIED);
		}

		@Override
		public GroupSpec forGroup(String name, HttpServiceGroup.ClientType clientType) {
			return new DefaultGroupSpec(name, clientType);
		}

		private class DefaultGroupSpec implements GroupSpec {

			private final String groupName;

			private final HttpServiceGroup.ClientType clientType;

			public DefaultGroupSpec(String groupName, HttpServiceGroup.ClientType clientType) {
				this.groupName = groupName;
				this.clientType = clientType;
			}

			@Override
			public GroupSpec register(Class<?>... serviceTypes) {
				registerHttpServiceType(this.groupName, this.clientType, serviceTypes);
				return this;
			}

			@Override
			public GroupSpec detectInBasePackages(Class<?>... packageClasses) {
				for (Class<?> packageClass : packageClasses) {
					detect(this.groupName, this.clientType, packageClass.getPackageName());
				}
				return this;
			}

			@Override
			public GroupSpec detectInBasePackages(String... packageNames) {
				for (String packageName : packageNames) {
					detect(this.groupName, this.clientType, packageName);
				}
				return this;
			}

			private void detect(String groupName, HttpServiceGroup.ClientType clientType, String packageName) {
				for (BeanDefinition definition : getScanner().findCandidateComponents(packageName)) {
					String className = definition.getBeanClassName();
					if (className != null) {
						try {
							Class<?> clazz = ClassUtils.forName(className, getClass().getClassLoader());
							registerHttpServiceType(groupName, clientType, clazz);
						}
						catch (ClassNotFoundException ex) {
							throw new IllegalStateException("Failed to load '" + className + "'", ex);
						}
					}
				}
			}

			private void registerHttpServiceType(
					String groupName, HttpServiceGroup.ClientType clientType, Class<?>... serviceTypes) {

				groupMap.computeIfAbsent(groupName, name -> new RegisteredGroup(name, new LinkedHashSet<>(), clientType))
						.httpServiceTypes().addAll(Arrays.asList(serviceTypes));
			}
		}

		private record RegisteredGroup(
				String name, Set<Class<?>> httpServiceTypes, ClientType clientType) implements HttpServiceGroup {
		}
	}


	/**
	 * Extension of ClassPathScanningCandidateComponentProvider to look for HTTP Services.
	 */
	private static class HttpExchangeClassPathScanningCandidateComponentProvider
			extends ClassPathScanningCandidateComponentProvider {

		public HttpExchangeClassPathScanningCandidateComponentProvider() {
			addIncludeFilter(new HttpExchangeFilter());
		}

		@Override
		protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
			AnnotationMetadata metadata = beanDefinition.getMetadata();
			return (metadata.isIndependent() && !metadata.isAnnotation());
		}

		/**
		 * Find interfaces with type and/or method {@code @HttpExchange}.
		 */
		private static class HttpExchangeFilter extends AnnotationTypeFilter {

			public HttpExchangeFilter() {
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

}
