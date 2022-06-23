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

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.javapoet.TypeSpec;
import org.springframework.javapoet.TypeSpec.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link GeneratedClasses}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class GeneratedClassesTests {

	private static final Consumer<TypeSpec.Builder> emptyTypeCustomizer = type -> {};

	private final GeneratedClasses generatedClasses = new GeneratedClasses(
			new ClassNameGenerator(Object.class));

	@Test
	void createWhenClassNameGeneratorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GeneratedClasses(null))
				.withMessage("'classNameGenerator' must not be null");
	}

	@Test
	void forFeatureComponentWhenFeatureNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.generateClass("", TestComponent.class))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void forFeatureWhenFeatureNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.generateClass(""))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void generateWhenTypeSpecCustomizerIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses
						.generateClass("test", TestComponent.class).using(null))
				.withMessage("'builder' must not be null");
	}

	@Test
	void forFeatureUsesDefaultTarget() {
		GeneratedClass generatedClass = this.generatedClasses
				.generateClass("Test").using(emptyTypeCustomizer);
		assertThat(generatedClass.getName()).hasToString("java.lang.Object__Test");
	}

	@Test
	void forFeatureComponentUsesComponent() {
		GeneratedClass generatedClass = this.generatedClasses
				.generateClass("Test", TestComponent.class).using(emptyTypeCustomizer);
		assertThat(generatedClass.getName().toString()).endsWith("TestComponent__Test");
	}

	@Test
	void generateReturnsDifferentInstances() {
		Consumer<Builder> typeCustomizer = mockTypeCustomizer();
		GeneratedClass generatedClass1 = this.generatedClasses
				.generateClass("one", TestComponent.class).using(typeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.generateClass("one", TestComponent.class).using(typeCustomizer);
		assertThat(generatedClass1).isNotSameAs(generatedClass2);
		assertThat(generatedClass1.getName().simpleName()).endsWith("__One");
		assertThat(generatedClass2.getName().simpleName()).endsWith("__One1");
	}

	@Test
	void getOrGenerateWhenNewReturnsGeneratedMethod() {
		Consumer<Builder> typeCustomizer = mockTypeCustomizer();
		GeneratedClass generatedClass1 = this.generatedClasses
				.getOrGenerateClass("one", TestComponent.class).using(typeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.getOrGenerateClass("two", TestComponent.class).using(typeCustomizer);
		assertThat(generatedClass1).isNotNull().isNotEqualTo(generatedClass2);
		assertThat(generatedClass2).isNotNull();
	}

	@Test
	void getOrGenerateWhenRepeatReturnsSameGeneratedMethod() {
		Consumer<Builder> typeCustomizer = mockTypeCustomizer();
		GeneratedClass generatedClass1 = this.generatedClasses
				.getOrGenerateClass("one", TestComponent.class).using(typeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.getOrGenerateClass("one", TestComponent.class).using(typeCustomizer);
		GeneratedClass generatedClass3 = this.generatedClasses
				.getOrGenerateClass("one", TestComponent.class).using(typeCustomizer);
		assertThat(generatedClass1).isNotNull().isSameAs(generatedClass2)
				.isSameAs(generatedClass3);
	}

	@Test
	@SuppressWarnings("unchecked")
	void writeToInvokeTypeSpecCustomizer() throws IOException {
		Consumer<TypeSpec.Builder> typeSpecCustomizer = mock(Consumer.class);
		this.generatedClasses.generateClass("one", TestComponent.class)
				.using(typeSpecCustomizer);
		verifyNoInteractions(typeSpecCustomizer);
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		this.generatedClasses.writeTo(generatedFiles);
		verify(typeSpecCustomizer).accept(any());
		assertThat(generatedFiles.getGeneratedFiles(Kind.SOURCE)).hasSize(1);
	}

	@Test
	void withNameUpdatesNamingConventions() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.generateClass("one", TestComponent.class).using(emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses.withName("Another")
				.generateClass("one", TestComponent.class).using(emptyTypeCustomizer);
		assertThat(generatedClass1.getName().toString()).endsWith("TestComponent__One");
		assertThat(generatedClass2.getName().toString()).endsWith("TestComponent__AnotherOne");
	}


	@SuppressWarnings("unchecked")
	private Consumer<TypeSpec.Builder> mockTypeCustomizer() {
		return mock(Consumer.class);
	}


	private static class TestComponent {

	}

}
