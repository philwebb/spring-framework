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

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.context.annotation.ScannedComponentProxyFactory;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.service.invoker.HttpServiceProxyCreator;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * {@link ScannedComponentProxyFactory} for {@link HttpService @HttpService} annotated
 * interfaces and {@link HttpServiceProxyFactory}/{@link HttpServiceProxyCreator} beans.
 *
 * @author Phillip Webb
 * @since 7.0
 */
class HttpServiceScannedComponentProxyFactory implements ScannedComponentProxyFactory {

	@Override
	public InstanceSupplier<?> createProxyInstanceSupplier(AnnotationMetadata scannedComponentMetadata, Object bean,
			String beanName) {
		if (bean instanceof HttpServiceProxyFactory || bean instanceof HttpServiceProxyCreator) {
			return createProxyInstanceSupplierForBean(scannedComponentMetadata, bean, beanName);
		}
		MergedAnnotation<HttpService> annotation = scannedComponentMetadata.getAnnotations().get(HttpService.class);
		if (annotation.isPresent()) {
			return createProxyForHttpServiceAnnotatedType(scannedComponentMetadata, annotation);
		}
		return null;
	}

	private InstanceSupplier<?> createProxyInstanceSupplierForBean(AnnotationMetadata scannedComponentMetadata,
			Object bean, String beanName) {
		return (registeredBean) -> {
			ConfigurableListableBeanFactory beanFactory = registeredBean.getBeanFactory();
			return createProxy(beanFactory.getBeanClassLoader(), scannedComponentMetadata, bean, beanName);
		};
	}

	private InstanceSupplier<?> createProxyForHttpServiceAnnotatedType(AnnotationMetadata scannedComponentMetadata,
			MergedAnnotation<HttpService> annotation) {
		return (registeredBean) -> {
			ConfigurableListableBeanFactory beanFactory = registeredBean.getBeanFactory();
			String beanName = annotation.getString("createdBy");
			Object bean = !StringUtils.hasLength(beanName) ? deduceBean(beanFactory) : beanFactory.getBean(beanName);
			return createProxy(beanFactory.getBeanClassLoader(), scannedComponentMetadata, bean, beanName);
		};
	}

	private Object deduceBean(ConfigurableListableBeanFactory beanFactory) {
		try {
			return beanFactory.getBean(HttpServiceProxyFactory.class);
		} catch (NoSuchBeanDefinitionException ex) {
		}
		try {
			return beanFactory.getBean(HttpServiceProxyCreator.class);
		} catch (NoSuchBeanDefinitionException ex) {
		}
		throw new NoSuchBeanDefinitionException(HttpServiceProxyFactory.class,
				"No HttpServiceProxyFactory or HttpServiceProxyCreator bean available");
	}

	@Nullable
	private Object createProxy(ClassLoader classLoader, AnnotationMetadata scannedComponentMetadata, Object bean,
			String beanName) throws ClassNotFoundException, LinkageError {
		Class<?> type = ClassUtils.forName(scannedComponentMetadata.getClassName(), classLoader);
		if (bean instanceof HttpServiceProxyFactory factory) {
			return factory.serviceProxy(type);
		}
		if (bean instanceof HttpServiceProxyCreator creator) {
			return creator.serviceProxy(type);
		}
		throw new IllegalStateException("The bean '%s' is not a HttpServiceProxyFactory or HttpServiceProxyCreator"
			.formatted((beanName != null) ? beanName : bean.getClass().getName()));
	}

}
