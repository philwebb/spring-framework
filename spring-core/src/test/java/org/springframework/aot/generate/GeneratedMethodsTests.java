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

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.MethodNameGenerator;
import org.springframework.javapoet.MethodSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GeneratedMethods}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class GeneratedMethodsTests {

	private List<MethodSpec> specs = new ArrayList<>();

	private GeneratedMethods registrationMethods = new GeneratedMethods(new MethodNameGenerator());

	@Test
	void getNameReturnsGeneratedName() {
		GeneratedMethod generated = this.registrationMethods.add("spring", "framework");
		assertThat(generated.getName()).hasToString("springFramework");
	}

	@Test
	void addGeneratedByConsumerAddsSpec() {
		this.registrationMethods.add("spring").generateBy((builder) -> builder.addModifiers(Modifier.PUBLIC));
		this.registrationMethods.doWithMethodSpecs(this.specs::add);
		assertThat(this.specs).hasSize(1);
		assertThat(this.specs.get(0).toString()).isEqualToIgnoringNewLines("public void spring() {}");
	}

	@Test
	void addGeneratedByBuilderAddsSpec() {
		GeneratedMethod method = this.registrationMethods.add("spring");
		MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getName().toString())
				.addModifiers(Modifier.PUBLIC);
		method.generateBy(builder);
		this.registrationMethods.doWithMethodSpecs(this.specs::add);
		assertThat(this.specs).hasSize(1);
		assertThat(this.specs.get(0).toString()).isEqualToIgnoringNewLines("public void spring() {}");
	}

	@Test
	void addGeneratedBySpecAddsSpec() {
		GeneratedMethod method = this.registrationMethods.add("spring");
		MethodSpec spec = MethodSpec.methodBuilder(method.getName().toString()).addModifiers(Modifier.PUBLIC).build();
		method.generateBy(spec);
		this.registrationMethods.doWithMethodSpecs(this.specs::add);
		assertThat(this.specs).hasSize(1);
		assertThat(this.specs.get(0).toString()).isEqualToIgnoringNewLines("public void spring() {}");
	}

	@Test
	void addGeneratedBySpecWithIncorrectNameThrowsException() {
		GeneratedMethod method = this.registrationMethods.add("spring");
		MethodSpec spec = MethodSpec.methodBuilder("badname").addModifiers(Modifier.PUBLIC).build();
		assertThatIllegalArgumentException().isThrownBy(() -> method.generateBy(spec))
				.withMessage("'spec' must use the generated name \"spring\"");
	}

	@Test
	void doWithMethodSpecsWhenMethodHasNotHadSpecDefinedThrowsException() {
		this.registrationMethods.add("spring");
		assertThatIllegalStateException().isThrownBy(() -> this.registrationMethods.doWithMethodSpecs(this.specs::add))
				.withMessage("Method 'spring' has no method spec defined");
	}

}
