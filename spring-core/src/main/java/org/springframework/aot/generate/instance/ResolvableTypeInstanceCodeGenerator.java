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

import java.util.Arrays;

import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link InstanceCodeGenerator} to support {@link ResolvableType ResolvableTypes}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class ResolvableTypeInstanceCodeGenerator implements InstanceCodeGenerator {

	static final ResolvableTypeInstanceCodeGenerator INSTANCE = new ResolvableTypeInstanceCodeGenerator();

	@Override
	public CodeBlock generateCode(@Nullable String name, Object value, ResolvableType type,
			InstanceCodeGenerationService service) {
		if (value instanceof ResolvableType resolvableType) {
			return generateCode(resolvableType, false);
		}
		return null;
	}

	private CodeBlock generateCode(ResolvableType resolvableType, boolean allowClassResult) {
		if (ResolvableType.NONE.equals(resolvableType)) {
			return CodeBlock.of("$T.NONE", ResolvableType.class);
		}
		Class<?> type = ClassUtils.getUserClass(resolvableType.toClass());
		if (resolvableType.hasGenerics()) {
			return generateCodeWithGenerics(resolvableType, type);
		}
		if (allowClassResult) {
			return CodeBlock.of("$T.class", type);
		}
		return CodeBlock.of("$T.forClass($T.class)", ResolvableType.class, type);
	}

	private CodeBlock generateCodeWithGenerics(ResolvableType target, Class<?> type) {
		ResolvableType[] generics = target.getGenerics();
		boolean hasNoNestedGenerics = Arrays.stream(generics).noneMatch(ResolvableType::hasGenerics);
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.add("$T.forClassWithGenerics($T.class", ResolvableType.class, type);
		for (ResolvableType generic : generics) {
			builder.add(", $L", generateCode(generic, hasNoNestedGenerics));
		}
		builder.add(")");
		return builder.build();
	}

}
