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

package org.springframework.beans.factory.support;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.function.ThrowableConsumer;

/**
 *
 * @author pwebb
 * @since 6.0
 */
public class AutowiredFieldResolver {

	public void resolve(RegisteredBean registeredBean, ThrowableConsumer<Object> action) {

	}

	public void resolveAndSet(RegisteredBean registeredBean, Object instance) {

	}

	public Object resolve(RegisteredBean registeredBean) {
		Field field = getField();
		boolean required = false;
		Class<?> beanClass = null;
		String beanName = null;
		ConfigurableListableBeanFactory beanFactory = null;
		DependencyDescriptor desc = new DependencyDescriptor(field, required);
		desc.setContainingClass(beanClass);
		Set<String> autowiredBeanNames = new LinkedHashSet<>(1);
		Assert.state(beanFactory != null, "No BeanFactory available");
		TypeConverter typeConverter = beanFactory.getTypeConverter();
		Object value;
		try {
			value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter);
		}
		catch (BeansException ex) {
			throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(field), ex);
		}
		if (value != null || required) {
			registerDependentBeans(beanName, autowiredBeanNames);
		}
		return value;
	}

	/**
	 * Register the specified bean as dependent on the autowired beans.
	 */
	private void registerDependentBeans(@Nullable String beanName, Set<String> autowiredBeanNames) {
		if (beanName != null) {
			for (String autowiredBeanName : autowiredBeanNames) {
				if (this.beanFactory != null && this.beanFactory.containsBean(autowiredBeanName)) {
					this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Autowiring by type from bean name '" + beanName + "' to bean named '"
							+ autowiredBeanName + "'");
				}
			}
		}
	}

	/**
	 * @return
	 */
	private Field getField() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	static AutowiredFieldResolver forField(String fieldName) {
		return null;
	}

	static AutowiredFieldResolver forRequiredField(String fieldName) {
		return null;
	}

}
