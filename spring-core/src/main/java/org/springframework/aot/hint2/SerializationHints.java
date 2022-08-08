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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.springframework.aot.hint2.ResourceHints.PatternRegistration;

/**
 * Hints for runtime Java serialization needs.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @see JavaSerializationHint
 * @see RuntimeHints
 */
public class SerializationHints {

	private final Map<TypeReference, JavaSerializationHint> javaSerialization = new ConcurrentHashMap<>();

	public JavaSerializationRegistration registerJavaSerialization() {
		return new JavaSerializationRegistration();
	}

	/**
	 * Return the {@link JavaSerializationHint java serialization hints} for
	 * types that need to be serialized using Java serialization at runtime.
	 * @return a stream of {@link JavaSerializationHint java serialization
	 * hints}
	 */
	public Stream<JavaSerializationHint> javaSerialization() {
		return this.javaSerialization.values().stream();
	}

	public class JavaSerializationRegistration extends ReachableTypeRegistration<PatternRegistration> {

		public void forType(Class<?>... types) {
			forType(TypeReference.arrayOf(types));
		}

		public void forType(String... types) {
			forType(TypeReference.arrayOf(types));
		}

		public void forType(TypeReference... types) {
		}

	}

}
