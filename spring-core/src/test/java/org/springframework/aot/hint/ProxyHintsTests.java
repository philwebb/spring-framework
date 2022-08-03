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

package org.springframework.aot.hint;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.JavaProxyHint;
import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ProxyHints}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Phillip Webb
 */
class ProxyHintsTests {

	private final ProxyHints hints = new ProxyHints();

	@Test
	void registerJavaProxyWhenSealedInterfaceThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.hints.registerJavaProxy().forInterfaces(SealedInterface.class))
				.withMessageContaining(SealedInterface.class.getName());
	}

	@Test
	void registerJavaProxyWhenConcreteClassThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.hints.registerJavaProxy().forInterfaces(String.class))
				.withMessageContaining(String.class.getName());
	}

	@Test
	void registerJavaProxyWithClass() {
		this.hints.registerJavaProxy().forInterfaces(Function.class);
		assertThat(this.hints.javaProxies()).singleElement().satisfies(proxiedInterfaces(Function.class));
	}

	@Test
	void registerJavaProxyWithString() {
		this.hints.registerJavaProxy().forInterfaces(Function.class.getName());
		assertThat(this.hints.javaProxies()).singleElement().satisfies(proxiedInterfaces(Function.class));
	}

	@Test
	void registerJavaProxyWithTypeReferences() {
		this.hints.registerJavaProxy().forInterfaces(TypeReference.of(Function.class),
				TypeReference.of("com.example.Advised"));
		assertThat(this.hints.javaProxies()).singleElement()
				.satisfies(proxiedInterfaces(Function.class.getName(), "com.example.Advised"));
	}

	@Test
	void registerJavaProxyWithReachableTypeCondition() {
		this.hints.registerJavaProxy().whenReachable(Stream.class).forInterfaces(Function.class);
		assertThat(this.hints.javaProxies()).singleElement()
				.satisfies((hint) -> assertThat(hint.getReachableType()).hasToString(Stream.class.getCanonicalName()));
	}

	@Test
	void registerJavaProxyWhenSameTypeRegisteredTwiceExposesOneHint() {
		this.hints.registerJavaProxy().forInterfaces(Function.class);
		this.hints.registerJavaProxy().forInterfaces(TypeReference.of(Function.class.getName()));
		assertThat(this.hints.javaProxies()).singleElement().satisfies(proxiedInterfaces(Function.class));
	}

	@Test
	void registerJavaProxyWithClassMapperAppliesMapping() {
		this.hints.registerJavaProxy()
				.withClassMapper(existing -> ObjectUtils.addObjectToArray(existing, BiFunction.class))
				.forInterfaces(Function.class);
		assertThat(this.hints.javaProxies()).singleElement()
				.satisfies(proxiedInterfaces(Function.class, BiFunction.class));
	}

	@Test
	void registerJavaProxyWhenSameTypeWithDifferentReachableRegistersBoth() {
		this.hints.registerJavaProxy().forInterfaces(Function.class);
		this.hints.registerJavaProxy().whenReachable(String.class).forInterfaces(Function.class);
		this.hints.registerJavaProxy().whenReachable(Integer.class).forInterfaces(Function.class);
		assertThat(this.hints.javaProxies()).hasSize(3);
	}

	private static Consumer<JavaProxyHint> proxiedInterfaces(String... proxiedInterfaces) {
		return jdkProxyHint -> assertThat(jdkProxyHint.getProxiedInterfaces())
				.containsExactly(TypeReference.arrayOf(proxiedInterfaces));
	}

	private static Consumer<JavaProxyHint> proxiedInterfaces(Class<?>... proxiedInterfaces) {
		return jdkProxyHint -> assertThat(jdkProxyHint.getProxiedInterfaces())
				.containsExactly(TypeReference.arrayOf(proxiedInterfaces));
	}

	sealed interface SealedInterface {
	}

	static final class SealedClass implements SealedInterface {
	}

}
