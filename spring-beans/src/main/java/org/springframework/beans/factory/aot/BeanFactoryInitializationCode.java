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

package org.springframework.beans.factory.aot;

import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactoryInitializer;

/**
 * Interface that can be used to configure the code that will be generated to perform bean
 * factory initialization.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public interface BeanFactoryInitializationCode {

	/**
	 * Add an initializer method call.
	 * @param methodReference a reference to the initialize method to call. The referenced
	 * method must have the same functional signature as
	 * {@link DefaultListableBeanFactoryInitializer}.
	 */
	void addInitializer(MethodReference methodReference);

}
