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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.CollectionFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.ThrowableBiFunction;
import org.springframework.util.function.ThrowableFunction;
import org.springframework.util.function.ThrowableSupplier;

/**
 * Builder class that can be used to create a {@link RootBeanDefinition} configured with a
 * {@link AbstractBeanDefinition#setInstanceSupplier(java.util.function.Supplier) instance
 * supplier}.
 * <p>
 * This builder is designed primary used by AOT generated code, specifically in a native
 * image. As long as the {@link Constructor} or {@link Method} being used has been marked
 * with an {@link ExecutableMode#INTROSPECT introspection} hint, the builder can provide
 * resolved arguments that should be injected. In other words, the class helps separate
 * injection from instantiation and allows for lighter native images since full
 * {@link ExecutableMode#INVOKE invocation} hints are not necessary.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see RootBeanDefinition#supply(Class)
 * @see RootBeanDefinition#supply(ResolvableType)
 */
public class SuppliedRootBeanDefinitionBuilder {

	private final Class<?> beanClass;

	@Nullable
	private final ResolvableType beanType;

	SuppliedRootBeanDefinitionBuilder(Class<?> beanType) {
		Assert.notNull(beanType, "'beanType' must not be null");
		this.beanClass = beanType;
		this.beanType = null;
	}

	SuppliedRootBeanDefinitionBuilder(ResolvableType beanType) {
		Assert.notNull(beanType, "'beanType' must not be null");
		Assert.notNull(beanType.resolve(), "'beanType' must be resolvable");
		this.beanClass = beanType.resolve();
		this.beanType = beanType;
	}

	/**
	 * Build the definition using annotations and meta-data from the identified
	 * constructor.
	 * @param parameterTypes the constructor parameters
	 * @return a {@link Using} instance that be used to build the
	 * {@link RootBeanDefinition}
	 */
	public Using usingConstructor(Class<?>... parameterTypes) {
		try {
			boolean innerClass = ClassUtils.isInnerClass(this.beanClass);
			int parameterOffset = (!innerClass) ? 0 : 1;
			Class<?>[] actualParameterTypes = (!innerClass) ? parameterTypes
					: ObjectUtils.addObjectToArray(parameterTypes, this.beanClass.getEnclosingClass(), 0);
			Constructor<?> constructor = this.beanClass.getDeclaredConstructor(actualParameterTypes);
			return new Using(constructor, parameterOffset);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException(String.format("No constructor with type(s) [%s] found on %s",
					toCommaSeparatedNames(parameterTypes), this.beanClass.getName()), ex);
		}
	}

	/**
	 * Build the definition using annotations and meta-data from the identified factory
	 * method.
	 * @param declaringClass the class the declares the factory method
	 * @param parameterTypes the method parameters
	 * @return a {@link Using} instance that be used to build the
	 * {@link RootBeanDefinition}
	 */
	public Using usingFactoryMethod(Class<?> declaringClass, String name, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(declaringClass, name, parameterTypes);
		Assert.notNull(method, () -> String.format("No method '%s' with type(s) [%s] found on %s", name,
				toCommaSeparatedNames(parameterTypes), declaringClass.getName()));
		return new Using(MethodIntrospector.selectInvocableMethod(method, declaringClass), 0);
	}

