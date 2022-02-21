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

package org.springframework.context.generator;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.generator.BeanRegistrationGenerator;

/**
 * Thrown when no suitable {@link BeanRegistrationGenerator} be provided for a
 * given bean definition.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
@SuppressWarnings("serial")
public class BeanRegistrationGeneratorNotFoundException extends BeanDefinitionGenerationException {

	public BeanRegistrationGeneratorNotFoundException(String beanName, BeanDefinition beanDefinition) {
		super(beanName, beanDefinition, String.format(
				"No suitable generator found for bean with name '%s' and type '%s'",
				beanName, beanDefinition.getResolvableType()));
	}

}
