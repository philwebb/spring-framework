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
 * Tests for {@link StandardDeclaredAnnotation}.
 *
 * @author Phillip Webb
 */
public class StandardDeclaredAnnotationTests {

	private StandardDeclaredAnnotation annotation;

	@Before
	public void setup() {
		this.annotation = new StandardDeclaredAnnotation(
				ExampleClass.class.getDeclaredAnnotation(ExampleAnnotation.class));
	}

	@Test
	public void getClassNameReturnsClassName() {
		assertThat(this.annotation.getClassName()).isEqualTo(
				ExampleAnnotation.class.getName());
	}

	@Test
	public void getAttributesReturnsAttributes() {
		assertThat(this.annotation.getAttributes().get("value")).isNotNull();
	}

	@ExampleAnnotation("str")
	static class ExampleClass {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ExampleAnnotation {

		String value();

	}

}
