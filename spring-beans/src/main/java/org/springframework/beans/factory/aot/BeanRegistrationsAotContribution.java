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

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.util.MultiValueMap;

/**
 * AOT contribution from a {@link BeanRegistrationsAotProcessor} used to register bean
 * definitions.
 *
 * @author pwebb
 * @since 6.0
 * @see BeanRegistrationsAotProcessor
 */
class BeanRegistrationsAotContribution implements BeanFactoryInitializationAotContribution {

	// MVMap? BeanRegistration ->
	MultiValueMap<RegisteredBean, BeanRegistrationAotContribution> registrations;

	BeanRegistrationCodeGeneratorFactory factory;

	@Override
	public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCodeGenerator generator) {
		this.registrations.forEach((registeredBean, registrationContributions) -> {
			BeanRegistrationCodeGenerator beanRegistrationGenerator = null;
			for (BeanRegistrationAotContribution registrationContribution : registrationContributions) {
				registrationContribution.applyTo(generationContext, beanRegistrationGenerator);
			}
			beanRegistrationGenerator.applyTo(generationContext, generator);
		});
	}

}
