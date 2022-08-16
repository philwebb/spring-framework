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

package org.springframework.aot.hint2;

import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JavaProxyHint}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class JavaProxyHintTests {

	@Test
	void equalsWithSameInstanceIsTrue() {
		JavaProxyHint hint = new JavaProxyHint(TypeReference.arrayOf(Function.class, Consumer.class), null);
		assertThat(hint).isEqualTo(hint);
	}

	@Test
	void equalsWithSameProxiedInterfacesIsTrue() {
		JavaProxyHint first = new JavaProxyHint(TypeReference.arrayOf(Function.class, Consumer.class), null);
		JavaProxyHint second = new JavaProxyHint(
				TypeReference.arrayOf(Function.class.getName(), Consumer.class.getName()), null);
		assertThat(first).isEqualTo(second);
	}

	@Test
	void equalsWithSameProxiedInterfacesAndDifferentConditionIsFalse() {
		JavaProxyHint first = new JavaProxyHint(TypeReference.arrayOf(Function.class, Consumer.class),
				TypeReference.of(String.class));
		JavaProxyHint second = new JavaProxyHint(
				TypeReference.arrayOf(Function.class.getName(), Consumer.class.getName()),
				TypeReference.of(Function.class));
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalsWithSameProxiedInterfacesDifferentOrderIsFalse() {
		JavaProxyHint first = new JavaProxyHint(TypeReference.arrayOf(Function.class, Consumer.class), null);
		JavaProxyHint second = new JavaProxyHint(TypeReference.arrayOf(Consumer.class, Function.class), null);
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalsWithDifferentProxiedInterfacesIsFalse() {
		JavaProxyHint first = new JavaProxyHint(TypeReference.arrayOf(Function.class), null);
		JavaProxyHint second = new JavaProxyHint(TypeReference.arrayOf(Function.class, Consumer.class), null);
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void equalsWithNonJdkProxyHintIsFalse() {
		JavaProxyHint first = new JavaProxyHint(TypeReference.arrayOf(Function.class), null);
		TypeReference second = TypeReference.of(Function.class);
		assertThat(first).isNotEqualTo(second);
	}

}
