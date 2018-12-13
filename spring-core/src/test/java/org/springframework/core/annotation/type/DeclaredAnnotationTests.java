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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DeclaredAnnotation}.
 *
 * @author Phillip Webb
 */
public class DeclaredAnnotationTests {

	@Test
	public void validateWhenValidDoesNotThrow() {
		Annotation annotation = WithTestAnnotation.class.getDeclaredAnnotation(
				TestAnnotation.class);
		DeclaredAnnotation.validate(annotation);
	}

	@Test
	public void validateWhenNotValidThrowsException() {
		Annotation annotation = createFailingAnnotation();
		assertThatIllegalStateException().isThrownBy(
				() -> DeclaredAnnotation.validate(annotation));
	}

	@Test
	public void fromReturnsStandardDeclaredAnnotation() {
		DeclaredAnnotation declaredAnnotation = DeclaredAnnotation.from(
				WithTestAnnotation.class.getDeclaredAnnotation(TestAnnotation.class));
		assertThat(declaredAnnotation).isInstanceOf(StandardDeclaredAnnotation.class);
	}

	@Test
	public void ofReturnsSimpleDeclaredAnnotation() {
		DeclaredAnnotation declaredAnnotation = DeclaredAnnotation.of(
				"com.example.Component", DeclaredAttributes.NONE);
		assertThat(declaredAnnotation).isInstanceOf(SimpleDeclaredAnnotation.class);
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

		Class<?> value() default Void.class;

	}

	@TestAnnotation
	private static class WithTestAnnotation {

	}

}
