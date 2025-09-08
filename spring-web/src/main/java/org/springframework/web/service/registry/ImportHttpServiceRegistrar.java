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
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;
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

	private MetadataReaderFactory metadataReaderFactory;

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
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

	private void processImportAnnotation(MergedAnnotation<?> importHttpServices, GroupRegistry groupRegistry) {

		String group = importHttpServices.getString("group");
		Class<?> groupProvider = importHttpServices.getClass("groupProvider");
		HttpServiceGroup.ClientType clientType = importHttpServices.getEnum("clientType", HttpServiceGroup.ClientType.class);

		ImportProcessor importProcessor = new ImportProcessor(groupRegistry, group, groupProvider, clientType);

		importProcessor.processTypes(importHttpServices.getClassArray("types"));
		importProcessor.processBasePackages(importHttpServices.getStringArray("basePackages"));
		importProcessor.processBasePackages(importHttpServices.getClassArray("basePackageClasses"));
	}

	class ImportProcessor {

		private GroupRegistry groupRegistry;

		private String group;

		GroupProvider groupProvider;

		private ClientType clientType;

		ImportProcessor(GroupRegistry groupRegistry, String group, Class<?> groupProviderClass, ClientType clientType) {
			this.groupRegistry = groupRegistry;
			this.clientType = clientType;
		}

		void processTypes(Class<?>[] classArray) {
			if (this.groupProvider == null) {
				this.groupRegistry.forGroup(this.group, this.clientType).register(classArray);
			}
			else {
//				Arrays.stream(classArray).map(Class::getName).forEach(this::dunno);
//				metadataReaderFactory.getMetadataReader();
				// FIXME dance
			}
		}



		void processBasePackages(String[] basePackages) {
			processBasePackages(Arrays.stream(basePackages));
		}

		void processBasePackages(Class<?>[] basePackages) {
			processBasePackages(Arrays.stream(basePackages).map(Class::getPackageName));
		}

		private void processBasePackages(Stream<String> basePackages) {
			basePackages.flatMap(ImportHttpServiceRegistrar.this::findHttpServices)
				.map(this::getRegistration)
				.forEach(this::register);
		}

		private Registration getRegistration(BeanDefinition beanDefinition) {
			String group = getGroup(beanDefinition);
			return (group != null) ? new Registration(group, beanDefinition.getBeanClassName()) : null;
		}

		private String getGroup(BeanDefinition beanDefinition) {
			if (this.groupProvider == null) {
				return this.group;
			}
			Assert.state(beanDefinition instanceof AnnotatedBeanDefinition,
					"AnnotatedBeanDefinition required when using 'groupProvider'");
			return this.groupProvider.group(((AnnotatedBeanDefinition) beanDefinition).getMetadata());
		}


		private void register(AnnotationMetadata annotationMetadata) {
			annotationMetadata.getClassName();
		}


		private void register(Registration registration) {
			if (registration != null) {
				this.groupRegistry.forGroup(registration.group(), this.clientType)
					.registerTypeNames(registration.typeName());
			}
		}

		record Registration(String group, String typeName) {

		}

	}

}
