/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link AttributeMethods}.
 *
 * @author Phillip Webb
 */
public class AttributeMethodsTests {

	@Test
	public void forAnnotationTypeWhenNullReturnsNone() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(
				null);
		assertThat(methods).isSameAs(AttributeMethods.NONE);
	}

	@Test
	public void forAnnotationWithNoAttributesReturnsNone() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(
				NoAttributes.class);
		assertThat(methods).isSameAs(AttributeMethods.NONE);
	}

	@Test
	public void forAnnotationWithMultipleAttributesReturnsAttributes() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(
				MultipleAttributes.class);
		assertThat(methods.get("value").getName()).isEqualTo("value");
		assertThat(methods.get("intValue").getName()).isEqualTo("intValue");
		assertThat(methods.iterator()).flatExtracting(Method::getName).containsExactly(
				"intValue", "value");
	}

	@Test
	public void forAnnotationWithOnlyValueAttributeReturnsAttributes() {
		AttributeMethods methods = AttributeMethods.forAnnotationType(
				ValueOnly.class);
		assertThat(methods.get("value").getName()).isEqualTo("value");
		assertThat(methods.isOnlyValueAttribute()).isTrue();
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface NoAttributes {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface MultipleAttributes {

		String value();

		int intValue();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ValueOnly {

		String value();

	}
}
