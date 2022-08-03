/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.aot.hint;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.JavaSerializationHint;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.JavaReflectionHint.Category;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RuntimeHints}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class RuntimeHintsTests {

	private final RuntimeHints hints = new RuntimeHints();

	@Test
	void reflectionHintWithClass() {
		this.hints.reflection().registerInvoke().forPublicConstructorsIn(String.class);
		assertThat(this.hints.reflection().javaReflection()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(String.class.getCanonicalName());
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
			assertThat(typeHint.getCategories()).containsOnly(Category.INVOKE_PUBLIC_CONSTRUCTORS);
		});
	}

	@Test
	void resourceHintWithClass() {
		this.hints.resources().registerInclude().forClassBytecode(String.class);
		assertThat(this.hints.resources().includeResourcePatterns()).singleElement()
				.satisfies(resourceHint -> assertThat(resourceHint.getPattern()).isEqualTo("java/lang/String.class"));
	}

	@Test
	void javaSerializationHintWithClass() {
		this.hints.serialization().registerJavaSerialization().forType(String.class);
		assertThat(this.hints.serialization().javaSerialization().map(JavaSerializationHint::getType))
				.containsExactly(TypeReference.of(String.class));
	}

	@Test
	void jdkProxyWithClass() {
		this.hints.proxies().registerJavaProxy().forInterfaces(Function.class);
		assertThat(this.hints.proxies().javaProxies()).singleElement()
				.satisfies(jdkProxyHint -> assertThat(jdkProxyHint.getProxiedInterfaces())
						.containsExactly(TypeReference.of(Function.class)));
	}

}
