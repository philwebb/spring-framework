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

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * A {@link ListableBeanFactory} extension for functional bean factory
 * implementations. Provides {@link BeanSelector} based methods that can be used
 * to query the factory.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see AbstractFunctionalBeanFactory
 * @see FunctionalBeanRegistry
 */
public interface FunctionalBeanFactory extends ListableBeanFactory {

	/**
	 * Return an instance, which may be shared or independent, using the
	 * specified selector.
	 * <p>This method allows a Spring BeanFactory to be used as a replacement
	 * for the Singleton or Prototype design pattern. Callers may retain
	 * references to returned objects in the case of Singleton beans.
	 * <p>Will ask the parent factory if the bean cannot be found in this
	 * factory instance.
	 * @param selector the bean selector
	 * @return an instance of the bean
	 * @throws NoSuchBeanDefinitionException if there is no selectable bean
	 * @throws BeansException if the bean could not be obtained
	 */
	<T> T getBean(BeanSelector<T> selector) throws BeansException;

	/**
	 * Return an instance, which may be shared or independent, using the
	 * specified selector.
	 * <p>Behaves the same as {@link #getBean(BeanSelector)}, but provides a
	 * measure of type safety by throwing a BeanNotOfRequiredTypeException if
	 * the bean is not of the required type. This means that ClassCastException
	 * can't be thrown on casting the result correctly, as can happen with
	 * {@link #getBean(BeanSelector)}.
	 * <p>Will ask the parent factory if the bean cannot be found in this
	 * factory instance.
	 * @param selector the bean selector
	 * @param requiredType type the bean must match; can be an interface or
	 * superclass
	 * @return an instance of the bean
	 * @throws NoSuchBeanDefinitionException if there is no selectable bean
	 * @throws BeanNotOfRequiredTypeException if the bean is not of the required
	 * type
	 * @throws BeansException if the bean could not be created
	 */
	<S, T> T getBean(BeanSelector<S> selector, Class<T> requiredType)
			throws BeansException;

	/**
	 * Return an instance, which may be shared or independent, using the
	 * specified selector.
	 * <p>Allows for specifying explicit constructor arguments / factory method
	 * arguments, overriding the specified default arguments (if any) in the
	 * bean definition.
	 * @param selector the bean selector
	 * @param args arguments to use when creating a bean instance using explicit
	 * arguments (only applied when creating a new instance as opposed to
	 * retrieving an existing one)
	 * @return an instance of the bean
	 * @throws NoSuchBeanDefinitionException if there is no selectable bean
	 * @throws BeanDefinitionStoreException if arguments have been given but the
	 * affected bean isn't a prototype
	 * @throws BeansException if the bean could not be created
	 */
	<T> T getBean(BeanSelector<T> selector, Object... args) throws BeansException;

	/**
	 * Return a provider for the selected beans, allowing for lazy on-demand
	 * retrieval of instances, including availability and uniqueness options.
	 * @param selector the bean selector
	 * @return a corresponding provider handle
	 * @see #getBeanProvider(ResolvableType)
	 */
	<T> ObjectProvider<T> getBeanProvider(BeanSelector<T> selector) throws BeansException;

	/**
	 * Return a provider for the selected beans, allowing for lazy on-demand
	 * retrieval of instances, including availability and uniqueness options.
	 * @param selector the bean selector
	 * @param allowEagerInit whether stream-based access may initialize
	 * <i>lazy-init singletons</i> and <i>objects created by FactoryBeans</i>
	 * (or by factory methods with a "factory-bean" reference) for the type
	 * check
	 * @return a corresponding provider handle
	 * @see #getBeanProvider(BeanSelector)
	 * @see ObjectProvider#iterator()
	 * @see ObjectProvider#stream()
	 * @see ObjectProvider#orderedStream()
	 * @see #getBeanNames(BeanSelector, boolean, boolean)
	 */
	<T> ObjectProvider<T> getBeanProvider(BeanSelector<T> selector,
			boolean allowEagerInit) throws BeansException;

