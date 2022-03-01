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

package org.springframework.beans.factory.aot;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class DefaultDefinedBean implements DefinedBean {

	public DefaultDefinedBean(UniqueBeanFactoryName beanFactoryName, ConfigurableListableBeanFactory beanFactory,
			String beanName) {
	}

	@Override
	public ConfigurableListableBeanFactory getBeanFactory() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String getBeanName() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public UniqueBeanName getUniqueBeanName() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Class<?> getResolvedBeanClass() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public BeanDefinition getBeanDefinition() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public BeanDefinition getMergedBeanDefinition() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
