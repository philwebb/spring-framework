/*
 * Copyright 2002-present the original author or authors.
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
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.ThrowingFunction;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar.GroupRegistry.GroupSpec;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;
import org.springframework.web.service.registry.ImportHttpServices.GroupProvider;

/**
 * Built-in implementation of {@link AbstractHttpServiceRegistrar} that uses
 * {@link ImportHttpServices} annotations on the importing configuration class
 * to determine the HTTP services and groups to register.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Olga Maciaszek-Sharma
 * @since 7.0
 */
public class ImportHttpServiceRegistrar extends AbstractHttpServiceRegistrar {

	protected static final Class<?>[] NO_CLASSES = {};

	private @Nullable MetadataReaderFactory metadataReaderFactory;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		super.setResourceLoader(resourceLoader);
		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
	}

	@Override
	protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata importingClassMetadata) {

		Consumer<MergedAnnotation<?>> processImportAnnotation = annotation ->
				processImportAnnotation(annotation, registry, importingClassMetadata);
		MergedAnnotation<?> container = importingClassMetadata.getAnnotations().get(ImportHttpServices.Container.class);
		if (container.isPresent()) {
			Arrays.stream(container.getAnnotationArray(MergedAnnotation.VALUE, ImportHttpServices.class))
				.forEach(processImportAnnotation);
		}
		importingClassMetadata.getAnnotations().stream(ImportHttpServices.class).forEach(processImportAnnotation);
	}

	private void processImportAnnotation(MergedAnnotation<?> importHttpServices, GroupRegistry registry,
			AnnotationMetadata metadata) {

		GroupProvider groupProvider = getGroupProvider(importHttpServices);
		HttpServiceGroup.ClientType clientType = importHttpServices.getEnum("clientType", HttpServiceGroup.ClientType.class);
		Class<?>[] types = importHttpServices.getClassArray("types");
		Class<?>[] basePackageClasses = importHttpServices.getClassArray("basePackageClasses");
		String[] basePackages = importHttpServices.getStringArray("basePackages");
		if (ObjectUtils.isEmpty(types) && ObjectUtils.isEmpty(basePackages) && ObjectUtils.isEmpty(basePackageClasses)) {
			basePackages = new String[] { ClassUtils.getPackageName(metadata.getClassName()) };
		}
		registerHttpServices(registry, groupProvider, clientType, types, basePackageClasses, basePackages);
	}

	private GroupProvider getGroupProvider(MergedAnnotation<?> annotation) {
		String group = annotation.getString("group");
		Class<?> groupProvider = annotation.getClass("groupProvider");
		if (groupProvider == GroupProvider.class) {
			return new FixedGroupProvider(StringUtils.hasText(group) ? group : HttpServiceGroup.DEFAULT_GROUP_NAME);
		}
		Assert.state(!StringUtils.hasText(group), "'group' cannot be mixed with 'groupProvider'");
		return (GroupProvider) BeanUtils.instantiateClass(groupProvider);
	}

	/**
	 * Register HTTP service to given registry.
	 * @param registry the group registry
	 * @param groupProvider the group provider to use
	 * @param clientType the client type to use
	 * @param types the types to register
	 * @param basePackages the base packages to register
	 */
	protected final void registerHttpServices(GroupRegistry registry, GroupProvider groupProvider,
			ClientType clientType, Class<?>[] types, Class<?>[] basePackageClasses, String[] basePackages) {

		if (groupProvider instanceof FixedGroupProvider fixedGroupProvider) {
			GroupSpec groupSpec = registry.forGroup(fixedGroupProvider.group(), clientType);
			groupSpec.register(types);
			groupSpec.detectInBasePackages(basePackageClasses);
			groupSpec.detectInBasePackages(basePackages);
			return;
		}
		MetadataReaderFactory metadataReaderFactory = (this.metadataReaderFactory != null) ?
				this.metadataReaderFactory : new CachingMetadataReaderFactory();
		Consumer<AnnotationMetadata> register = metadata -> {
			String group = groupProvider.group(metadata);
			if (group != null) {
				registry.forGroup(group, clientType).registerTypeNames(metadata.getClassName());
			}
		};
		Arrays.stream(types)
			.map(Class::getName)
			.map(ThrowingFunction.of(metadataReaderFactory::getMetadataReader))
			.map(MetadataReader::getAnnotationMetadata)
			.forEach(register);
		Arrays.stream(basePackageClasses)
			.map(Class::getPackageName)
			.flatMap(this::findHttpServices)
			.map(this::getMetadata)
			.forEach(register);
		Arrays.stream(basePackages)
			.flatMap(this::findHttpServices)
			.map(this::getMetadata)
			.forEach(register);
	}

	private AnnotationMetadata getMetadata(BeanDefinition beanDefinition) {
		Assert.state(beanDefinition instanceof AnnotatedBeanDefinition,
				"AnnotatedBeanDefinition required when using 'groupProvider'");
		return ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
	}

	private static record FixedGroupProvider(String group) implements GroupProvider {

		@Override
		public String group(AnnotationMetadata metadata) {
			return this.group;
		}
	}
}
