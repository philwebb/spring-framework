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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.invoker.HttpServiceProxyFactoryProvider;

/**
 * {@link InstanceSupplier} that uses a {@link HttpServiceProxyFactory} to create the bean
 * instance.
 *
 * @author Phillip Webb
 * @since 7.0
 * @param <T> the type of instance supplied by this supplier
 */
class HttpServiceProxyClientInstanceSupplier<T> implements InstanceSupplier<T> {

	private String serviceType;

	private @Nullable String usingId;

	HttpServiceProxyClientInstanceSupplier(AnnotationMetadata componentMetadata, @Nullable String factory) {
		this(componentMetadata.getClassName(), factory);
	}

	HttpServiceProxyClientInstanceSupplier(String serviceType, @Nullable String usingId) {
		this.serviceType = serviceType;
		this.usingId = usingId;
	}

	@Override
	public T get(RegisteredBean registeredBean) throws Exception {
		ConfigurableListableBeanFactory beanFactory = registeredBean.getBeanFactory();
		Class<T> serviceType = resolveServiceType(beanFactory.getBeanClassLoader());
		return getFactory(beanFactory).createClient(serviceType);
	}

	@SuppressWarnings("unchecked")
	private Class<T> resolveServiceType(@Nullable ClassLoader classLoader) throws ClassNotFoundException, LinkageError {
		return (Class<T>) ClassUtils.forName(this.serviceType, classLoader);
	}

	private HttpServiceProxyFactory getFactory(ConfigurableListableBeanFactory beanFactory) {
		if (!StringUtils.hasLength(this.usingId)) {
			return beanFactory.getBean(HttpServiceProxyFactory.class);
		}
		HttpServiceProxyFactoryProvider provider = beanFactory.getBean(HttpServiceProxyFactoryProvider.class);
		return provider.getRequiredHttpServiceProxyFactory(this.usingId);
	}

}
