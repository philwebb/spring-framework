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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.log.LogMessage;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;

/**
 * Generates bean registration code and returns a reference to the method that should be
 * called.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationMethodGeneratorFactory
 */
class BeanRegistrationMethodGenerator {

	private static final Log logger = LogFactory.getLog(BeanRegistrationMethodGenerator.class);

	private final RegisteredBean registeredBean;

	@Nullable
	private final String innerBeanPropertyName;

	private final List<BeanRegistrationAotContribution> aotContributions;

	private final List<BeanRegistrationCodeGeneratorFactory> codeGeneratorFactories;

	/**
	 * Create a new {@link BeanRegistrationMethodGenerator} instance.
	 * @param registeredBean the registered bean
	 * @param aotContributions the AOT contributions that should be applied before
	 * generating the registration method
	 */
	BeanRegistrationMethodGenerator(RegisteredBean registeredBean, @Nullable String innerBeanPropertyName,
			List<BeanRegistrationAotContribution> aotContributions,
			List<BeanRegistrationCodeGeneratorFactory> codeGeneratorFactories) {
		this.registeredBean = registeredBean;
		this.innerBeanPropertyName = innerBeanPropertyName;
		this.aotContributions = aotContributions;
		this.codeGeneratorFactories = codeGeneratorFactories;
	}

	/**
	 * Generate the registration method and return a {@link MethodReference} that can be
	 * called when the {@link DefaultListableBeanFactory} is initialized.
	 * @param generationContext
	 * @param beanRegistrationsCode
	 * @return
	 */
	MethodReference generateRegistrationMethod(GenerationContext generationContext,
			BeanRegistrationsCode beanRegistrationsCode) {
		BeanRegistrationCodeGenerator codeGenerator = getCodeGenerator(beanRegistrationsCode);
		this.aotContributions.forEach((aotContribution) -> aotContribution.applyTo(generationContext, codeGenerator));
		CodeBlock generatedCode = codeGenerator.generateCode(generationContext);
		// FIXME wrap in a function named something or other
		// return a reference to the function
		return null;
	}

	private BeanRegistrationCodeGenerator getCodeGenerator(BeanRegistrationsCode beanRegistrationsCode) {
		for (BeanRegistrationCodeGeneratorFactory codeGeneratorFactory : this.codeGeneratorFactories) {
			BeanRegistrationCodeGenerator codeGenerator = codeGeneratorFactory
					.getBeanRegistrationCodeGenerator(registeredBean, innerBeanPropertyName, beanRegistrationsCode);
			if (codeGenerator != null) {
				logger.trace(LogMessage.format("Using bean registration code generator %S for '%S'",
						codeGenerator.getClass().getName(), registeredBean.getBeanName()));
				return codeGenerator;
			}
		}
		return new DefaultBeanRegistrationCodeGenerator(registeredBean, null, beanRegistrationsCode);
	}

}