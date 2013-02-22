/*
 * Copyright 2002-2013 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.Conventions;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;

/**
 * Utilities for processing @{@link Configuration} classes.
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.1
 */
abstract class ConfigurationClassUtils {

	private static final Log logger = LogFactory.getLog(ConfigurationClassUtils.class);

	private static final String CONFIGURATION_CLASS_ATTRIBUTE =
		Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");


	/**
	 * Check whether the given bean definition is a candidate for a configuration class,
	 * and mark it accordingly.
	 * @param beanDef the bean definition to check
	 * @param metadataReaderFactory the current factory in use by the caller
	 * @return whether the candidate qualifies as (any kind of) configuration class
	 */
	public static boolean checkConfigurationClassCandidate(BeanDefinition beanDef,
			MetadataReaderFactory metadataReaderFactory) {
		Type type = getType(beanDef, metadataReaderFactory);
		if(type != null) {
			beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, type);
			return true;
		}
		return false;
	}

	private static Type getType(BeanDefinition beanDef,
			MetadataReaderFactory metadataReaderFactory) {
		if (beanDef instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) beanDef).hasBeanClass()) {
			return getType(((AbstractBeanDefinition) beanDef).getBeanClass(), metadataReaderFactory);
		}
		return getType(beanDef.getBeanClassName(), metadataReaderFactory);
	}

	private static Type getType(Object beanClass, MetadataReaderFactory metadataReaderFactory) {
		if(beanClass != null) {
			AnnotationMetadata metadata = getMetadata(metadataReaderFactory, beanClass);
			if (metadata != null) {
				Type type = Type.get(metadata);
				if (type == null) {
					type = getType(
							beanClass instanceof Class ? ((Class<?>) beanClass).getSuperclass()
									: metadata.getSuperClassName(), metadataReaderFactory);
				}
				return type;
			}
		}
		return null;
	}

	private static AnnotationMetadata getMetadata(
			MetadataReaderFactory metadataReaderFactory, Object beanClass) {
		if (beanClass instanceof Class) {
			return new StandardAnnotationMetadata((Class<?>) beanClass, true);
		}
		try {
			MetadataReader metadataReader = metadataReaderFactory.getMetadataReader((String) beanClass);
			return metadataReader.getAnnotationMetadata();
		}
		catch (IOException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not find class file for introspecting factory methods: " + beanClass, ex);
			}
		}
		return null;
	}

	public static boolean isConfigurationCandidate(AnnotationMetadata metadata,
			MetadataReaderFactory metadataReaderFactory) {
		return getType(metadata.getClassName(), metadataReaderFactory) != null;
	}

	/**
	 * Determine whether the given bean definition indicates a full @Configuration class.
	 */
	public static boolean isFullConfigurationClass(BeanDefinition beanDef) {
		return Type.FULL.equals(beanDef.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE));
	}

	private enum Type {
		FULL {
			@Override
			public boolean matches(AnnotationMetadata metadata) {
				return metadata.isAnnotated(Configuration.class.getName());
			}
		},
		LITE {
			@Override
			public boolean matches(AnnotationMetadata metadata) {
				return !metadata.isInterface() && // not an interface or an annotation
						(metadata.isAnnotated(Component.class.getName()) || metadata.hasAnnotatedMethods(Bean.class.getName()));
			}
		};

		public abstract boolean matches(AnnotationMetadata metadata);

		public static Type get(AnnotationMetadata metadata) {
			for (Type type : values()) {
				if (type.matches(metadata)) {
					return type;
				}
			}
			return null;
		}
	}

}
