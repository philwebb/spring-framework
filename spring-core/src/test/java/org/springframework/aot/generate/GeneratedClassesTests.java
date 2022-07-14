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
	void addForComponentWithTargetWhenFeatureNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.addForComponent(TestComponent.class, "", emptyTypeCustomizer))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void addWhenFeatureNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.add("", emptyTypeCustomizer))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void addForComponentWhenTypeSpecCustomizerIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses
						.addForComponent(TestComponent.class, "test", null))
				.withMessage("'type' must not be null");
	}

	@Test
	void addUsesDefaultTarget() {
		GeneratedClass generatedClass = this.generatedClasses.add("Test", emptyTypeCustomizer);
		assertThat(generatedClass.getName()).hasToString("java.lang.Object__Test");
	}

	@Test
	void addForComponentUsesTarget() {
		GeneratedClass generatedClass = this.generatedClasses
				.addForComponent(TestComponent.class, "Test", emptyTypeCustomizer);
		assertThat(generatedClass.getName().toString()).endsWith("TestComponent__Test");
	}

	@Test
	void addForComponentWithSameNameReturnsDifferentInstances() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.addForComponent(TestComponent.class, "one", emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.addForComponent(TestComponent.class, "one", emptyTypeCustomizer);
		assertThat(generatedClass1).isNotSameAs(generatedClass2);
		assertThat(generatedClass1.getName().simpleName()).endsWith("__One");
		assertThat(generatedClass2.getName().simpleName()).endsWith("__One1");
	}

	@Test
	void getOrAddForComponentWhenNewReturnsGeneratedMethod() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.getOrAddForComponent("one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.getOrAddForComponent("two", TestComponent.class, emptyTypeCustomizer);
		assertThat(generatedClass1).isNotNull().isNotEqualTo(generatedClass2);
		assertThat(generatedClass2).isNotNull();
	}

	@Test
	void getOrAddWhenNewReturnsGeneratedMethod() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.getOrAdd("one", emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.getOrAdd("two", emptyTypeCustomizer);
		assertThat(generatedClass1).isNotNull().isNotEqualTo(generatedClass2);
		assertThat(generatedClass2).isNotNull();
	}

	@Test
	void getOrAddForComponentWhenRepeatReturnsSameGeneratedMethod() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.getOrAddForComponent("one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.getOrAddForComponent("one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass3 = this.generatedClasses
				.getOrAddForComponent("one", TestComponent.class, emptyTypeCustomizer);
		assertThat(generatedClass1).isNotNull().isSameAs(generatedClass2)
				.isSameAs(generatedClass3);
	}

	@Test
	void getOrAddWhenRepeatReturnsSameGeneratedMethod() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.getOrAdd("one", emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.getOrAdd("one", emptyTypeCustomizer);
		GeneratedClass generatedClass3 = this.generatedClasses
				.getOrAdd("one", emptyTypeCustomizer);
		assertThat(generatedClass1).isNotNull().isSameAs(generatedClass2)
				.isSameAs(generatedClass3);
	}

	@Test
	void getOrAddForComponentWhenHasFeatureNamePrefix() {
		GeneratedClasses prefixed = this.generatedClasses.withFeatureNamePrefix("prefix");
		GeneratedClass generatedClass1 = this.generatedClasses.getOrAddForComponent("one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses.getOrAddForComponent("one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass3 = prefixed.getOrAddForComponent("one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass4 = prefixed.getOrAddForComponent("one", TestComponent.class, emptyTypeCustomizer);
		assertThat(generatedClass1).isSameAs(generatedClass2).isNotSameAs(generatedClass3);
		assertThat(generatedClass3).isSameAs(generatedClass4);
	}

	@Test
	@SuppressWarnings("unchecked")
	void writeToInvokeTypeSpecCustomizer() throws IOException {
		Consumer<TypeSpec.Builder> typeSpecCustomizer = mock(Consumer.class);
		this.generatedClasses.addForComponent(TestComponent.class, "one", typeSpecCustomizer);
		verifyNoInteractions(typeSpecCustomizer);
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		this.generatedClasses.writeTo(generatedFiles);
		verify(typeSpecCustomizer).accept(any());
		assertThat(generatedFiles.getGeneratedFiles(Kind.SOURCE)).hasSize(1);
	}

	@Test
	void withNameUpdatesNamingConventions() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.addForComponent(TestComponent.class, "one", emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses.withFeatureNamePrefix("Another")
				.addForComponent(TestComponent.class, "one", emptyTypeCustomizer);
		assertThat(generatedClass1.getName().toString()).endsWith("TestComponent__One");
		assertThat(generatedClass2.getName().toString()).endsWith("TestComponent__AnotherOne");
	}


	private static class TestComponent {

	}

}
