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

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.MethodSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GeneratedMethod}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class GeneratedMethodTests {

	private static final GeneratedMethodName NAME = new GeneratedMethodName("spring");

	@Test
	void getNameReturnsName() {
		GeneratedMethod method = new GeneratedMethod(NAME);
		assertThat(method.getName()).isSameAs(NAME);
	}

	@Test
	void getSpecReturnsSpec() {
		GeneratedMethod method = new GeneratedMethod(NAME);
		MethodSpec spec = MethodSpec.methodBuilder("spring").build();
		method.generateBy(spec);
		assertThat(method.getSpec()).isSameAs(spec);
	}

	@Test
	void getSpecReturnsSpecWhenNoSpecDefinedThrowsException() {
		GeneratedMethod method = new GeneratedMethod(NAME);
		assertThatIllegalStateException().isThrownBy(() -> method.getSpec())
				.withMessage("Method 'spring' has no method spec defined");
	}

	@Test
	void generateByConsumerAddsSpec() {
		GeneratedMethod method = new GeneratedMethod(NAME);
		method.generateBy((builder) -> builder.addModifiers(Modifier.PUBLIC));
		assertThat(method.getSpec().toString()).isEqualToIgnoringNewLines("public void spring() {}");
	}

	@Test
	void generateByBuilderAddsSpec() {
		GeneratedMethod method = new GeneratedMethod(NAME);
		MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getName().toString())
				.addModifiers(Modifier.PUBLIC);
		method.generateBy(builder);
		assertThat(method.getSpec().toString()).isEqualToIgnoringNewLines("public void spring() {}");
	}

	@Test
	void generateBySpecAddsSpec() {
		GeneratedMethod method = new GeneratedMethod(NAME);
		MethodSpec spec = MethodSpec.methodBuilder(method.getName().toString()).addModifiers(Modifier.PUBLIC).build();
		method.generateBy(spec);
		assertThat(method.getSpec()).isSameAs(spec);
	}

	@Test
	void generateBySpecWhenWrongNameThrowsException() {
		GeneratedMethod method = new GeneratedMethod(NAME);
		MethodSpec spec = MethodSpec.methodBuilder("badname").addModifiers(Modifier.PUBLIC).build();
		assertThatIllegalArgumentException().isThrownBy(() -> method.generateBy(spec))
				.withMessage("'spec' must use the generated name \"spring\"");
	}

}
