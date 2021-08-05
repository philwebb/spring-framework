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

import org.springframework.beans.factory.ObjectProvider;

/**
 *
 * @author Phillip Webb
 * @since 6.0
 */
public interface InjectionContext {

	// FIXME we probably want to resolve multiple in one hit for future threading

	default <T> T get(Class<T> type) {
		return get(BeanSelector.byType(type));
	}

	default <T> T get(String name) {
		return get(BeanSelector.byName(name));
	}

	default <T> T get(String name, Class<T> requiredType){
		return getProvider(BeanSelector.byName(name)).getObject(requiredType);
	}

	default <T> T get(BeanSelector<T> selector){
		return getProvider(selector).getObject();
	}

	<T> ObjectProvider<T> getProvider(BeanSelector<T> selector);

	Object[] getArgs();

}
