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

import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;

/**
 * {@link InstanceCodeGenerator} to {@link Class#isPrimitive() primitives}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class PrimitiveInstanceCodeGenerator implements InstanceCodeGenerator {

	static final PrimitiveInstanceCodeGenerator INSTANCE = new PrimitiveInstanceCodeGenerator();

	@Override
	public CodeBlock generateCode(@Nullable String name, Object value, ResolvableType type,
			InstanceCodeGenerationService service) {
		if (value instanceof Boolean || value instanceof Integer) {
			return CodeBlock.of("$L", value);
		}
		if (value instanceof Byte) {
			return CodeBlock.of("(byte) $L", value);
		}
		if (value instanceof Short) {
			return CodeBlock.of("(short) $L", value);
		}
		if (value instanceof Long) {
			return CodeBlock.of("$LL", value);
		}
		if (value instanceof Float) {
			return CodeBlock.of("$LF", value);
		}
		if (value instanceof Double) {
			return CodeBlock.of("(double) $L", value);
		}
		return null;
	}

}
