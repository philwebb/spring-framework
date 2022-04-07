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

import java.util.List;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.CodeBlock;

/**
 * Internal helper class used to manage contributed bean registrations. Holds the
 * {@link BeanRegistrationCodeGenerator code generator} and any
 * {@link BeanRegistrationAotContribution AOT contributions} to apply.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class ContributedBeanRegistration {

	private final RegisteredBean registeredBean;

	private final List<BeanRegistrationAotContribution> aotContributions;

	private final BeanRegistrationCodeGenerator codeGenerator;

	ContributedBeanRegistration(RegisteredBean registeredBean, List<BeanRegistrationAotContribution> aotContributions,
			BeanRegistrationCodeGenerator codeGenerator) {
		this.registeredBean = registeredBean;
		this.aotContributions = aotContributions;
		this.codeGenerator = codeGenerator;
	}

	MethodReference generateRegistrationMethod(GenerationContext generationContext) {
		this.aotContributions.forEach((aotContribution) -> aotContribution.applyTo(generationContext, codeGenerator));
		CodeBlock generatedCode = this.codeGenerator.generateCode(generationContext);
		// wrap in a function named something or other
		// return a reference to the function
		return null;
	}

}