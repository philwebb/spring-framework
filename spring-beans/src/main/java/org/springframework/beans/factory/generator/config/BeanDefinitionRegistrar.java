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

package org.springframework.beans.factory.generator.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.ResolvableType;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.ThrowableConsumer;
import org.springframework.util.function.ThrowableFunction;
import org.springframework.util.function.ThrowableSupplier;

/**
 * {@link BeanDefinition} registration mechanism offering transparent
 * dependency resolution, as well as exception management.
 *
 * <p>Used by code generators and for internal use within the framework
 * only.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class BeanDefinitionRegistrar {

	private static final Log logger = LogFactory.getLog(BeanDefinitionRegistrar.class);

	@Nullable
	private final String beanName;

	private final Class<?> beanClass;

	@Nullable
	private final ResolvableType beanType;

	private final List<Consumer<RootBeanDefinition>> customizers;

	@Nullable
	private Executable instanceCreator;

	@Nullable
	private RootBeanDefinition beanDefinition;


	private BeanDefinitionRegistrar(@Nullable String beanName, Class<?> beanClass, @Nullable ResolvableType beanType) {
		this.beanName = beanName;
		this.beanClass = beanClass;
		this.beanType = beanType;
		this.customizers = new ArrayList<>();
	}

	/**
	 * Specify the factory method to use to instantiate the bean.
	 * @param declaredType the {@link Method#getDeclaringClass() declared type}
	 * of the factory method.
	 * @param name the name of the method
	 * @param parameterTypes the parameter types of the method
	 * @return {@code this}, to facilitate method chaining
	 * @see RootBeanDefinition#getResolvedFactoryMethod()
	 */
	public BeanDefinitionRegistrar withFactoryMethod(Class<?> declaredType, String name, Class<?>... parameterTypes) {
		this.instanceCreator = getMethod(declaredType, name, parameterTypes);
		return this;
	}

	/**
	 * Specify the constructor to use to instantiate the bean.
	 * @param parameterTypes the parameter types of the constructor
	 * @return {@code this}, to facilitate method chaining
	 */
	public BeanDefinitionRegistrar withConstructor(Class<?>... parameterTypes) {
		this.instanceCreator = getConstructor(this.beanClass, parameterTypes);
		return this;
	}

	/**
	 * Specify how the bean instance should be created and initialized, using
	 * the {@link BeanInstanceContext} to resolve dependencies if necessary.
	 * @param instanceContext the {@link BeanInstanceContext} to use
	 * @return {@code this}, to facilitate method chaining
	 */
	public BeanDefinitionRegistrar instanceSupplier(ThrowableFunction<BeanInstanceContext, ?> instanceContext) {
		return customize(beanDefinition -> beanDefinition.setInstanceSupplier(() ->
				instanceContext.apply(createBeanInstanceContext())));
	}

	/**
	 * Specify how the bean instance should be created and initialized.
	 * @return {@code this}, to facilitate method chaining
	 */
	public BeanDefinitionRegistrar instanceSupplier(ThrowableSupplier<?> instanceSupplier) {
		return customize(beanDefinition -> beanDefinition.setInstanceSupplier(instanceSupplier));
	}

	/**
	 * Customize the {@link RootBeanDefinition} using the specified consumer.
	 * @param bd a consumer for the bean definition
	 * @return {@code this}, to facilitate method chaining
	 */
	public BeanDefinitionRegistrar customize(ThrowableConsumer<RootBeanDefinition> bd) {
		this.customizers.add(bd);
		return this;
	}

	/**
	 * Register the {@link RootBeanDefinition} defined by this instance to
	 * the specified bean factory.
	 * @param beanFactory the bean factory to use
	 */
	public void register(DefaultListableBeanFactory beanFactory) {
		BeanDefinition beanDefinition = toBeanDefinition();
		Assert.state(this.beanName != null, () -> "Bean name not set. Could not register " + beanDefinition);
		logger.debug(LogMessage.format("Register bean definition with name '%s'", this.beanName));
		beanFactory.registerBeanDefinition(this.beanName, beanDefinition);
	}

	/**
	 * Return the {@link RootBeanDefinition} defined by this instance.
	 * @return the bean definition
	 */
	public RootBeanDefinition toBeanDefinition() {
		try {
			this.beanDefinition = createBeanDefinition();
			return this.beanDefinition;
		}
		catch (Exception ex) {
			throw new FatalBeanException("Failed to create bean definition for bean with name '" + this.beanName + "'", ex);
		}
	}

	private RootBeanDefinition createBeanDefinition() {
		RootBeanDefinition bd = (RootBeanDefinition) BeanDefinitionBuilder.rootBeanDefinition(beanClass).getBeanDefinition();
		if (this.beanType != null) {
			bd.setTargetType(this.beanType);
		}
		if (this.instanceCreator instanceof Method) {
			bd.setResolvedFactoryMethod((Method) this.instanceCreator);
		}
		this.customizers.forEach(customizer -> customizer.accept(bd));
		return bd;
	}

	private BeanInstanceContext createBeanInstanceContext() {
		String resolvedBeanName = this.beanName != null ? this.beanName : createInnerBeanName();
		return new BeanInstanceContext(resolvedBeanName, this.beanClass);
	}

	private String createInnerBeanName() {
		return "(inner bean)" + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR +
				(this.beanDefinition != null ? ObjectUtils.getIdentityHexString(this.beanDefinition) : 0);
	}

	@Nullable
	private BeanDefinition resolveBeanDefinition(DefaultListableBeanFactory beanFactory) {
		return this.beanDefinition;
	}

	private static Constructor<?> getConstructor(Class<?> beanType, Class<?>... parameterTypes) {
		try {
			return beanType.getDeclaredConstructor(parameterTypes);
		}
		catch (NoSuchMethodException ex) {
			String message = String.format("No constructor with type(s) [%s] found on %s",
					toCommaSeparatedNames(parameterTypes), beanType.getName());
			throw new IllegalArgumentException(message, ex);
		}
	}

	private static Method getMethod(Class<?> declaredType, String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(declaredType, methodName, parameterTypes);
		if (method == null) {
			String message = String.format("No method '%s' with type(s) [%s] found on %s", methodName,
					toCommaSeparatedNames(parameterTypes), declaredType.getName());
			throw new IllegalArgumentException(message);
		}
		return MethodIntrospector.selectInvocableMethod(method, declaredType);
	}

	private static String toCommaSeparatedNames(Class<?>... parameterTypes) {
		return Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", "));
	}


	/**
	 * Initialize the registration of a bean with the specified name and type.
	 * @param beanName the name of the bean
	 * @param beanType the type of the bean
	 * @return a registrar for the specified bean
	 */
	public static BeanDefinitionRegistrar of(String beanName, ResolvableType beanType) {
		return new BeanDefinitionRegistrar(beanName, beanType.toClass(), beanType);
	}

	/**
	 * Initialize the registration of a bean with the specified name and type.
	 * @param beanName the name of the bean
	 * @param beanType the type of the bean
	 * @return a registrar for the specified bean
	 */
	public static BeanDefinitionRegistrar of(String beanName, Class<?> beanType) {
		return new BeanDefinitionRegistrar(beanName, beanType, null);
	}

	/**
	 * Initialize the registration of an inner bean with the specified type.
	 * @param beanType the type of the inner bean
	 * @return a registrar for the specified inner bean
	 */
	public static BeanDefinitionRegistrar inner(ResolvableType beanType) {
		return new BeanDefinitionRegistrar(null, beanType.toClass(), beanType);
	}

	/**
	 * Initialize the registration of an inner bean with the specified type.
	 * @param beanType the type of the inner bean
	 * @return a registrar for the specified inner bean
	 */
	public static BeanDefinitionRegistrar inner(Class<?> beanType) {
		return new BeanDefinitionRegistrar(null, beanType, null);
	}


	public final class BeanInstanceContext {

		private final String beanName;

		private final Class<?> beanType;

		private BeanInstanceContext(String beanName, Class<?> beanType) {
			this.beanName = beanName;
			this.beanType = beanType;
		}

		/**
		 * Return the bean instance using the {@code factory}.
		 * @param beanFactory the bean factory to use
		 * @param factory a function that returns the bean instance based on
		 * the resolved attributes required by its instance creator
		 * @param <T> the type of the bean
		 * @return the bean instance
		 */
		public <T> T create(DefaultListableBeanFactory beanFactory, ThrowableFunction<InjectedElementAttributes, T> factory) {
			return resolveInstanceCreator(BeanDefinitionRegistrar.this.instanceCreator).create(beanFactory, factory);
		}

		private InjectedElementResolver resolveInstanceCreator(@Nullable Executable instanceCreator) {
			if (instanceCreator instanceof Method) {
				return new InjectedConstructionResolver(instanceCreator, instanceCreator.getDeclaringClass(), this.beanName,
						BeanDefinitionRegistrar.this::resolveBeanDefinition);
			}
			if (instanceCreator instanceof Constructor) {
				return new InjectedConstructionResolver(instanceCreator, this.beanType, this.beanName,
						BeanDefinitionRegistrar.this::resolveBeanDefinition);
			}
			throw new IllegalStateException("No factory method or constructor is set");
		}

		/**
		 * Create an {@link InjectedElementResolver} for the specified field.
		 * @param name the name of the field
		 * @param type the type of the field
		 * @return a resolved for the specified field
		 */
		public InjectedElementResolver field(String name, Class<?> type) {
			return new InjectedFieldResolver(getField(name, type), this.beanName);
		}

		/**
		 * Create an {@link InjectedElementResolver} for the specified bean method.
		 * @param name the name of the method on the target bean
		 * @param parameterTypes the method parameter types
		 * @return a resolved for the specified bean method
		 */
		public InjectedElementResolver method(String name, Class<?>... parameterTypes) {
			return new InjectedMethodResolver(getMethod(this.beanType, name, parameterTypes), this.beanType, this.beanName);
		}

		private Field getField(String fieldName, Class<?> fieldType) {
			Field field = ReflectionUtils.findField(this.beanType, fieldName, fieldType);
			Assert.notNull(field, () -> "No field '" + fieldName + "' with type " + fieldType.getName() + " found on " + this.beanType);
			return field;
		}

	}

}
