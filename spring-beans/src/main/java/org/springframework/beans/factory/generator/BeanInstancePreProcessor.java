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

package org.springframework.beans.factory.generator;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.lang.Nullable;

/**
 * Strategy interface to be implemented by components that participates in a
 * bean instance setup at runtime so that a {@link BeanInstanceContributor} can
 * provide an equivalent setup using generated code.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public interface BeanInstancePreProcessor {

	/**
	 * Pre-process the given bean definition, returning a
	 * {@link BeanInstanceContributor} when applicable.
	 * @param beanDefinition the merged bean definition for the bean
	 * @param beanType the actual type of the managed bean instance
	 * @param beanName the name of the bean
	 * @return the contributor to use or {@code null} if no pre-processing is
	 * applicable
	 */
	@Nullable
	BeanInstanceContributor preProcess(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName);

}
