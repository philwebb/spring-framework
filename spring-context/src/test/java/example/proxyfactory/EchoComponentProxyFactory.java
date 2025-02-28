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

package example.proxyfactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.context.annotation.ComponentProxyFactory;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

public class EchoComponentProxyFactory implements ComponentProxyFactory {

	@Override
	public @Nullable InstanceSupplier<?> createProxyInstanceSupplier(AnnotationMetadata componentMetadata) {
		return registeredBean -> createProxy(registeredBean, componentMetadata);
	}

	@SuppressWarnings("unchecked")
	private <T> T createProxy(RegisteredBean registeredBean, AnnotationMetadata componentMetadata) throws Exception {
		ConfigurableListableBeanFactory beanFactory = registeredBean.getBeanFactory();
		ClassLoader beanClassLoader = (beanFactory != null) ? beanFactory.getBeanClassLoader() : null;
		Class<?> type = ClassUtils.forName(componentMetadata.getClassName(), beanClassLoader);
		return (T) Proxy.newProxyInstance(getClassLoader(registeredBean), new Class<?>[] { type }, this::invoke);
	}

	private ClassLoader getClassLoader(RegisteredBean registeredBean) {
		ConfigurableListableBeanFactory beanFactory = registeredBean.getBeanFactory();
		return (beanFactory != null) ? beanFactory.getBeanClassLoader() : ClassUtils.getDefaultClassLoader();
	}

	private Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return (!String.class.equals(method.getReturnType())) ? null : method.getName();
	}

}
