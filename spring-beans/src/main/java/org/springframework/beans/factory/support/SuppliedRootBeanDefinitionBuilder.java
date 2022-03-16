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
import org.springframework.util.function.ThrowableFunction;
import org.springframework.util.function.ThrowableSupplier;

/**
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see RootBeanDefinition#supply(String, ResolvableType)
 */
public class SuppliedRootBeanDefinitionBuilder {

	private static final Object[] NO_ARGUMENTS = {};

	@Nullable
	private final String beanName;

	private final Class<?> beanClass;

	@Nullable
	private final ResolvableType beanType;

	SuppliedRootBeanDefinitionBuilder(@Nullable String beanName, Class<?> beanType) {
		Assert.notNull(beanType, "'type' must not be null");
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

	public Using usingConstructor(Class<?>... parameterTypes) {
		try {
			return new Using(this.beanClass.getDeclaredConstructor(parameterTypes));
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException(String.format("No constructor with type(s) [%s] found on %s",
					toCommaSeparatedNames(parameterTypes), this.beanClass.getName()), ex);
		}
	}

	public Using usingFactoryMethod(Class<?> declaringClass, String name, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(declaringClass, name, parameterTypes);
		Assert.notNull(method, () -> String.format("No method '%s' with type(s) [%s] found on %s", name,
				toCommaSeparatedNames(parameterTypes), declaringClass.getName()));
		return new Using(MethodIntrospector.selectInvocableMethod(method, declaringClass));
	}

	private static String toCommaSeparatedNames(Class<?>... parameterTypes) {
		return Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", "));
	}

	public class Using {

		private final Executable executable;

		Using(Executable executable) {
			this.executable = executable;
		}

		public RootBeanDefinition resolvedBy(DefaultListableBeanFactory beanFactory,
				ThrowableFunction<Object[], Object> instantiator) {
			RootBeanDefinition beanDefinition = createBeanDefinition();
			beanDefinition.setInstanceSupplier(createInstanceSupplier(beanFactory, instantiator, beanDefinition));
			return beanDefinition;
		}

		public RootBeanDefinition suppliedBy(ThrowableSupplier<Object> instantiator) {
			RootBeanDefinition beanDefinition = createBeanDefinition();
			beanDefinition.setInstanceSupplier(instantiator);
			return beanDefinition;
		}

		private ThrowableSupplier<Object> createInstanceSupplier(DefaultListableBeanFactory beanFactory,
				ThrowableFunction<Object[], Object> instantiator, RootBeanDefinition beanDefinition) {
			if (this.executable.getParameterCount() == 0) {
				return () -> instantiator.apply(NO_ARGUMENTS);
			}
			ArgumentsResolver argumentResolver = createArgumentResolver(beanFactory, beanDefinition);
			return new InstanceSupplier(beanFactory, SuppliedRootBeanDefinitionBuilder.this.beanName, beanDefinition,
					argumentResolver, instantiator);
		}

		private ArgumentsResolver createArgumentResolver(DefaultListableBeanFactory beanFactory,
				RootBeanDefinition beanDefinition) {
			if (this.executable instanceof Constructor<?> constructor) {
				return new ConstructorArgumentsResolver(beanFactory, beanDefinition,
						SuppliedRootBeanDefinitionBuilder.this.beanClass, constructor);

			}
			if (this.executable instanceof Method method) {

			}
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		private RootBeanDefinition createBeanDefinition() {
			// FIXME setTargetType
			// FIMXE getResolvedFactoryMethod
			return null;
		}

	}

	static class InstanceSupplier implements ThrowableSupplier<Object> {

		private final RootBeanDefinition beanDefinition;

		private final ArgumentsResolver argumentsResolver;

		private final ConfigurableListableBeanFactory beanFactory;

		private final ThrowableFunction<Object[], Object> instantiator;

		@Nullable
		private final String beanName;

		public InstanceSupplier(ConfigurableListableBeanFactory beanFactory, @Nullable String beanName,
				RootBeanDefinition beanDefinition, ArgumentsResolver argumentsResolver,
				ThrowableFunction<Object[], Object> instantiator) {
			this.beanDefinition = beanDefinition;
			this.argumentsResolver = argumentsResolver;
			this.beanFactory = beanFactory;
			this.instantiator = instantiator;
			this.beanName = beanName;
		}

		@Override
		public Object getWithException() throws Exception {
			String beanName = (this.beanName != null) ? this.beanName : findBeanName();
			Object[] arguments = this.argumentsResolver.resolveArguments(beanName);
			return this.instantiator.apply(arguments);
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

	interface ArgumentsResolver {

		Object[] resolveArguments(String beanName);

	}

	static class ConstructorArgumentsResolver implements ArgumentsResolver {

		private final AbstractAutowireCapableBeanFactory beanFactory;

		private final RootBeanDefinition beanDefinition;

		private final Class<?> beanClass;

		private final Constructor<?> constructor;

		ConstructorArgumentsResolver(AbstractAutowireCapableBeanFactory beanFactory, RootBeanDefinition beanDefinition,
				Class<?> beanClass, Constructor<?> constructor) {
			this.beanFactory = beanFactory;
			this.beanDefinition = beanDefinition;
			this.beanClass = beanClass;
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
				ValueHolder argumentValue = argumentValues.getIndexedArgumentValue(i, null);
				resolvedArguments[i] = resolveArgument(beanName, autowiredBeans, typeConverter, parameter,
						argumentValue);
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
				MethodParameter parameter, ValueHolder argumentValue) {
			Class<?> parameterType = parameter.getParameterType();
			if (argumentValue != null) {
				return (!argumentValue.isConverted())
						? typeConverter.convertIfNecessary(argumentValue.getValue(), parameterType)
						: argumentValue.getConvertedValue();
			}
			DependencyDescriptor dependencyDescriptor = new DependencyDescriptor(parameter, true);
			dependencyDescriptor.setContainingClass(this.beanClass);
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

	static class MethodArgumentsResolver {

	}

}
