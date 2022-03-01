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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Details that can be used when AOT processing a bean that has been defined in a
 * {@link BeanFactory}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see AotBeanDefinitionProcessor
 * @see AotDefinedBeanExcludeFilter
 */
public interface DefinedBean {

	ConfigurableListableBeanFactory getBeanFactory();

	String getBeanName();

	UniqueBeanName getUniqueBeanName();

	Class<?> getResolvedBeanClass();

	BeanDefinition getBeanDefinition();

	BeanDefinition getMergedBeanDefinition();

}
