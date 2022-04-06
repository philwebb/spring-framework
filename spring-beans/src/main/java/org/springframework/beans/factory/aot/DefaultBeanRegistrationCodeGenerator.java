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

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.javapoet.CodeBlock;

/**
 * Default implementation of {@link BeanRegistrationCodeGenerator} that should work for
 * most beans.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class DefaultBeanRegistrationCodeGenerator implements BeanRegistrationCodeGenerator {

	private final List<CodeBlock> instancePostProcessors = new ArrayList<>();

	@Override
	public void addInstancePostProcessor(MethodReference methodReference) {
		// FIXME
	}

	@Override
	public void applyTo(GenerationContext generationContext,
			BeanFactoryInitializationCodeGenerator beanFactoryInitializationCodeGenerator) {
	}

}
