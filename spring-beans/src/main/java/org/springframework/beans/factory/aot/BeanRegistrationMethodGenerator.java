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

import java.util.function.Function;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;

/**
 *
 * @author pwebb
 * @since 6.0
 */
class BeanRegistrationHandler {

	private BeanRegistrationAotProcessors processors;

	private BeanRegistrationExcludeFilters filters;

	private BeanRegistrationCodeGeneratorFactories factories;

	BeanRegistrationHandler(BeanRegistrationAotProcessors processors, BeanRegistrationCodeGeneratorFactories factories,
			BeanRegistrationExcludeFilters filters) {
		this.processors = processors;
		this.factories = factories;
	}

	@Nullable
	public BeanRegistrationContributions get(RegisteredBean registeredBean) {
		// FIXME for each processor
		// FIXME filter
		// FIXME check if contribution and add
		BeanRegistrationAotContribution contribution = processors.processAheadOfTime(registeredBean);
		return null;
	}

	class BeanRegistrationContributions {

		private RegisteredBean registeredBean;

		private BeanRegistrationAotContribution contributions;

		public BeanRegistrationContributions(RegisteredBean registeredBean, BeanRegistrationAotContribution contributions) {
			this.registeredBean = registeredBean;
			this.contributions = contributions;
		}

		MethodReference generateMethod(GenerationContext generationContext) {
			BeanRegistrationCodeGenerator codeGenerator = factories.getBeanRegistrationCodeGenerator(registeredBean,
					this::generateInnerBeanRegistrationMethod);
			contributions.applyTo(generationContext, codeGenerator);
			codeGenerator.generateCode(generationContext);
			// wrap in a function named something or other
			return null;
		}

		private MethodReference generateInnerBeanRegistrationMethod(GenerationContext generationContext,
				RegisteredBean innerRegisteredBean) {
			return get(innerRegisteredBean).generateMethod(generationContext);
		}

	}

}
