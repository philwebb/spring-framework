/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation.type;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClassReference}.
 *
 * @author Phillip Webb
 */
public class ClassReferenceTests {

	@Test
	public void ofWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> ClassReference.of((String) null)).withMessage(
						"ClassName must not be empty");
	}

	@Test
	public void fromWhenClassIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> ClassReference.from((Class<?>) null)).withMessage(
						"ClassType must not be null");
	}

	@Test
	public void ofWhenNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> ClassReference.of("")).withMessage("ClassName must not be empty");
	}

	@Test
	public void fromReturnsClassReference() {
		ClassReference reference = ClassReference.from(String.class);
		assertThat(reference.getClassName()).isEqualTo("java.lang.String");
	}

	@Test
	public void ofStringReturnsClassReference() {
		ClassReference reference = ClassReference.of("io.spring.Framework");
		assertThat(reference.getClassName()).isEqualTo("io.spring.Framework");
	}

	@Test
	public void toStringReturnsClassName() {
		ClassReference reference = ClassReference.of("io.spring.Framework");
		assertThat(reference.toString()).isEqualTo("io.spring.Framework");
	}

	@Test
	public void equalsAndHashCodeUsesClassName() {
		ClassReference reference1 = ClassReference.of("io.spring.Framework");
		ClassReference reference2 = ClassReference.of("io.spring.Framework");
		ClassReference reference3 = ClassReference.of("org.spring.Framework");
		assertThat(reference1.hashCode()).isEqualTo(reference2.hashCode());
		assertThat(reference1).isEqualTo(reference1).isEqualTo(reference2).isNotEqualTo(
				reference3);
	}

}
