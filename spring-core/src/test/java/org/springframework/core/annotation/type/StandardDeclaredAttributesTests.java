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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StandardDeclaredAttributes}.
 *
 * @author Phillip Webb
 */
public class StandardDeclaredAttributesTests extends AbstractDeclaredAttributesTests {

	private StandardDeclaredAttributes attributes;

	@Before
	public void setup() {
		this.attributes = new StandardDeclaredAttributes(
				WithExampleAnnotation.class.getDeclaredAnnotation(
						ExampleAnnotation.class));
	}

	@Test
	public void createWhenAnnotationIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new StandardDeclaredAttributes(null)).withMessage(
						"Annotation must not be null");
	}

	@Test
	public void createWhenNotValidThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new StandardDeclaredAttributes(
				createFailingAnnotation())).withMessageContaining(
						"Could not obtain annotation attribute value");

	}

	@Test
	public void namesReturnsNames() {
		assertThat(this.attributes.names()).containsOnly("stringValue", "byteArray",
				"classValue", "classArray", "enumValue", "enumArray", "annotationValue",
				"annotationArray");
	}

	@Test
	public void getReturnsValue() {
		assertThat(this.attributes.get("stringValue")).isEqualTo("str");
		assertThat(this.attributes.get("byteArray")).isEqualTo(new byte[] { 1, 2, 3 });
	}

	@Test
	public void getWhenClassReturnsClassReference() {
		assertThat(this.attributes.get("classValue")).isEqualTo(
				ClassReference.from(String.class));
		assertThat(this.attributes.get("classArray")).isEqualTo(new ClassReference[] {
			ClassReference.from(String.class), ClassReference.from(StringBuilder.class) });
	}

	@Test
	public void getWhenEnumReturnsEnumReference() {
		assertThat(this.attributes.get("enumValue")).isEqualTo(
				EnumValueReference.from(ExampleEnum.ONE));
		assertThat(this.attributes.get("enumArray")).isEqualTo(
				new EnumValueReference[] { EnumValueReference.from(ExampleEnum.ONE),
					EnumValueReference.from(ExampleEnum.THREE) });
	}

	@Test
	public void getWhenAnnotationReturnsDeclaredAttributes() {
		DeclaredAttributes value = (DeclaredAttributes) this.attributes.get(
				"annotationValue");
		assertThat(value.get("value")).isEqualTo("abc");
		DeclaredAttributes[] array = (DeclaredAttributes[]) this.attributes.get(
				"annotationArray");
		assertThat(array[0].get("value")).isEqualTo("def");
	}

	@Override
	protected DeclaredAttributes createTestAttributes() {
		return new StandardDeclaredAttributes(
				WithTestAnnotation.class.getAnnotation(TestAnnotation.class));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private TestAnnotation createFailingAnnotation() {
		TestAnnotation annotation = mock(TestAnnotation.class);
		given(annotation.annotationType()).willReturn((Class) TestAnnotation.class);
		given(annotation.value()).willThrow(new NoClassDefFoundError("FailFailFail"));
		return annotation;
	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface TestAnnotation {

		String value();

	}

	@TestAnnotation(value = "test")
	private static class WithTestAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface ExampleAnnotation {

		String stringValue();

		byte[] byteArray();

		Class<? extends CharSequence> classValue();

		Class<? extends CharSequence>[] classArray();

		ExampleEnum enumValue();

		ExampleEnum[] enumArray();

		TestAnnotation annotationValue();

		TestAnnotation[] annotationArray();
	}

	// @formatter:off
	@ExampleAnnotation(
			stringValue = "str",
			byteArray = { 1, 2, 3 },
			classValue = String.class,
			classArray = { String.class, StringBuilder.class },
			enumValue = ExampleEnum.ONE,
			enumArray = { ExampleEnum.ONE, ExampleEnum.THREE },
			annotationValue = @TestAnnotation("abc"),
			annotationArray = { @TestAnnotation("def") })
	private static class WithExampleAnnotation {

	}
	// @formatter:on

	private enum ExampleEnum {

		ONE, TWO, THREE

	}

}
