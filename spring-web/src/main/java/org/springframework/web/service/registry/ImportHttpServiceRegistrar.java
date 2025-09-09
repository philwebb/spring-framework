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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
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

	private @Nullable MetadataReaderFactory metadataReaderFactory;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		super.setResourceLoader(resourceLoader);
		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
	}

	@Override
	protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {

		Consumer<MergedAnnotation<?>> processImportAnnotation = annotation ->
				processImportAnnotation(annotation, registry, metadata);

				MergedAnnotation<?> container = metadata.getAnnotations().get(ImportHttpServices.Container.class);
		if (container.isPresent()) {
			Arrays.stream(container.getAnnotationArray(MergedAnnotation.VALUE, ImportHttpServices.class))
				.forEach(processImportAnnotation);
		}

		metadata.getAnnotations().stream(ImportHttpServices.class).forEach(processImportAnnotation);
	}

	private void processImportAnnotation(MergedAnnotation<?> annotation, GroupRegistry registry,
			AnnotationMetadata metadata) {

		GroupProvider groupProvider = getGroupProvider(annotation, metadata);

		HttpServiceGroup.ClientType clientType = annotation.getEnum("clientType", HttpServiceGroup.ClientType.class);
		Class<?>[] types = annotation.getClassArray("types");
		String[] basePackages = annotation.getStringArray("basePackages");
		Class<?>[] basePackageClasses = annotation.getClassArray("basePackageClasses");

		if (ObjectUtils.isEmpty(types) && ObjectUtils.isEmpty(basePackages) && ObjectUtils.isEmpty(basePackageClasses)) {
			Stream<String> basePackagesStream = Stream.of(ClassUtils.getPackageName(metadata.getClassName()));
			registerHttpServices(registry, groupProvider, clientType, basePackagesStream);
		}
		else {
			Stream<String> basePackagesStream = Stream.concat(Arrays.stream(basePackages), Arrays.stream(basePackageClasses).map(Class::getPackageName));
			registerHttpServices(registry, groupProvider, clientType, basePackagesStream, types);
		}
	}

	private GroupProvider getGroupProvider(MergedAnnotation<?> annotation, AnnotationMetadata importingClassMetadata) {
		String group = annotation.getString("group");
		Class<?> groupProvider = annotation.getClass("groupProvider");
		if (groupProvider == GroupProvider.class) {
			return GroupProvider.of(StringUtils.hasText(group) ? group : HttpServiceGroup.DEFAULT_GROUP_NAME);
		}
		Assert.state(!StringUtils.hasText(group),
				"'group' attribute cannot be used when a 'groupProvider' is specified");
		return (GroupProvider) BeanUtils.instantiateClass(groupProvider);
	}

	/**
	 * Register HTTP service to given registry.
	 * @param registry the group registry
	 * @param groupProvider the group provider to use
	 * @param clientType the client type to use
	 * @param basePackages the base packages to register
	 * @param types the types to register
	 */
	protected final void registerHttpServices(GroupRegistry registry, GroupProvider groupProvider,
			ClientType clientType, Stream<String> basePackages, Class<?>... types) {

		MetadataReaderFactory metadataReaderFactory = (this.metadataReaderFactory != null) ?
				this.metadataReaderFactory : new CachingMetadataReaderFactory();

		Consumer<AnnotationMetadata> register = metadata -> {
			String group = groupProvider.group(metadata);
			if (group != null) {
				registry.forGroup(group, clientType).registerTypeNames(metadata.getClassName());
			}
		};

		basePackages.flatMap(this::findHttpServices).map(this::getMetadata).forEach(register);
		Arrays.stream(types).map(type -> getMetadata(metadataReaderFactory, type)).forEach(register);
	}

	private AnnotationMetadata getMetadata(BeanDefinition beanDefinition) {
		Assert.state(beanDefinition instanceof AnnotatedBeanDefinition,
				"AnnotatedBeanDefinition required when using 'groupProvider'");
		return ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
	}

	private AnnotationMetadata getMetadata(MetadataReaderFactory metadataReaderFactory, Class<?> type) {
		try {
			return metadataReaderFactory.getMetadataReader(type.getName()).getAnnotationMetadata();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
