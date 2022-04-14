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
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;

/**
 * Interface that can be used to configure the code that will be generated to perform
 * registration of a single bean.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationCodeGenerator
 */
public interface BeanRegistrationCode  {

	/**
	 * Return the source bean that is being registered.
	 * @return the registered bean
	 */
	RegisteredBean getRegisteredBean();

	/**
	 * Return the name of the bean factory that will accept the registration.
	 * @return the bean factory name
	 */
	String getBeanFactoryName();

	/**
	 * Return a {@link MethodGenerator} that can be used to add more methods to the registrations code.
	 * @return the method generator
	 */
	MethodGenerator getMethodGenerator();

	/**
	 * Add an instance post processor method call to the registration code.
	 * @param methodReference a reference to the post-process method to call. The
	 * referenced method must have a functional signature compatible with
	 * {@link InstanceSupplier#andThen}.
	 * @see InstanceSupplier#andThen(org.springframework.util.function.ThrowableBiFunction)
	 */
	void addInstancePostProcessor(MethodReference methodReference);

}
