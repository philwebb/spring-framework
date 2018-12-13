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

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SimpleDeclaredAnnotation}.
 *
 * @author Phillip Webb
 */
public class SimpleDeclaredAnnotationTests {

	private DeclaredAttributes attributes;

	private SimpleDeclaredAnnotation annotation;

	@Before
	public void setup() {
		this.attributes = new SimpleDeclaredAttributes(
				Collections.singletonMap("value", "test"));
		this.annotation = new SimpleDeclaredAnnotation("Type", this.attributes);
	}

	@Test
	public void createWhenTypeIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SimpleDeclaredAnnotation(null,
						DeclaredAttributes.NONE)).withMessage("Type must not be empty");
	}

	@Test
	public void createWhenTypeIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SimpleDeclaredAnnotation("",
						DeclaredAttributes.NONE)).withMessage("Type must not be empty");
	}

	@Test
	public void createWhenAttributesIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SimpleDeclaredAnnotation("Type", null)).withMessage(
						"Attributes must not be null");
	}

	@Test
	public void getTypeReturnsType() {
		assertThat(this.annotation.getType()).isEqualTo("Type");
	}

	@Test
	public void getAttributesReturnsAttributes() {
		assertThat(this.annotation.getAttributes()).isSameAs(this.attributes);
	}

	@Test
	public void toStringReturnsString() {
		assertThat(this.annotation.toString()).isEqualTo("@Type(value=\"test\")");
	}

}
