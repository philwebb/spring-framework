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
 * Strategy used by {@link InstanceCodeGenerationService} to support instance code
 * generation.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see InstanceCodeGenerationService
 */
@FunctionalInterface
public interface InstanceCodeGenerator {

	/**
	 * Return the generated code to recreate the given instance or {@code null} if the
	 * instance type is not supported.
	 * @param name the name of the instance (or {@code null})
	 * @param value the instance value that should be recreated by the generated code
	 * @param type the value type or {@code ResolvableType#NONE}
	 * @param service the generation service calling the generator
	 * @return the generated code or {@code null}
	 */
	@Nullable
	CodeBlock generateCode(@Nullable String name, Object value, ResolvableType type,
			InstanceCodeGenerationService service);

}