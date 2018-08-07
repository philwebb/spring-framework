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
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.StreamSupport;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimpleAnnotationTypeResolver}.
 *
 * @author Phillip Webb
 */
public class SimpleAnnotationTypeResolverTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SimpleAnnotationTypeResolver resolver = SimpleAnnotationTypeResolver.get();

	@Test
	public void createWhenResourceLoaderIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ResourceLoader must not be null");
		new SimpleAnnotationTypeResolver(null);
	}

	@Test
	public void resolveWhenSourceClassNameIsNullThrowsException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ClassName must not be null");
		this.resolver.resolve(null);
	}

	@Test
	public void resolveCachesForSameResource() {
		AnnotationType first = this.resolver.resolve(
				ExampleSimpleAnnotation.class.getName());
		AnnotationType second = this.resolver.resolve(
				ExampleSimpleAnnotation.class.getName());
		assertThat(first).isSameAs(second);
	}

	@Test
	public void resolveReadsSimpleAnnotation() {
		Class<? extends Annotation> sourceClass = ExampleSimpleAnnotation.class;
		AnnotationType annotationType = resolver.resolve(sourceClass.getName(), false);
		AttributeTypes attributeTypes = annotationType.getAttributeTypes();
		assertThat(attributeTypes).hasSize(1);
		AttributeType attributeType = attributeTypes.get("value");
		assertThat(attributeType.getAttributeName()).isEqualTo("value");
		assertThat(attributeType.getClassName()).isEqualTo("java.lang.String");
		assertThat(attributeType.getDeclaredAnnotations()).isEmpty();
		assertThat(attributeType.getDefaultValue()).isEqualTo("abc");
		Iterable<DeclaredAnnotation> metaAnnotations = annotationType.getDeclaredAnnotations();
		assertThat(metaAnnotations).hasSize(2);
		assertThat(StreamSupport.stream(metaAnnotations.spliterator(), false).map(
				DeclaredAnnotation::getClassName)).contains(Retention.class.getName());
	}

	@Test
	public void resolveReadsArrayAttributes() {
		Class<? extends Annotation> sourceClass = ArrayAttributesAnnotation.class;
		AnnotationType annotationType = resolver.resolve(sourceClass.getName(), false);
		AttributeTypes attributeTypes = annotationType.getAttributeTypes();
		assertThat(attributeTypes).hasSize(3);
		AttributeType strings = annotationType.getAttributeTypes().get("value");
		assertThat(strings.getClassName()).isEqualTo("java.lang.String[]");
		assertThat((Object[]) strings.getDefaultValue()).isEmpty();
		AttributeType ints = annotationType.getAttributeTypes().get("ints");
		assertThat(ints.getClassName()).isEqualTo("int[]");
		assertThat((int[]) ints.getDefaultValue()).containsExactly(1, 2, 3);
		AttributeType bools = annotationType.getAttributeTypes().get("bools");
		assertThat(bools.getClassName()).isEqualTo("boolean[]");
		assertThat((boolean[]) bools.getDefaultValue()).isNull();
	}

	@Test
	public void resolveReadsSelfAnnotated() {
		AnnotationType resolved = this.resolver.resolve(
				SelfAnnotatedAnnotation.class.getName(), false);
		assertThat(resolved.getDeclaredAnnotations().find(
				SelfAnnotatedAnnotation.class.getName())).isNotNull();
	}

	@Test
	public void resolveReadAnnotationOnAttribute() {
		AnnotationType annotationType = this.resolver.resolve(
				AnnoatedAttributeAnnotation.class.getName(), false);
		DeclaredAnnotations attributeAnnotations = annotationType.getAttributeTypes().get(
				"value").getDeclaredAnnotations();
		DeclaredAnnotation attributeAnnotation = attributeAnnotations.find(
				ExampleSimpleAnnotation.class.getName());
		assertThat(attributeAnnotation).isNotNull();
		assertThat(attributeAnnotation.getAttributes().get("value")).isEqualTo("test");
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface ExampleSimpleAnnotation {

		String value() default "abc";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface ArrayAttributesAnnotation {

		String[] value() default {};

		int[] ints() default { 1, 2, 3 };

		boolean[] bools();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@SelfAnnotatedAnnotation
	@interface SelfAnnotatedAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface AnnoatedAttributeAnnotation {

		@ExampleSimpleAnnotation("test")
		String value();

	}

}
