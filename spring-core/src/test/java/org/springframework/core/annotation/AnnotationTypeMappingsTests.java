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

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import org.springframework.core.annotation.AnnotationTypeMapping.MappedAttributes;
import org.springframework.lang.UsesSunMisc;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link AnnotationTypeMappings} and {@link AnnotationTypeMapping}.
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

	@Test
	public void forAnnotationTypeWhenSelfAnnotatedReturnsMapping() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				SelfAnnotated.class);
		assertThat(mappings.size()).isEqualTo(1);
		assertThat(mappings.iterator()).flatExtracting(
				AnnotationTypeMapping::getAnnotationType).containsExactly(
						SelfAnnotated.class);
	}

	@Test
	public void forAnnotationTypeWhenFormsLoopReturnsMapping() {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				LoopA.class);
		assertThat(mappings.size()).isEqualTo(2);
		assertThat(mappings.iterator()).flatExtracting(
				AnnotationTypeMapping::getAnnotationType).containsExactly(LoopA.class,
						LoopB.class);
	}

	@Test
	public void forAnnotationTypeWhenHasAliasForWithBothValueAndAttributeThrowsException() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(
				() -> AnnotationTypeMappings.forAnnotationType(
						AliasForWithBothValueAndAttribute.class)).withMessage(
								"In @AliasFor declared on attribute 'test' in annotation ["
										+ AliasForWithBothValueAndAttribute.class.getName()
										+ "], attribute 'attribute' and its alias 'value' are present with values of 'foo' and 'bar', but only one is permitted.");
	}

	@Test
	public void mappingWhenMirroredReturnsMirroredValues() {
		testExplicitMirror(WithExplicitMirrorA.class);
		testExplicitMirror(WithExplicitMirrorB.class);
	}

	private void testExplicitMirror(Class<?> annotatedClass) {
		ExplicitMirror annotation = annotatedClass.getAnnotation(ExplicitMirror.class);
		AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(
				annotation.annotationType()).get(0);
		MappedAttributes attributes = mapping.mapAttributes(annotation);
		assertThat(attributes.getValue("a")).isEqualTo("test");
		assertThat(attributes.getValue("b")).isEqualTo("test");
	}

	@Test
	public void mappingExplicitAliasToMetaAnnotationReturnsMappedValues() {
		ExplicitAliasToMetaAnnotation annotation = WithExplicitAliasToMetaAnnotation.class.getAnnotation(
				ExplicitAliasToMetaAnnotation.class);
		AnnotationTypeMapping mapping = getMapping(annotation,
				ExplicitAliasMetaAnnotationTarget.class);
		MappedAttributes attributes = mapping.mapAttributes(annotation);
		assertThat(attributes.getValue("aliased")).isEqualTo("aliased");
		assertThat(attributes.getValue("nonAliased")).isEqualTo("nonAliased");
	}

	@Test
	public void mappingConventionAliasToMetaAnnotationReturnsMappedValues() {
		ConventionAliasToMetaAnnotation annotation = WithConventionAliasToMetaAnnotation.class.getAnnotation(
				ConventionAliasToMetaAnnotation.class);
		AnnotationTypeMapping mapping = getMapping(annotation,
				ConventionAliasMetaAnnotationTarget.class);
		MappedAttributes attributes = mapping.mapAttributes(annotation);
		assertThat(attributes.getValue("value")).isEqualTo("");
		assertThat(attributes.getValue("convention")).isEqualTo("convention");
	}

	private AnnotationTypeMapping getMapping(Annotation annotation,
			Class<? extends Annotation> mappedAnnotationType) {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				annotation.annotationType());
		for (int i = 0; i < mappings.size(); i++) {
			AnnotationTypeMapping candidate = mappings.get(i);
			if (candidate.getAnnotationType().equals(mappedAnnotationType)) {
				return candidate;
			}
		}
		throw new IllegalStateException(
				"No mapping from " + annotation + " to " + mappedAnnotationType);
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

	@Retention(RetentionPolicy.RUNTIME)
	@SelfAnnotated
	static @interface SelfAnnotated {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@LoopB
	static @interface LoopA {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@LoopA
	static @interface LoopB {

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface AliasForWithBothValueAndAttribute {

		@AliasFor(value = "bar", attribute = "foo")
		String test();

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ExplicitMirror {

		@AliasFor("b")
		String a() default "";

		@AliasFor("a")
		String b() default "";

	}

	@ExplicitMirror(a = "test")
	static class WithExplicitMirrorA {

	}

	@ExplicitMirror(b = "test")
	static class WithExplicitMirrorB {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ExplicitAliasMetaAnnotationTarget(nonAliased = "nonAliased")
	static @interface ExplicitAliasToMetaAnnotation {

		@AliasFor(annotation = ExplicitAliasMetaAnnotationTarget.class)
		String aliased() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ExplicitAliasMetaAnnotationTarget {

		String aliased() default "";

		String nonAliased() default "";

	}

	@ExplicitAliasToMetaAnnotation(aliased = "aliased")
	private static class WithExplicitAliasToMetaAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ConventionAliasMetaAnnotationTarget
	static @interface ConventionAliasToMetaAnnotation {

		String value() default "";

		String convention() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ConventionAliasMetaAnnotationTarget {

		String value() default "";

		String convention() default "";

	}

	@ConventionAliasToMetaAnnotation(value = "value", convention = "convention")
	private static class WithConventionAliasToMetaAnnotation {

	}
}
