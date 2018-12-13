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
 * Tests for {@link SimpleAnnotationType}.
 *
 * @author Phillip Webb
 */
public class SimpleAnnotationTypeTests {

	private SimpleDeclaredAnnotations declaredAnnotations;

	private SimpleAttributeTypes attributeTypes;

	private SimpleAnnotationType type;

	@Before
	public void setup() {
		this.declaredAnnotations = new SimpleDeclaredAnnotations(new Object(),
				Collections.emptyList());
		this.attributeTypes = new SimpleAttributeTypes(Collections.emptyList());
		this.type = new SimpleAnnotationType("className", this.declaredAnnotations,
				this.attributeTypes);
	}

	@Test
	public void createWhenClassNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SimpleAnnotationType(null, this.declaredAnnotations,
						this.attributeTypes)).withMessage("ClassName must not be empty");
	}

	@Test
	public void createWhenClassNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SimpleAnnotationType("",
				this.declaredAnnotations, this.attributeTypes)).withMessage(
						"ClassName must not be empty");
	}

	@Test
	public void createWhenDeclaredAnnotationsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SimpleAnnotationType(
				"com.example.Component", null, this.attributeTypes)).withMessage(
						"DeclaredAnnotations must not be null");
	}

	@Test
	public void createWhenAttributeTypesIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SimpleAnnotationType(
				"com.example.Component", this.declaredAnnotations, null)).withMessage(
						"AttributeTypes must not be null");
	}

	@Test
	public void getClassNameReturnsClassName() {
		assertThat(this.type.getClassName()).isEqualTo("className");
	}

	@Test
	public void getDeclaredAnnotationsReturnsDeclaredAnnotations() {
		assertThat(this.type.getDeclaredAnnotations()).isSameAs(this.declaredAnnotations);
	}

	@Test
	public void getAttributeTypesReturnsAttributeTypes() {
		assertThat(this.type.getAttributeTypes()).isSameAs(this.attributeTypes);
	}

	@Test
	public void toStringReturnsString() {
		assertThat(this.type.toString()).isEqualTo("@interface className {\n}");
	}

}
