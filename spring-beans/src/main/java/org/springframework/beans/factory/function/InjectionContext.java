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

	Object[] getArgs();

	default <T> T getBean(Class<T> type) {
		return getBean(BeanSelector.byType(type));
	}

	default <T> T getBean(String name) {
		return getBean(BeanSelector.byName(name));
	}

	default <T> T getBean(String name, Class<T> requiredType){
		return getBeanProvider(BeanSelector.byName(name)).getObject(requiredType);
	}

	default <T> T getBean(BeanSelector<T> selector){
		return getBeanProvider(selector).getObject();
	}

	<T> ObjectProvider<T> getBeanProvider(BeanSelector<T> selector);

	String resolveEmbeddedValue(String value);

}