	/**
	 * Does this bean factory contain a bean definition or externally registered
	 * singleton instance matching the given selector?
	 * <p>If this factory is hierarchical, will ask any parent factory if the
	 * bean cannot be found in this factory instance.
	 * <p>If a bean definition or singleton instance matching the given selector
	 * is found, this method will return {@code true} whether the named bean
	 * definition is concrete or abstract, lazy or eager, in scope or not.
	 * Therefore, note that a {@code true} return value from this method does
	 * not necessarily indicate that {@link #getBean} will be able to obtain an
	 * instance for the same name.
	 * @param selector the bean selector
	 * @return whether a bean with the given name is present
	 */
	<T> boolean containsBean(BeanSelector<T> selector);

	/**
	 * Is this bean a shared singleton? That is, will {@link #getBean} always
	 * return the same instance?
	 * <p>Note: This method returning {@code false} does not clearly indicate
	 * independent instances. It indicates non-singleton instances, which may
	 * correspond to a scoped bean as well. Use the {@link #isPrototype}
	 * operation to explicitly check for independent instances.
	 * <p>Will ask the parent factory if the bean cannot be found in this
	 * factory instance.
	 * @param selector the bean selector
	 * @return whether this bean corresponds to a singleton instance
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given
	 * name
	 * @see #getBean
	 * @see #isPrototype
	 */
	<T> boolean isSingleton(BeanSelector<T> selector)
			throws NoSuchBeanDefinitionException;

	/**
	 * Is this bean a prototype? That is, will {@link #getBean} always return
	 * independent instances?
	 * <p>Note: This method returning {@code false} does not clearly indicate a
	 * singleton object. It indicates non-independent instances, which may
	 * correspond to a scoped bean as well. Use the {@link #isSingleton}
	 * operation to explicitly check for a shared singleton instance.
	 * <p>Will ask the parent factory if the bean cannot be found in this
	 * factory instance.
	 * @param selector the bean selector
	 * @return whether this bean will always deliver independent instances
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given
	 * name
	 * @see #getBean
	 * @see #isSingleton
	 */
	<T> boolean isPrototype(BeanSelector<T> selector)
			throws NoSuchBeanDefinitionException;

	/**
	 * Check whether the selected bean matches the specified type.
	 * More specifically, check whether a {@link #getBean} call for the given
	 * selector would return an object that is assignable to the specified
	 * target type.
	 * <p>Will ask the parent factory if the bean cannot be found in this
	 * factory instance.
	 * @param selector the bean selector
	 * @param typeToMatch the type to match against (as a
	 * {@code ResolvableType})
	 * @return {@code true} if the bean type matches, {@code false} if it
	 * doesn't match or cannot be determined yet
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given
	 * name
	 * @see #getBean
	 * @see #getType
	 */
	<T> boolean isTypeMatch(BeanSelector<T> selector, ResolvableType typeToMatch)
			throws NoSuchBeanDefinitionException;

	/**
	 * Check whether the selected bean matches the specified type.
	 * More specifically, check whether a {@link #getBean} call for the given
	 * selector would return an object that is assignable to the specified
	 * target type.
	 * <p>Will ask the parent factory if the bean cannot be found in this
	 * factory instance.
	 * @param selector the bean selector
	 * @param typeToMatch the type to match against (as a {@code Class})
	 * @return {@code true} if the bean type matches, {@code false} if it
	 * doesn't match or cannot be determined yet
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given
	 * name
	 * @see #getBean
	 * @see #getType
	 */
	<T> boolean isTypeMatch(BeanSelector<T> selector, Class<?> typeToMatch)
			throws NoSuchBeanDefinitionException;

