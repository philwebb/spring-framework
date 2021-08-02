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
import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ResolvableType;

/**
 *
 * @author pwebb
 * @since 5.2
 */
public abstract class DefaultFunctionalBeanFactory implements FunctionalBeanFactory, FunctionalBeanRegistry {

	@Override
	public Object getBean(String name) throws BeansException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean containsBean(String name) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch)
			throws NoSuchBeanDefinitionException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch)
			throws NoSuchBeanDefinitionException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Class<?> getType(String name, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String[] getAliases(String name) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean containsBeanDefinition(String beanName) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public int getBeanDefinitionCount() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String[] getBeanDefinitionNames() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType,
			boolean allowEagerInit) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType,
			boolean allowEagerInit) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons,
			boolean allowEagerInit) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String[] getBeanNamesForType(Class<?> type) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons,
			boolean allowEagerInit) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons,
			boolean allowEagerInit) throws BeansException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String[] getBeanNamesForAnnotation(
			Class<? extends Annotation> annotationType) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(
			Class<? extends Annotation> annotationType) throws BeansException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <A extends Annotation> A findAnnotationOnBean(String beanName,
			Class<A> annotationType) throws NoSuchBeanDefinitionException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}


}
