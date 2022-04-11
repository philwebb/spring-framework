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
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.instance.DefaultInstanceCodeGenerationService.InstanceCodeGenerators;
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
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(null, null,
				InstanceCodeGenerators::none);
		assertThat(service).extracting("generators").asList().isEmpty();
	}

	@Test
	void createWhenGeneratorsConsumerIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DefaultInstanceCodeGenerationService(null, null, null))
				.withMessage("'instanceCodeGenerators' must not be null");
	}

	@Test
	void createWithAddedGeneratorWhenAddedIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new DefaultInstanceCodeGenerationService(null, null, (generators) -> generators.add(null)))
				.withMessage("'instanceCodeGenerator' must not be null");
	}

	@Test
	void createWithAddedGeneratorAddsGenerator() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(null, null,
				(generators) -> generators.add(new TestInstanceCodeGenerator()));
		assertThat(service.generateCode(new TestInstance())).hasToString("test");
	}

	@Test
	void getSharedInstanceReturnsSharedInstance() {
		assertThat(DefaultInstanceCodeGenerationService.getSharedInstance())
				.isSameAs(DefaultInstanceCodeGenerationService.getSharedInstance());
	}

	@Test
	void supportsGeneratedMethodsWhenHasGeneratedMethodsReturnsTrue() {
		GeneratedMethods generatedMethods = new GeneratedMethods();
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(generatedMethods);
		assertThat(service.supportsMethodGeneration()).isTrue();
	}

	@Test
	void supportsGeneratedMethodsWhenHasNoGeneratedMethodsReturnsFalse() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService();
		assertThat(service.supportsMethodGeneration()).isFalse();
	}

	@Test
	void getGeneratedMethodsWhenHasGeneratedMethodsReturnsGeneratedMethods() {
		GeneratedMethods generatedMethods = new GeneratedMethods();
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(generatedMethods);
		assertThat(service.getMethodGenerator()).isSameAs(generatedMethods);
	}

	@Test
	void getGeneratedMethodsWhenHasNoGeneratedMethodsThrowsException() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService();
		assertThatIllegalStateException().isThrownBy(() -> service.getMethodGenerator())
				.withMessage("No MethodGenerator available");
	}

	@Test
	void generateCodeWhenValueIsNullReturnsNullInstanceCodeBlock() {
		DefaultInstanceCodeGenerationService generator = DefaultInstanceCodeGenerationService.getSharedInstance();
		CodeBlock generated = generator.generateCode(null);
		assertThat(generated).isSameAs(DefaultInstanceCodeGenerationService.NULL_INSTANCE_CODE_BLOCK);
	}

	@Test
	void generateCodeGeneratesUsingFirstInstanceCodeGenerator() {
		InstanceCodeGenerator generator1 = mock(InstanceCodeGenerator.class);
		InstanceCodeGenerator generator2 = mock(InstanceCodeGenerator.class);
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService((generators) -> {
			generators.add(generator1);
			generators.add(generator2);
		});
		CodeBlock block = CodeBlock.of("test");
		given(generator1.generateCode("test", ResolvableType.forClass(String.class), service)).willReturn(block);
		assertThat(service.generateCode("test")).isSameAs(block);
		verifyNoInteractions(generator2);
	}

	@Test
	void generateCodeWhenHasNotInstanceCodeGeneratorThrowsException() {
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(
				InstanceCodeGenerators::none);
		assertThatIllegalArgumentException().isThrownBy(() -> service.generateCode("test"))
				.withMessage("'type' java.lang.String must be supported for instance code generation");
	}

	@Test
	void createWithParent() {
		GeneratedMethods generatedMethods = new GeneratedMethods();
		MethodGenerator methodGenerator = generatedMethods;
		DefaultInstanceCodeGenerationService parent = new DefaultInstanceCodeGenerationService(null, methodGenerator,
				(generators) -> generators.add(new TestInstanceCodeGenerator()));
		DefaultInstanceCodeGenerationService service = new DefaultInstanceCodeGenerationService(parent, null,
				InstanceCodeGenerators::none);
		assertThat(service.supportsMethodGeneration()).isTrue();
		assertThat(service.generateCode(new TestInstance())).isNotNull();
		assertThatIllegalArgumentException().isThrownBy(() -> service.generateCode("test"));
	}

	private void assertCanGenerate(DefaultInstanceCodeGenerationService service, Object value) {
		assertThat(service.generateCode(value)).isNotNull()
				.isNotEqualTo(DefaultInstanceCodeGenerationService.NULL_INSTANCE_CODE_BLOCK);
	}

	private static class TestInstanceCodeGenerator implements InstanceCodeGenerator {

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type, InstanceCodeGenerationService service) {
			return (value instanceof TestInstance) ? CodeBlock.of("test") : null;
		}

	}

	private static class TestInstance {

	}

}
