/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.function;

import org.springframework.beans.factory.BeanDefinitionStoreException;

/**
 * Subclass of {@link BeanDefinitionStoreException} indicating an attempt to override an
 * existing {@link FunctionalBeanDefinition}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
@SuppressWarnings("serial")
public class FunctionalBeanDefinitionOverrideException extends BeanDefinitionStoreException {

	private final FunctionalBeanRegistration<?> beanRegistration;

	private final FunctionalBeanRegistration<?> existingRegistration;

	FunctionalBeanDefinitionOverrideException(FunctionalBeanRegistration<?> beanRegistration,
			FunctionalBeanRegistration<?> existingRegistration) {
		super("", existingRegistration.getDefinition().getName(), ""); // FIXME
		this.existingRegistration = existingRegistration;
		this.beanRegistration = beanRegistration;
	}

	/**
	 * Return the bean definition that was attempting registration.
	 * @see #getBeanName()
	 */
	public FunctionalBeanDefinition<?> getBeanDefinition() {
		return this.beanRegistration.getDefinition();
	}

	/**
	 * Return the existing bean definition that prevented registration.
	 * @see #getBeanName()
	 */
	public FunctionalBeanDefinition<?> getExistingDefinition() {
		return this.existingRegistration.getDefinition();
	}

}
