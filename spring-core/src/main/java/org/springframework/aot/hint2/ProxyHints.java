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
 * Hints for runtime proxy needs.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @see JdkProxyHint
 * @see RuntimeHints
 */
public class ProxyHints {

	private final Map<InterfaceTypes, JdkProxyHint> hints = new ConcurrentHashMap<>();

	public JdkProxyHintRegistration registerJdkProxy() {
		return new JdkProxyHintRegistration();
	}

	Condition update(InterfaceTypes interfaceTypes, UnaryOperator<JdkProxyHint> mapper) {
		this.hints.compute(interfaceTypes, (key, hint) -> mapper.apply(
				(hint != null) ? hint : new JdkProxyHint(interfaceTypes.toArray())));
		return new Condition(reachableType -> update(interfaceTypes,
				hint -> hint.andReachableType(reachableType)));
	}

	public class JdkProxyHintRegistration {

		private final UnaryOperator<Class<?>[]> mapper;

		JdkProxyHintRegistration() {
			this.mapper = UnaryOperator.identity();
		}

		private JdkProxyHintRegistration(UnaryOperator<Class<?>[]> mapper) {
			this.mapper = mapper;
		}

		JdkProxyHintRegistration with(UnaryOperator<Class<?>[]> mapper) {
			return new JdkProxyHintRegistration(
					interfaceTypes -> mapper.apply(this.mapper.apply(interfaceTypes)));
		}

		public Condition forInterfaces(Class<?>... interfaceTypes) {
			return update(new InterfaceTypes(this.mapper.apply(interfaceTypes)),
					UnaryOperator.identity());
		}

	}

	public static class Condition extends RegistrationCondition<Condition> {

		Condition(Consumer<TypeReference> action) {
			super(action);
		}

	}

	private static class InterfaceTypes {

		InterfaceTypes(Class<?>[] apply) {
		}

		Class<?>[] toArray() {
			return null;
		}

	}

}
