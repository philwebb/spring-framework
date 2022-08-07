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

import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SerializationHints}.
 *
 * @author Stephane Nicoll
 */
class SerializationHintsTests {

	private final SerializationHints serializationHints = new SerializationHints();

	@Test
	void registerTypeTwiceExposesOneHint() {
		this.serializationHints.registerType(URL.class);
		this.serializationHints.registerType(TypeReference.of(URL.class.getName()));
		assertThat(this.serializationHints.javaSerialization()).singleElement()
				.extracting(JavaSerializationHint::getType).isEqualTo(TypeReference.of(URL.class));
	}

}
