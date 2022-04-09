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
import org.springframework.javapoet.CodeBlock;

/**
 * Default implementation of {@link BeanRegistrationCode} that should work for most beans.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationCodeGeneratorFactory
 */
public class DefaultBeanRegistrationCodeGenerator extends AbstractBeanRegistrationCodeGenerator {

	public DefaultBeanRegistrationCodeGenerator(BeanRegistrationsCode beanRegistrationsCode,
			RegisteredBean registeredBean) {
		super(beanRegistrationsCode, registeredBean);
	}

	@Override
	public CodeBlock generateCode(GenerationContext generationContext) {
		return CodeBlock.of("// FIXME");
		// FIXME hide other code generators in protected methods
	}

}
