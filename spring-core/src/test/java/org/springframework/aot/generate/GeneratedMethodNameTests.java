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

import org.springframework.javapoet.MethodSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GeneratedMethodName}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class GeneratedMethodNameTests {

	private static final String NAME = "springFramework";

	@Test
	void getNameReturnsName() {
		GeneratedMethodName generated = new GeneratedMethodName(NAME);
		assertThat(generated.getName()).isEqualTo(NAME);
	}

	@Test
	void methodBuildReturnsBuilder() {
		GeneratedMethodName generated = new GeneratedMethodName(NAME);
		MethodSpec.Builder builder = generated.methodBuilder();
		assertThat(builder.build().name).isEqualTo(NAME);
	}

	@Test
	void hashCodeAndEquals() {
		GeneratedMethodName generated1 = new GeneratedMethodName(NAME);
		GeneratedMethodName generated2 = new GeneratedMethodName(new String(NAME));
		GeneratedMethodName generated3 = new GeneratedMethodName("other");
		assertThat(generated1.hashCode()).isEqualTo(generated2.hashCode());
		assertThat(generated1).isEqualTo(generated1).isEqualTo(generated2).isNotEqualTo(
				generated3);
	}

	@Test
	void toStringReturnsName() {
		GeneratedMethodName generated = new GeneratedMethodName(NAME);
		assertThat(generated).hasToString(NAME);
	}

}