	/**
	 * Determine the type of the selected bean. More specifically, determine the
	 * type of object that {@link #getBean} would return for the given selector.
	 * <p>For a {@link FactoryBean}, return the type of object that the
	 * FactoryBean creates, as exposed by {@link FactoryBean#getObjectType()}.
	 * This may lead to the initialization of a previously uninitialized
	 * {@code FactoryBean} (see {@link #getType(BeanSelector, boolean)}).
	 * <p>Will ask the parent factory if the bean cannot be found in this
	 * factory instance.
	 * @param selector the bean selector
	 * @return the type of the bean, or {@code null} if not determinable
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given
	 * name
	 * @see #getBean
	 * @see #isTypeMatch
	 */
	@Nullable
	<T> Class<?> getType(BeanSelector<T> selector) throws NoSuchBeanDefinitionException;

	/**
	 * Determine the type of the selected bean. More specifically, determine the
	 * type of object that {@link #getBean} would return for the given selector.
	 * <p>For a {@link FactoryBean}, return the type of object that the
	 * FactoryBean creates, as exposed by {@link FactoryBean#getObjectType()}.
	 * Depending on the {@code allowFactoryBeanInit} flag, this may lead to the
	 * initialization of a previously uninitialized {@code FactoryBean} if no
	 * early type information is available.
	 * <p>Will ask the parent factory if the bean cannot be found in this
	 * factory instance.
	 * @param selector the bean selector
	 * @param allowFactoryBeanInit whether a {@code FactoryBean} may get
	 * initialized just for the purpose of determining its object type
	 * @return the type of the bean, or {@code null} if not determinable
	 * @throws NoSuchBeanDefinitionException if there is no bean with the given
	 * name
	 * @see #getBean
	 * @see #isTypeMatch
	 */
	@Nullable
	<T> Class<?> getType(BeanSelector<T> selector, boolean allowFactoryBeanInit)
			throws NoSuchBeanDefinitionException;

	/**
	 * Check if this bean factory contains a bean definition with the given
	 * selector.
	 * <p>Does not consider any hierarchy this factory may participate in, and
	 * ignores any singleton beans that have been registered by other means than
	 * bean definitions.
	 * @param selector the bean selector
	 * @return if this bean factory contains a bean definition with the given
	 * name
	 * @see #containsBean
	 */
	<T> boolean containsBeanDefinition(BeanSelector<T> selector);

	/**
	 * Return the names of beans matching the given selector.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does
	 * <i>not</i> check nested beans which might match the specified type as
	 * well.
	 * <p>Does consider objects created by FactoryBeans, which means that
	 * FactoryBeans will get initialized. If the object created by the
	 * FactoryBean doesn't match, the raw FactoryBean itself will be matched
	 * against the type.
	 * <p>Does not consider any hierarchy this factory may participate in. Use
	 * BeanFactoryUtils' {@code beanNamesForTypeIncludingAncestors} to include
	 * beans in ancestor factories too.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
	 * by other means than bean definitions.
	 * <p>This version of {@code getBeanNamesForType} matches all kinds of beans,
	 * be it singletons, prototypes, or FactoryBeans. In most implementations,
	 * the result will be the same as for
	 * {@code getBeanNamesForType(type, true, true)}.
	 * <p>Bean names returned by this method should always return bean names
	 * <i>in the order of definition</i> in the backend configuration, as far as
	 * possible.
	 * @param selector the bean selector
	 * @return the names of beans (or objects created by FactoryBeans) matching
	 * the given object type (including subclasses), or an empty array if none
	 * @see #isTypeMatch(BeanSelector, ResolvableType)
	 * @see FactoryBean#getObjectType
	 */
	<T> String[] getBeanNames(BeanSelector<T> selector);

