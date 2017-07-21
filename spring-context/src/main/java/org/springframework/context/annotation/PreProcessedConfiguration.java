/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.BeanMethodInvoker.MethodInvoker;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class for use with pre-processed {@code @Configuration} classes.
 *
 * @author Phillip Webb
 * @since 5.0
 */
public class PreProcessedConfiguration {

	/**
	 * The class suffix that pre-processed configuration proxy classes must use.
	 */
	public static final String CLASS_SUFFIX = "$$ConfigurationProxy";


	private final ConfigurableBeanFactory beanFactory;

	private final Object instance;

	private final Lookup lookup;


	/**
	 * Create a new {@link PreProcessedConfiguration} instance.
	 * @param beanFactory the source bean factory
	 * @param instance the {@code @Configuration} subclass instance
	 * @param lookup a {@link MethodHandles#Lookup} created by the instance
	 */
	public PreProcessedConfiguration(BeanFactory beanFactory, Object instance,
			Lookup lookup) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.notNull(instance, "Instance must not be null");
		Assert.notNull(lookup, "Lookup must not be null");
		Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
		this.instance = instance;
		this.lookup = lookup;

	}


	/**
	 * Get a {@link BeanMethodInvoker} for the given method details
	 * @param name the method name
	 * @param paramTypes the parameters types
	 * @return a {@link BeanMethodInvoker}
	 */
	public BeanMethodInvoker beanMethod(String name, Class<?>... paramTypes) {
		Class<? extends Object> type = this.instance.getClass();
		Method method = findMethod(type, name, paramTypes);
		MethodInvoker invoker = getInvoker(type, name, paramTypes);
		return new BeanMethodInvoker(this.beanFactory, method, invoker);
	}

	private MethodInvoker getInvoker(Class<?> type, String name, Class<?>[] paramTypes) {
		try {
			Method superMethod = ReflectionUtils.findMethod(type.getSuperclass(), name, paramTypes);
			MethodHandle handle = this.lookup.unreflectSpecial(superMethod, type);
			return (args) -> handle.invokeWithArguments(withInstance(args));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Method findMethod(Class<?> type, String name, Class<?>... paramTypes) {
		Method method = ReflectionUtils.findMethod(type, name, paramTypes);
		Assert.notNull(method, "Unable to find method '" + name + "' with parameters "
				+ StringUtils.arrayToCommaDelimitedString(paramTypes));
		return method;
	}

	private Object[] withInstance(Object[] args) {
		Object[] result = new Object[args.length+1];
		result[0] = instance;
		System.arraycopy(args, 0, result, 1, args.length);
		return result;
	}

	/**
	 * Determine if the specified metadata reader is loading a pre-processed configuration
	 * proxy class.
	 * @param metadataReader the metdata reader
	 * @return if reading a pre-processed configuration proxy
	 */
	public static boolean isPreProcessedConfigurationProxy(MetadataReader metadataReader) {
		Assert.notNull(metadataReader, "MetadataReader must not be null");
		return isPreProcessedConfigurationProxy(metadataReader.getClassMetadata().getClassName());
	}

	/**
	 * Determine if the specified class name is for a pre-processed configuration proxy
	 * class.
	 * @param className the class name
	 * @return if reading a pre-processed configuration proxy
	 */
	public static boolean isPreProcessedConfigurationProxy(String className) {
		return (className != null && className.endsWith(CLASS_SUFFIX));
	}
}
