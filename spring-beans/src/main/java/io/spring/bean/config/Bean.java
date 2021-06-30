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

package io.spring.bean.config;

/*
 * DESIGN NOTES
 *
 * This object could act as the thread lock needed when instances are created.
 * It holds the singleton instance which is created as needed.
 */

/**
 * Internal class used to hold a bean registered with a {@link BeanContainer}.
 *
 * @param <T>
 * @author Phillip Webb
 */
final class Bean<T> {

	private final BeanContainer beanContainer;

	private final BeanRegistration<T> registration;

	private volatile Object instance;

	Bean(BeanContainer beanContainer, BeanRegistration<T> registration) {
		this.beanContainer = beanContainer;
		this.registration = registration;
	}

	BeanRegistration<T> getRegistration() {
		return registration;
	}

	@SuppressWarnings("unchecked")
	public T getBeanInstance() throws Exception {
		Object instance = this.instance;
		if (instance == null) {
			synchronized (this) {
				instance = this.instance;
				if (instance == null) {
					instance = this.registration.getInstanceSupplier().get(
							this.beanContainer);
					this.instance = instance;
				}
			}
		}
		return (T) instance;
	}

}
