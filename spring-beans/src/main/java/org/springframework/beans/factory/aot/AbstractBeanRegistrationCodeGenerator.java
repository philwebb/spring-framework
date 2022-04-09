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

import java.util.ArrayList;
import java.util.List;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.util.Assert;

/**
 * Abstract base class for a {@link BeanRegistrationCodeGenerator} implementations.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationCodeGenerator
 */
public abstract class AbstractBeanRegistrationCodeGenerator implements BeanRegistrationCodeGenerator {

	private final BeanRegistrationsCode beanRegistrationsCode;

	private final RegisteredBean registeredBean;

	private final List<MethodReference> instancePostProcessors = new ArrayList<>();

	public AbstractBeanRegistrationCodeGenerator(BeanRegistrationsCode beanRegistrationsCode,
			RegisteredBean registeredBean) {
		this.beanRegistrationsCode = beanRegistrationsCode;
		this.registeredBean = registeredBean;
	}

	protected BeanRegistrationsCode getBeanRegistrationsCode() {
		return this.beanRegistrationsCode;
	}

	protected final List<MethodReference> getInstancePostProcessors() {
		return this.instancePostProcessors;
	}

	@Override
	public RegisteredBean getRegisteredBean() {
		return this.registeredBean;
	}

	@Override
	public String getBeanFactoryName() {
		return this.beanRegistrationsCode.getBeanFactoryName();
	}

	@Override
	public GeneratedMethod addMethod(Object... methodNameParts) {
		return this.beanRegistrationsCode.addMethod(methodNameParts);
	}

	@Override
	public void addInstancePostProcessor(MethodReference methodReference) {
		Assert.notNull(methodReference, "'methodReference' must not be null");
		this.instancePostProcessors.add(methodReference);
	}

}
