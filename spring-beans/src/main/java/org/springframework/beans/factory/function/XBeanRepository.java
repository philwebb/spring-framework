/*
 * Copyright 2012-2021 the original author or authors.
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

/*
 * DESIGN NOTES
 *
 * Similar to ListableBeanFactory. Allows beans to be selected using
 * selection criteria. Implementation will probably need index so that
 * common selections are fast.
 */

/**
 * A repository that allows beans to be selected based on various criteria. This
 * interface is passed to {@link FunctionalBeanInstanceSupplier BeanInstanceSuppliers} so
 * that they can perform dependency injection.
 *
 * @author Phillip Webb
 * @since 6.0.0
 */
public interface XBeanRepository {

	/**
	 * Return the bean instance that uniquely matches the given object type.
	 * @param <T> the bean type
	 * @param type the bean type
	 * @return an instance of the bean
	 */
	default <T> T get(Class<T> type) {
		return select(FunctionalBeanSelector.havingType(type)).get();
	}

	/**
	 * Return the bean instance with the given name or alias.
	 * @param <T>
	 * @param name
	 * @return an instance of the bean
	 */
	default <T> T get(String name) {
		FunctionalBeanSelector<T> selector = FunctionalBeanSelector.havingName(name);
		return select(selector).get();
	}

	/**
	 * Select beans based on the given {@link FunctionalBeanSelector}.
	 * @param <T> the selected bean type
	 * @param selector the selector used to limit the selection
	 * @return a bean selection
	 */
	<T> FunctionalBeanSelection<T> select(FunctionalBeanSelector<T> selector);

	<T> FunctionalBeanSelection<T> select(Class<T> type);

}
