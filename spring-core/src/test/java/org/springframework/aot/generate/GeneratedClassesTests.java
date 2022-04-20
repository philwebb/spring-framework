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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link GeneratedClasses}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class GeneratedClassesTests {

	private GeneratedClasses generatedClasses = new GeneratedClasses(new ClassNameGenerator());

	@Test
	void createWhenClassNameGeneratorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GeneratedClasses(null))
				.withMessage("'classNameGenerator' must not be null");
	}

	@Test
	void getOrGenerateWithClassTargetWhenTargetIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.getOrGenerateClass((Class<?>) null, "test"))
				.withMessage("'target' must not be null");
	}

	@Test
	void getOrGenerateWithClassTargetWhenFeatureIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.getOrGenerateClass((String) null, "test"))
				.withMessage("'target' must not be null");
	}

	@Test
	void getOrGenerateWithStringTargetWhenTargetIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.getOrGenerateClass(TestTarget.class, null))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void getOrGenerateWithStringTargetWhenFeatureIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.getOrGenerateClass(TestTarget.class.getName(), null))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void getOrGenerateWhenNewReturnsGeneratedMethod() {
		GeneratedClass generatedClass1 = this.generatedClasses.getOrGenerateClass(TestTarget.class, "one");
		GeneratedClass generatedClass2 = this.generatedClasses.getOrGenerateClass(TestTarget.class.getName(), "two");
		assertThat(generatedClass1).isNotNull().isNotEqualTo(generatedClass2);
		assertThat(generatedClass2).isNotNull();
	}

	@Test
	void getOrGenerateWhenRepeatReturnsSameGeneratedMethod() {
		GeneratedClass generatedClass1 = this.generatedClasses.getOrGenerateClass(TestTarget.class, "one");
		GeneratedClass generatedClass2 = this.generatedClasses.getOrGenerateClass(TestTarget.class, "one");
		GeneratedClass generatedClass3 = this.generatedClasses.getOrGenerateClass(TestTarget.class.getName(), "one");
		GeneratedClass generatedClass4 = this.generatedClasses.getOrGenerateClass(TestTarget.class, "two");
		assertThat(generatedClass1).isNotNull().isSameAs(generatedClass2).isSameAs(generatedClass3)
				.isNotSameAs(generatedClass4);
	}

	private static class TestTarget {

	}

}
