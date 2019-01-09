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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link RepeatableContainers}.
 *
 * @author Phillip Webb
 */
public class RepeatableContainersTests {

	@Test
	public void standardRepeatablesWhenNonRepeatableVisistsAnnotation() {
		MultiValueMap<String, Object> result = visit(WithNonRepeatable.class,
				NonRepeatable.class, RepeatableContainers.standardRepeatables());
		assertThat(result.get(NonRepeatable.class.getName())).containsExactly("a");
	}

	@Test
	public void standardRepeatablesWhenSingleVisitsRepeatedAnnotations() {
		MultiValueMap<String, Object> result = visit(WithSingleStandardRepeatable.class,
				StandardRepeatable.class, RepeatableContainers.standardRepeatables());
		assertThat(result.get(StandardRepeatable.class.getName())).containsExactly("a");
	}

	@Test
	public void standardRepeatablesWhenContainerVisitsRepeatedAnnotations() {
		MultiValueMap<String, Object> result = visit(WithStandardRepeatables.class,
				StandardContainer.class, RepeatableContainers.standardRepeatables());
		assertThat(result.get(StandardRepeatable.class.getName())).containsExactly("a",
				"b");
	}

	@Test
	public void standardRepeatablesWhenContainerButNotRepeatableVisitsRepeatedAnnotations() {
		MultiValueMap<String, Object> result = visit(WithExplicitRepeatables.class,
				ExplicitContainer.class, RepeatableContainers.standardRepeatables());
		DeclaredAttributes[] attributes = (DeclaredAttributes[]) result.get(
				ExplicitContainer.class.getName()).get(0);
		assertThat(attributes).hasSize(2);
		assertThat(attributes[0].get("value")).isEqualTo("a");
		assertThat(attributes[1].get("value")).isEqualTo("b");
	}

	@Test
	public void ofExplicitWhenNonRepeatableVisistsAnnotation() {
		MultiValueMap<String, Object> result = visit(WithNonRepeatable.class,
				NonRepeatable.class, RepeatableContainers.of(ExplicitRepeatable.class,
						ExplicitContainer.class));
		assertThat(result.get(NonRepeatable.class.getName())).containsExactly("a");
	}

	@Test
	public void ofExplicitWhenStandardRepeatableContainerVisitsContainerAnnotation() {
		MultiValueMap<String, Object> result = visit(WithStandardRepeatables.class,
				StandardContainer.class, RepeatableContainers.of(ExplicitRepeatable.class,
						ExplicitContainer.class));
		DeclaredAttributes[] attributes = (DeclaredAttributes[]) result.get(
				StandardContainer.class.getName()).get(0);
		assertThat(attributes).hasSize(2);
		assertThat(attributes[0].get("value")).isEqualTo("a");
		assertThat(attributes[1].get("value")).isEqualTo("b");
	}

	@Test
	public void ofExplicitWhenContainerVisitsRepeatedAnnotations() {
		MultiValueMap<String, Object> result = visit(WithExplicitRepeatables.class,
				ExplicitContainer.class, RepeatableContainers.of(ExplicitRepeatable.class,
						ExplicitContainer.class));
		assertThat(result.get(ExplicitRepeatable.class.getName())).containsExactly("a",
				"b");
	}

