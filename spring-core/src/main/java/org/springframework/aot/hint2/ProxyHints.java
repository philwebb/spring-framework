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

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.springframework.util.Assert;

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

	/**
	 * Registration methods for JDK proxy hints.
	 * @return JDK proxy hint registration methods
	 */
	public JdkProxyHintRegistration registerJdkProxy() {
		return new JdkProxyHintRegistration();
	}

	/**
	 * Return an unordered {@link Stream} if {@link JdkProxyHint JdkProxyHints}
	 * that have been registered.
	 * @return the registered JDK proxy hints
	 */
	public Stream<JdkProxyHint> jdkProxies() {
		return this.hints.values().stream();
	}

	Condition update(InterfaceTypes interfaceTypes, UnaryOperator<JdkProxyHint> mapper) {
		this.hints.compute(interfaceTypes,
				(key, hint) -> mapper.apply((hint != null) ? hint : new JdkProxyHint(interfaceTypes.toArray())));
		return new Condition(reachableType -> update(interfaceTypes, hint -> hint.andReachableType(reachableType)));
	}

	/**
	 * Registration methods for JDK proxy hints.
	 */
	public class JdkProxyHintRegistration {

		private final UnaryOperator<Class<?>[]> classesMapper;

		JdkProxyHintRegistration() {
			this.classesMapper = UnaryOperator.identity();
		}

		private JdkProxyHintRegistration(UnaryOperator<Class<?>[]> classesMapper) {
			this.classesMapper = classesMapper;
		}

		/**
		 * Return a new {@link JdkProxyHintRegistration} that applies the given
		 * {@link UnaryOperator} to the interface classes before they are
		 * registered. This method is often used to add standard AOP classes.
		 * For example:<pre class=
		 * code>hints.registerJdkProxy().withClassMapper(AopProxyUtils::completeJdkProxyInterfaces)
		 * 	.forInterface(Example.class);</pre>
		 * @param mapper the class mapper
		 * @return a new {@link JdkProxyHintRegistration} instance
		 */
		JdkProxyHintRegistration withClassMapper(UnaryOperator<Class<?>[]> mapper) {
			Assert.notNull(mapper, "'mapper' must not be null");
			return new JdkProxyHintRegistration(
					interfaceTypes -> mapper.apply(this.classesMapper.apply(interfaceTypes)));
		}

		/**
		 * Complete the JDK proxy hint registration for the given interface
		 * types (applying any {@link #withClassMapper(UnaryOperator) class
		 * mappers}).
		 * @param interfaceTypes the interface types to register
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 */
		public Condition forInterfaces(Class<?>... interfaceTypes) {
			Assert.notNull(interfaceTypes, "'interfaceTypes' must not be null");
			return forInterfaces(TypeReference.arrayOf(mapAndVeryifyInterfaceTypes(interfaceTypes)));
		}

		/**
		 * Complete the JDK proxy hint registration for the given interface
		 * types.
		 * @param interfaceTypes the names of interface types to register
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 */
		public Condition forInterfaces(String... interfaceTypes) {
			Assert.notNull(interfaceTypes, "'interfaceTypes' must not be null");
			return forInterfaces(TypeReference.arrayOf(interfaceTypes));
		}

		/**
		 * Complete the JDK proxy hint registration for the given interface
		 * types.
		 * @param interfaceTypes the interface types to register
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 */
		public Condition forInterfaces(TypeReference... interfaceTypes) {
			Assert.notNull(interfaceTypes, "'interfaceTypes' must not be null");
			return update(new InterfaceTypes(interfaceTypes), UnaryOperator.identity());
		}

		private Class<?>[] mapAndVeryifyInterfaceTypes(Class<?>... interfaceTypes) {
			Class<?>[] mapped = this.classesMapper.apply(interfaceTypes);
			Assert.isTrue(Arrays.stream(mapped).noneMatch(this::isNotInterface),
					() -> "The following must be interfaces: "
							+ Arrays.stream(mapped).filter(this::isNotInterface).map(Class::getName).toList());
			Assert.isTrue(Arrays.stream(mapped).noneMatch(Class::isSealed),
					() -> "The following must be non-sealed interfaces: "
							+ Arrays.stream(mapped).filter(Class::isSealed).map(Class::getName).toList());
			return mapped;
		}

		private boolean isNotInterface(Class<?> candidate) {
			return !candidate.isInterface();
		}

	}

	/**
	 * {@link RegistrationCondition} for proxy hints.
	 */
	public static class Condition extends RegistrationCondition<Condition> {

		Condition(Consumer<TypeReference> action) {
			super(action);
		}

	}

	/**
	 * Interface types used as a hint key.
	 */
	private static class InterfaceTypes {

		private final TypeReference[] interfaceTypes;

		InterfaceTypes(TypeReference[] interfaceTypes) {
			this.interfaceTypes = interfaceTypes;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.interfaceTypes);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			return Arrays.equals(this.interfaceTypes, ((InterfaceTypes) obj).interfaceTypes);
		}

		TypeReference[] toArray() {
			return this.interfaceTypes;
		}

	}

}
