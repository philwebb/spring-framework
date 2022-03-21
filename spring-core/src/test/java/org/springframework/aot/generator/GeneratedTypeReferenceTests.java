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

package org.springframework.aot.generator;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.TypeReference;
import org.springframework.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GeneratedTypeReference}.
 *
 * @author Stephane Nicoll
 */
class GeneratedTypeReferenceTests {

	@Test
	void createWithClassName() {
		GeneratedTypeReference typeReference = GeneratedTypeReference.of(
				ClassName.get("com.example", "Test"));
		assertThat(typeReference.getPackageName()).isEqualTo("com.example");
		assertThat(typeReference.getSimpleName()).isEqualTo("Test");
		assertThat(typeReference.getCanonicalName()).isEqualTo("com.example.Test");
		assertThat(typeReference.getEnclosingType()).isNull();
	}

	@Test
	void createWithClassNameAndParent() {
		GeneratedTypeReference typeReference = GeneratedTypeReference.of(
				ClassName.get("com.example", "Test").nestedClass("Nested"));
		assertThat(typeReference.getPackageName()).isEqualTo("com.example");
		assertThat(typeReference.getSimpleName()).isEqualTo("Nested");
		assertThat(typeReference.getCanonicalName()).isEqualTo("com.example.Test.Nested");
		assertThat(typeReference.getEnclosingType()).satisfies(parentTypeReference -> {
			assertThat(parentTypeReference.getPackageName()).isEqualTo("com.example");
			assertThat(parentTypeReference.getSimpleName()).isEqualTo("Test");
			assertThat(parentTypeReference.getCanonicalName()).isEqualTo("com.example.Test");
			assertThat(parentTypeReference.getEnclosingType()).isNull();
		});
	}

	@Test
	void equalsWithIdenticalCanonicalNameIsTrue() {
		assertThat(GeneratedTypeReference.of(ClassName.get("java.lang", "String")))
				.isEqualTo(TypeReference.of(String.class));
	}

}
