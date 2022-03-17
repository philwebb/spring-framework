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

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.BeanDefinition;
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
 * @see RootBeanDefinition#supply(String, Class)
 * @see RootBeanDefinition#supply(String, ResolvableType)
 */
public class SuppliedRootBeanDefinitionBuilder {

	@Nullable
	private final String beanName;

	private final Class<?> beanClass;

	@Nullable
	private final ResolvableType beanType;

	SuppliedRootBeanDefinitionBuilder(@Nullable String beanName, Class<?> beanType) {
		Assert.notNull(beanType, "'beanType' must not be null");
		this.beanName = beanName;
		this.beanClass = beanType;
		this.beanType = null;
	}

	SuppliedRootBeanDefinitionBuilder(@Nullable String beanName, ResolvableType beanType) {
		Assert.notNull(beanType, "'beanType' must not be null");
		Assert.notNull(beanType.resolve(), "'beanType' must be resolvable");
		this.beanName = beanName;
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
			return new Using(this.beanClass.getDeclaredConstructor(parameterTypes));
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
		return new Using(MethodIntrospector.selectInvocableMethod(method, declaringClass));
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

		Using(Executable executable) {
			this.executable = executable;
		}

		Executable getExecutable() {
			return executable;
		}

		/**
		 * Build a {@link RootBeanDefinition} where the instance supplier calls the
		 * specified {@code instantiator} with resolved arguments.
		 * @param beanFactory the bean factory that should resolve arguments
		 * @param instantiator a function that takes the resolved arguments and returns
		 * the bean instance by calling the appropriate constructor or factory method.
		 * @return the built root bean definition
		 * @see #resolvedBy(DefaultListableBeanFactory, ThrowableBiFunction)
		 * @see #suppliedBy(ThrowableSupplier)
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
		 * @see #suppliedBy(ThrowableSupplier)
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
		public RootBeanDefinition suppliedBy(ThrowableSupplier<Object> supplier) {
			RootBeanDefinition beanDefinition = createBeanDefinition();
			beanDefinition.setInstanceSupplier(supplier);
			return beanDefinition;
		}

		InstanceSupplier createInstanceSupplier(DefaultListableBeanFactory beanFactory,
				RootBeanDefinition beanDefinition, ThrowableBiFunction<BeanFactory, Object[], Object> instantiator) {
			if (this.executable.getParameterCount() == 0) {
				return new NoArgumentsInstanceSupplier(beanFactory, instantiator);
			}
			ArgumentsResolver argumentResolver = createArgumentResolver(beanFactory, beanDefinition);
			return new ResolvingInstanceSupplier(beanFactory, SuppliedRootBeanDefinitionBuilder.this.beanName, beanDefinition,
					argumentResolver, instantiator);
		}

		private ArgumentsResolver createArgumentResolver(DefaultListableBeanFactory beanFactory,
				RootBeanDefinition beanDefinition) {
			Class<?> beanClass = SuppliedRootBeanDefinitionBuilder.this.beanClass;
			if (this.executable instanceof Constructor<?> constructor) {
				return new ConstructorArgumentsResolver(beanFactory, beanClass, constructor, beanDefinition);
			}
			if (this.executable instanceof Method method) {
				return new MethodArgumentsResolver(beanFactory, beanClass, method);
			}
			throw new IllegalStateException("Unsupported executable " + executable.getClass().getName());
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

	}

	abstract static class InstanceSupplier implements ThrowableSupplier<Object> {

		abstract Object[] getArguments();

	}

	static class NoArgumentsInstanceSupplier extends InstanceSupplier {

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
			return instantiator.apply(this.beanFactory, getArguments());
		}

		@Override
		Object[] getArguments() {
			return NO_ARGUMENTS;
		}

	}

	/**
	 * {@link ThrowableSupplier} implementation that uses an {@link ArgumentsResolver} to
	 * resolve arguments before delegating to an {@code instantiator} function.
	 */
	static class ResolvingInstanceSupplier extends InstanceSupplier {

		private final RootBeanDefinition beanDefinition;

		private final ArgumentsResolver argumentsResolver;

		private final ConfigurableListableBeanFactory beanFactory;

		private final ThrowableBiFunction<BeanFactory, Object[], Object> instantiator;

		@Nullable
		private final String beanName;

		ResolvingInstanceSupplier(ConfigurableListableBeanFactory beanFactory, @Nullable String beanName,
				RootBeanDefinition beanDefinition, ArgumentsResolver argumentsResolver,
				ThrowableBiFunction<BeanFactory, Object[], Object> instantiator) {
			this.beanDefinition = beanDefinition;
			this.argumentsResolver = argumentsResolver;
			this.beanFactory = beanFactory;
			this.instantiator = instantiator;
			this.beanName = beanName;
		}

		@Override
		public Object getWithException() throws Exception {
			Object[] arguments = getArguments();
			return this.instantiator.apply(this.beanFactory, arguments);
		}

		Object[] getArguments() {
			String beanName = (this.beanName != null) ? this.beanName : findBeanName();
			Object[] arguments = this.argumentsResolver.resolveArguments(beanName);
			return arguments;
		}

		private String findBeanName() {
			String beanName = null;
			for (String candidate : this.beanFactory.getBeanDefinitionNames()) {
				if (isDefinedFromUs(candidate)) {
					Assert.state(beanName == null, "Multiple beans");
					beanName = candidate;
				}
			}
			Assert.state(beanName != null, "No bean");
			return beanName;
		}

		private boolean isDefinedFromUs(String beanName) {
			return isDefinedFromUs(this.beanFactory.getBeanDefinition(beanName));
		}

		private boolean isDefinedFromUs(@Nullable BeanDefinition beanDefinition) {
			while (beanDefinition != null) {
				if (this.beanDefinition.getInstanceSupplier() == this) {
					return true;
				}
				beanDefinition = beanDefinition.getOriginatingBeanDefinition();
			}
			return false;
		}

	}

	/**
	 * Resolves arguments so that they can be passed to an {@code instantiator} function.
	 */
	private abstract static class ArgumentsResolver {

		protected final AbstractAutowireCapableBeanFactory beanFactory;

		protected final Class<?> beanClass;

		ArgumentsResolver(AbstractAutowireCapableBeanFactory beanFactory, Class<?> beanClass) {
			this.beanFactory = beanFactory;
			this.beanClass = beanClass;
		}

		abstract Object[] resolveArguments(String beanName);

		protected final DependencyDescriptor getDependencyDescriptor(MethodParameter parameter) {
			DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(parameter, true);
			dependencyDescriptor.setContainingClass(this.beanClass);
			return dependencyDescriptor;
		}

	}

	/**
	 * {@link ArgumentsResolver} for a {@link Constructor}.
	 */
	private static class ConstructorArgumentsResolver extends ArgumentsResolver {

		private final RootBeanDefinition beanDefinition;

		private final Constructor<?> constructor;

		ConstructorArgumentsResolver(AbstractAutowireCapableBeanFactory beanFactory, Class<?> beanClass,
				Constructor<?> constructor, RootBeanDefinition beanDefinition) {
			super(beanFactory, beanClass);
			this.beanDefinition = beanDefinition;
			this.constructor = constructor;
		}

		public Object[] resolveArguments(String beanName) {
			int parameterCount = this.constructor.getParameterCount();
			Object[] resolvedArguments = new Object[parameterCount];
			Set<String> autowiredBeans = new LinkedHashSet<>(resolvedArguments.length);
			TypeConverter typeConverter = this.beanFactory.getTypeConverter();
			ConstructorArgumentValues argumentValues = resolveArgumentValues(beanName);
			for (int i = 0; i < parameterCount; i++) {
				MethodParameter parameter = new MethodParameter(this.constructor, i);
				DependencyDescriptor dependencyDescriptor = getDependencyDescriptor(parameter);
				ValueHolder argumentValue = argumentValues.getIndexedArgumentValue(i, null);
				resolvedArguments[i] = resolveArgument(beanName, autowiredBeans, typeConverter, parameter,
						dependencyDescriptor, argumentValue);
			}
			return resolvedArguments;
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
					// Single constructor or factory method -> let's return an empty
					// array/collection for e.g. a vararg or a non-null List/Set/Map
					// parameter.
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

	/**
	 * {@link ArgumentsResolver} for a factory {@link Method}.
	 */
	private static class MethodArgumentsResolver extends ArgumentsResolver {

		private final Method method;

		MethodArgumentsResolver(AbstractAutowireCapableBeanFactory beanFactory, Class<?> beanClass, Method method) {
			super(beanFactory, beanClass);
			this.method = method;
		}

		@Override
		public Object[] resolveArguments(String beanName) {
			int parameterCount = this.method.getParameterCount();
			Object[] resolvedArguments = new Object[parameterCount];
			Set<String> autowiredBeans = new LinkedHashSet<>(parameterCount);
			TypeConverter typeConverter = beanFactory.getTypeConverter();
			for (int i = 0; i < parameterCount; i++) {
				MethodParameter parameter = new MethodParameter(this.method, i);
				DependencyDescriptor dependencyDescriptor = getDependencyDescriptor(parameter);
				resolvedArguments[i] = resolveArgument(beanName, autowiredBeans, typeConverter, parameter,
						dependencyDescriptor);
			}
			return resolvedArguments;
		}

		private Object resolveArgument(String beanName, Set<String> autowiredBeans, TypeConverter typeConverter,
				MethodParameter methodParam, DependencyDescriptor dependencyDescriptor) {
			try {
				return this.beanFactory.resolveDependency(dependencyDescriptor, beanName, autowiredBeans,
						typeConverter);
			}
			catch (BeansException ex) {
				throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(methodParam), ex);
			}
		}

	}

}
