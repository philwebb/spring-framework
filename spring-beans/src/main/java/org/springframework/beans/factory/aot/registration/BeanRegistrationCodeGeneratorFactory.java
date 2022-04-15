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

package org.springframework.beans.factory.aot.registration;

import org.springframework.aot.generate.MethodGenerator;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.lang.Nullable;

/**
 * Strategy factory interface that can be used to create a custom
 * {@link BeanRegistrationCodeGenerator} for a given {@link RegisteredBean} if the
 * {@link DefaultBeanRegistrationCodeGenerator} is not suitable.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public interface BeanRegistrationCodeGeneratorFactory {

	boolean isSupported(RegisteredBean registeredBean);

	@Nullable
	default Class<?> getPackagePrivateTarget(RegisteredBean registeredBean) {
		return InstanceSupplierCodeGenerator.getPackagePrivateTarget(registeredBean);
	}

	/**
	 * Return the {@link BeanRegistrationCode} that should be used for the given
	 * registered bean. This method will only be called if
	 * {@link #isSupported(RegisteredBean)} returns true.
	 * @param methodGenerator a method generator that can be used to add additional
	 * methods to support registration
	 * @param innerBeanDefinitionMethodGenerator the inner-bean definition method
	 * generator to use if inner-bean methods are required
	 * @param registeredBean the registered bean
	 * @return a {@link BeanRegistrationCode} instance
	 */
	BeanRegistrationCodeGenerator getBeanRegistrationCodeGenerator(MethodGenerator methodGenerator,
			InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator, RegisteredBean registeredBean);

}
