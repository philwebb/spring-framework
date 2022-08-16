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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JavaSerializationHint}.
 *
 * @author Phillip Webb
 */
class JavaSerializationHintTests {

	@Test
	void getTypeReturnsType() {
		TypeReference type = TypeReference.of(String.class);
		JavaSerializationHint hint = new JavaSerializationHint(type, null);
		assertThat(hint.getType()).isEqualTo(type);
	}

	@Test
	void getReachableTypeReturnsReachableType() {
		TypeReference reachableType = TypeReference.of(Integer.class);
		JavaSerializationHint hint = new JavaSerializationHint(TypeReference.of(String.class), reachableType);
		assertThat(hint.getReachableType()).isEqualTo(reachableType);
	}

	@Test
	void equalsAndHashCode() {
		JavaSerializationHint h1 = new JavaSerializationHint(TypeReference.of(String.class), null);
		JavaSerializationHint h2 = new JavaSerializationHint(TypeReference.of(String.class.getName()), null);
		JavaSerializationHint h3 = new JavaSerializationHint(TypeReference.of(String.class),
				TypeReference.of(Integer.class));
		JavaSerializationHint h4 = new JavaSerializationHint(TypeReference.of(String.class.getName()),
				TypeReference.of(Integer.class.getName()));
		assertThat(h1.hashCode()).isEqualTo(h2.hashCode());
		assertThat(h1).isEqualTo(h1).isEqualTo(h2).isNotEqualTo(h3);
		assertThat(h3).isEqualTo(h4).isNotEqualTo(h1);
	}

}
