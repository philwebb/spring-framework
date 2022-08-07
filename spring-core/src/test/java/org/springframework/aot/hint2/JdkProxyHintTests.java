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

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Supplier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JdkProxyHint}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class JdkProxyHintTests {

	@Test
	void andReachableTypeReturnsNewInstance() {
		JdkProxyHint without = new JdkProxyHint(TypeReference.arrayOf(Function.class));
		JdkProxyHint with = without.andReachableType(TypeReference.of(Consumer.class));
		assertThat(without).isNotSameAs(with);
		assertThat(without.getReachableType()).isNull();
		assertThat(with.getReachableType()).isEqualTo(TypeReference.of(Consumer.class));
	}

	@Test
	void andReachableTypeWhenAlreadySetWithDifferentTypeThrowsException() {
		JdkProxyHint hint = new JdkProxyHint(TypeReference.arrayOf(Function.class))
				.andReachableType(TypeReference.of(Consumer.class));
		assertThatIllegalStateException().isThrownBy(() -> hint.andReachableType(TypeReference.of(Supplier.class)))
				.withMessage("A reachableType condition has already been applied");
	}

	@Test
	void andReachableTypeWhenAlreadySetWithSameTypeReturnsSameInstance() {
		JdkProxyHint hint = new JdkProxyHint(TypeReference.arrayOf(Function.class))
				.andReachableType(TypeReference.of(Consumer.class));
		assertThat(hint.andReachableType(TypeReference.of(Consumer.class))).isSameAs(hint);
	}

}
