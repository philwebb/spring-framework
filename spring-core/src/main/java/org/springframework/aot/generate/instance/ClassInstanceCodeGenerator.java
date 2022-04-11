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
import org.springframework.util.ClassUtils;

/**
 * {@link InstanceCodeGenerator} to support {@link Class Classes}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class ClassInstanceCodeGenerator implements InstanceCodeGenerator {

	static final ClassInstanceCodeGenerator INSTANCE = new ClassInstanceCodeGenerator();

	@Override
	public CodeBlock generateCode(Object value, ResolvableType type, InstanceCodeGenerationService service) {
		if (value instanceof Class<?> clazz) {
			return CodeBlock.of("$T.class", ClassUtils.getUserClass(clazz));
		}
		return null;
	}

}
