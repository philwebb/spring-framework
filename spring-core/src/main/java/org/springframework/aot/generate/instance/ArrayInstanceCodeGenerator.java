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

package org.springframework.aot.generate.instance;

import java.lang.reflect.Array;

import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;

/**
 * {@link InstanceCodeGenerator} to support {@link Class#isArray() arrays}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class ArrayInstanceCodeGenerator implements InstanceCodeGenerator {

	private final InstanceCodeGenerationService codeGenerationService;

	ArrayInstanceCodeGenerator(InstanceCodeGenerationService codeGenerationService) {
		this.codeGenerationService = codeGenerationService;
	}

	@Override
	public CodeBlock generateCode(@Nullable String name, @Nullable Object value,
			ResolvableType type) {
		if (type.isArray()) {
			ResolvableType componentType = type.getComponentType();
			int length = Array.getLength(value);
			CodeBlock.Builder builder = CodeBlock.builder();
			builder.add("new $T {", type.toClass());
			for (int i = 0; i < length; i++) {
				Object component = Array.get(value, i);
				CodeBlock componentCode = this.codeGenerationService.generateCode(name,
						component, componentType);
				builder.add((i != 0) ? ", " : "");
				builder.add("$L", componentCode);
			}
			builder.add("}");
			return builder.build();
		}
		return null;
	}

}