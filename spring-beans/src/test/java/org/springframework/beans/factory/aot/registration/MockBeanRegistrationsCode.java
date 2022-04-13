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

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.MethodGenerator;

/**
 * Mock {@link BeanRegistrationsCode} implementation.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class MockBeanRegistrationsCode implements BeanRegistrationsCode {

	private final GeneratedMethods generatedMethods = new GeneratedMethods();

	private final InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator;

	MockBeanRegistrationsCode(BeanDefinitionMethodGeneratorFactory methodGeneratorFactory) {
		this.innerBeanDefinitionMethodGenerator = methodGeneratorFactory.getInnerBeanDefinitionMethodGenerator(this);
	}

	GeneratedMethods getGeneratedMethods() {
		return this.generatedMethods;
	}

	@Override
	public String getBeanFactoryName() {
		return "test";
	}

	@Override
	public MethodGenerator getMethodGenerator() {
		return this.generatedMethods;
	}

	@Override
	public InnerBeanDefinitionMethodGenerator getInnerBeanDefinitionMethodGenerator() {
		return this.innerBeanDefinitionMethodGenerator;
	}

}
