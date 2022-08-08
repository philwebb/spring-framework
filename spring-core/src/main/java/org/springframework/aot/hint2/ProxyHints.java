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
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

	private final Set<JdkProxyHint> hints = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
		return this.hints.stream();
	}

	/**
	 * Registration methods for JDK proxy hints.
	 */
	public class JdkProxyHintRegistration extends ReachableTypeRegistration<JdkProxyHintRegistration> {

		private UnaryOperator<Class<?>[]> classesMapper;

		JdkProxyHintRegistration() {
			this.classesMapper = UnaryOperator.identity();
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
			this.classesMapper = mapper;
			return self();
		}

		/**
		 * Complete the JDK proxy hint registration for the given interface
		 * types (applying any {@link #withClassMapper(UnaryOperator) class
		 * mappers}).
		 * @param interfaceTypes the interface types to register
		 */
		public void forInterfaces(Class<?>... interfaceTypes) {
			Assert.notNull(interfaceTypes, "'interfaceTypes' must not be null");
			forInterfaces(TypeReference.arrayOf(mapAndVeryifyInterfaceTypes(interfaceTypes)));
		}

		/**
		 * Complete the JDK proxy hint registration for the given interface
		 * types.
		 * @param interfaceTypes the names of interface types to register
		 */
		public void forInterfaces(String... interfaceTypes) {
			Assert.notNull(interfaceTypes, "'interfaceTypes' must not be null");
			forInterfaces(TypeReference.arrayOf(interfaceTypes));
		}

		/**
		 * Complete the JDK proxy hint registration for the given interface
		 * types.
		 * @param interfaceTypes the interface types to register
		 */
		public void forInterfaces(TypeReference... interfaceTypes) {
			Assert.notNull(interfaceTypes, "'interfaceTypes' must not be null");
			ProxyHints.this.hints.add(new JdkProxyHint(interfaceTypes, getReachableType()));
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

}
