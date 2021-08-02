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

package io.spring.context;

import io.spring.bean.config.BeanContainer;
import io.spring.bean.config.BeanRepository;
import io.spring.bean.config.BeanSelection;
import io.spring.bean.config.BeanSelector;

/**
 * Abstract base class for {@link ApplicationContext} implementations backed by
 * a {@link BeanContainer}.
 */
public abstract class AbstractApplicationContext implements BeanRepository {

	private final BeanContainer beanContainer;

	protected AbstractApplicationContext(BeanContainer beanContainer) {
		this.beanContainer = beanContainer;
	}

	public <T> T get(Class<T> type) {
		return this.beanContainer.get(type);
	}

	public <T> T get(String name) {
		return this.beanContainer.get(name);
	}

	@Override
	public <T> BeanSelection<T> select(Class<T> type) {
		return this.beanContainer.select(type);
	}

	public <T> BeanSelection<T> select(BeanSelector<T> selector) {
		return this.beanContainer.select(selector);
	}

}
