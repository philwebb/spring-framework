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

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;

/**
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class AnnotationHttpServiceRegistrar extends AbstractHttpServiceRegistrar {

	@Override
	protected final void registerHttpServices(HttpServiceRegistry registry, AnnotationMetadata importMetadata) {

		if (getClass().equals(AnnotationHttpServiceRegistrar.class)) {
			if (isSuperseded(importMetadata)) {
				return;
			}
		}

		importMetadata.getAnnotations().stream(HttpServiceGroups.class).forEach(baseAnnot -> {
			HttpServiceGroup.ClientType clientType = baseAnnot.getEnum("clientType", HttpServiceGroup.ClientType.class);
			for (MergedAnnotation<?> annot : baseAnnot.getAnnotationArray("value", ImportHttpServices.class)) {
				processAnnotation(annot, registry, clientType);
			}
		});

		importMetadata.getAnnotations().stream(ImportHttpServices.class).forEach(annot ->
				processAnnotation(annot, registry, HttpServiceGroup.ClientType.UNSPECIFIED));

		extendRegistrations(registry, importMetadata);
	}

	private boolean isSuperseded(AnnotationMetadata metadata) {
		return metadata.getAnnotations().stream(Import.class).anyMatch(annot -> {
			for (Class<?> importedClass : annot.getClassArray("value")) {
				if (getClass().isAssignableFrom(importedClass) && !getClass().equals(importedClass)) {
					return true;
				}
			}
			return false;
		});
	}

	private void processAnnotation(
			MergedAnnotation<?> annotation, HttpServiceRegistry httpServiceRegistry,
			HttpServiceGroup.ClientType containerClientType) {

		String groupName = annotation.getString("group");

		HttpServiceGroup.ClientType clientType = annotation.getEnum("clientType", HttpServiceGroup.ClientType.class);
		clientType = (clientType != HttpServiceGroup.ClientType.UNSPECIFIED ? clientType : containerClientType);

		httpServiceRegistry.forGroup(groupName, clientType)
				.registerHttpServiceTypes(annotation.getClassArray("httpServiceTypes"))
				.detectInBasePackages(annotation.getStringArray("basePackages"))
				.detectInBasePackages(annotation.getClassArray("basePackageClasses"));
	}

	/**
	 *
	 * @param httpServiceRegistry
	 * @param metadata
	 */
	protected void extendRegistrations(HttpServiceRegistry httpServiceRegistry, AnnotationMetadata metadata) {
	}

}
