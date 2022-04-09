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
import org.springframework.core.log.LogMessage;
import org.springframework.javapoet.CodeBlock;

/**
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationMethodGeneratorFactory
 */
class BeanRegistrationMethodGenerator {

	private final BeanRegistrationMethodGeneratorFactory manager;

	private final RegisteredBean registeredBean;

	private final List<BeanRegistrationAotContribution> aotContributions;

	BeanRegistrationMethodGenerator(BeanRegistrationMethodGeneratorFactory manager, RegisteredBean registeredBean,
			List<BeanRegistrationAotContribution> aotContributions) {
		this.manager = manager;
		this.registeredBean = registeredBean;
		this.aotContributions = aotContributions;
	}

	MethodReference generateRegistrationMethod(GenerationContext generationContext,
			BeanRegistrationsCode beanRegistrationsCode) {
		BeanRegistrationCodeGenerator codeGenerator = getCodeGenerator(beanRegistrationsCode,
				this.registeredBean);
		this.aotContributions.forEach((aotContribution) -> aotContribution.applyTo(generationContext, codeGenerator));
		CodeBlock generatedCode = codeGenerator.generateCode(generationContext);
		// wrap in a function named something or other
		// return a reference to the function
		return new MethodReference() {
		};
	}

	/**
	 * @param beanRegistrationsCode
	 * @param registeredBean2
	 * @return
	 */
	private BeanRegistrationCodeGenerator getCodeGenerator(BeanRegistrationsCode beanRegistrationsCode,
			RegisteredBean registeredBean2) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

//
//	BeanRegistrationCodeGenerator getCodeGenerator(BeanRegistrationsCode beanRegistrationsCode,
//			RegisteredBean registeredBean) {
//		InnerBeanRegistrationMethodGenerator innerBeanRegistrationMethodGenerator = getInnerBeanRegistrationMethodGenerator(
//				beanRegistrationsCode);
//		for (BeanRegistrationCodeGeneratorFactory codeGeneratorFactory : this.codeGeneratorFactories) {
//			BeanRegistrationCodeGenerator codeGenerator = codeGeneratorFactory.getBeanRegistrationCodeGenerator(
//					beanRegistrationsCode, registeredBean, innerBeanRegistrationMethodGenerator);
//			if (codeGenerator != null) {
//				logger.trace(LogMessage.format("Using bean registration code generator %S for '%S'",
//						codeGenerator.getClass().getName(), registeredBean.getBeanName()));
//				return codeGenerator;
//			}
//		}
//		return new DefaultBeanRegistrationCodeGenerator(beanRegistrationsCode, registeredBean);
//	}
//
//	private InnerBeanRegistrationMethodGenerator getInnerBeanRegistrationMethodGenerator(
//			BeanRegistrationsCode beanRegistrationsCode) {
//		return (generationContext, innerRegisteredBean) -> getContributedBeanRegistration(innerRegisteredBean)
//				.generateRegistrationMethod(generationContext, beanRegistrationsCode);
//	}

}