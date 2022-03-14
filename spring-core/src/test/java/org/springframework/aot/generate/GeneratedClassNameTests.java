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

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link GeneratedClassName}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class GeneratedClassNameTests {

	private static final String NAME = "com.example.Test$Thing$$Feature";

	@Test
	void getNameReturnsName() {
		GeneratedClassName generated = new GeneratedClassName(NAME);
		assertThat(generated.getName()).isEqualTo(NAME);
	}

	@Test
	void getPackageNameReturnsPackageName() {
		GeneratedClassName generated = new GeneratedClassName(NAME);
		assertThat(generated.getPackageName()).isEqualTo("com.example");
	}

	@Test
	void classBuilderReturnsClassBuilder() {
		GeneratedClassName generated = new GeneratedClassName(NAME);
		TypeSpec.Builder builder = generated.classBuilder();
		assertThat(builder).isNotNull();
		assertThat(builder.build().name).isEqualTo(NAME);
	}

	@Test
	void javaFileBuilderWhenNameIsNullThrowsException() {
		GeneratedClassName generated = new GeneratedClassName(NAME);
		assertThatIllegalArgumentException().isThrownBy(
				() -> generated.javaFileBuilder(null)).withMessage(
						"'typeSpec' must not be null");
	}

	@Test
	void javaFileBuilderWhenNameIsWrongThrowsException() {
		GeneratedClassName generated = new GeneratedClassName(NAME);
		TypeSpec typeSpec = new GeneratedClassName(
				"com.example.Bad").classBuilder().build();
		assertThatIllegalArgumentException().isThrownBy(
				() -> generated.javaFileBuilder(typeSpec)).withMessage(
						"'typeSpec' must be named 'com.example.Test$Thing$$Feature' instead of 'com.example.Bad'");
	}

	@Test
	void javaFileBuilderReturnsJavaFileBuilder() {
		GeneratedClassName generated = new GeneratedClassName(NAME);
		TypeSpec typeSpec = generated.classBuilder().build();
		JavaFile.Builder builder = generated.javaFileBuilder(typeSpec);
		assertThat(builder).isNotNull();
		assertThat(builder.build().packageName).isEqualTo("com.example");
		assertThat(builder.build().skipJavaLangImports).isTrue();
	}

	@Test
	void hashCodeAndEquals() {
		GeneratedClassName generated1 = new GeneratedClassName(NAME);
		GeneratedClassName generated2 = new GeneratedClassName(new String(NAME));
		GeneratedClassName generated3 = new GeneratedClassName("com.example.Other");
		assertThat(generated1.hashCode()).isEqualTo(generated2.hashCode());
		assertThat(generated1).isEqualTo(generated1).isEqualTo(generated2).isNotEqualTo(
				generated3);
	}

	@Test
	void toStringReturnsName() {
		GeneratedClassName generated = new GeneratedClassName(NAME);
		assertThat(generated).hasToString(NAME);
	}

}
