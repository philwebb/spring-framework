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

package org.springframework.context.function;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.function.BeanSelector;
import org.springframework.beans.factory.function.FunctionalBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Abstract implementation of {@link FunctionalApplicationContext}. Delegates
 * {@link MessageSource}, {@link ApplicationEventPublisher},
 * {@link ResourcePatternResolver} and {@link FunctionalBeanFactory} to the
 * instances provided by {@link #getMessageSource()},
 * {@link #getApplicationEventPublisher()},
 * {@link #getResourcePatternResolver()} and {@link #getBeanFactory()}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public abstract class AbstractFunctionalApplicationContext
		implements FunctionalApplicationContext {

	public String getMessage(String code, Object[] args, String defaultMessage,
			Locale locale) {
		return getMessageSource().getMessage(code, args, defaultMessage, locale);
	}

	public String getMessage(String code, Object[] args, Locale locale)
			throws NoSuchMessageException {
		return getMessageSource().getMessage(code, args, locale);
	}

	public String getMessage(MessageSourceResolvable resolvable, Locale locale)
			throws NoSuchMessageException {
		return getMessageSource().getMessage(resolvable, locale);
	}

	public void publishEvent(ApplicationEvent event) {
		getApplicationEventPublisher().publishEvent(event);
	}

	public void publishEvent(Object event) {
		getApplicationEventPublisher().publishEvent(event);
	}

	public Resource getResource(String location) {
		return getResourcePatternResolver().getResource(location);
	}

	public ClassLoader getClassLoader() {
		return getResourcePatternResolver().getClassLoader();
	}

	public Resource[] getResources(String locationPattern) throws IOException {
		return getResourcePatternResolver().getResources(locationPattern);
	}

	public <T> T getBean(BeanSelector<T> selector) throws BeansException {
		return getBeanFactory().getBean(selector);
	}

	public <S, T> T getBean(BeanSelector<S> selector, Class<T> requiredType)
			throws BeansException {
		return getBeanFactory().getBean(selector, requiredType);
	}

	public boolean containsBeanDefinition(String beanName) {
		return getBeanFactory().containsBeanDefinition(beanName);
	}

	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}

	public <T> T getBean(BeanSelector<T> selector, Object... args) throws BeansException {
		return getBeanFactory().getBean(selector, args);
	}

	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType,
			boolean allowEagerInit) {
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	public <T> ObjectProvider<T> getBeanProvider(BeanSelector<T> selector)
			throws BeansException {
		return getBeanFactory().getBeanProvider(selector);
	}

	public <T> ObjectProvider<T> getBeanProvider(BeanSelector<T> selector,
			boolean allowEagerInit) throws BeansException {
		return getBeanFactory().getBeanProvider(selector, allowEagerInit);
	}

	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType,
			boolean allowEagerInit) {
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	public <T> boolean containsBean(BeanSelector<T> selector) {
		return getBeanFactory().containsBean(selector);
	}

	public String[] getBeanNamesForType(ResolvableType type) {
		return getBeanFactory().getBeanNamesForType(type);
	}

	public <T> boolean isSingleton(BeanSelector<T> selector)
			throws NoSuchBeanDefinitionException {
		return getBeanFactory().isSingleton(selector);
	}

	public Object getBean(String name) throws BeansException {
		return getBeanFactory().getBean(name);
	}

	public <T> boolean isPrototype(BeanSelector<T> selector)
			throws NoSuchBeanDefinitionException {
		return getBeanFactory().isPrototype(selector);
	}

	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return getBeanFactory().getBean(name, requiredType);
	}

	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons,
			boolean allowEagerInit) {
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons,
				allowEagerInit);
	}

	public <T> boolean isTypeMatch(BeanSelector<T> selector, ResolvableType typeToMatch)
			throws NoSuchBeanDefinitionException {
		return getBeanFactory().isTypeMatch(selector, typeToMatch);
	}

	public Object getBean(String name, Object... args) throws BeansException {
		return getBeanFactory().getBean(name, args);
	}

	public <T> boolean isTypeMatch(BeanSelector<T> selector, Class<?> typeToMatch)
			throws NoSuchBeanDefinitionException {
		return getBeanFactory().isTypeMatch(selector, typeToMatch);
	}

	public <T> Class<?> getType(BeanSelector<T> selector)
			throws NoSuchBeanDefinitionException {
		return getBeanFactory().getType(selector);
	}

	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return getBeanFactory().getBean(requiredType);
	}

	public String[] getBeanNamesForType(Class<?> type) {
		return getBeanFactory().getBeanNamesForType(type);
	}

	public <T> Class<?> getType(BeanSelector<T> selector, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {
		return getBeanFactory().getType(selector, allowFactoryBeanInit);
	}

	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		return getBeanFactory().getBean(requiredType, args);
	}

	public <T> boolean containsBeanDefinition(BeanSelector<T> selector) {
		return getBeanFactory().containsBeanDefinition(selector);
	}

	public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons,
			boolean allowEagerInit) {
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons,
				allowEagerInit);
	}

	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		return getBeanFactory().getBeanProvider(requiredType);
	}

	public <T> String[] getBeanNames(BeanSelector<T> selector) {
		return getBeanFactory().getBeanNames(selector);
	}

	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		return getBeanFactory().getBeanProvider(requiredType);
	}

	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	public <T> String[] getBeanNames(BeanSelector<T> selector,
			boolean includeNonSingletons, boolean allowEagerInit) {
		return getBeanFactory().getBeanNames(selector, includeNonSingletons,
				allowEagerInit);
	}

	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().isSingleton(name);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		return getBeanFactory().getBeansOfType(type);
	}

	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().isPrototype(name);
	}

	public <T> Map<String, T> getBeans(BeanSelector<T> selector) throws BeansException {
		return getBeanFactory().getBeans(selector);
	}

	public boolean isTypeMatch(String name, ResolvableType typeToMatch)
			throws NoSuchBeanDefinitionException {
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons,
			boolean allowEagerInit) throws BeansException {
		return getBeanFactory().getBeansOfType(type, includeNonSingletons,
				allowEagerInit);
	}

	public boolean isTypeMatch(String name, Class<?> typeToMatch)
			throws NoSuchBeanDefinitionException {
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	public <T> Map<String, T> getBeans(BeanSelector<T> selector,
			boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
		return getBeanFactory().getBeans(selector, includeNonSingletons, allowEagerInit);
	}

	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getType(name);
	}

	public String[] getBeanNamesForAnnotation(
			Class<? extends Annotation> annotationType) {
		return getBeanFactory().getBeanNamesForAnnotation(annotationType);
	}

	public Class<?> getType(String name, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException {
		return getBeanFactory().getType(name, allowFactoryBeanInit);
	}

	public Map<String, Object> getBeansWithAnnotation(
			Class<? extends Annotation> annotationType) throws BeansException {
		return getBeanFactory().getBeansWithAnnotation(annotationType);
	}

	public String[] getAliases(String name) {
		return getBeanFactory().getAliases(name);
	}

	public <A extends Annotation> A findAnnotationOnBean(String beanName,
			Class<A> annotationType) throws NoSuchBeanDefinitionException {
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
	}

	public BeanFactory getParentBeanFactory() {
		return getHierarchicalBeanFactory().getParentBeanFactory();
	}

	public boolean containsLocalBean(String name) {
		return getHierarchicalBeanFactory().containsLocalBean(name);
	}

	@Override
	public void close() {
	}

	protected abstract MessageSource getMessageSource();

	protected abstract ApplicationEventPublisher getApplicationEventPublisher();

	protected abstract ResourcePatternResolver getResourcePatternResolver();

	protected HierarchicalBeanFactory getHierarchicalBeanFactory() {
		return (HierarchicalBeanFactory) getBeanFactory();
	}

}
