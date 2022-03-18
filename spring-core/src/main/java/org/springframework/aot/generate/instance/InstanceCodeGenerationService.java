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

import java.util.List;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;

/**
 * Service that generates code to re-create an instance of an object. For
 * example, given a {@link List} containing the Strings "a", "b", "c", a
 * generators might return {@code List.of("a", "b", "c")}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see DefaultInstanceCodeGenerationService
 */
public interface InstanceCodeGenerationService extends Iterable<InstanceCodeGenerator> {

	/**
	 * Return a shared instance backed by the
	 * {@link DefaultInstanceCodeGenerationService}.
	 * @return the shared instance
	 */
	static InstanceCodeGenerationService getSharedInstance() {
		return DefaultInstanceCodeGenerationService.getSharedInstance();
	}

	/**
	 * Generate code that we recreate the given instance value.
	 * @param value the value to recreate
	 * @return the generated code
	 * @throws IllegalArgumentException if no generation is possible for the
	 * given value
	 */
	default CodeBlock generateCode(@Nullable Object value) {
		return generateCode(null, value);
	}

	/**
	 * Generate code that we recreate the given instance value.
	 * @param name a name to identify value instance, or {@code null} if unnamed
	 * @param value the value to recreate
	 * @return the generated code
	 * @throws IllegalArgumentException if no generation is possible for the
	 * given value
	 */
	default CodeBlock generateCode(@Nullable String name, @Nullable Object value) {
		ResolvableType type = (value != null) ? ResolvableType.forInstance(value)
				: ResolvableType.NONE;
		return generateCode(name, value, type);
	}

	/**
	 * Generate code that we recreate the given instance value.
	 * @param name a name to identify value instance, or {@code null} if unnamed
	 * @param value the value to recreate
	 * @param type the type of the value object
	 * @return the generated code
	 * @throws IllegalArgumentException if no generation is possible for the
	 * given value
	 */
	CodeBlock generateCode(@Nullable String name, @Nullable Object value,
			ResolvableType type);

	/**
	 * Return if this service supports {@link GeneratedMethods}. If this method
	 * returns {@code true} then {@link #getGeneratedMethods()} can be safely
	 * called.
	 * @return {@code true} if generated methods are supported
	 * @see #getGeneratedMethods()
	 */
	boolean supportsGeneratedMethods();

	/**
	 * Return a {@link GeneratedMethods} instance that can be used to add
	 * methods to support instance generation.
	 * @return a generated methods instance
	 * @throws IllegalStateException if generation methods cannot be used with
	 * this service
	 * @see #supportsGeneratedMethods()
	 */
	GeneratedMethods getGeneratedMethods();

}
