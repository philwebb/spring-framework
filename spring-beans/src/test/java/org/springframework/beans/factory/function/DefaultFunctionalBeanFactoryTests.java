/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DefaultFunctionalBeanFactory}.
 *
 * @author Phillip Webb
 */
class DefaultFunctionalBeanFactoryTests {

	private final DefaultFunctionalBeanFactory beanFactory = new DefaultFunctionalBeanFactory();

	@Test
	void registerWhenNameAlreadyInUseThrowsException() {
		this.beanFactory.register((definition) -> {
			definition.setName("foo");
			definition.setType(TestBean.class);
		});
		assertThatExceptionOfType(
				FunctionalBeanDefinitionOverrideException.class).isThrownBy(
						() -> this.beanFactory.register((definition) -> {
							definition.setName("foo");
							definition.setType(AnotherTestBean.class);
						}));
	}

	static class TestBean {
	}

	static class AnotherTestBean {
	}

}
