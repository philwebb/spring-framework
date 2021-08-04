/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.function;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 *
 * @author Phillip Webb
 * @since 6.0
 */
public abstract class AbstractFunctionalBeanFactory implements FunctionalBeanFactory {

	protected static final Object[] NO_ARGS = {};

	@Override
	public Object getBean(String name) throws BeansException {
		return getBean(BeanSelector.byName(name));
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return getBean(BeanSelector.byName(name), requiredType);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		return getBean(BeanSelector.byName(name), args);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return getBean(BeanSelector.byType(requiredType));
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		return getBean(BeanSelector.byType(requiredType), args);
	}

	@Override
	public <T> T getBean(BeanSelector<T> selector) throws BeansException {
		return getBean(selector, NO_ARGS);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		return getBeanProvider(BeanSelector.byType(requiredType));
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		return getBeanProvider(BeanSelector.byType(requiredType));
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType,
			boolean allowEagerInit) {
		return getBeanProvider(BeanSelector.byType(requiredType), allowEagerInit);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType,
			boolean allowEagerInit) {
		return getBeanProvider(BeanSelector.byType(requiredType), allowEagerInit);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(BeanSelector<T> selector)
			throws BeansException {
		return getBeanProvider(selector, true);
	}

	@Override
	public boolean containsBean(String name) {
		return containsBean(BeanSelector.byName(name));
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return isSingleton(BeanSelector.byName(name));
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		return isPrototype(BeanSelector.byName(name));
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch)
			throws NoSuchBeanDefinitionException {
		return isTypeMatch(BeanSelector.byName(name), typeToMatch);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch)
			throws NoSuchBeanDefinitionException {
		return isTypeMatch(BeanSelector.byName(name), typeToMatch);
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return getType(BeanSelector.byName(name));
	}

	@Override
	public Class<?> getType(String name, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {
		return getType(BeanSelector.byName(name), allowFactoryBeanInit);
	}

	public <T> Class<?> getType(BeanSelector<T> selector)
			throws NoSuchBeanDefinitionException {
		return getType(selector, true);
	}

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return containsBeanDefinition(BeanSelector.byName(beanName));
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		return getBeanNames(BeanSelector.byType(type));
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons,
			boolean allowEagerInit) {
		return getBeanNames(BeanSelector.byType(type), includeNonSingletons,
				allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(Class<?> type) {
		return getBeanNames(BeanSelector.byType(type));
	}

	@Override
	public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons,
			boolean allowEagerInit) {
		return getBeanNames(BeanSelector.byType(type), includeNonSingletons,
				allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		return getBeans(BeanSelector.byType(type));
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons,
			boolean allowEagerInit) throws BeansException {
		return getBeans(BeanSelector.byType(type), includeNonSingletons, allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeans(BeanSelector<T> selector) throws BeansException {
		return getBeans(selector, true, true);
	}

	@Override
	public <T> String[] getBeanNames(BeanSelector<T> selector) {
		return getBeanNames(selector, true, true);
	}

	@Override
	public String[] getBeanNamesForAnnotation(
			Class<? extends Annotation> annotationType) {
		return getBeanNames(BeanSelector.byAnnotation(annotationType));
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(
			Class<? extends Annotation> annotationType) throws BeansException {
		return getBeans(BeanSelector.byAnnotation(annotationType));
	}

	@Override
	public <A extends Annotation> A findAnnotationOnBean(String beanName,
			Class<A> annotationType) throws NoSuchBeanDefinitionException {
		Class<?> type = getType(BeanSelector.byName(beanName));
		MergedAnnotation<A> annotation = (type != null)
				? MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY).get(
						annotationType)
				: MergedAnnotation.missing();
		return (annotation.isPresent()) ? annotation.synthesize() : null;
	}

}
