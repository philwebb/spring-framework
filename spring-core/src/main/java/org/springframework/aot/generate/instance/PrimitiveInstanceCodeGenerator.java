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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;

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

	private static final Map<Character, String> CHAR_ESCAPES;

	static {
		Map<Character, String> escapes = new HashMap<>();
		escapes.put('\b', "\\b");
		escapes.put('\t', "\\t");
		escapes.put('\n', "\\n");
		escapes.put('\f', "\\f");
		escapes.put('\r', "\\r");
		escapes.put('\"', "\"");
		escapes.put('\'', "\\'");
		escapes.put('\\', "\\\\");
		CHAR_ESCAPES = Collections.unmodifiableMap(escapes);
	}

	@Override
	public CodeBlock generateCode(Object value, ResolvableType type, InstanceCodeGenerationService service) {
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
		if (value instanceof Character character) {
			return CodeBlock.of("'$L'", escape(character));
		}
		return null;
	}

	private String escape(char ch) {
		String escaped = CHAR_ESCAPES.get(ch);
		if (escaped != null) {
			return escaped;
		}
		return (!Character.isISOControl(ch)) ? Character.toString(ch) : String.format("\\u%04x", (int) ch);
	}
}
