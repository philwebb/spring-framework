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

package org.springframework.beans.factory.support.aot;

import groovyjarjarantlr4.v4.runtime.misc.Nullable;

import org.springframework.beans.factory.aot.DefinedBean;
import org.springframework.beans.factory.support.generate.BeanRegistrationMethodCodeGenerator;

/**
 * Strategy used by {@link BeanRegistrationsAotBeanFactoryProcessor} in order to handle
 * customized bean registration. Most bean definitions won't need a custom handling and
 * can rely on the default behavior.
 *
 * specific {@link BeanRegistrationMethodCodeGenerator} for a {@link DefinedBean}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public interface DefinedBeanRegistrationHandler {

	/**
	 * Return {@code true} if this handler supports the given defined bean.
	 * @param definedBean the defined bean to check
	 * @return {@code true} if the defined bean should be handled by this handler
	 */
	boolean canHandle(DefinedBean definedBean);

	/**
	 * Return the {@link BeanRegistrationMethodCodeGenerator} that should be used.
	 * @param definedBean the defined bean that should be registered
	 * @return a {@link BeanRegistrationMethodCodeGenerator} instance of {@code null} to
	 * use default method code generation
	 */
	@Nullable
	BeanRegistrationMethodCodeGenerator getBeanRegistrationMethodCodeGenerator(DefinedBean definedBean);

}
