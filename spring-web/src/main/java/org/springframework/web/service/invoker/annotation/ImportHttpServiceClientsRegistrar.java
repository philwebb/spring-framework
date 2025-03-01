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

package org.springframework.web.service.invoker.annotation;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.TypeFilterUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.service.invoker.HttpServiceProxyFactoryProvider;

/**
 * {@link ImportBeanDefinitionRegistrar} to support
 * {@link ImportHttpServiceClients @ImportHttpServiceClients}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Phillip Webb
 * @since 7.0
 */
class ImportHttpServiceClientsRegistrar implements ImportBeanDefinitionRegistrar {

	private static final String ID_ATTRIBUTE_NAME = ImportHttpServiceClients.ID_ATTRIBUTE_NAME;

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	ImportHttpServiceClientsRegistrar(Environment environment, @Nullable ResourceLoader resourceLoader) {
		this.environment = environment;
		this.resourceLoader = (resourceLoader != null) ? resourceLoader : new DefaultResourceLoader();
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry,
			BeanNameGenerator importBeanNameGenerator) {
		String declaringClass = importingClassMetadata.getClassName();
		MergedAnnotations annotations = importingClassMetadata.getAnnotations();
		BeanNameGenerator defaultNameGenerator = defaultBeanNameGenerator(importBeanNameGenerator);
		annotations.stream(ImportHttpServiceClients.Container.class)
			.flatMap(this::getContainedAnnotations)
			.forEach(annotation -> register(registry, defaultNameGenerator, declaringClass, annotation));
		annotations.stream(ImportHttpServiceClients.class)
			.forEach(annotation -> register(registry, defaultNameGenerator, declaringClass, annotation));
	}

	private static BeanNameGenerator defaultBeanNameGenerator(BeanNameGenerator importBeanNameGenerator) {
		return (definition, registry) -> {
			Object id = definition.getAttribute(ID_ATTRIBUTE_NAME);
			String beanName = importBeanNameGenerator.generateBeanName(definition, registry);
			return (id != null) ? beanName + StringUtils.capitalize(id.toString()) : beanName;
		};
	}

	private Stream<MergedAnnotation<ImportHttpServiceClients>> getContainedAnnotations(
			MergedAnnotation<ImportHttpServiceClients.Container> container) {
		return Arrays.stream(container.getAnnotationArray(MergedAnnotation.VALUE, ImportHttpServiceClients.class));
	}

	private void register(BeanDefinitionRegistry registry, BeanNameGenerator defaultNameGenerator,
			String declaringClass, MergedAnnotation<ImportHttpServiceClients> annotation) {
		Group group = new Group(annotation.getString("group"));
		BeanNameGenerator beanNameGenerator = getBeanNameGenerator(annotation, defaultNameGenerator);
		Scanner scanner = new Scanner(registry, this.environment, this.resourceLoader, group);
		scanner.setResourcePattern(annotation.getString("resourcePattern"));
		scanner.setBeanNameGenerator(beanNameGenerator);
		TypeFilters scanFilters = new TypeFilters(this.environment, this.resourceLoader, registry, annotation);
		scanFilters.forEach("includeFilters", scanner::addIncludeFilter);
		scanFilters.forEach("excludeFilters", scanner::addExcludeFilter);
		scanner.addExcludeFilter(new DeclaringClassFilter(declaringClass));
		setLazyInit(annotation, scanner);
		scanner.setProxyFactory(group::instanceSupplier);
		for (String client : annotation.getStringArray("clients")) {
			registerClient(registry, scanner, group, client, beanNameGenerator);
		}
		Set<String> basePackages = getBasePackages(declaringClass, annotation);
		if (!basePackages.isEmpty()) {
			scanner.scanAndRegisterClients(StringUtils.toStringArray(basePackages));
		}
	}

	private BeanNameGenerator getBeanNameGenerator(MergedAnnotation<?> annotation,
			BeanNameGenerator defaultNameGenerator) {
		Class<? extends BeanNameGenerator> generatorClass = getNameGeneratorClass(annotation);
		return (generatorClass != BeanNameGenerator.class) ? BeanUtils.instantiateClass(generatorClass)
				: defaultNameGenerator;
	}

	@SuppressWarnings("unchecked")
	private Class<? extends BeanNameGenerator> getNameGeneratorClass(MergedAnnotation<?> annotation) {
		return (Class<? extends BeanNameGenerator>) annotation.getClass("nameGenerator");
	}

