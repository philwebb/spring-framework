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

import org.springframework.aot.generate.GeneratedClassName;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.util.Assert;

/**
 * AOT contribution from a {@link BeanRegistrationsAotProcessor} used to register bean
 * definitions.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationsAotProcessor
 */
class BeanRegistrationsAotContribution implements BeanFactoryInitializationAotContribution {

	private final BeanRegistrationMethodGeneratorFactory methodGeneratorFactory;

	private final List<BeanRegistrationMethodGenerator> methodGenerators;

	BeanRegistrationsAotContribution(BeanRegistrationMethodGeneratorFactory methodGeneratorFactory,
			List<BeanRegistrationMethodGenerator> methodGenerators) {
		this.methodGenerators = methodGenerators;
		this.methodGeneratorFactory = methodGeneratorFactory;
	}

	@Override
	public void applyTo(GenerationContext generationContext,
			BeanFactoryInitializationCode beanFactoryInitializationCode) {
		String beanFactoryName = beanFactoryInitializationCode.getBeanFactoryName();
		BeanRegistrationsCode code = new BeanRegistrationsCodeGenerator(beanFactoryInitializationCode,
				this.methodGeneratorFactory);

		// FIXME create a new file, create a new context, do the stuff, write the file,
		// add a reference

		GeneratedClassName generateClassName = generationContext.getClassNameGenerator()
				.generateClassName(beanFactoryName, "Registrations");

		List<MethodReference> registrationMethods = new ArrayList<>();
		for (BeanRegistrationMethodGenerator contributedBeanRegistration : this.methodGenerators) {
			registrationMethods.add(contributedBeanRegistration.generateRegistrationMethod(generationContext, code));
		}

		// FIXME add the method

		beanFactoryInitializationCode.addInitializer(null);
	}

	static class BeanRegistrationsCodeGenerator implements BeanRegistrationsCode {

		private final BeanFactoryInitializationCode beanFactoryInitializationCode;

		private final InnerBeanRegistrationMethodGenerator innerBeanRegistrationMethodGenerator;

		private final GeneratedMethods generatedMethods = new GeneratedMethods();

		public BeanRegistrationsCodeGenerator(BeanFactoryInitializationCode beanFactoryInitializationCode,
				BeanRegistrationMethodGeneratorFactory methodGeneratorFactory) {
			this.innerBeanRegistrationMethodGenerator = (generationContext, innerRegisteredBean) -> {
				BeanRegistrationMethodGenerator methodGenerator = methodGeneratorFactory
						.getBeanRegistrationMethodGenerator(innerRegisteredBean);
				Assert.state(methodGenerator != null, "Unexpected filtering of inner-bean");
				return methodGenerator.generateRegistrationMethod(generationContext, this);
			};
			this.beanFactoryInitializationCode = beanFactoryInitializationCode;

		}

		@Override
		public String getBeanFactoryName() {
			return beanFactoryInitializationCode.getBeanFactoryName();
		}

		@Override
		public InnerBeanRegistrationMethodGenerator getBeanRegistrationMethodGenerator() {
			return this.innerBeanRegistrationMethodGenerator;
		}

		@Override
		public GeneratedMethod addMethod(Object... methodNameParts) {
			return this.generatedMethods.add(methodNameParts);
		}

	}

}
