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

/**
 * Hints for runtime Java serialization needs.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @see SerializationHint
 * @see RuntimeHints
 */
public class SerializationHints {

	private final Map<TypeReference, SerializationHint> hints = new ConcurrentHashMap<>();

	public SerializationRegistration registerSerialization() {
		return new SerializationRegistration();
	}

	Condition update(TypeReference[] types, UnaryOperator<SerializationHint> mapper) {
		for (TypeReference type : types) {
			this.hints.compute(type, (key, hint) -> mapper
					.apply((hint != null) ? hint : new SerializationHint(type)));
		}
		return new Condition(reachableType -> update(types,
				hint -> hint.andReachableType(reachableType)));
	}

	public class SerializationRegistration {

		Condition forType(Class<?>... types) {
			return forType(TypeReference.arrayOf(types));
		}

		Condition forType(String... types) {
			return forType(TypeReference.arrayOf(types));
		}

		Condition forType(TypeReference... types) {
			return update(types, UnaryOperator.identity());
		}

	}

	public static class Condition extends RegistrationCondition<Condition> {

		Condition(Consumer<TypeReference> action) {
			super(action);
		}

	}

}
