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

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Utilities for processing @{@link Configuration} classes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
abstract class ConfigurationClassUtils {

	private static final String CONFIGURATION_CLASS_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");

	private static final String PRE_PROCESSED_CONFIGURATION_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "preProcessedConfiguration");

	private static final String ORDER_ATTRIBUTE =
			Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "order");


	private static final Log logger = LogFactory.getLog(ConfigurationClassUtils.class);

	private static final Set<String> candidateIndicators = new HashSet<>(4);

	static {
		candidateIndicators.add(Component.class.getName());
		candidateIndicators.add(ComponentScan.class.getName());
		candidateIndicators.add(Import.class.getName());
		candidateIndicators.add(ImportResource.class.getName());
	}


	/**
	 * Check whether the given bean definition is a candidate for a configuration class
	 * (or a nested component class declared within a configuration/component class,
	 * to be auto-registered as well), and mark it accordingly.
	 * @param beanDef the bean definition to check
	 * @param metadataReaderFactory the current factory in use by the caller
	 * @return whether the candidate qualifies as (any kind of) configuration class
	 */
	public static boolean checkConfigurationClassCandidate(BeanDefinition beanDef, MetadataReaderFactory metadataReaderFactory) {
		String className = beanDef.getBeanClassName();
		if (className == null) {
			return false;
		}

		AnnotationMetadata metadata;
		if (beanDef instanceof AnnotatedBeanDefinition &&
				className.equals(((AnnotatedBeanDefinition) beanDef).getMetadata().getClassName())) {
			// Can reuse the pre-parsed metadata from the given BeanDefinition...
			metadata = ((AnnotatedBeanDefinition) beanDef).getMetadata();
		}
		else if (beanDef instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) beanDef).hasBeanClass()) {
			// Check already loaded Class if present...
			// since we possibly can't even load the class file for this Class.
			Class<?> beanClass = ((AbstractBeanDefinition) beanDef).getBeanClass();
			metadata = new StandardAnnotationMetadata(beanClass, true);
		}
		else {
			try {
				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
				metadata = metadataReader.getAnnotationMetadata();
			}
			catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not find class file for introspecting configuration annotations: " + className, ex);
				}
				return false;
			}
		}

		String preProcessed = findPreProcessedConfiguration(metadataReaderFactory, className);
		if (preProcessed != null) {
			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, ConfigurationClassType.PRE_PROCESSED);
			beanDef.setAttribute(PRE_PROCESSED_CONFIGURATION_ATTRIBUTE, preProcessed);
		}
		else if (isFullConfigurationCandidate(metadata)) {
			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, ConfigurationClassType.FULL);
		}
		else if (isLiteConfigurationCandidate(metadata)) {
			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, ConfigurationClassType.LITE);
		}
		else {
			return false;
		}

		// It's a full or lite configuration candidate... Let's determine the order value, if any.
		Integer order = getOrder(metadata);
		if (order != null) {
			beanDef.setAttribute(ORDER_ATTRIBUTE, order);
		}

		return true;
	}

	/**
	 * Check the given metadata for a configuration class candidate
	 * (or nested component class declared within a configuration/component class).
	 * @param metadata the metadata of the annotated class
	 * @return {@code true} if the given class is to be registered as a
	 * reflection-detected bean definition; {@code false} otherwise
	 */
	public static boolean isConfigurationCandidate(AnnotationMetadata metadata) {
		return (isFullConfigurationCandidate(metadata) || isLiteConfigurationCandidate(metadata));
	}


	/**
	 * Check if the given class name as a pre-processed variant available. Deals with
	 * simple top level classes as well as nested inner classes.
	 * @param metadataReaderFactory the metadata reader factory
	 * @param className the configuration class name
	 * @return {@code true} if a pre-processed class is available.
	 */
	private static String findPreProcessedConfiguration(
			MetadataReaderFactory metadataReaderFactory, String className) {
		String prefix = className;
		String suffix = "";
		while (true) {
			String candidate = prefix + PreProcessedConfiguration.CLASS_SUFFIX + suffix;
			if (canReadClassName(metadataReaderFactory, candidate)) {
				return candidate;
			}
			int split = prefix.lastIndexOf('$');
			if (split == -1) {
				return null;
			}
			suffix = prefix.substring(split) + PreProcessedConfiguration.CLASS_SUFFIX + suffix;
			prefix = prefix.substring(0, split);
		}
	}

	private static boolean canReadClassName(MetadataReaderFactory metadataReaderFactory,
			String className) {
		ResourceLoader loader = (metadataReaderFactory instanceof SimpleMetadataReaderFactory
				? ((SimpleMetadataReaderFactory) metadataReaderFactory).getResourceLoader()
				: null);
		if (loader != null) {
			// Use the resource loader if possible to save throwing exceptions
			String resourcePath = ResourceLoader.CLASSPATH_URL_PREFIX +
					ClassUtils.convertClassNameToResourcePath(className) + ClassUtils.CLASS_FILE_SUFFIX;
			return loader.getResource(resourcePath).exists();
		}
		try {
			MetadataReader reader = metadataReaderFactory.getMetadataReader(className);
			return (reader != null);
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Check the given metadata for a full configuration class candidate
	 * (i.e. a class annotated with {@code @Configuration}).
	 * @param metadata the metadata of the annotated class
	 * @return {@code true} if the given class is to be processed as a full
	 * configuration class, including cross-method call interception
	 */
	private static boolean isFullConfigurationCandidate(AnnotationMetadata metadata) {
		return metadata.isAnnotated(Configuration.class.getName());
	}

	/**
	 * Check the given metadata for a lite configuration class candidate
	 * (e.g. a class annotated with {@code @Component} or just having
	 * {@code @Import} declarations or {@code @Bean methods}).
	 * @param metadata the metadata of the annotated class
	 * @return {@code true} if the given class is to be processed as a lite
	 * configuration class, just registering it and scanning it for {@code @Bean} methods
	 */
	private static boolean isLiteConfigurationCandidate(AnnotationMetadata metadata) {
		// Do not consider an interface or an annotation...
		if (metadata.isInterface()) {
			return false;
		}

		// Any of the typical annotations found?
		for (String indicator : candidateIndicators) {
			if (metadata.isAnnotated(indicator)) {
				return true;
			}
		}

		// Finally, let's look for @Bean methods...
		try {
			return metadata.hasAnnotatedMethods(Bean.class.getName());
		}
		catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to introspect @Bean methods on class [" + metadata.getClassName() + "]: " + ex);
			}
			return false;
		}
	}

	public static ConfigurationClassType getConfigurationType(BeanDefinition beanDefinition) {
		return (ConfigurationClassType) beanDefinition.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE);
	}

	/**
	 * Determine the order for the given configuration class metadata.
	 * @param metadata the metadata of the annotated class
	 * @return the {@link @Order} annotation value on the configuration class,
	 * or {@link Ordered#LOWEST_PRECEDENCE} if none declared
	 * @since 5.0
	 */
	public static Integer getOrder(AnnotationMetadata metadata) {
		Map<String, Object> orderAttributes = metadata.getAnnotationAttributes(Order.class.getName());
		return (orderAttributes != null ? ((Integer) orderAttributes.get(AnnotationUtils.VALUE)) : null);
	}

	/**
	 * Determine the order for the given configuration class bean definition,
	 * as set by {@link #checkConfigurationClassCandidate}.
	 * @param beanDef the bean definition to check
	 * @return the {@link @Order} annotation value on the configuration class,
	 * or {@link Ordered#LOWEST_PRECEDENCE} if none declared
	 * @since 4.2
	 */
	public static int getOrder(BeanDefinition beanDef) {
		Integer order = (Integer) beanDef.getAttribute(ORDER_ATTRIBUTE);
		return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
	}

	public static Class<?> getPreProcessedClass(BeanDefinition beanDefinition,
			ClassLoader classLoader) {
		String className = (String) beanDefinition.getAttribute(
				PRE_PROCESSED_CONFIGURATION_ATTRIBUTE);
		return ClassUtils.resolveClassName(className, classLoader);
	}


	/**
	 * Configuration class types.
	 */
	public static enum ConfigurationClassType {

		/**
		 * A pre-processed {@code @Configuration} class using {@link ConfigurationProxy}.
		 */
		PRE_PROCESSED,

		/**
		 * A full {@code @Configuration} class using {@link ConfigurationClassEnhancer}.
		 */
		FULL,

		/**
		 * A lite {@code @Configuration} class that doesn't use a proxy.
		 */
		LITE;
	}
}
