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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.context.annotation.ComponentProxyFactory;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link ComponentProxyFactory} registered in {@code spring.factories} to support
 * {@link HttpServiceClient @HttpServiceClient} annotated interfaces.
 *
 * @author Phillip Webb
 * @since 7.0
 */
class HttpServiceClientComponentProxyFactory implements ComponentProxyFactory {

	@Override
	public @Nullable InstanceSupplier<?> createProxyInstanceSupplier(AnnotationMetadata componentMetadata) {
		MergedAnnotation<HttpServiceClient> annotation = componentMetadata.getAnnotations().get(HttpServiceClient.class);
		if (!annotation.isPresent()) {
			return null;
		}
		return new HttpServiceProxyClientInstanceSupplier<>(componentMetadata, annotation.getString("group"));
	}

}