	/**
	 * Return the names of beans matching the given selector.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does
	 * <i>not</i> check nested beans which might match the specified type as
	 * well.
	 * <p>Does consider objects created by FactoryBeans if the "allowEagerInit"
	 * flag is set, which means that FactoryBeans will get initialized. If the
	 * object created by the FactoryBean doesn't match, the raw FactoryBean
	 * itself will be matched against the type. If "allowEagerInit" is not set,
	 * only raw FactoryBeans will be checked (which doesn't require
	 * initialization of each FactoryBean).
	 * <p>Does not consider any hierarchy this factory may participate in.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered by
	 * other means than bean definitions.
	 * <p>Bean names returned by this method should always return bean names
	 * <i>in the order of definition</i> in the backend configuration, as far as
	 * possible.
	 * @param selector the bean selector
	 * @param includeNonSingletons whether to include prototype or scoped beans
	 * too or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i>
	 * and <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need
	 * to be eagerly initialized to determine their type: So be aware that
	 * passing in "true" for this flag will initialize FactoryBeans and
	 * "factory-bean" references.
	 * @return the names of beans (or objects created by FactoryBeans) matching
	 * the given object type (including subclasses), or an empty array if none
	 * @see FactoryBean#getObjectType
	 */
	<T> String[] getBeanNames(BeanSelector<T> selector, boolean includeNonSingletons,
			boolean allowEagerInit);

	/**
	 * Return the bean instances that match the given selector.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does
	 * <i>not</i> check nested beans which might match the specified type as
	 * well.
	 * <p>Does consider objects created by FactoryBeans, which means that
	 * FactoryBeans will get initialized. If the object created by the
	 * FactoryBean doesn't match, the raw FactoryBean itself will be matched
	 * against the type.
	 * <p>Does not consider any hierarchy this factory may participate in.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
	 * by other means than bean definitions.
	 * <p>This version of getBeansOfType matches all kinds of beans, be it
	 * singletons, prototypes, or FactoryBeans. In most implementations, the
	 * result will be the same as for {@code getBeansOfType(type, true, true)}.
	 * <p>The Map returned by this method should always return bean names and
	 * corresponding bean instances <i>in the order of definition</i> in the
	 * backend configuration, as far as possible.
	 * @param selector the bean selector
	 * @return a Map with the matching beans, containing the bean names as keys
	 * and the corresponding bean instances as values
	 * @throws BeansException if a bean could not be created
	 * @see FactoryBean#getObjectType
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory,
	 * Class)
	 */
	<T> Map<String, T> getBeans(BeanSelector<T> selector) throws BeansException;

	/**
	 * Return the bean instances that match the given selector.
	 * <p><b>NOTE: This method introspects top-level beans only.</b> It does
	 * <i>not</i> check nested beans which might match the specified type as
	 * well.
	 * <p>Does consider objects created by FactoryBeans if the "allowEagerInit"
	 * flag is set, which means that FactoryBeans will get initialized. If the
	 * object created by the FactoryBean doesn't match, the raw FactoryBean
	 * itself will be matched against the type. If "allowEagerInit" is not set,
	 * only raw FactoryBeans will be checked (which doesn't require
	 * initialization of each FactoryBean).
	 * <p>Does not consider any hierarchy this factory may participate in. Use
	 * BeanFactoryUtils' {@code beansOfTypeIncludingAncestors} to include beans
	 * in ancestor factories too.
	 * <p>Note: Does <i>not</i> ignore singleton beans that have been registered by
	 * other means than bean definitions.
	 * <p>The Map returned by this method should always return bean names and
	 * corresponding bean instances <i>in the order of definition</i> in the
	 * backend configuration, as far as possible.
	 * @param selector the bean selector
	 * @param includeNonSingletons whether to include prototype or scoped beans
	 * too or just singletons (also applies to FactoryBeans)
	 * @param allowEagerInit whether to initialize <i>lazy-init singletons</i>
	 * and <i>objects created by FactoryBeans</i> (or by factory methods with a
	 * "factory-bean" reference) for the type check. Note that FactoryBeans need
	 * to be eagerly initialized to determine their type: So be aware that
	 * passing in "true" for this flag will initialize FactoryBeans and
	 * "factory-bean" references.
	 * @return a Map with the matching beans, containing the bean names as keys
	 * and the corresponding bean instances as values
	 * @throws BeansException if a bean could not be created
	 * @see FactoryBean#getObjectType
	 */
	<T> Map<String, T> getBeans(BeanSelector<T> selector, boolean includeNonSingletons,
			boolean allowEagerInit) throws BeansException;

}
