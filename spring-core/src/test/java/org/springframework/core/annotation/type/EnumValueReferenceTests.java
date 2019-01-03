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
 * Tests for {@link EnumValueReference}.
 *
 * @author Phillip Webb
 */
public class EnumValueReferenceTests {

	@Test
	public void fromEnumWhenEnumIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> EnumValueReference.from(null)).withMessage(
						"EnumValue must not be null");
	}

	@Test
	public void ofWhenEnumTypeIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> EnumValueReference.of((String) null, "ONE")).withMessage(
						"EnumType must not be empty");
	}

	@Test
	public void ofWhenValueIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> EnumValueReference.of("io.spring.Number", null)).withMessage(
						"Value must not be empty");
	}

	@Test
	public void ofWhenValueIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> EnumValueReference.of("io.spring.Number", "")).withMessage(
						"Value must not be empty");
	}

	@Test
	public void ofReturnsEnumValue() {
		EnumValueReference reference = EnumValueReference.of("io.spring.Number", "ONE");
		assertThat(reference.getEnumType()).isEqualTo("io.spring.Number");
		assertThat(reference.getValue()).isEqualTo("ONE");
	}

	@Test
	public void fromReturnsEnumValue() {
		EnumValueReference reference = EnumValueReference.from(TestEnum.TWO);
		assertThat(reference.getEnumType()).isEqualTo(TestEnum.class.getName());
		assertThat(reference.getValue()).isEqualTo("TWO");
	}

	@Test
	public void toStringReturnsEnumValue() {
		EnumValueReference reference = EnumValueReference.of("io.spring.Number", "ONE");
		assertThat(reference.toString()).isEqualTo("ONE");
	}

	@Test
	public void equalsAndHashCodeUsesContainedData() {
		EnumValueReference reference1 = EnumValueReference.of("io.spring.Number", "ONE");
		EnumValueReference reference2 = EnumValueReference.of("io.spring.Number", "ONE");
		EnumValueReference reference3 = EnumValueReference.of("io.spring.Long", "ONE");
		EnumValueReference reference4 = EnumValueReference.of("io.spring.Number", "TWO");
		assertThat(reference1.hashCode()).isEqualTo(reference2.hashCode());
		assertThat(reference1).isEqualTo(reference1).isEqualTo(reference2).isNotEqualTo(
				reference3).isNotEqualTo(reference4);
	}

	private static enum TestEnum {

		ONE, TWO, THREE

	}

}
