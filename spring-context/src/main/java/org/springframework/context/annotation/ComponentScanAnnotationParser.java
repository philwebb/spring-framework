/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the @{@link ComponentScan} annotation.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 * @see ClassPathBeanDefinitionScanner#scan(String...)
 * @see ComponentScanBeanDefinitionParser
 */
class ComponentScanAnnotationParser {

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	private final BeanNameGenerator beanNameGenerator;

	private final BeanDefinitionRegistry registry;


	public ComponentScanAnnotationParser(Environment environment, ResourceLoader resourceLoader,
			BeanNameGenerator beanNameGenerator, BeanDefinitionRegistry registry) {

		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.beanNameGenerator = beanNameGenerator;
		this.registry = registry;
	}

	public Set<BeanDefinitionHolder> parse(AnnotationAttributes componentScan, String declaringClass) {
		boolean useDefaultFilters = componentScan.getBoolean("useDefaultFilters");
		// FIXME if we have a proxyFactory then useDefault to false?
		// FIXME what if we have a proxyFactoryBean?

		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this.registry,
				useDefaultFilters, this.environment, this.resourceLoader);

		Class<? extends BeanNameGenerator> generatorClass = componentScan.getClass("nameGenerator");
		boolean useInheritedGenerator = (BeanNameGenerator.class == generatorClass);
		scanner.setBeanNameGenerator(useInheritedGenerator ? this.beanNameGenerator :
				BeanUtils.instantiateClass(generatorClass));

		ScopedProxyMode scopedProxyMode = componentScan.getEnum("scopedProxy");
		if (scopedProxyMode != ScopedProxyMode.DEFAULT) {
			scanner.setScopedProxyMode(scopedProxyMode);
		}
		else {
			Class<? extends ScopeMetadataResolver> resolverClass = componentScan.getClass("scopeResolver");
			scanner.setScopeMetadataResolver(BeanUtils.instantiateClass(resolverClass));
		}

		scanner.setResourcePattern(componentScan.getString("resourcePattern"));

		for (AnnotationAttributes includeFilterAttributes : componentScan.getAnnotationArray("includeFilters")) {
			List<TypeFilter> typeFilters = TypeFilterUtils.createTypeFiltersFor(includeFilterAttributes, this.environment,
					this.resourceLoader, this.registry);
			for (TypeFilter typeFilter : typeFilters) {
				scanner.addIncludeFilter(typeFilter);
			}
		}
		for (AnnotationAttributes excludeFilterAttributes : componentScan.getAnnotationArray("excludeFilters")) {
			List<TypeFilter> typeFilters = TypeFilterUtils.createTypeFiltersFor(excludeFilterAttributes, this.environment,
				this.resourceLoader, this.registry);
			for (TypeFilter typeFilter : typeFilters) {
				scanner.addExcludeFilter(typeFilter);
			}
		}

		boolean lazyInit = componentScan.getBoolean("lazyInit");
		if (lazyInit) {
			scanner.getBeanDefinitionDefaults().setLazyInit(true);
		}

		Set<String> basePackages = new LinkedHashSet<>();
		String[] basePackagesArray = componentScan.getStringArray("basePackages");
		for (String pkg : basePackagesArray) {
			String[] tokenized = StringUtils.tokenizeToStringArray(this.environment.resolvePlaceholders(pkg),
					ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
			Collections.addAll(basePackages, tokenized);
		}
		for (Class<?> clazz : componentScan.getClassArray("basePackageClasses")) {
			basePackages.add(ClassUtils.getPackageName(clazz));
		}
		if (basePackages.isEmpty()) {
			basePackages.add(ClassUtils.getPackageName(declaringClass));
		}

		scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false) {
			@Override
			protected boolean matchClassName(String className) {
				return declaringClass.equals(className);
			}
		});

		scanner.setProxyFactory(getProxyFactory(componentScan));

		return scanner.doScan(StringUtils.toStringArray(basePackages));
	}

	@Nullable
	private Function<AnnotationMetadata, InstanceSupplier<?>> getProxyFactory(AnnotationAttributes componentScan) {
		Class<? extends ScannedComponentProxyFactory> proxyFactory = componentScan.getClass("proxyFactory");
		if (ScannedComponentProxyFactory.None.class.equals(proxyFactory)) {
			return null;
		}
		SpringFactoriesLoader loader = getSpringFactoriesLoader(proxyFactory);
		List<ScannedComponentProxyFactory> factories = loader.load(ScannedComponentProxyFactory.class,
				this::resolveProxyFactoryArgument);
		ScannedComponentProxyFactory composite = ScannedComponentProxyFactory.composite(factories);
		return asProxyFactoryFunction(composite, componentScan.getString("proxyFactoryBean"));
	}

	private SpringFactoriesLoader getSpringFactoriesLoader(Class<?> proxyFactory) {
		return (ScannedComponentProxyFactory.class != proxyFactory) ? SpringFactoriesLoader.of(proxyFactory)
				: SpringFactoriesLoader.forDefaultResourceLocation();
	}

	private Function<AnnotationMetadata, InstanceSupplier<?>> asProxyFactoryFunction(
			ScannedComponentProxyFactory proxyFactory, String beanName) {
		if (StringUtils.hasLength(beanName)) {
			return (scannedComponentMetadata) -> asProxyFactoryFunctionWithBean(scannedComponentMetadata, proxyFactory, beanName);
		}
		return (scannedComponentMetadata) -> proxyFactory.createProxyInstanceSupplier(scannedComponentMetadata, null, null);
	}

	private InstanceSupplier<?> asProxyFactoryFunctionWithBean(AnnotationMetadata metadata,
			ScannedComponentProxyFactory proxyFactory, String beanName) {
		return (registeredBean) -> {
			Object bean = registeredBean.getBeanFactory().getBean(beanName);
			return proxyFactory.createProxyInstanceSupplier(metadata, bean, beanName).get(registeredBean);
		};
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveProxyFactoryArgument(Class<T> type) {
		if (Environment.class.isAssignableFrom(type)) {
			return (T) this.environment;
		}
		return null;
	}
}
