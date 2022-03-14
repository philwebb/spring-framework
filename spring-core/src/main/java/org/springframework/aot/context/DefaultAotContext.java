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

package org.springframework.aot.context;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.GeneratedSpringFactories;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link AotContext}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class DefaultAotContext extends DefaultGenerationContext implements AotContext {

	private final AotProcessors processors;

	/**
	 * Create a new {@link DefaultAotContext} instance backed by the specified
	 * {@code generatedFiles} and {@code processors}.
	 * @param generatedFiles the generated files
	 * @param processors the processors
	 */
	public DefaultAotContext(GeneratedFiles generatedFiles, AotProcessors processors) {
		super(generatedFiles);
		Assert.notNull(processors, "'processors' must not be null");
		this.processors = processors;
	}

	/**
	 * Create a new {@link DefaultAotContext} instance backed by the specified items.
	 * @param classNameGenerator the class name generator
	 * @param generatedFiles the generated files
	 * @param generatedSpringFactories the generated spring factories
	 * @param runtimeHints the runtime hints
	 * @param processors the processors
	 */
	public DefaultAotContext(ClassNameGenerator classNameGenerator,
			GeneratedFiles generatedFiles,
			GeneratedSpringFactories generatedSpringFactories, RuntimeHints runtimeHints,
			AotProcessors processors) {
		super(classNameGenerator, generatedFiles, generatedSpringFactories, runtimeHints);
		Assert.notNull(processors, "'processors' must not be null");
		this.processors = processors;
	}

	@Override
	public AotProcessors getProcessors() {
		return this.processors;
	}

}