	private static String toCommaSeparatedNames(Class<?>... parameterTypes) {
		return Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", "));
	}

	/**
	 * Inner-class used to continue building a {@link RootBeanDefinition} once a
	 * constructor of factory method has been selected for use.
	 */
	public class Using {

		private final Executable executable;

		private final int parameterOffset;

		Using(Executable executable, int parameterOffset) {
			this.executable = executable;
			this.parameterOffset = parameterOffset;
		}

		Executable getExecutable() {
			return this.executable;
		}

		/**
		 * Build a {@link RootBeanDefinition} where the instance supplier calls the
		 * specified {@code instantiator} with resolved arguments.
		 * @param beanFactory the bean factory that should resolve arguments
		 * @param instantiator a function that takes the resolved arguments and returns
		 * the bean instance by calling the appropriate constructor or factory method.
		 * @return the built root bean definition
		 * @see #resolvedBy(DefaultListableBeanFactory, ThrowableBiFunction)
		 * @see #resolvedBy(ThrowableSupplier)
		 */
		public RootBeanDefinition resolvedBy(DefaultListableBeanFactory beanFactory,
				ThrowableFunction<Object[], Object> instantiator) {
			return resolvedBy(beanFactory, (bf, arguments) -> instantiator.apply(arguments));
		}

		/**
		 * Build a {@link RootBeanDefinition} where the instance supplier calls the
		 * specified {@code instantiator} with the {@link BeanFactory} and the resolved
		 * arguments.
		 * @param beanFactory the bean factory that should resolve arguments
		 * @param instantiator a function that takes the {@link BeanFactory} and resolved
		 * arguments and returns the bean instance by calling the appropriate constructor
		 * or factory method.
		 * @return the built root bean definition
		 * @see #resolvedBy(DefaultListableBeanFactory, ThrowableBiFunction)
		 * @see #resolvedBy(ThrowableSupplier)
		 */
		public RootBeanDefinition resolvedBy(DefaultListableBeanFactory beanFactory,
				ThrowableBiFunction<BeanFactory, Object[], Object> instantiator) {
			RootBeanDefinition beanDefinition = createBeanDefinition();
			beanDefinition.setInstanceSupplier(createInstanceSupplier(beanFactory, beanDefinition, instantiator));
			return beanDefinition;
		}

		/**
		 * Build a {@link RootBeanDefinition} with the given instance supplier. This
		 * method can be used when no injected arguments are needed.
		 * @param supplier the bean instance supplier
		 * @return the built root bean definition
		 * @see #resolvedBy(DefaultListableBeanFactory, ThrowableBiFunction)
		 * @see #resolvedBy(DefaultListableBeanFactory, ThrowableBiFunction)
		 */
		public RootBeanDefinition resolvedBy(Supplier<Object> supplier) {
			RootBeanDefinition beanDefinition = createBeanDefinition();
			beanDefinition.setInstanceSupplier(supplier);
			return beanDefinition;
		}

		private Supplier<Object> createInstanceSupplier(DefaultListableBeanFactory beanFactory,
				RootBeanDefinition beanDefinition, ThrowableBiFunction<BeanFactory, Object[], Object> instantiator) {
			if ((this.executable.getParameterCount() - this.parameterOffset) == 0) {
				return new NoArgumentsInstanceSupplier(beanFactory, instantiator);
			}
			ArgumentsResolver argumentResolver = new ArgumentsResolver(beanFactory,
					SuppliedRootBeanDefinitionBuilder.this.beanClass, this.executable, this.parameterOffset,
					beanDefinition);
			return new ResolvingInstanceSupplier(beanFactory, argumentResolver, instantiator);
		}

		private RootBeanDefinition createBeanDefinition() {
			RootBeanDefinition beanDefinition = new RootBeanDefinition(
					SuppliedRootBeanDefinitionBuilder.this.beanClass);
			beanDefinition.setTargetType(SuppliedRootBeanDefinitionBuilder.this.beanType);
			if (this.executable instanceof Method method) {
				beanDefinition.setResolvedFactoryMethod(method);
			}
			return beanDefinition;
		}

		@Override
		public String toString() {
			return this.executable.toString();
		}

	}

	static class NoArgumentsInstanceSupplier implements ThrowableSupplier<Object> {

		private static final Object[] NO_ARGUMENTS = {};

		private final DefaultListableBeanFactory beanFactory;

		private final ThrowableBiFunction<BeanFactory, Object[], Object> instantiator;

		NoArgumentsInstanceSupplier(DefaultListableBeanFactory beanFactory,
				ThrowableBiFunction<BeanFactory, Object[], Object> instantiator) {
			this.beanFactory = beanFactory;
			this.instantiator = instantiator;
		}

		@Override
		public Object getWithException() throws Exception {
			return this.instantiator.apply(this.beanFactory, NO_ARGUMENTS);
		}

	}

	/**
	 * {@link ThrowableSupplier} implementation that uses an {@link XArgumentsResolver} to
	 * resolve arguments before delegating to an {@code instantiator} function.
	 */
	static class ResolvingInstanceSupplier implements BeanNameAwareInstanceSupplier<Object> {

		private final ArgumentsResolver argumentsResolver;

		private final ConfigurableListableBeanFactory beanFactory;

		private final ThrowableBiFunction<BeanFactory, Object[], Object> instantiator;

		ResolvingInstanceSupplier(ConfigurableListableBeanFactory beanFactory, ArgumentsResolver argumentsResolver,
				ThrowableBiFunction<BeanFactory, Object[], Object> instantiator) {
			this.argumentsResolver = argumentsResolver;
			this.beanFactory = beanFactory;
			this.instantiator = instantiator;
		}

		@Override
		public Object get(String beanName) {
			Assert.state(beanName != null, "A bean name must be provided in order to resolve instance arguments");
			Object[] arguments = this.argumentsResolver.resolveArguments(beanName);
			return this.instantiator.apply(this.beanFactory, arguments);
		}

	}

	/**
	 * Resolved arguments using the {@link BeanFactory} so that they can be passed to the
	 * {@code instantiator}.
	 */
	private static class ArgumentsResolver {

		private final AbstractAutowireCapableBeanFactory beanFactory;

		private final Class<?> beanClass;

		private final int parameterOffset;

		private final RootBeanDefinition beanDefinition;

		private final Executable executable;

		ArgumentsResolver(AbstractAutowireCapableBeanFactory beanFactory, Class<?> beanClass, Executable executable,
				int parameterOffset, RootBeanDefinition beanDefinition) {
			this.beanFactory = beanFactory;
			this.beanClass = beanClass;
			this.parameterOffset = parameterOffset;
			this.beanDefinition = beanDefinition;
			this.executable = executable;
		}

		Object[] resolveArguments(String beanName) {
			int startIndex = this.parameterOffset;
			int parameterCount = this.executable.getParameterCount();
			Object[] resolvedArguments = new Object[parameterCount - startIndex];
			Set<String> autowiredBeans = new LinkedHashSet<>(resolvedArguments.length);
			TypeConverter typeConverter = this.beanFactory.getTypeConverter();
			ConstructorArgumentValues argumentValues = resolveArgumentValues(beanName);
			for (int i = startIndex; i < parameterCount; i++) {
				MethodParameter parameter = getMethodParameter(i);
				DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(parameter, true);
				dependencyDescriptor.setContainingClass(this.beanClass);
				ValueHolder argumentValue = argumentValues.getIndexedArgumentValue(i, null);
				resolvedArguments[i - startIndex] = resolveArgument(beanName, autowiredBeans, typeConverter, parameter,
						dependencyDescriptor, argumentValue);
			}
			return resolvedArguments;
		}

		private MethodParameter getMethodParameter(int index) {
			if (this.executable instanceof Constructor<?> constructor) {
				return new MethodParameter(constructor, index);
			}
			if (this.executable instanceof Method method) {
				return new MethodParameter(method, index);
			}
			throw new IllegalStateException("Unsupported executable " + this.executable.getClass().getName());
		}

		private ConstructorArgumentValues resolveArgumentValues(String beanName) {
			ConstructorArgumentValues resolved = new ConstructorArgumentValues();
			if (this.beanDefinition.hasConstructorArgumentValues()) {
				BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this.beanFactory, beanName,
						this.beanDefinition);
				ConstructorArgumentValues values = this.beanDefinition.getConstructorArgumentValues();
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
			ValueHolder resolvedValueHolder = new ValueHolder(resolvedValue, valueHolder.getType(),
					valueHolder.getName());
			resolvedValueHolder.setSource(valueHolder);
			return resolvedValueHolder;
		}

		private Object resolveArgument(String beanName, Set<String> autowiredBeans, TypeConverter typeConverter,
				MethodParameter parameter, DependencyDescriptor dependencyDescriptor, ValueHolder argumentValue) {
			Class<?> parameterType = parameter.getParameterType();
			if (argumentValue != null) {
				return (!argumentValue.isConverted())
						? typeConverter.convertIfNecessary(argumentValue.getValue(), parameterType)
						: argumentValue.getConvertedValue();
			}
			try {
				try {
					return this.beanFactory.resolveDependency(dependencyDescriptor, beanName, autowiredBeans,
							typeConverter);
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

	}

}
