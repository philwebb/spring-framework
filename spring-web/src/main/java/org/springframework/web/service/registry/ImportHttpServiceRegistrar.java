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
import org.springframework.util.StringUtils;
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
class ImportHttpServiceRegistrar extends AbstractHttpServiceRegistrar {

	private @Nullable MetadataReaderFactory metadataReaderFactory;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		super.setResourceLoader(resourceLoader);
		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
	}

	@Override
	protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {

		MergedAnnotation<?> groupsAnnot = metadata.getAnnotations().get(ImportHttpServices.Container.class);
		if (groupsAnnot.isPresent()) {
			for (MergedAnnotation<?> annot : groupsAnnot.getAnnotationArray("value", ImportHttpServices.class)) {
				processImportAnnotation(annot, registry);
			}
		}

		metadata.getAnnotations().stream(ImportHttpServices.class)
				.forEach(annot -> processImportAnnotation(annot, registry));
	}

	private void processImportAnnotation(MergedAnnotation<?> annotation, GroupRegistry groupRegistry) {

		String group = annotation.getString("group");
		Class<?> groupProvider = annotation.getClass("groupProvider");
		HttpServiceGroup.ClientType clientType = annotation.getEnum("clientType", HttpServiceGroup.ClientType.class);

		ImportProcessor importProcessor = new ImportProcessor(groupRegistry, group, groupProvider, clientType);

		importProcessor.processBasePackages(annotation.getStringArray("basePackages"));
		importProcessor.processBasePackages(annotation.getClassArray("basePackageClasses"));
		importProcessor.processTypes(annotation.getClassArray("types"));
	}


	private class ImportProcessor {

		private final MetadataReaderFactory metadataReaderFactory;

		private final GroupRegistry groupRegistry;

		private final GroupProvider groupProvider;

		private ClientType clientType;

		ImportProcessor(GroupRegistry groupRegistry, String group, Class<?> groupProviderClass, ClientType clientType) {
			this.metadataReaderFactory = (ImportHttpServiceRegistrar.this.metadataReaderFactory != null) ?
					ImportHttpServiceRegistrar.this.metadataReaderFactory : new CachingMetadataReaderFactory();
			this.groupRegistry = groupRegistry;
			this.groupProvider = getGroupProvider(group, groupProviderClass);
			this.clientType = clientType;
		}

		private GroupProvider getGroupProvider(String group, Class<?> groupProviderClass) {
			if (groupProviderClass == GroupProvider.class) {
				return new FixedGroupProvider(StringUtils.hasText(group) ? group : HttpServiceGroup.DEFAULT_GROUP_NAME);
			}
			Assert.state(!StringUtils.hasText(group), "'group' attribute cannot be used when a 'groupProvider' is specified");
			return (GroupProvider) BeanUtils.instantiateClass(groupProviderClass);
		}

		void processBasePackages(String[] basePackages) {
			if (this.groupProvider instanceof FixedGroupProvider fixedGroupProvider) {
				forGroup(fixedGroupProvider.group()).detectInBasePackages(basePackages);
				return;
			}
			Arrays.stream(basePackages)
				.flatMap(ImportHttpServiceRegistrar.this::findHttpServices)
				.forEach(this::register);
		}

		void processBasePackages(Class<?>[] basePackages) {
			if (this.groupProvider instanceof FixedGroupProvider fixedGroupProvider) {
				forGroup(fixedGroupProvider.group()).detectInBasePackages(basePackages);
				return;
			}
			Arrays.stream(basePackages)
				.map(Class::getPackageName)
				.flatMap(ImportHttpServiceRegistrar.this::findHttpServices)
				.forEach(this::register);
		}

		void processTypes(Class<?>[] classArray) {
			if (this.groupProvider instanceof FixedGroupProvider fixedGroupProvider) {
				forGroup(fixedGroupProvider.group()).register(classArray);
				return;
			}
			Arrays.stream(classArray).map(this::getMetadata).forEach(this::register);
		}

		private AnnotationMetadata getMetadata(Class<?> type) {
			try {
				return this.metadataReaderFactory.getMetadataReader(type.getName()).getAnnotationMetadata();
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		private void register(BeanDefinition beanDefinition) {
			Assert.state(beanDefinition instanceof AnnotatedBeanDefinition,
					"AnnotatedBeanDefinition required when using 'groupProvider'");
			register(((AnnotatedBeanDefinition) beanDefinition).getMetadata());
		}

		private void register(AnnotationMetadata metadata) {
			String group = this.groupProvider.group(metadata);
			if (group != null) {
				forGroup(group).registerTypeNames(metadata.getClassName());
			}
		}

		private GroupSpec forGroup(String group) {
			return this.groupRegistry.forGroup(group, this.clientType);
		}
	}


	private static class FixedGroupProvider implements GroupProvider {

		private final String group;

		FixedGroupProvider(String group) {
			this.group =group;
		}

		@Override
		public @Nullable String group(AnnotationMetadata metadata) {
			return group();
		}

		String group() {
			return this.group;
		}


	}
}
