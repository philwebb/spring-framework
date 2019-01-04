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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SimpleAttributeType}.
 *
 * @author Phillip Webb
 */
public class SimpleAttributeTypeTests {

	private SimpleDeclaredAnnotations declaredAnnotations;

	private SimpleAttributeType type;

	@Before
	public void setup() {
		this.declaredAnnotations = new SimpleDeclaredAnnotations(new Object(),
				Arrays.asList(new SimpleDeclaredAnnotation("Declared",
						SimpleDeclaredAttributes.of("value", "test"))));
		this.type = new SimpleAttributeType("attributeName", "className",
				this.declaredAnnotations, "defaultValue");
	}

	@Test
	public void createWhenAttributeNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SimpleAttributeType(null, "className", DeclaredAnnotations.NONE,
						null)).withMessage("AttributeName must not be null");
	}

	@Test
	public void createWhenAttributeNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SimpleAttributeType("",
				"className", DeclaredAnnotations.NONE, null)).withMessage(
						"AttributeName must not be null");
	}

	@Test
	public void createWhenClassNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SimpleAttributeType(
				"attributeName", null, DeclaredAnnotations.NONE, null)).withMessage(
						"ClassName must not be null");
	}

	@Test
	public void createWhenClassNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SimpleAttributeType(
				"attributeName", "", DeclaredAnnotations.NONE, null)).withMessage(
						"ClassName must not be null");
	}

	@Test
	public void createWhenDeclaredAnnotationsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SimpleAttributeType("attributeName", "className", null,
						null)).withMessage("DeclaredAnnotations must not be null");
	}

	@Test
	public void getAttributeNameReturnsAttributeName() {
		assertThat(this.type.getAttributeName()).isEqualTo("attributeName");
	}

	@Test
	public void getClassNameReturnsClassName() {
		assertThat(this.type.getClassName()).isEqualTo("className");
	}

	@Test
	public void getDeclaredAnnotationReturnsDeclaredAnnotations() {
		assertThat(this.type.getDeclaredAnnotations()).isSameAs(this.declaredAnnotations);
	}

	@Test
	public void getDefaultValueReturnsDefaultValue() {
		assertThat(this.type.getDefaultValue()).isEqualTo("defaultValue");
	}

	@Test
	public void getDefaultValueWhenDefaultValueIsNullReturnsNull() {
		SimpleAttributeType type = new SimpleAttributeType("attributeName", "className",
				this.declaredAnnotations, null);
		assertThat(type.getDefaultValue()).isNull();
	}

	@Test
	public void toStringReturnsString() {
		assertThat(this.type.toString()).isEqualTo("@Declared(value=\"test\")\n"
				+ "className attributeName() default \"defaultValue\";");
	}

}
