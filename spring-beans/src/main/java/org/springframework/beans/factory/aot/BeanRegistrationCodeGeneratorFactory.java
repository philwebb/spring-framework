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

import groovyjarjarantlr4.v4.runtime.misc.Nullable;

import org.springframework.beans.factory.support.RegisteredBean;

/**
 * Strategy interface that can be used to create a custom
 * {@link BeanRegistrationCodeGenerator} for a given {@link RegisteredBean} if the
 * {@link DefaultBeanRegistrationCodeGenerator} is not suitable.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
@FunctionalInterface
public interface BeanRegistrationCodeGeneratorFactory {

	/**
	 * Return the {@link BeanRegistrationCodeGenerator} that should be used for the given
	 * registered bean or {@code null} if the registered bean isn't supported by this
	 * factory.
	 * @param registeredBean the registered bean
	 * @return a {@link BeanRegistrationCodeGenerator} instance or {@code null}
	 */
	@Nullable
	BeanRegistrationCodeGenerator getBeanRegistrationCodeGenerator(RegisteredBean registeredBean);

}
