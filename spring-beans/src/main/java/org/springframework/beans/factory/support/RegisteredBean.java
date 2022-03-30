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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanFactory;

/**
 * A {@code RegisteredBean} represents a bean that has been registered with a
 * {@link BeanFactory}, but has not necessarily been instantiated. It provides access to
 * the bean factory that contains the bean as well as the bean name. In the case of
 * inner-beans, the bean name may have been generated.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public final class RegisteredBean {

	private final String beanName;

	private final BeanFactory beanFactory;

	RegisteredBean(String beanName, BeanFactory beanFactory) {
		this.beanName = beanName;
		this.beanFactory = beanFactory;
	}

	/**
	 * Return the name of the bean.
	 * @return the beanName the bean name
	 */
	public String getBeanName() {
		return beanName;
	}

	/**
	 * Return the bean factory containing the bean.
	 * @return the bean factory
	 */
	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

}
