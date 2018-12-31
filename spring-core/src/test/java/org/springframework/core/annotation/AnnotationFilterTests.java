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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationFilter}.
 *
 * @author Phillip Webb
 */
public class AnnotationFilterTests {

	private static final AnnotationFilter FILTER = annotationType -> ObjectUtils.nullSafeEquals(
			annotationType, TestAnnotation.class.getName());

	@Test
	public void matchesAnnotationWhenAnnotationIsNullReturnsFalse() {
		TestAnnotation annotation = null;
		assertThat(FILTER.matches(annotation)).isFalse();
	}

	@Test
	public void matchesAnnotationWhenMatchReturnsTrue() {
		TestAnnotation annotation = WithTestAnnotation.class.getDeclaredAnnotation(
				TestAnnotation.class);
		assertThat(FILTER.matches(annotation)).isTrue();
	}

	@Test
	public void matchesAnnotationWhenNoMatchReturnsFalse() {
		OtherAnnotation annotation = WithOtherAnnotation.class.getDeclaredAnnotation(
				OtherAnnotation.class);
		assertThat(FILTER.matches(annotation)).isFalse();
	}

	@Test
	public void matchesAnnotationClassWhenAnnotationClassIsNullReturnsFalse() {
		Class<Annotation> annotationType = null;
		assertThat(FILTER.matches(annotationType)).isFalse();
	}

	@Test
	public void matchesAnnotationClassWhenMatchReturnsTrue() {
		Class<TestAnnotation> annotationType = TestAnnotation.class;
		assertThat(FILTER.matches(annotationType)).isTrue();
	}

	@Test
	public void matchesAnnotationClassWhenNoMatchReturnsFalse() {
		Class<OtherAnnotation> annotationType = OtherAnnotation.class;
		assertThat(FILTER.matches(annotationType)).isFalse();
	}

	@Test
	public void plainWhenJavaLangAnnotationReturnsTrue() {
		assertThat(AnnotationFilter.PLAIN.matches(Retention.class)).isTrue();
	}

	@Test
	public void plainWhenSpringLangAnnotationReturnsTrue() {
		assertThat(AnnotationFilter.PLAIN.matches(Nullable.class)).isTrue();
	}

	@Test
	public void plainWhenOtherAnnotationReturnsFalse() {
		assertThat(AnnotationFilter.PLAIN.matches(TestAnnotation.class)).isFalse();
	}

	@Test
	public void javaWhenJavaLangAnnotationReturnsTrue() {
		assertThat(AnnotationFilter.JAVA.matches(Retention.class)).isTrue();
	}

	@Test
	public void javaWhenSpringLangAnnotationReturnsFalse() {
		assertThat(AnnotationFilter.JAVA.matches(Nullable.class)).isFalse();
	}

	@Test
	public void javaWhenOtherAnnotationReturnsFalse() {
		assertThat(AnnotationFilter.JAVA.matches(TestAnnotation.class)).isFalse();
	}

	@Test
	public void noneWhenNonNullReturnsTrue() {
		assertThat(AnnotationFilter.NONE.matches(Retention.class)).isTrue();
		assertThat(AnnotationFilter.NONE.matches(Nullable.class)).isTrue();
		assertThat(AnnotationFilter.NONE.matches(TestAnnotation.class)).isTrue();
		assertThat(AnnotationFilter.NONE.matches((Annotation) null)).isTrue();
		assertThat(AnnotationFilter.NONE.matches((Class<Annotation>) null)).isTrue();
		assertThat(AnnotationFilter.NONE.matches((String) null)).isTrue();
	}

	@Test
	public void pacakgesReturnsPackagesAnnotationFilter() {
		assertThat(AnnotationFilter.packages("com.example")).isInstanceOf(
				PackagesAnnotationFilter.class);
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface TestAnnotation {

	}

	@TestAnnotation
	static class WithTestAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface OtherAnnotation {

	}

	@OtherAnnotation
	static class WithOtherAnnotation {

	}

}
