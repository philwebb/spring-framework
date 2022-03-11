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

package org.springframework.beans.factory.aot;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.MethodNameGenerator;
import org.springframework.beans.factory.aot.BeanRegistrationMethods.BeanRegistrationMethod;
import org.springframework.javapoet.MethodSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanRegistrationMethods}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class BeanRegistrationMethodsTests {

	private List<MethodSpec> specs = new ArrayList<>();

	private BeanRegistrationMethods registrationMethods = new BeanRegistrationMethods(new MethodNameGenerator(),
			this.specs::add);

	@Test
	void getNameReturnsGeneratedName() {
		BeanRegistrationMethod generated = this.registrationMethods.withName("spring", "framework");
		assertThat(generated.getName()).hasToString("springFramework");
	}

	@Test
	void addWithConsumedBuilderAddsSpec() {
		this.registrationMethods.withName("spring").add((builder) -> builder.addModifiers(Modifier.PUBLIC));
		assertThat(this.specs).hasSize(1);
		assertThat(this.specs.get(0).toString()).isEqualToIgnoringNewLines("public void spring() {}");
	}

	@Test
	void addWithBuilderAddsSpec() {
		BeanRegistrationMethod method = this.registrationMethods.withName("spring");
		MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getName().toString())
				.addModifiers(Modifier.PUBLIC);
		method.add(builder);
		assertThat(this.specs).hasSize(1);
		assertThat(this.specs.get(0).toString()).isEqualToIgnoringNewLines("public void spring() {}");
	}

	@Test
	void addWithSpecAddsSpec() {
		BeanRegistrationMethod method = this.registrationMethods.withName("spring");
		MethodSpec spec = MethodSpec.methodBuilder(method.getName().toString()).addModifiers(Modifier.PUBLIC).build();
		method.add(spec);
		assertThat(this.specs).hasSize(1);
		assertThat(this.specs.get(0).toString()).isEqualToIgnoringNewLines("public void spring() {}");
	}

	@Test
	void generateMethodWithIncorrectNameThrowsException() {

	}

}
