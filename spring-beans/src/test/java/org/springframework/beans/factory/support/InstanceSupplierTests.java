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

package org.springframework.beans.factory.support;

import org.junit.jupiter.api.Test;

import org.springframework.util.function.ThrowableBiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link InstanceSupplier}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class InstanceSupplierTests {

	private final RegisteredBean registeredBean = RegisteredBean.of(new DefaultListableBeanFactory(), "test");

	@Test
	void getWithoutRegisteredBeanThrowsException() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThatIllegalStateException().isThrownBy(() -> supplier.get())
				.withMessage("No RegisteredBean parameter provided");
	}

	@Test
	void getWithExceptionWithoutRegisteredBeanThrowsException() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThatIllegalStateException().isThrownBy(() -> supplier.getWithException())
				.withMessage("No RegisteredBean parameter provided");
	}

	@Test
	void getReturnsResult() throws Exception {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThat(supplier.get(this.registeredBean)).isEqualTo("test");
	}

	@Test
	void andThenWithBiFunctionWhenFunctionIsNullThrowsException() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		ThrowableBiFunction<RegisteredBean, String, String> after = null;
		assertThatIllegalArgumentException().isThrownBy(() -> supplier.andThen(after))
				.withMessage("After must not be null");
	}

	@Test
	void andThenWithBiFunctionAppliesFunctionToObtainResult() throws Exception {
		InstanceSupplier<String> supplier = registeredBean -> "bean";
		supplier = supplier.andThen((registeredBean, string) -> registeredBean.getBeanName() + "-" + string);
		assertThat(supplier.get(this.registeredBean)).isEqualTo("test-bean");
	}

	@Test
	void ofSupplierWhenInstanceSupplierReturnsSameInstance() {
		InstanceSupplier<String> supplier = registeredBean -> "test";
		assertThat(InstanceSupplier.of(supplier)).isSameAs(supplier);
	}

	@Test
	void usingSupplierAdaptsToInstanceSupplier() throws Exception {
		InstanceSupplier<String> instanceSupplier = InstanceSupplier.using(() -> "test");
		assertThat(instanceSupplier.get(this.registeredBean)).isEqualTo("test");
	}

	@Test
	void ofInstanceSupplierAdaptsToInstanceSupplier() throws Exception {
		InstanceSupplier<String> instanceSupplier = InstanceSupplier.of(registeredBean -> "test");
		assertThat(instanceSupplier.get(this.registeredBean)).isEqualTo("test");
	}

}