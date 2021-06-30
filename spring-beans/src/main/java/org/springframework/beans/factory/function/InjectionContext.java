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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.function.InstanceSupplier;

/**
 * Context object passed to bean {@link InstanceSupplier InstanceSuppliers} so that they
 * can perform dependency injection.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public interface InjectionContext {

	/**
	 * Obtain a bean dependency to inject based on its name.
	 * @param <T> the bean type
	 * @param type the bean type
	 * @return the bean instance
	 * @throws BeansException if the bean could not be supplied
	 */
	default <T> T getBean(Class<T> type) throws BeansException {
		return getBean(BeanSelector.byType(type));
	}

	/**
	 * Obtain a bean dependency to inject based on its name.
	 * @param <T> the bean type
	 * @param name the bean name
	 * @return the bean instance
	 * @throws BeansException if the bean could not be supplied
	 */
	default <T> T getBean(String name) throws BeansException {
		return getBean(BeanSelector.byName(name));
	}

	/**
	 * Obtain a bean dependency to inject based on its name.
	 * <p>
	 * Behaves the same as {@link #getBean(String)}, but provides a measure of type safety
	 * by throwing a {@link BeanNotOfRequiredTypeException} if the bean is not of the
	 * required type
	 * @param <T> the bean type
	 * @param name the bean name
	 * @return the bean instance
	 * @throws BeanNotOfRequiredTypeException if the bean is not of the required type
	 * @throws BeansException if the bean could not be supplied
	 */
	default <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return getBean(BeanSelector.byName(name), requiredType);
	}

	/**
	 * Obtain a bean dependency to inject based on the specified selector.
	 * @param <T> the bean type
	 * @param selector the bean selector
	 * @return the bean instance
	 * @throws BeansException if the bean could not be supplied
	 */
	<T> T getBean(BeanSelector<T> selector) throws BeansException;

	/**
	 * Obtain a bean dependency to inject based on the specified selector.
	 * <p>
	 * Behaves the same as {@link #getBean(BeanSelector)}, but provides a measure of type
	 * safety by throwing a BeanNotOfRequiredTypeException if the bean is not of the
	 * required type.
	 * @param selector the bean selector
	 * @param requiredType type the bean must match; can be an interface or superclass
	 * @return an instance of the bean
	 * @throws NoSuchBeanDefinitionException if there is no selectable bean
	 * @throws BeanNotOfRequiredTypeException if the bean is not of the required type
	 * @throws BeansException if the bean could not be created
	 */
	<S, T> T getBean(BeanSelector<S> selector, Class<T> requiredType) throws BeansException;

	/**
	 * Obtain a bean dependency to inject based on its name.
	 * <p>
	 * Behaves the same as {@link #getBean(String)}, but provides a measure of type safety
	 * by throwing a {@link BeanNotOfRequiredTypeException} if the bean is not of the
	 * required type
	 * @param <T> the bean type
	 * @param selector the bean selector
	 * @return the bean instance
	 * @throws BeanNotOfRequiredTypeException if the bean is not of the required type
	 * @throws BeansException if the bean could not be supplied
	 */
	<T> T getBean(BeanSelector<T> selector, Object... args) throws BeansException;

	/**
	 * Obtain a provider for the specified bean, allowing for lazy on-demand retrieval of
	 * instances, including availability and uniqueness options.
	 * @param <T> the bean type
	 * @param selector the bean selector
	 * @return an object provider for the selection
	 */
	<T> ObjectProvider<T> getBeanProvider(BeanSelector<T> selector);

	/**
	 * Resolve the given embedded value, e.g. an annotation attribute.
	 * @param value the value to resolve
	 * @return the resolved value (may be the original value as-is)
	 */
	String resolveEmbeddedValue(String value);

	/**
	 * Return the arguments explicitly passed to the {@link FunctionalBeanFactory} for use
	 * when creating the bean instance.
	 * @return the explicit arguments or {@code null} if not arguments were passed
	 * @see ListableBeanFactory#getBean(Class, Object...)
	 * @see ListableBeanFactory#getBean(String, Object...)
	 * @see FunctionalBeanFactory#getBean(BeanSelector, Object...)
	 */
	@Nullable
	Object[] getArgs();

}
