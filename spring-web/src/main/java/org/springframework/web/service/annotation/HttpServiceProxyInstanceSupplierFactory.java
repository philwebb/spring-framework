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

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.ProxyInstanceSupplierFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.service.invoker.HttpServiceProxyCreator;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * {@link ProxyInstanceSupplierFactory} for {@link HttpService @HttpService}
 * annotated interfaces.
 *
 * @author Phillip Webb
 * @since 7.0
 */
class HttpServiceProxyInstanceSupplierFactory implements ProxyInstanceSupplierFactory {

	@Override
	@Nullable
	public <T> InstanceSupplier<T> createProxyInstanceSupplier(Class<T> type) {
		MergedAnnotation<HttpService> annotation = MergedAnnotations.from(type)
				.get(HttpService.class);
		return (!annotation.isPresent()) ? null : new HttpServiceProxyInstanceSupplier<>(
				type, annotation.getString("connection"));
	}

	private static class HttpServiceProxyInstanceSupplier<T>
			implements InstanceSupplier<T> {

		private final Class<T> type;

		private final String connection;

		HttpServiceProxyInstanceSupplier(Class<T> type, String connection) {
			this.type = type;
			this.connection = connection;
		}

		@Override
		public T get(RegisteredBean registeredBean) throws Exception {
			ListableBeanFactory beanFactory = registeredBean.getBeanFactory();
			if (!StringUtils.hasLength(this.connection)) {
				return createProxy(beanFactory.getBean(HttpServiceProxyFactory.class));
			}
			Object bean = beanFactory.getBean(this.connection);
			if (bean instanceof HttpServiceProxyFactory factory) {
				return createProxy(factory);
			}
			if (bean instanceof HttpServiceProxyCreator creator) {
				return createProxy(creator.serviceProxyFactory());
			}
			throw new IllegalStateException("The bean '" + this.connection
					+ "' is not a HttpServiceProxyFactory or HttpServiceProxyCreator");
		}

		private T createProxy(HttpServiceProxyFactory factory) {
			return factory.createClient(this.type);
		}

	}

}
