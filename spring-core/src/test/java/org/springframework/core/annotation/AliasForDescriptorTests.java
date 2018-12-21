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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;

import org.junit.Test;

import org.springframework.core.annotation.type.ClassReference;
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AliasForDescriptor}.
 *
 * @author Phillip Webb
 */
public class AliasForDescriptorTests {

	private static final String SOURCE_ANNOTATION = "com.example.Component";

	private static final String SOURCE_ATTRIBUTE = "name";

	@Test
	public void getAnnotationWhenAlaisForAnnotationIsNotSpecifiedReturnsSourceAnnotation() {
		AliasForDescriptor descriptor = createDescriptor("annotation",
				ClassReference.from(Annotation.class), "value", "test");
		assertThat(descriptor.getAnnotation()).isEqualTo(SOURCE_ANNOTATION);
	}

	@Test
	public void getAnnotationWhenAliasForAnnotationIsNullReturnsSourceAnnotation() {
		AliasForDescriptor descriptor = createDescriptor("value", "test");
		assertThat(descriptor.getAnnotation()).isEqualTo(SOURCE_ANNOTATION);
	}

	@Test
	public void getAnnotationWhenAliasForSpecifiedAnnotationReturnsAnnotation() {
		AliasForDescriptor descriptor = createDescriptor("annotation",
				ClassReference.of("com.example.Service"));
		assertThat(descriptor.getAnnotation()).isEqualTo("com.example.Service");
	}

	@Test
	public void getAttributeWhenAliasForAttributeNotSpecifiedReturnsSourceAttribute() {
		AliasForDescriptor descriptor = createDescriptor("annotation",
				ClassReference.of("com.example.Service"));
		assertThat(descriptor.getAttribute()).isEqualTo(SOURCE_ATTRIBUTE);
	}

	@Test
	public void getAttributeWhenAlaisForHasValueReturnsTrimmedValue() {
		AliasForDescriptor descriptor = createDescriptor("value", " test ");
		assertThat(descriptor.getAttribute()).isEqualTo("test");
	}

	@Test
	public void getAttributeWhenAliasForHasAttributeReturnsTrimmedAttribute() {
		AliasForDescriptor descriptor = createDescriptor("attribute", " test ");
		assertThat(descriptor.getAttribute()).isEqualTo("test");
	}

	@Test
	public void toStringReturnsString() {
		AliasForDescriptor descriptor = createDescriptor("annotation",
				ClassReference.of("com.example.Service"), "attribute", "test");
		assertThat(descriptor.toString()).isEqualTo(
				"attribute 'test' in annotation [com.example.Service]");
	}

	@Test
	public void createWhenAliasForHasValueAndAttributeThrowsException() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> createDescriptor("value", "test1", "attribute",
						"test2")).withMessage("In @AliasFor declared on attribute "
								+ "'name' in annotation [com.example.Component], "
								+ "attribute 'attribute' and its alias 'value' "
								+ "are present with values of 'test1' and 'test2', "
								+ "but only one is permitted.");
	}

	@Test
	public void createWhenAliasForIsSelfPointingThrowsException() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> createDescriptor("annotation", ClassReference.of(SOURCE_ANNOTATION),
						"attriubute", SOURCE_ATTRIBUTE)).withMessage(
								"@AliasFor declaration on attribute "
										+ "'name' in annotation [com.example.Component] "
										+ "points to itself. Specify 'annotation' to point "
										+ "to a same-named attribute on a meta-annotation.");
	}

	private AliasForDescriptor createDescriptor(Object... attributePairs) {
		DeclaredAnnotation aliasFor = DeclaredAnnotation.of(AliasFor.class.getName(),
				DeclaredAttributes.of(attributePairs));
		return new AliasForDescriptor(SOURCE_ANNOTATION, SOURCE_ATTRIBUTE, aliasFor);
	}

}
