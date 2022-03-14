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

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link GenerationContext}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class DefaultGenerationContext implements GenerationContext {

	private final ClassNameGenerator classNameGenerator;

	private final GeneratedFiles generatedFiles;

	private final GeneratedSpringFactories generatedSpringFactories;

	private final RuntimeHints runtimeHints;

	/**
	 * Create a new {@link DefaultGenerationContext} instance backed by the
	 * specified {@code generatedFiles}.
	 * @param generatedFiles the generated files
	 * @param processors the processors
	 */
	public DefaultGenerationContext(GeneratedFiles generatedFiles) {
		this(new ClassNameGenerator(), generatedFiles,
				new DefaultGeneratedSpringFactories(), new RuntimeHints());
	}

	/**
	 * Create a new {@link DefaultGenerationContext} instance backed by the
	 * specified items.
	 * @param classNameGenerator the class name generator
	 * @param generatedFiles the generated files
	 * @param generatedSpringFactories the generated spring factories
	 * @param runtimeHints the runtime hints
	 */
	public DefaultGenerationContext(ClassNameGenerator classNameGenerator,
			GeneratedFiles generatedFiles,
			GeneratedSpringFactories generatedSpringFactories,
			RuntimeHints runtimeHints) {
		Assert.notNull(classNameGenerator, "'classNameGenerator' must not be null");
		Assert.notNull(generatedFiles, "'generatedFiles' must not be null");
		Assert.notNull(generatedSpringFactories,
				"'generatedSpringFactories' must not be null");
		Assert.notNull(runtimeHints, "'runtimeHints' must not be null");
		this.classNameGenerator = classNameGenerator;
		this.generatedFiles = generatedFiles;
		this.generatedSpringFactories = generatedSpringFactories;
		this.runtimeHints = runtimeHints;
	}

	@Override
	public ClassNameGenerator getClassNameGenerator() {
		return this.classNameGenerator;
	}

	@Override
	public GeneratedFiles getGeneratedFiles() {
		return this.generatedFiles;
	}

	@Override
	public GeneratedSpringFactories getGeneratedSpringFactories() {
		return this.generatedSpringFactories;
	}

	@Override
	public RuntimeHints getRuntimeHints() {
		return this.runtimeHints;
	}

}
