/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.generator;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.BeanInfoFactory;
import org.springframework.beans.ExtendedBeanInfoFactory;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.lang.Nullable;

/**
 * A {@link BeanInstantiationContributor} for bean {@linkplain PropertyValues
 * properties}.
 *
 * @author Stephane Nicoll
 */
class PropertyValuesBeanInstantiationContributor implements BeanInstantiationContributor {

	private static final BeanInfoFactory beanInfoFactory = new ExtendedBeanInfoFactory();

	private final BeanDefinition beanDefinition;

	public PropertyValuesBeanInstantiationContributor(BeanDefinition beanDefinition) {
		this.beanDefinition = beanDefinition;
	}

	@Override
	public void contribute(CodeContribution contribution) {
		if (!this.beanDefinition.hasPropertyValues()) {
			return;
		}
		BeanInfo beanInfo = getBeanInfo(this.beanDefinition.getResolvableType().toClass());
		if (beanInfo != null) {
			ReflectionHints reflectionHints = contribution.runtimeHints().reflection();
			this.beanDefinition.getPropertyValues().getPropertyValueList().forEach(propertyValue -> {
				Method writeMethod = findWriteMethod(beanInfo, propertyValue.getName());
				if (writeMethod != null) {
					reflectionHints.registerMethod(writeMethod, hint -> hint.withMode(ExecutableMode.INVOKE));
				}
			});
		}
	}

	@Nullable
	private BeanInfo getBeanInfo(Class<?> beanType) {
		try {
			BeanInfo beanInfo = beanInfoFactory.getBeanInfo(beanType);
			if (beanInfo != null) {
				return beanInfo;
			}
			return Introspector.getBeanInfo(beanType, Introspector.IGNORE_ALL_BEANINFO);
		}
		catch (IntrospectionException ex) {
			return null;
		}
	}

	@Nullable
	private Method findWriteMethod(BeanInfo beanInfo, String propertyName) {
		return Arrays.stream(beanInfo.getPropertyDescriptors())
				.filter(pd -> propertyName.equals(pd.getName()))
				.map(java.beans.PropertyDescriptor::getWriteMethod)
				.filter(Objects::nonNull).findFirst().orElse(null);
	}
}
