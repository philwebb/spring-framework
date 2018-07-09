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

/**
 * Tests for {@link StandardDeclaredAttributes}.
 *
 * @author Phillip Webb
 */
public class StandardDeclaredAttributesTests {

	private StandardDeclaredAttributes attributes;

	@Before
	public void setup() {
		this.attributes = new StandardDeclaredAttributes(
				ExampleClass.class.getDeclaredAnnotation(ExampleAnnotation.class));

	}

	@Test
	public void getReturnsValue() {
		assertThat(this.attributes.get("stringValue")).isEqualTo("str");
		assertThat(this.attributes.get("byteArray")).isEqualTo(new byte[] { 1, 2, 3 });
	}

	@Test
	public void getWhenClassReturnsClassReference() {
		assertThat(this.attributes.get("classValue")).isEqualTo(
				ClassReference.of(String.class));
		assertThat(this.attributes.get("classArray")).isEqualTo(new ClassReference[] {
			ClassReference.of(String.class), ClassReference.of(StringBuilder.class) });
	}

	@Test
	public void getWhenEnumReturnsEnumReference() {
		assertThat(this.attributes.get("enumValue")).isEqualTo(
				EnumValueReference.of(ExampleEnum.ONE));
		assertThat(this.attributes.get("enumArray")).isEqualTo(
				new EnumValueReference[] { EnumValueReference.of(ExampleEnum.ONE),
					EnumValueReference.of(ExampleEnum.THREE) });
	}

	@Test
	public void toStringReturnsString() {
		assertThat(this.attributes.toString()).startsWith("(").endsWith(")").contains(
				"classArray = { java.lang.String.class, java.lang.StringBuilder.class }");
	}

	@ExampleAnnotation(stringValue = "str", byteArray = { 1, 2,
		3 }, classValue = String.class, classArray = { String.class,
			StringBuilder.class }, enumValue = ExampleEnum.ONE, enumArray = {
				ExampleEnum.ONE, ExampleEnum.THREE })
	static class ExampleClass {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ExampleAnnotation {

		String stringValue();

		byte[] byteArray();

		Class<? extends CharSequence> classValue();

		Class<? extends CharSequence>[] classArray();

		ExampleEnum enumValue();

		ExampleEnum[] enumArray();

	}

	enum ExampleEnum {

		ONE, TWO, THREE

	}

}
