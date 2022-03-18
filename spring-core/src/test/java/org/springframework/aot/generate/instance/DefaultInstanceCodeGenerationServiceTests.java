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

import java.io.InputStream;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link DefaultInstanceCodeGenerationService}. See also
 * {@code DefaultInstanceCodeGenerationServiceCompilerTests} in
 * {@literal spring-core-test}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
class DefaultInstanceCodeGenerationServiceTests {

	@Test
	void createWhenAddingDefaultGeneratorsAddsDefaultGenerators() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService();
		assertCanGenerate(service, 'c');
		assertCanGenerate(service, (byte) 1);
		assertCanGenerate(service, (short) 1);
		assertCanGenerate(service, 1);
		assertCanGenerate(service, 1L);
		assertCanGenerate(service, 1.0);
		assertCanGenerate(service, 1.0F);
		assertCanGenerate(service, "test");
		assertCanGenerate(service, ChronoUnit.DAYS);
		assertCanGenerate(service, InputStream.class);
		assertCanGenerate(service, ResolvableType.forClass(InputStream.class));
		assertCanGenerate(service, new int[0]);
		assertCanGenerate(service, Collections.emptyList());
		assertCanGenerate(service, Collections.emptySet());
		assertCanGenerate(service, Collections.emptyMap());
	}

	@Test
	void createWhenNotAddingDefaultGeneratorsHasNoGenerators() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(null, false);
		assertThat(service).extracting("generators").asList().isEmpty();
	}

	@Test
	void getSharedInstanceReturnsSharedInstance() {
		assertThat(DefaultInstanceCodeGenerationService.getSharedInstance())
				.isSameAs(DefaultInstanceCodeGenerationService.getSharedInstance());
	}

	@Test
	void addWhenSharedInstanceThrowsException() {
		assertThatIllegalStateException().isThrownBy(
				() -> DefaultInstanceCodeGenerationService.getSharedInstance().add(new TestInstanceCodeGenerator()))
				.withMessage("'DefaultInstanceCodeGenerationService.sharedInstance()' cannot be modified");
	}

	@Test
	void addAddsGenerator() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService();
		service.add(new TestInstanceCodeGenerator());
		assertThat(service.generateCode(new TestInstance())).hasToString("test");
	}

	@Test
	void addWhenExistingGeneratorAddsAbove() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService();
		CodeBlock block = CodeBlock.of("tset");
		service.add((name, value, type, service2) -> "test".equals(value) ? block : null);
		assertThat(service.generateCode("test")).isSameAs(block);
	}

	@Test
	void addWhenNullThrowsException() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService();
		assertThatIllegalArgumentException().isThrownBy(() -> service.add(null))
				.withMessage("'generator' must not be null");
	}

	@Test
	void supportsGeneratedMethodsWhenHasGeneratedMethodsReturnsTrue() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(new GeneratedMethods());
		assertThat(service.supportsGeneratedMethods()).isTrue();
	}

	@Test
	void supportsGeneratedMethodsWhenHasNoGeneratedMethodsReturnsFalse() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService();
		assertThat(service.supportsGeneratedMethods()).isFalse();
	}

	@Test
	void getGeneratedMethodsWhenHasGeneratedMethodsReturnsGeneratedMethods() {
		GeneratedMethods generatedMethods = new GeneratedMethods();
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(generatedMethods);
		assertThat(service.getGeneratedMethods()).isSameAs(generatedMethods);
	}

	@Test
	void getGeneratedMethodsWhenHasNoGeneratedMethodsThrowsException() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService();
		assertThatIllegalStateException().isThrownBy(() -> service.getGeneratedMethods())
				.withMessage("No GeneratedMethods instance available");
	}

	@Test
	void generateCodeWhenValueIsNullReturnsNullInstanceCodeBlock() {
		DefaultInstanceCodeGenerationService generator = DefaultInstanceCodeGenerationService.getSharedInstance();
		CodeBlock generated = generator.generateCode(null);
		assertThat(generated).isSameAs(DefaultInstanceCodeGenerationService.NULL_INSTANCE_CODE_BLOCK);
	}

	@Test
	void generateCodeGeneratesUsingFirstInstanceCodeGenerator() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(null, false);
		InstanceCodeGenerator generator1 = mock(InstanceCodeGenerator.class);
		InstanceCodeGenerator generator2 = mock(InstanceCodeGenerator.class);
		CodeBlock block = CodeBlock.of("test");
		given(generator1.generateCode(null, "test", ResolvableType.forClass(String.class), service)).willReturn(block);
		service.add(generator2); // Adds have higher priority
		service.add(generator1);
		assertThat(service.generateCode("test")).isSameAs(block);
		verifyNoInteractions(generator2);
	}

	@Test
	void generateCodeWhenHasNotInstanceCodeGeneratorThrowsException() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(null, false);
		assertThatIllegalArgumentException().isThrownBy(() -> service.generateCode("test"))
				.withMessage("'type' java.lang.String must be supported for instance code generation");
	}

	@Test
	void createWithParent() {
		DefaultInstanceCodeGenerationService parent = new DefaultInstanceCodeGenerationService(new GeneratedMethods(),
				false);
		parent.add(new TestInstanceCodeGenerator());
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(parent);
		assertThat(service.supportsGeneratedMethods()).isTrue();
		assertThat(service.generateCode(new TestInstance())).isNotNull();
		assertThatIllegalArgumentException().isThrownBy(() -> assertThat(service.generateCode("test")).isNull());
	}

	private void assertCanGenerate(DefaultInstanceCodeGenerationService service, Object value) {
		assertThat(service.generateCode(value)).isNotNull()
				.isNotEqualTo(DefaultInstanceCodeGenerationService.NULL_INSTANCE_CODE_BLOCK);
	}

	private static class TestInstanceCodeGenerator implements InstanceCodeGenerator {

		@Override
		public CodeBlock generateCode(String name, Object value, ResolvableType type,
				InstanceCodeGenerationService service) {
			return (value instanceof TestInstance) ? CodeBlock.of("test") : null;
		}

	}

	private static class TestInstance {

	}

}
