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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests for {@link AttributeValues}.
 *
 * @author Phillip Webb
 */
public class AttributeValuesTests {

	@Test
	public void areValidWhenHasTypeNotPresentExceptionReturnsFalse() {
		ClassValue annotation = mockAnnotation(ClassValue.class);
		given(annotation.value()).willThrow(TypeNotPresentException.class);
		assertThat(AttributeValues.areValid(annotation)).isFalse();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void areValidWhenDoesNotHaveTypeNotPresentExceptionReturnsTrue() {
		ClassValue annotation = mock(ClassValue.class);
		given(annotation.value()).willReturn((Class) InputStream.class);
		assertThat(AttributeValues.areValid(annotation)).isTrue();
	}

	@Test
	public void validateWhenHasTypeNotPresentExceptionThrowsException() {
		ClassValue annotation = mockAnnotation(ClassValue.class);
		given(annotation.value()).willThrow(TypeNotPresentException.class);
		assertThatIllegalStateException().isThrownBy(
				() -> AttributeValues.validate(annotation));
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void validateWhenDoesNotHaveTypeNotPresentExceptionThrowsNothing() {
		ClassValue annotation = mockAnnotation(ClassValue.class);
		given(annotation.value()).willReturn((Class) InputStream.class);
		AttributeValues.validate(annotation);
	}

	@Test
	public void isDefaultValueWhenValueAndDefaultAreNullReturnsTrue() {
		ClassValue annotation = mockAnnotation(ClassValue.class);
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				annotation.annotationType());
		assertThat(AttributeValues.isDefaultValue(null, attributes.get("value"),
				ReflectionUtils::invokeMethod)).isTrue();
	}

	@Test
	public void isDefaultValueWhenValueAndDefaultMatchReturnsTrue() {
		ClassValueWithDefault annotation = mockAnnotation(ClassValueWithDefault.class);
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				annotation.annotationType());
		assertThat(AttributeValues.isDefaultValue(InputStream.class,
				attributes.get("value"), ReflectionUtils::invokeMethod)).isTrue();
	}

	@Test
	public void isDefaultValueWhenClassAndStringNamesMatchReturnsTrue() {
		ClassValueWithDefault annotation = mockAnnotation(ClassValueWithDefault.class);
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				annotation.annotationType());
		assertThat(AttributeValues.isDefaultValue("java.io.InputStream",
				attributes.get("value"), ReflectionUtils::invokeMethod)).isTrue();
	}

	@Test
	public void isDefaultValueWhenClassArrayAndStringArrayNamesMatchReturnsTrue() {
		ClassArrayValueWithDefault annotation = mockAnnotation(
				ClassArrayValueWithDefault.class);
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				annotation.annotationType());
		assertThat(AttributeValues.isDefaultValue(
				new String[] { "java.io.InputStream", "java.io.OutputStream" },
				attributes.get("value"), ReflectionUtils::invokeMethod)).isTrue();
	}

	@Test
	public void isDefaultValueWhenNestedAnnotationAndExtractedValuesMatchReturnsTrue() {
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				NestedValue.class);
		Map<String, Object> value = Collections.singletonMap("value",
				"java.io.InputStream");
		assertThat(AttributeValues.isDefaultValue(value, attributes.get("value"),
				AttributeValuesTests::extractFromMap)).isTrue();
	}

	@Test
	public void isDefaultValueWhenNotMatchingReturnsFalse() {
		ClassValueWithDefault annotation = mockAnnotation(ClassValueWithDefault.class);
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				annotation.annotationType());
		assertThat(AttributeValues.isDefaultValue(OutputStream.class,
				attributes.get("value"), ReflectionUtils::invokeMethod)).isFalse();
	}

	@SuppressWarnings("unchecked")
	private static Object extractFromMap(Method attribute, Object map) {
		return map != null ? ((Map<String, ?>) map).get(attribute.getName()) : null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <A extends Annotation> A mockAnnotation(Class<A> annotationType) {
		A annotation = mock(annotationType);
		given(annotation.annotationType()).willReturn((Class) annotationType);
		return annotation;
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ClassValue {

		Class<?> value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ClassValueWithDefault {

		Class<?> value() default InputStream.class;

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ClassArrayValueWithDefault {

		Class<?>[] value() default { InputStream.class, OutputStream.class };

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface NestedValue {

		ClassValue value() default @ClassValue(InputStream.class);

	}


}
