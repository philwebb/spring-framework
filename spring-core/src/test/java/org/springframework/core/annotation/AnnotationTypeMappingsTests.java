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

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import org.springframework.lang.UsesSunMisc;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link AnnotationTypeMappings}.
 *
 * @author Phillip Webb
 */
public class AnnotationTypeMappingsTests {

	@Test
	public void forAnnotationTypeWhenAnnotationIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(null)).withMessage(
						"AnnotationType must not be null");
	}

	@Test
	public void forAnnotationTypeWhenNoMetaAnnotationsReturnsMappings() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				SimpleAnnotation.class);
		assertThat(mappings.size()).isEqualTo(1);
		assertThat(mappings.get(0).getAnnotationType()).isEqualTo(SimpleAnnotation.class);
		assertThat(mappings.iterator()).flatExtracting(
				AnnotationTypeMapping::getAnnotationType).containsExactly(
						SimpleAnnotation.class);
	}

	@Test
	public void forAnnotationWhenHasSpringAnnotationReturnsFilteredMappings() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				WithSpringLangAnnotation.class);
		assertThat(mappings.size()).isEqualTo(1);
	}

	@Test
	public void forAnnotationTypeWhenMetaAnnotationsReturnsMappings() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				MetaAnnotated.class);
		assertThat(mappings.size()).isEqualTo(6);
		assertThat(mappings.iterator()).flatExtracting(
				AnnotationTypeMapping::getAnnotationType).containsExactly(
						MetaAnnotated.class, A.class, B.class, AA.class, AB.class,
						ABC.class);
	}

	@Test
	public void forAnnotationTypeWhenHasRepeatingMetaAnnotationReturnsMapping() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				WithRepeatedMetaAnnotations.class);
		assertThat(mappings.size()).isEqualTo(3);
		assertThat(mappings.iterator()).flatExtracting(
				AnnotationTypeMapping::getAnnotationType).containsExactly(
						WithRepeatedMetaAnnotations.class, Repeating.class,
						Repeating.class);
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface SimpleAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@UsesSunMisc
	static @interface WithSpringLangAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@A
	@B
	static @interface MetaAnnotated {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@AA
	@AB
	static @interface A {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AA {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ABC
	static @interface AB {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ABC {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface B {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Repeating
	@Repeating
	static @interface WithRepeatedMetaAnnotations {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(Repeatings.class)
	static @interface Repeating {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface Repeatings {

		Repeating[] value();

	}

}
