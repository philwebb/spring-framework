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

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGeneratedSpringFactories;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.GeneratedSpringFactories;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.RuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultAotContext}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class DefaultAotContextTests {

	private final ClassNameGenerator classNameGenerator = new ClassNameGenerator();

	private final GeneratedFiles generatedFiles = new InMemoryGeneratedFiles();

	private final GeneratedSpringFactories generatedSpringFactories = new DefaultGeneratedSpringFactories();

	private final RuntimeHints runtimeHints = new RuntimeHints();

	private final AotProcessors processors = mock(AotProcessors.class);

	@Test
	void createWithGeneratedFilesAndAotProcessorsCreatesContext() {
		AotContext context = new DefaultAotContext(this.generatedFiles, this.processors);
		assertThat(context.getClassNameGenerator()).isInstanceOf(ClassNameGenerator.class);
		assertThat(context.getGeneratedFiles()).isSameAs(this.generatedFiles);
		assertThat(context.getGeneratedSpringFactories()).isInstanceOf(DefaultGeneratedSpringFactories.class);
		assertThat(context.getRuntimeHints()).isInstanceOf(RuntimeHints.class);
		assertThat(context.getProcessors()).isSameAs(this.processors);
	}

	@Test
	void createCreatesContext() {
		AotContext context = new DefaultAotContext(this.classNameGenerator, this.generatedFiles,
				this.generatedSpringFactories, this.runtimeHints, this.processors);
		assertThat(context.getClassNameGenerator()).isNotNull();
		assertThat(context.getGeneratedFiles()).isNotNull();
		assertThat(context.getGeneratedSpringFactories()).isNotNull();
		assertThat(context.getRuntimeHints()).isNotNull();
		assertThat(context.getProcessors()).isNotNull();
	}

	@Test
	void createWhenAotProcessorsIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DefaultAotContext(this.classNameGenerator, this.generatedFiles,
						this.generatedSpringFactories, this.runtimeHints, null))
				.withMessage("'processors' must not be null");
		assertThatIllegalArgumentException().isThrownBy(() -> new DefaultAotContext(this.generatedFiles, null))
				.withMessage("'processors' must not be null");
	}

	@Test
	void getProcessorsReturnsProcessors() {
		AotContext context = new DefaultAotContext(this.classNameGenerator, this.generatedFiles,
				this.generatedSpringFactories, this.runtimeHints, this.processors);
		assertThat(context.getProcessors()).isSameAs(this.processors);
	}

}