	@Test
	public void ofExplicitWhenHasNoValueThrowsException() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> RepeatableContainers.of(ExplicitRepeatable.class,
						InvalidNoValue.class)).withMessageContaining(
								"Invalid declaration of container type ["
										+ InvalidNoValue.class.getName()
										+ "] for repeatable annotation ["
										+ ExplicitRepeatable.class.getName() + "]");
	}

	@Test
	public void ofExplicitWhenValueIsNotArrayThrowsException() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> RepeatableContainers.of(ExplicitRepeatable.class,
						InvalidNotArray.class)).withMessage("Container type ["
								+ InvalidNotArray.class.getName()
								+ "] must declare a 'value' attribute for an array of type ["
								+ ExplicitRepeatable.class.getName() + "]");
	}

	@Test
	public void ofExplicitWhenValueIsArrayOfWrongTypeThrowsException() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> RepeatableContainers.of(ExplicitRepeatable.class,
						InvalidWrongArrayType.class)).withMessage("Container type ["
								+ InvalidWrongArrayType.class.getName()
								+ "] must declare a 'value' attribute for an array of type ["
								+ ExplicitRepeatable.class.getName() + "]");
	}

	@Test
	public void ofExplicitWhenAnnotationIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> RepeatableContainers.of(null, null)).withMessage(
						"Repeatable must not be null");
	}

	@Test
	public void ofExplicitWhenContainerIsNullDeducesContainer() {
		RepeatableContainers repeatableContainers = RepeatableContainers.of(StandardRepeatable.class,
				null);
		assertThat(visit(WithStandardRepeatables.class, StandardContainer.class,
				repeatableContainers).get(
						StandardRepeatable.class.getName())).containsExactly("a", "b");
	}

	@Test
	public void ofExplicitWhenContainerIsNullAndNotRepeatableThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> RepeatableContainers.of(
				ExplicitRepeatable.class, null)).withMessage(
						"Annotation type must be a repeatable annotation: "
								+ "failed to resolve container type for "
								+ ExplicitRepeatable.class.getName());
	}

	@Test
	public void standardAndExplicitVistsAnnotations() {
		RepeatableContainers repeatableContainers = RepeatableContainers.standardRepeatables().and(
				ExplicitContainer.class, ExplicitRepeatable.class);
		assertThat(visit(WithStandardRepeatables.class, StandardContainer.class,
				repeatableContainers).get(
						StandardRepeatable.class.getName())).containsExactly("a", "b");
		assertThat(visit(WithExplicitRepeatables.class, ExplicitContainer.class,
				repeatableContainers).get(
						ExplicitRepeatable.class.getName())).containsExactly("a", "b");
	}

	@Test
	public void equalsAndHashcode() {
		RepeatableContainers c1 = RepeatableContainers.of(ExplicitRepeatable.class,
				ExplicitContainer.class);
		RepeatableContainers c2 = RepeatableContainers.of(ExplicitRepeatable.class,
				ExplicitContainer.class);
		RepeatableContainers c3 = RepeatableContainers.standardRepeatables();
		RepeatableContainers c4 = RepeatableContainers.standardRepeatables().and(
				ExplicitContainer.class, ExplicitRepeatable.class);
		assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
		assertThat(c1).isEqualTo(c1).isEqualTo(c2);
		assertThat(c1).isNotEqualTo(c3).isNotEqualTo(c4);
	}

	private MultiValueMap<String, Object> visit(Class<?> element,
			Class<? extends Annotation> annotationClass,
			RepeatableContainers repeatableContainers) {
		DeclaredAnnotation annotation = DeclaredAnnotation.from(
				element.getDeclaredAnnotation(annotationClass));
		MultiValueMap<String, Object> result = new LinkedMultiValueMap<>();
		repeatableContainers.visit(annotation, getClass().getClassLoader(),
				(annotationType, attributes) -> {
					result.add(annotationType.getClassName(), attributes.get("value"));
				});
		return result;
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface NonRepeatable {

		String value() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(StandardContainer.class)
	static @interface StandardRepeatable {

		String value() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface StandardContainer {

		StandardRepeatable[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ExplicitRepeatable {

		String value() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ExplicitContainer {

		ExplicitRepeatable[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface InvalidNoValue {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface InvalidNotArray {

		int value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface InvalidWrongArrayType {

		StandardRepeatable[] value();

	}

	@NonRepeatable("a")
	static class WithNonRepeatable {

	}

	@StandardRepeatable("a")
	static class WithSingleStandardRepeatable {

	}

	@StandardRepeatable("a")
	@StandardRepeatable("b")
	static class WithStandardRepeatables {

	}

	@ExplicitContainer({ @ExplicitRepeatable("a"), @ExplicitRepeatable("b") })
	static class WithExplicitRepeatables {

	}

}
