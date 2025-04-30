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


import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.service.registry.ImportHttpServices.Include;

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
class AnnotationHttpServiceRegistrar extends AbstractHttpServiceRegistrar {

	@Override
	protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {

		MergedAnnotation<?> container = metadata.getAnnotations().get(ImportHttpServices.Container.class);
		if (container.isPresent()) {
			for (MergedAnnotation<?> importHttpServices : container.getAnnotationArray("value", ImportHttpServices.class)) {
				processImportAnnotation(importHttpServices, registry);
			}
		}

		metadata.getAnnotations().stream(ImportHttpServices.class)
				.forEach(annot -> processImportAnnotation(annot, registry));
	}

	private void processImportAnnotation(MergedAnnotation<?> importHttpServices, GroupRegistry groupRegistry) {

		GroupRegistrar groupRegistrar = new GroupRegistrar(importHttpServices);

		groupRegistry.forGroup(groupRegistrar::groupName, groupRegistrar::clientType)
				.register(importHttpServices.getClassArray("types"))
				.detectInBasePackages(importHttpServices.getStringArray("basePackages"))
				.detectInBasePackages(importHttpServices.getClassArray("basePackageClasses"));
	}

	private record GroupRegistrar(ImportHttpServices.Include include, String defaultGroupName,
			HttpServiceGroup.ClientType defaultClientType) {


		GroupRegistrar(MergedAnnotation<?> annotation) {

			this(annotation.getEnum("include", ImportHttpServices.Include.class),
					getGroup(annotation), getClientType(annotation));
		}


		@Nullable String groupName(Class<?> httpServiceType) {
			MergedAnnotation<?> httpServiceClient = getHttpServiceClientAnnotation(httpServiceType);
			if (include() == Include.ANNOTATED_CLIENTS && !httpServiceClient.isPresent()) {
				return null;
			}
			String name = getGroup(httpServiceClient);
			if (StringUtils.hasText(name)) {
				Assert.state(name.equals(defaultGroupName()) || !StringUtils.hasText(defaultGroupName()),
						"HTTP Service 'group' attributes cannot be specified on both @HttpServiceClient and @ImportHttpServices annotations");
				return name;
			}
			return (StringUtils.hasText(defaultGroupName()) ? defaultGroupName() : HttpServiceGroup.DEFAULT_GROUP_NAME);
		}

		HttpServiceGroup.ClientType clientType(Class<?> httpServiceType) {
			HttpServiceGroup.ClientType clientType = getClientType(getHttpServiceClientAnnotation(httpServiceType));
			if (clientType != HttpServiceGroup.ClientType.UNSPECIFIED) {
				Assert.state(clientType == defaultClientType() || defaultClientType() == HttpServiceGroup.ClientType.UNSPECIFIED,
						"HTTP Service 'clientType' attributes cannot be specified on both @HttpServiceClient and @ImportHttpServices annotations");
				return clientType;
			}
			return defaultClientType();
		}

		private MergedAnnotation<HttpServiceClient> getHttpServiceClientAnnotation(Class<?> httpServiceType) {
			return MergedAnnotations.from(httpServiceType).get(HttpServiceClient.class);
		}

		private static HttpServiceGroup.ClientType getClientType(MergedAnnotation<?> annotation) {
			return (annotation.isPresent()) ?
					annotation.getEnum("clientType", HttpServiceGroup.ClientType.class) :
					HttpServiceGroup.ClientType.UNSPECIFIED;
		}

		private static String getGroup(MergedAnnotation<?> annotation) {
			return (annotation.isPresent()) ? annotation.getString("group") : "";
		}
	}
}
