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

import java.util.Collections;
import java.util.Set;

/**
 * {@link BeanSelector} that limits by Type.
 *
 * @author Phillip Webb
 */
class TypeBeanSelector<T> implements BeanSelector<T> {

	private final Class<?> type;

	private final Set<Class<?>> types;

	TypeBeanSelector(Class<?> type) {
		this.type = type;
		this.types = Collections.unmodifiableSet(Collections.singleton(type));
	}

	@Override
	public boolean test(BeanRegistration<?> registration) {
		return this.type.isAssignableFrom(registration.getType());
	}

	@Override
	public Set<Class<?>> getTypes() {
		return this.types;
	}

}
