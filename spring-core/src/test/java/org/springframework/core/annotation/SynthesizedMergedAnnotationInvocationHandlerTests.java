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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SynthesizedMergedAnnotationInvocationHandler}.
 *
 * @author Phillip Webb
 */
public class SynthesizedMergedAnnotationInvocationHandlerTests {

	@Test
	public void createWhenAnnotationIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SynthesizedMergedAnnotationInvocationHandler<>(null,
						TestAnnotation.class)).withMessage("Annotation must not be null");
	}

	@Test
	public void createWhenTypeIsNullThrowsException() {
		MergedAnnotation<TestAnnotation> annotation = MergedAnnotations.from(
				WithTestAnnotation.class).get(TestAnnotation.class);
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SynthesizedMergedAnnotationInvocationHandler<>(annotation,
						null)).withMessage("Type must not be null");
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void createWhenTypeIsNotAnnotationTypeThrowsException() {
		MergedAnnotation<TestAnnotation> annotation = MergedAnnotations.from(
				WithTestAnnotation.class).get(TestAnnotation.class);
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SynthesizedMergedAnnotationInvocationHandler<>(annotation,
						(Class) CharSequence.class)).withMessage(
								"Type must be an annotation");
	}

	@Test
	public void invokeEqualsChecksUsingsAttributeMethods() {
		TestAnnotation synthesized = MergedAnnotations.from(WithTestAnnotation.class).get(
				TestAnnotation.class).synthesize();
		TestAnnotation regular = WithTestAnnotation.class.getDeclaredAnnotation(
				TestAnnotation.class);
		assertThat(synthesized).isEqualTo(regular);
		assertThat(regular).isEqualTo(synthesized);
	}

	@Test
	public void invokeHashCodeGeneratesAnnotationCompatibleHashCode() {
		TestAnnotation synthesized = MergedAnnotations.from(WithTestAnnotation.class).get(
				TestAnnotation.class).synthesize();
		TestAnnotation regular = WithTestAnnotation.class.getDeclaredAnnotation(
				TestAnnotation.class);
		assertThat(synthesized.hashCode()).isEqualTo(regular.hashCode());
	}

	@Test
	public void invokeToStringReturnsToString() {
		MergedAnnotation<TestAnnotation> mergedAnnotation = MergedAnnotations.from(WithTestAnnotation.class).get(
				TestAnnotation.class);
		TestAnnotation synthesized = SynthesizedMergedAnnotationInvocationHandler.createProxy(
				mergedAnnotation, TestAnnotation.class);
		assertThat(synthesized.toString()).isEqualTo("@" + TestAnnotation.class.getName()
				+ "(byteValue=1, booleanValue=true, charValue=c, shortValue=2, "
				+ "intValue=3, longValue=4, floatValue=5.0, doubleValue=6.0, "
				+ "stringValue=string, classValue=java.lang.CharSequence, "
				+ "enumValue=ONE, "
				+ "annotationValue=@org.springframework.core.annotation.SynthesizedMergedAnnotationInvocationHandlerTests$NestedAnnotation(value=n), "
				+ "byteArrayValue=[1], booleanArrayValue=[true], charArrayValue=[c], "
				+ "shortArrayValue=[2], intArrayValue=[3], longArrayValue=[4], "
				+ "floatArrayValue=[5.0], doubleArrayValue=[6.0], "
				+ "stringArrayValue=[string], classArrayValue=[java.lang.CharSequence], "
				+ "enumArrayValue=[ONE], "
				+ "annotationArrayValue=[@org.springframework.core.annotation.SynthesizedMergedAnnotationInvocationHandlerTests$NestedAnnotation(value=n)])");
		TestAnnotation java = WithTestAnnotation.class.getDeclaredAnnotation(
				TestAnnotation.class);
		List<String> javaToStringParts = getToStringParts(java);
		List<String> springToStringParts = getToStringParts(synthesized);
		assertThat(springToStringParts).hasSameElementsAs(javaToStringParts);
	}

	private List<String> getToStringParts(TestAnnotation annotation) {
		String string = annotation.toString();
		string = string.substring(string.indexOf("(") + 1, string.lastIndexOf(")"));
		return Arrays.asList(string.replace("interface ", "").split(", "));
	}

	@Test
	public void invokeAnnotationTypeReturnsAnnotationType() {
		TestAnnotation synthesized = MergedAnnotations.from(WithTestAnnotation.class).get(
				TestAnnotation.class).synthesize();
		assertThat(synthesized.annotationType()).isEqualTo(TestAnnotation.class);
	}

	@Test
	public void invokeAttributeMethodReturnsAttributeValue() {
		TestAnnotation synthesized = MergedAnnotations.from(WithTestAnnotation.class).get(
				TestAnnotation.class).synthesize();
		assertThat(synthesized.byteValue()).isEqualTo((byte) 1);
		assertThat(synthesized.booleanValue()).isEqualTo(true);
		assertThat(synthesized.charValue()).isEqualTo('c');
		assertThat(synthesized.shortValue()).isEqualTo((short) 2);
		assertThat(synthesized.intValue()).isEqualTo(3);
		assertThat(synthesized.longValue()).isEqualTo(4);
		assertThat(synthesized.floatValue()).isEqualTo(5.0f);
		assertThat(synthesized.doubleValue()).isEqualTo(6.0);
		assertThat(synthesized.stringValue()).isEqualTo("string");
		assertThat(synthesized.classValue()).isEqualTo(CharSequence.class);
		assertThat(synthesized.enumValue()).isEqualTo(TestEnum.ONE);
		assertThat(synthesized.annotationValue().value()).isEqualTo("n");
		assertThat(synthesized.byteArrayValue()).containsExactly(1);
		assertThat(synthesized.booleanArrayValue()).containsExactly(true);
		assertThat(synthesized.charArrayValue()).containsExactly('c');
		assertThat(synthesized.shortArrayValue()).containsExactly((short) 2);
		assertThat(synthesized.intArrayValue()).containsExactly(3);
		assertThat(synthesized.longArrayValue()).containsExactly(4);
		assertThat(synthesized.floatArrayValue()).containsExactly(5.0f);
		assertThat(synthesized.doubleArrayValue()).containsExactly(6.0);
		assertThat(synthesized.stringArrayValue()).containsExactly("string");
		assertThat(synthesized.classArrayValue()).containsExactly(CharSequence.class);
		assertThat(synthesized.enumArrayValue()).containsExactly(TestEnum.ONE);
		assertThat(synthesized.annotationArrayValue()[0].value()).isEqualTo("n");
	}

	@Test
	public void invokeAttributeMethodWhenHasNotValueThrowsException() {
		MergedAnnotation<TestAnnotation> annotation = MergedAnnotations.from(
				WithTestAnnotation.class).get(TestAnnotation.class).filterAttributes(
						"byteValue"::equals);
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
				() -> annotation.synthesize()).withMessageStartingWith(
						"No value found for attribute named");
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAnnotation {

		byte byteValue();

		boolean booleanValue();

		char charValue();

		short shortValue();

		int intValue();

		long longValue();

		float floatValue();

		double doubleValue();

		String stringValue() default "";

		Class<?> classValue();

		TestEnum enumValue();

		NestedAnnotation annotationValue();

		byte[] byteArrayValue();

		boolean[] booleanArrayValue();

		char[] charArrayValue();

		short[] shortArrayValue();

		int[] intArrayValue();

		long[] longArrayValue();

		float[] floatArrayValue();

		double[] doubleArrayValue();

		String[] stringArrayValue();

		Class<?>[] classArrayValue();

		TestEnum[] enumArrayValue();

		NestedAnnotation[] annotationArrayValue();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface NestedAnnotation {

		String value();

	}

	static enum TestEnum {
		ONE, TWO, THREE
	}

	// @formatter:off
	@TestAnnotation(
		byteValue = 1,
		booleanValue = true,
		charValue = 'c',
		shortValue = 2,
		intValue = 3,
		longValue = 4,
		floatValue = 5.0f,
		doubleValue = 6.0,
		stringValue = "string",
		classValue = CharSequence.class,
		enumValue = TestEnum.ONE,
		annotationValue = @NestedAnnotation("n"),
		byteArrayValue = { 1 },
		booleanArrayValue = { true },
		charArrayValue = { 'c' },
		shortArrayValue = { 2 },
		intArrayValue = { 3 },
		longArrayValue = { 4 },
		floatArrayValue = { 5.0f },
		doubleArrayValue = { 6.0 },
		stringArrayValue = { "string" },
		classArrayValue = { CharSequence.class },
		enumArrayValue = { TestEnum.ONE },
		annotationArrayValue = { @NestedAnnotation("n") }
	)
	static class WithTestAnnotation {

	}
	// @formatter:on

}
