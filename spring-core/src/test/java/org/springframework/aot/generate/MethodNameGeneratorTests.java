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

package org.springframework.aot.generate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MethodNameGenerator}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class MethodNameGeneratorTests {

	private MethodNameGenerator generator = new MethodNameGenerator();

	@Test
	void generateMethodNameGeneratesName() {
		GeneratedMethodName generated = this.generator.generatedMethodName("register",
				"myBean", "bean");
		assertThat(generated).isEqualTo("registerMyBeanBean");
	}

	@Test
	void generateMethodNameWhenHasNonLettersGeneratesName() {
		GeneratedMethodName generated = this.generator.generatedMethodName("register",
				"myBean123", "bean");
		assertThat(generated).isEqualTo("registerMyBeanBean");
	}

	@Test
	void generateMethodNameWhenMultipleCallsGeneratesSequencedName() {
		GeneratedMethodName generated1 = this.generator.generatedMethodName("register",
				"myBean123", "bean");
		GeneratedMethodName generated2 = this.generator.generatedMethodName("register",
				"myBean!", "bean");
		GeneratedMethodName generated3 = this.generator.generatedMethodName("register",
				"myBean%%", "bean");
		assertThat(generated1).isEqualTo("registerMyBeanBean");
		assertThat(generated2).isEqualTo("registerMyBeanBean1");
		assertThat(generated3).isEqualTo("registerMyBeanBean2");
	}

	@Test
	void generateMethodNameWhenAllEmptyPartsGeneratesSetName() {
		GeneratedMethodName generated = this.generator.generatedMethodName("123");
		assertThat(generated).isEqualTo("$$aot");
	}

}
