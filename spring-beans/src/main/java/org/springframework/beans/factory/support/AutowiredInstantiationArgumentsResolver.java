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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.generator.config.BeanDefinitionRegistrar.ThrowableConsumer;
import org.springframework.core.CollectionFactory;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Resolver used to support the autowiring of constructors or factory methods. Typically
 * used in AOT-processed applications as a targeted alternative to the reflection based
 * injection.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class AutowiredInstantiationArgumentsResolver extends AutowiredElementResolver {

	private final ExecutableLookup lookup;

	private final String[] shortcuts;

	private AutowiredInstantiationArgumentsResolver(ExecutableLookup lookup, String[] shortcuts) {
		this.lookup = lookup;
		this.shortcuts = shortcuts;
	}

	public static AutowiredInstantiationArgumentsResolver forConstructor(Class<?>... parameterTypes) {
		return new AutowiredInstantiationArgumentsResolver(new ConstructorLookup(parameterTypes), null);
	}

	public static AutowiredInstantiationArgumentsResolver forFactoryMethod(Class<?> declaringClass, String name,
			Class<?>... parameterTypes) {
		return new AutowiredInstantiationArgumentsResolver(
				new FactoryMethodLookup(declaringClass, name, parameterTypes), null);
	}

	ExecutableLookup getLookup() {
		return this.lookup;
	}

	public AutowiredInstantiationArgumentsResolver withShortcuts(String... shortcuts) {
		return new AutowiredInstantiationArgumentsResolver(this.lookup, shortcuts);
	}

	public void resolve(RegisteredBean registeredBean, ThrowableConsumer<Object[]> action) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		Assert.notNull(registeredBean, "'action' must not be null");
		Object[] resolved = resolveArguments(registeredBean, this.lookup.get(registeredBean));
		action.accept(resolved);
	}

	public Object[] resolve(RegisteredBean registeredBean) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		return resolveArguments(registeredBean, this.lookup.get(registeredBean));
	}

	public Object resolveAndInstantiate(RegisteredBean registeredBean) {
		return resolveAndInstantiate(registeredBean, Object.class);
	}

	public <T> T resolveAndInstantiate(RegisteredBean registeredBean, Class<T> requiredType) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		Assert.notNull(registeredBean, "'requiredType' must not be null");
		Executable executable = this.lookup.get(registeredBean);
		Object[] resolved = resolveArguments(registeredBean, executable);
		// FIXME see Instantiator in SupplierBuilderTests
		return null;
	}

	private Object[] resolveArguments(RegisteredBean registeredBean, Executable executable) {
		Assert.isInstanceOf(AbstractAutowireCapableBeanFactory.class, registeredBean.getBeanFactory());
		String beanName = registeredBean.getBeanName();
		Class<?> beanClass = registeredBean.getBeanClass();
		AbstractAutowireCapableBeanFactory beanFactory = (AbstractAutowireCapableBeanFactory) registeredBean
				.getBeanFactory();
		RootBeanDefinition mergedBeanDefinition = registeredBean.getMergedBeanDefinition();
		int startIndex = (executable instanceof Constructor<?> constructor
				&& ClassUtils.isInnerClass(constructor.getDeclaringClass())) ? 1 : 0;
		int parameterCount = executable.getParameterCount();
		Object[] resolved = new Object[parameterCount - startIndex];
		Assert.isTrue(this.shortcuts == null || this.shortcuts.length == resolved.length,
				() -> "'shortcuts' must contain " + resolved.length + " elements");
		Set<String> autowiredBeans = new LinkedHashSet<>(resolved.length);
		ConstructorArgumentValues argumentValues = resolveArgumentValues(beanFactory, beanName, mergedBeanDefinition);
		for (int i = startIndex; i < parameterCount; i++) {
			String shortcut = (this.shortcuts != null) ? this.shortcuts[i - startIndex] : null;
			MethodParameter parameter = getMethodParameter(executable, i);
			DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(parameter, true);
			if (shortcut != null) {
				dependencyDescriptor = new ShortcutDependencyDescriptor(dependencyDescriptor, shortcut, beanClass);
			}
			ValueHolder argumentValue = argumentValues.getIndexedArgumentValue(i, null);
			resolved[i - startIndex] = resolveArgument(beanFactory, beanName, autowiredBeans, parameter,
					dependencyDescriptor, argumentValue);
		}
		registerDependentBeans(beanFactory, beanName, autowiredBeans);
		if (executable instanceof Method method) {
			mergedBeanDefinition.setResolvedFactoryMethod(method);
		}
		return resolved;
	}

	private MethodParameter getMethodParameter(Executable executable, int index) {
		if (executable instanceof Constructor<?> constructor) {
			return new MethodParameter(constructor, index);
		}
		if (executable instanceof Method method) {
			return new MethodParameter(method, index);
		}
		throw new IllegalStateException("Unsupported executable " + executable.getClass().getName());
	}

	private ConstructorArgumentValues resolveArgumentValues(AbstractAutowireCapableBeanFactory beanFactory,
			String beanName, RootBeanDefinition mergedBeanDefinition) {
		ConstructorArgumentValues resolved = new ConstructorArgumentValues();
		if (mergedBeanDefinition.hasConstructorArgumentValues()) {
			BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(beanFactory, beanName,
					mergedBeanDefinition, beanFactory.getTypeConverter());
			ConstructorArgumentValues values = mergedBeanDefinition.getConstructorArgumentValues();
			values.getIndexedArgumentValues().forEach((index, valueHolder) -> {
				ValueHolder resolvedValue = resolveArgumentValue(valueResolver, valueHolder);
				resolved.addIndexedArgumentValue(index, resolvedValue);
			});
		}
		return resolved;
	}

	private ValueHolder resolveArgumentValue(BeanDefinitionValueResolver resolver, ValueHolder valueHolder) {
		if (valueHolder.isConverted()) {
			return valueHolder;
		}
		Object resolvedValue = resolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
		ValueHolder resolvedValueHolder = new ValueHolder(resolvedValue, valueHolder.getType(), valueHolder.getName());
		resolvedValueHolder.setSource(valueHolder);
		return resolvedValueHolder;
	}

	private Object resolveArgument(AbstractAutowireCapableBeanFactory beanFactory, String beanName,
			Set<String> autowiredBeans, MethodParameter parameter, DependencyDescriptor dependencyDescriptor,
			ValueHolder argumentValue) {
		TypeConverter typeConverter = beanFactory.getTypeConverter();
		Class<?> parameterType = parameter.getParameterType();
		if (argumentValue != null) {
			return (!argumentValue.isConverted())
					? typeConverter.convertIfNecessary(argumentValue.getValue(), parameterType)
					: argumentValue.getConvertedValue();
		}
		try {
			try {
				return beanFactory.resolveDependency(dependencyDescriptor, beanName, autowiredBeans, typeConverter);
			}
			catch (NoSuchBeanDefinitionException ex) {
				if (parameterType.isArray()) {
					return Array.newInstance(parameterType.getComponentType(), 0);
				}
				if (CollectionFactory.isApproximableCollectionType(parameterType)) {
					return CollectionFactory.createCollection(parameterType, 0);
				}
				if (CollectionFactory.isApproximableMapType(parameterType)) {
					return CollectionFactory.createMap(parameterType, 0);
				}
				throw ex;
			}
		}
		catch (BeansException ex) {
			throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(parameter), ex);
		}
	}

	/**
	 * Performs lookup of the {@link Executable}.
	 */
	static abstract class ExecutableLookup {

		abstract Executable get(RegisteredBean registeredBean);

		final String toCommaSeparatedNames(Class<?>... parameterTypes) {
			return Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", "));
		}

	}

	/**
	 * Peforms lookup of the {@link Constructor}.
	 */
	private static class ConstructorLookup extends ExecutableLookup {

		private final Class<?>[] parameterTypes;

		ConstructorLookup(Class<?>[] parameterTypes) {
			this.parameterTypes = parameterTypes;
		}

		@Override
		public Executable get(RegisteredBean registeredBean) {
			Class<?> beanClass = registeredBean.getBeanClass();
			try {
				Class<?>[] actualParameterTypes = (!ClassUtils.isInnerClass(beanClass)) ? this.parameterTypes
						: ObjectUtils.addObjectToArray(this.parameterTypes, beanClass.getEnclosingClass(), 0);
				return beanClass.getDeclaredConstructor(actualParameterTypes);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalArgumentException(String.format("%s cannot be found on %s", this, beanClass.getName()),
						ex);
			}
		}

		@Override
		public String toString() {
			return String.format("Constructor with parameter types [%s]", toCommaSeparatedNames(this.parameterTypes));
		}

	}

	/**
	 * Performs lookup of the factory {@link Method}.
	 */
	private static class FactoryMethodLookup extends ExecutableLookup {

		private final Class<?> declaringClass;

		private final String name;

		private final Class<?>[] parameterTypes;

		FactoryMethodLookup(Class<?> declaringClass, String name, Class<?>[] parameterTypes) {
			this.declaringClass = declaringClass;
			this.name = name;
			this.parameterTypes = parameterTypes;
		}

		@Override
		public Executable get(RegisteredBean registeredBean) {
			Method method = ReflectionUtils.findMethod(this.declaringClass, this.name, this.parameterTypes);
			Assert.notNull(method, () -> String.format("%s cannot be found", method));
			return method;
		}

		@Override
		public String toString() {
			return String.format("Factory method '%s' with parameter types [%s] declared on %s", this.name,
					toCommaSeparatedNames(this.parameterTypes), this.declaringClass);
		}

	}

}
