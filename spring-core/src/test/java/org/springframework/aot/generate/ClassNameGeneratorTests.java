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

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClassNameGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ClassNameGeneratorTests {

	private final ClassNameGenerator generator = new ClassNameGenerator();

	@Test
	void generateClassNameWhenTargetClassIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.generator.generateClassName((Class<?>) null, "Test"))
				.withMessage("'target' must not be null");
	}

	@Test
	void generateClassNameWhenTargetStringIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.generator.generateClassName("", "Test"))
				.withMessage("'target' must not be empty");
	}

	@Test
	void generatedClassNameWhenFeatureIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.generator.generateClassName(InputStream.class, ""))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void generatedClassNameWhenFeatureIsNotAllLettersThrowsException() {
		String expectedMessage = "'featureName' must contain only letters";
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generator.generateClassName(InputStream.class, "noway!"))
				.withMessage(expectedMessage);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generator.generateClassName(InputStream.class, "1WontWork"))
				.withMessage(expectedMessage);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generator.generateClassName(InputStream.class, "N0pe"))
				.withMessage(expectedMessage);
	}

	@Test
	void generateClassNameWithClassGeneratesName() {
		GeneratedClassName generated = this.generator.generateClassName(InputStream.class, "Bytes");
		assertThat(generated).hasToString("java.io.InputStream__Bytes");
		assertThat(generated.getTargetClass()).isEqualTo(InputStream.class);
	}

	@Test
	void generateClassNameWithClassWhenLowercaseFeatureNameGeneratesName() {
		GeneratedClassName generated = this.generator.generateClassName(InputStream.class, "bytes");
		assertThat(generated).hasToString("java.io.InputStream__Bytes");
	}

	@Test
	void generateClassNameWithClassWhenInnerClassGeneratesName() {
		GeneratedClassName generated = this.generator.generateClassName(TestBean.class, "EventListener");
		assertThat(generated)
				.hasToString("org.springframework.aot.generate.ClassNameGeneratorTests$" + "TestBean__EventListener");
	}

	@Test
	void generateClassWithClassWhenMultipleCallsGeneratesSequencedName() {
		GeneratedClassName generated1 = this.generator.generateClassName(InputStream.class, "bytes");
		GeneratedClassName generated2 = this.generator.generateClassName(InputStream.class, "bytes");
		GeneratedClassName generated3 = this.generator.generateClassName(InputStream.class, "bytes");
		assertThat(generated1).hasToString("java.io.InputStream__Bytes");
		assertThat(generated2).hasToString("java.io.InputStream__Bytes1");
		assertThat(generated3).hasToString("java.io.InputStream__Bytes2");
	}

	@Test
	void generateClassNameWithStringGeneratesNameUsingOnlyLetters() {
		GeneratedClassName generated = this.generator.generateClassName("my-bean--factoryStuff", "beans");
		assertThat(generated).hasToString("__.MyBeanFactoryStuff__Beans");
	}

	@Test
	void generateClassNameWithStringWhenNoLettersGeneratesAotName() {
		GeneratedClassName generated = this.generator.generateClassName("1234!@#", "beans");
		assertThat(generated).hasToString("__.Aot__Beans");
	}

	static class TestBean {

	}

}
