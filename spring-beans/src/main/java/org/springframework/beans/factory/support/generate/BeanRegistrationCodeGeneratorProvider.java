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

package org.springframework.beans.factory.support.generate;

import javax.annotation.Nullable;

import org.springframework.beans.factory.aot.DefinedBean;

/**
 * Strategy used to provide specific {@link BeanRegistrationContribution} for a
 * {@link DefinedBean}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public interface BeanRegistrationCodeGeneratorProvider {

	/**
	 * Return the {@link BeanRegistrationContribution} that should be used for the
	 * {@link DefinedBean}.
	 * @param definedBean the defined bean that should be registered
	 * @return a {@link BeanRegistrationContribution} instance or {@code null} if no
	 * special registration code is required
	 */
	@Nullable
	BeanRegistrationContribution getBeanRegistrationMethodCodeGenerator(DefinedBean definedBean);

}
