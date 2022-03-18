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
import org.springframework.lang.Nullable;

/**
 * {@link InstanceCodeGenerator} to support {@link Character Characters}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class CharacterInstanceCodeGenerator implements InstanceCodeGenerator {

	static final CharacterInstanceCodeGenerator INSTANCE = new CharacterInstanceCodeGenerator();

	private static final Map<Character, String> ESCAPES;
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
		ESCAPES = Collections.unmodifiableMap(escapes);
	}

	@Override
	public CodeBlock generateCode(@Nullable String name, @Nullable Object value, ResolvableType type,
			InstanceCodeGenerationService service) {
		if (value instanceof Character character) {
			return CodeBlock.of("'$L'", escape(character));
		}
		return null;
	}

	private String escape(char ch) {
		String escaped = ESCAPES.get(ch);
		if (escaped != null) {
			return escaped;
		}
		return (!Character.isISOControl(ch)) ? Character.toString(ch) : String.format("\\u%04x", (int) ch);
	}

}
