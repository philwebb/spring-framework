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

package org.springframework.context.generator;

import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.javapoet.CodeBlock;

/**
 * An {@link ApplicationContextAotGenerator} contributor that configures
 * the low-level infrastructure necessary to process an AOT context.
 *
 * @author Stephane Nicoll
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
class InfrastructureContributor implements ApplicationContextInitializationContributor {

	@Override
	public CodeBlock contribute(GeneratedTypeContext generationContext) {
		CodeBlock.Builder code = CodeBlock.builder();
		code.add("// infrastructure\n");
		code.addStatement("beanFactory.setAutowireCandidateResolver(new $T())",
				ContextAnnotationAutowireCandidateResolver.class);
		return code.build();
	}

}
