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

import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link GeneratedClassName}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class GeneratedClassNameTests {

	private static final String NAME = "com.example.Test$Thing__Feature";

	@Test
	void getNameReturnsName() {
		GeneratedClassName generated = new GeneratedClassName(NAME, null);
		assertThat(generated.getName()).isEqualTo(NAME);
	}

	@Test
	void getPackageNameReturnsPackageName() {
		GeneratedClassName generated = new GeneratedClassName(NAME, null);
		assertThat(generated.getPackageName()).isEqualTo("com.example");
	}

	@Test
	void classBuilderReturnsClassBuilder() {
		GeneratedClassName generated = new GeneratedClassName(NAME, null);
		TypeSpec.Builder builder = generated.classBuilder();
		assertThat(builder).isNotNull();
		assertThat(builder.build().name).isEqualTo("Test$Thing__Feature");
	}

	@Test
	void javaFileBuilderWhenNameIsNullThrowsException() {
		GeneratedClassName generated = new GeneratedClassName(NAME, null);
		assertThatIllegalArgumentException().isThrownBy(() -> generated.javaFileBuilder(null))
				.withMessage("'typeSpec' must not be null");
	}

	@Test
	void javaFileBuilderWhenNameIsWrongThrowsException() {
		GeneratedClassName generated = new GeneratedClassName(NAME, null);
		TypeSpec typeSpec = TypeSpec.classBuilder("com.example.Bad").build();
		assertThatIllegalArgumentException().isThrownBy(() -> generated.javaFileBuilder(typeSpec))
				.withMessage("'typeSpec' must be named 'Test$Thing__Feature' instead of 'com.example.Bad'");
	}

	@Test
	void javaFileBuilderReturnsJavaFileBuilder() {
		GeneratedClassName generated = new GeneratedClassName(NAME, null);
		TypeSpec typeSpec = generated.classBuilder().build();
		JavaFile.Builder builder = generated.javaFileBuilder(typeSpec);
		assertThat(builder).isNotNull();
		assertThat(builder.build().packageName).isEqualTo("com.example");
		assertThat(builder.build().skipJavaLangImports).isTrue();
	}

	@Test
	void getTargetClassWhenTargetIsClassReturnsTargetClass() {
		GeneratedClassName generated = new GeneratedClassName(NAME, InputStream.class);
		assertThat(generated.getTargetClass()).isEqualTo(InputStream.class);
	}

	@Test
	void getTargetClassWhenTargetIsNotClassReturnsNull() {
		GeneratedClassName generated = new GeneratedClassName(NAME, "java.io.InputStream");
		assertThat(generated.getTargetClass()).isEqualTo(InputStream.class);
	}

	@Test
	void hashCodeAndEquals() {
		GeneratedClassName generated1 = new GeneratedClassName(NAME, null);
		GeneratedClassName generated2 = new GeneratedClassName(new String(NAME), null);
		GeneratedClassName generated3 = new GeneratedClassName("com.example.Other", null);
		assertThat(generated1.hashCode()).isEqualTo(generated2.hashCode());
		assertThat(generated1).isEqualTo(generated1).isEqualTo(generated2).isNotEqualTo(generated3);
	}

	@Test
	void toStringReturnsName() {
		GeneratedClassName generated = new GeneratedClassName(NAME, null);
		assertThat(generated).hasToString(NAME);
	}

}
