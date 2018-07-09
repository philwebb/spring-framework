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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.annotation.type.ClassReference;
import org.springframework.core.annotation.type.EnumValueReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnumValueReference}.
 *
 * @author Phillip Webb
 */
public class EnumValueReferenceTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void ofWhenEnumClassNameIsNullThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ClassName must not be empty");
		EnumValueReference.of((String) null, "ONE");
	}

	@Test
	public void ofWhenEnumClassIsNullThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("EnumClass must not be null");
		EnumValueReference.of((ClassReference) null, "ONE");
	}

	@Test
	public void ofWhenValueIsNullThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Value must not be empty");
		EnumValueReference.of("io.spring.Number", null);
	}

	@Test
	public void ofWhenValueIsEmptyShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Value must not be empty");
		EnumValueReference.of("io.spring.Number", "");
	}

	@Test
	public void ofReturnsEnumValueReference() {
		EnumValueReference reference = EnumValueReference.of("io.spring.Number", "ONE");
		assertThat(reference.getEnumType()).isEqualTo(
				ClassReference.of("io.spring.Number"));
		assertThat(reference.getValue()).isEqualTo("ONE");
	}

	@Test
	public void toStringReturnsEnumValue() {
		EnumValueReference reference = EnumValueReference.of("io.spring.Number", "ONE");
		assertThat(reference.toString()).isEqualTo("io.spring.Number.ONE");
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

}
