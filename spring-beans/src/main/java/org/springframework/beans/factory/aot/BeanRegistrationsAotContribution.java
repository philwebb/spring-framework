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
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;

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

	private final String beanFactoryName;

	private final List<BeanRegistrationMethodGenerator> contributedBeanRegistrations;

	BeanRegistrationsAotContribution(String beanFactoryName,
			List<BeanRegistrationMethodGenerator> contributedBeanRegistrations) {
		this.beanFactoryName = beanFactoryName;
		this.contributedBeanRegistrations = contributedBeanRegistrations;
	}

	@Override
	public void applyTo(GenerationContext generationContext,
			BeanFactoryInitializationCode beanFactoryInitializationCode) {

		BeanRegistrationsCode code = null;
		GeneratedClassName generateClassName = generationContext.getClassNameGenerator()
				.generateClassName(this.beanFactoryName, "Registrations");

		GeneratedMethods generatedMethods = new GeneratedMethods();

		// FIXME create a new file, create a new context, do the stuff, write the file,
		// add a reference

		List<MethodReference> registrationMethods = new ArrayList<>();
		for (BeanRegistrationMethodGenerator contributedBeanRegistration : this.contributedBeanRegistrations) {
			registrationMethods.add(contributedBeanRegistration.generateRegistrationMethod(generationContext, code));
		}

		// FIXME add the method

		beanFactoryInitializationCode.addInitializer(null);

	}

	void dunno(GenerationContext generationContext) {

		// GeneratedClass
		//
		// generationContext.generateClass("Foo");
		// generationContext.generateClass(MyClass.class, "Foo").using((gc, ty) -> {
		// // methods added
		// });
		//

		// generationContext

		// generationContext.addType(context-> {}).using((x, typeSpecBuilder -> ) {
		//
		// });

	}

}