	private void setLazyInit(MergedAnnotation<ImportHttpServiceClients> annotation, Scanner scanner) {
		boolean lazyInit = annotation.getBoolean("lazyInit");
		if (lazyInit) {
			scanner.getBeanDefinitionDefaults().setLazyInit(true);
		}
	}

	private void registerClient(BeanDefinitionRegistry registry, Scanner scanner, Group using, String client,
			BeanNameGenerator beanNameGenerator) {

		try {
			MetadataReader metadataReader = scanner.getMetadataReaderFactory().getMetadataReader(client);
			AbstractBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(
					metadataReader.getAnnotationMetadata());
			scanner.postProcessBeanDefinition(beanDefinition);
			String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
			scanner.postProcessBeanDefinition(beanDefinition, beanName);
			beanDefinition.setInstanceSupplier(using.instanceSupplier(client));
			registry.registerBeanDefinition(beanName, beanDefinition);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("Failed to read http service client class: " + client, ex);
		}
	}

	private Set<String> getBasePackages(String declaringClass, MergedAnnotation<ImportHttpServiceClients> annotation) {
		Set<String> basePackages = new LinkedHashSet<>();
		for (String basePackage : annotation.getStringArray("basePackages")) {
			Collections.addAll(basePackages, tokenize(basePackage));
		}
		for (String basePackageClasses : annotation.getStringArray("basePackageClasses")) {
			basePackages.add(ClassUtils.getPackageName(basePackageClasses));
		}
		return basePackages;
	}

	private String[] tokenize(String string) {
		String resolved = this.environment.resolvePlaceholders(string);
		return StringUtils.tokenizeToStringArray(resolved, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
	}

	/**
	 * {@link ClassPathBeanDefinitionScanner} used to find http service clients.
	 */
	private static class Scanner extends ClassPathBeanDefinitionScanner {

		private final Group using;

		Scanner(BeanDefinitionRegistry registry, Environment environment, @Nullable ResourceLoader resourceLoader,
				Group using) {
			super(registry, false, environment, resourceLoader);
			this.using = using;
		}

		@Override
		public Set<BeanDefinition> findCandidateComponents(String basePackage) {
			Set<BeanDefinition> candidateComponents = super.findCandidateComponents(basePackage);
			candidateComponents.forEach(this::postProcessBeanDefinition);
			return candidateComponents;
		}

		/**
		 * Post process the bean definition before the bean name is generated.
		 * @param beanDefinition the bean definition to post process
		 */
		protected void postProcessBeanDefinition(BeanDefinition beanDefinition) {
			String usingId = this.using.id();
			if (StringUtils.hasLength(usingId)) {
				beanDefinition.setAttribute(ID_ATTRIBUTE_NAME, usingId);
			}
		}

		@Override
		protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
			super.postProcessBeanDefinition(beanDefinition, beanName);
		}

		protected void scanAndRegisterClients(String... basePackages) {
			super.doScan(basePackages);
		}

	}

	/**
	 * {@link TypeFilter} used to filter out the declaring class.
	 */
	private static class DeclaringClassFilter extends AbstractTypeHierarchyTraversingFilter {

		private final String declaringClass;

		DeclaringClassFilter(String declaringClass) {
			super(false, false);
			this.declaringClass = declaringClass;
		}

		@Override
		protected boolean matchClassName(String className) {
			return this.declaringClass.equals(className);
		}

	}

	/**
	 * Provides access to {@link TypeFilter} instances as defined in the annotation.
	 */
	private record TypeFilters(Environment environment, ResourceLoader resourceLoader, BeanDefinitionRegistry registry,
			MergedAnnotation<ImportHttpServiceClients> annotation) {

		void forEach(String attributeName, Consumer<TypeFilter> action) {
			Arrays.stream(this.annotation.getAnnotationArray(attributeName, ComponentScan.Filter.class))
				.map(MergedAnnotation::asAnnotationAttributes)
				.flatMap(this::streamTypeFilters)
				.forEach(action);
		}

		private Stream<TypeFilter> streamTypeFilters(AnnotationAttributes attributes) {
			return TypeFilterUtils.createTypeFiltersFor(attributes, environment(), resourceLoader(), registry())
				.stream();
		}
	}

	/**
	 * A reference to the ID to use with {@link HttpServiceProxyFactoryProvider}.
	 */
	private record Group(String id) {

		InstanceSupplier<?> instanceSupplier(AnnotationMetadata componentMetadata) {
			return instanceSupplier(componentMetadata.getClassName());
		}

		InstanceSupplier<?> instanceSupplier(String serviceType) {
			return new HttpServiceProxyClientInstanceSupplier<>(serviceType, id());
		}
	}

}
