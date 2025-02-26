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

package org.springframework.web.service.annotation;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.context.annotation.ScannedComponentProxyFactory;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.service.invoker.HttpServiceProxyCreator;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * {@link ScannedComponentProxyFactory} for {@link HttpService @HttpService} annotated
 * interfaces.
 *
 * @author Phillip Webb
 * @since 7.0
 */
class HttpServiceProxyInstanceSupplierFactory implements ScannedComponentProxyFactory {

	@Override
	@Nullable
	public InstanceSupplier<?> createProxyInstanceSupplier(AnnotationMetadata type) {
		MergedAnnotation<HttpService> annotation = type.getAnnotations().get(HttpService.class);
		return (!annotation.isPresent()) ? null
				: new HttpServiceProxyInstanceSupplier<>(type.getClassName(), annotation.getString("createdBy"));
	}

	private static class HttpServiceProxyInstanceSupplier<T> implements InstanceSupplier<T> {

		// FIXME make reusable

		private final String typeName;

		private final String createdBy;

		HttpServiceProxyInstanceSupplier(String typeName, String createdBy) {
			this.typeName = typeName;
			this.createdBy = createdBy;
		}

		@Override
		public T get(RegisteredBean registeredBean) throws Exception {
			return get(registeredBean.getBeanFactory());
		}

		private T get(ListableBeanFactory beanFactory) {
			try {
				Class<T> type = (Class<T>) ClassUtils.forName(typeName, null);
				if (!StringUtils.hasLength(this.createdBy)) {
					return get(type, beanFactory.getBean(HttpServiceProxyFactory.class));
				}
				Object bean = beanFactory.getBean(this.createdBy);
				if (bean instanceof HttpServiceProxyFactory factory) {
					return get(type, factory);
				}
				if (bean instanceof HttpServiceProxyCreator creator) {
					return get(type, creator.serviceProxyFactory());
				}
				throw new IllegalStateException("The bean '" + this.createdBy
						+ "' is not a HttpServiceProxyFactory or HttpServiceProxyCreator");
			}
			catch (Exception ex) {
				throw new BeanCreationException("Unable to create HTTP service proxy for " + this.type, ex);
			}
		}

		private T get(Class<T> type, HttpServiceProxyFactory proxyFactory) {
			return proxyFactory.serviceProxy(type);
		}

	}

}
