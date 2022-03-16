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

import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link SuppliedRootBeanDefinitionBuilder}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
class SuppliedRootBeanDefinitionBuilderTests {

	@Test
	void createWhenClassTypeIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SuppliedRootBeanDefinitionBuilder(null, (Class<?>) null))
				.withMessage("'type' must not be null");
	}

	@Test
	void createWhenResolvableTypeIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SuppliedRootBeanDefinitionBuilder(null, (ResolvableType) null))
				.withMessage("'type' must not be null");
	}

	@Test
	void createWhenResolvableTypeCannotBeResolvedThrowsException() {
		fail();
	}

	@Test
	void usingConstructorWhenNoConstructorFoundThrowsException() {

	}

	@Test
	void usingConstructorWhenFound() {

	}

	@Test
	void usingMethodWhenNoMethodFoundThrowsException() {

	}

	@Test
	void usingWhenMethodOnInterface() {

	}

	@Test
	void usingMethodWhenFound() {

	}



}
