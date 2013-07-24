/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableLoadTimeWeaving.AspectJWeaving;
import org.springframework.context.weaving.AspectJWeavingEnabler;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.util.Assert;

public class LoadTimeWeavingEnablerRegistrar implements
		BeanClassLoaderAware, ImportBeanDefinitionRegistrar,
		BeanFactoryAware {

	private static final String ASPECTJ_WEAVING_ENABLER_CLASS_NAME =
			"org.springframework.context.weaving.AspectJWeavingEnabler";


	private ClassLoader beanClassLoader;

	private BeanFactory beanFactory;


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			BeanDefinitionRegistry registry) {
		AnnotationAttributes annotationAttributes = MetadataUtils.attributesFor(importingClassMetadata, EnableLoadTimeWeaving.class);
		Assert.notNull(annotationAttributes, "@EnableLoadTimeWeaving is not present on importing class "
						+ importingClassMetadata.getClassName());
		if (isEnabled(annotationAttributes)) {
			LoadTimeWeaver weaver = this.beanFactory.getBean(ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME, LoadTimeWeaver.class);
			AspectJWeavingEnabler.enableAspectJWeaving(weaver, this.beanClassLoader);
//			RootBeanDefinition beanDefinition = new RootBeanDefinition();
//			beanDefinition.setBeanClassName(ASPECTJ_WEAVING_ENABLER_CLASS_NAME);
//			beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
//			registry.registerBeanDefinition(ASPECTJ_WEAVING_ENABLER_CLASS_NAME, beanDefinition);
		}
	}

	private boolean isEnabled(AnnotationAttributes annotationAttributes) {
		AspectJWeaving aspectJWeaving = annotationAttributes.getEnum("aspectjWeaving");
		if (aspectJWeaving == AspectJWeaving.ENABLED) {
			return true;
		}
		if (aspectJWeaving == AspectJWeaving.AUTODETECT) {
			return this.beanClassLoader.getResource(AspectJWeavingEnabler.ASPECTJ_AOP_XML_RESOURCE) != null;
		}
		return false;
	}

}
