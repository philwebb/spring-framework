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

package org.springframework.aot.generate;

import org.springframework.aot.hint.JavaSerializationHints;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;

/**
 * Central interface used for code generation.
 * <p>
 * An generation context provides:
 * <ul>
 * <li>Support for {@link #getClassNameGenerator() class name generation}.</li>
 * <li>Central management of all {@link #getGeneratedFiles() generated
 * files}.</li>
 * <li>Support for the recording of {@link #getRuntimeHints() runtime
 * hints}.</li>
 * <li>Registration of {@link #getGeneratedSpringFactories() spring.factories}
 * content.
 * </ul>
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @since 6.0
 */
public interface GenerationContext {

	/**
	 * Return the {@link ClassNameGenerator} being used by the context. Allows
	 * new class names to be generated before they are added to the
	 * {@link #getGeneratedFiles() generated files}.
	 * @return the class name generator
	 */
	ClassNameGenerator getClassNameGenerator();

	/**
	 * Return the {@link GeneratedFiles} being used by the context. Used to
	 * write resource, java source or class bytecode files.
	 * @return the generated files
	 */
	GeneratedFiles getGeneratedFiles();

	/**
	 * Return the {@link GeneratedSpringFactories} files that will be written
	 * once AOT processing has completed. Typically used to register generated
	 * code as a service so that it will be applied when the AOT optimized
	 * application is run.
	 * @return the spring factories
	 */
	GeneratedSpringFactories getGeneratedSpringFactories();

	/**
	 * Return the {@link RuntimeHints} being used by the context. Used to record
	 * {@link ReflectionHints reflection}, {@link ResourceHints resource},
	 * {@link JavaSerializationHints serialization} and {@link ProxyHints proxy}
	 * hints so that the application can run as a native image.
	 * @return the runtime hints
	 */
	RuntimeHints getRuntimeHints();

}
