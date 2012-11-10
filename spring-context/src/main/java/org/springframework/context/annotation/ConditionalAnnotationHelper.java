/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.util.List;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * Utilities for processing {@link Conditional}-annotated types.
 *
 * @author Phillip Webb
 * @since 3.2
 */
abstract class ConditionalAnnotationHelper {

	/**
	 * Determines if the specified {@link ConfigurationClass} should be skipped based on
	 * any {@link Conditional @Conditional} annotations.
	 * @param registry the bean definition registry
	 * @param resourceLoader the resource loader (or {@code null}
	 * @param configurationClass the condifuration class
	 * @return {@code true} if the configuration class should be skipped
	 */
	public static boolean shouldSkip(BeanDefinitionRegistry registry,
			ResourceLoader resourceLoader, ConfigurationClass configurationClass) {
		if (configurationClass != null) {
			if (shouldSkip(registry, resourceLoader, configurationClass.getMetadata())) {
				return true;
			}
			return shouldSkip(registry, resourceLoader,
					configurationClass.getImportedBy());
		}
		return false;
	}

	/**
	 * Determines if the specified {@link BeanMethod} should be skipped based on
	 * any {@link Conditional @Conditional} annotations.
	 * @param registry the bean definition registry
	 * @param resourceLoader the resource loader (or {@code null}
	 * @param beanMethod the bean method
	 * @return {@code true} if the bean methodshould be skipped
	 */
	public static boolean shouldSkip(BeanDefinitionRegistry registry,
			ResourceLoader resourceLoader, BeanMethod beanMethod) {
		if (beanMethod != null) {
			return shouldSkip(registry, resourceLoader, beanMethod.getMetadata());
		}
		return false;
	}

	/**
	 * Determines if the specified {@link BeanDefinition} should be skipped based on
	 * any {@link Conditional @Conditional} annotations.
	 * @param registry the bean definition registry
	 * @param resourceLoader the resource loader (or {@code null}
	 * @param beanDefinition the bean definition
	 * @return {@code true} if the bean definition should be skipped
	 */
	public static boolean shouldSkip(BeanDefinitionRegistry registry,
			ResourceLoader resourceLoader, BeanDefinition beanDefinition) {
		if (beanDefinition != null && beanDefinition instanceof AnnotatedBeanDefinition) {
			return shouldSkip(registry, resourceLoader,
					((AnnotatedBeanDefinition) beanDefinition).getMetadata());
		}
		return false;
	}

	private static boolean shouldSkip(BeanDefinitionRegistry registry,
			ResourceLoader resourceLoader, AnnotatedTypeMetadata metadata) {
		if (metadata != null) {
			if(resourceLoader == null && registry instanceof ResourceLoader) {
				resourceLoader = (ResourceLoader) registry;
			}
			MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(Conditional.class.getName());
			List<Object> conditionClassList = (attributes == null ? null
					: attributes.get("value"));
			if (conditionClassList != null) {
				for (Object conditionClasses : conditionClassList) {
					for (Object conditionClass : ObjectUtils.toObjectArray(conditionClasses)) {
						if (!getCondition(registry, resourceLoader, conditionClass).matches(
								metadata)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private static Condition getCondition(BeanDefinitionRegistry registry,
			ResourceLoader resourceLoader, Object conditionClassOrClassName) {
		Class<Condition> conditionClass = getConditionClass(resourceLoader,
				conditionClassOrClassName);
		return getAutowireCapableBeanFactory(registry).createBean(conditionClass);
	}

	private static AutowireCapableBeanFactory getAutowireCapableBeanFactory(Object source) {
		if(source instanceof AutowireCapableBeanFactory) {
			return (AutowireCapableBeanFactory) source;
		}
		if(source instanceof ConfigurableApplicationContext) {
			return getAutowireCapableBeanFactory(((ConfigurableApplicationContext) source).getBeanFactory());
		}
		throw new IllegalStateException("@Conditional beans can only be used with an AutowireCapableBeanFactory");
	}

	@SuppressWarnings("unchecked")
	private static Class<Condition> getConditionClass(ResourceLoader resourceLoader,
			Object conditionClassOrClassName) {
		Class<?> conditionClass;
		if (conditionClassOrClassName instanceof Class) {
			conditionClass = (Class<?>) conditionClassOrClassName;
		}
		else {
			String className = (String) conditionClassOrClassName;
			try {
				if (resourceLoader != null) {
					conditionClass = Class.forName(className, true,
							resourceLoader.getClassLoader());
				}
				else {
					conditionClass = Class.forName(className);
				}
			} catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Unable to create Condition from class '"
						+ className + "'", ex);
			}
		}
		Assert.isAssignable(Condition.class, conditionClass);
		return (Class<Condition>) conditionClass;
	}

}
