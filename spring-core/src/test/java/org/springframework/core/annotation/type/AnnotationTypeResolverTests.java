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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.StreamSupport;

import org.junit.Test;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AnnotationTypeResolver}.
 *
 * @author Phillip Webb
 */
public class AnnotationTypeResolverTests {

	private final AnnotationTypeResolver resolver = AnnotationTypeResolver.get(
			getClass().getClassLoader());

	@Test
	public void resolveWhenClassNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.resolver.resolve((String) null)).withMessage(
						"ClassName must not be empty");
	}

	@Test
	public void resolveWhenClassNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> this.resolver.resolve("")).withMessage(
						"ClassName must not be empty");
	}

	@Test
	public void resolveWhenResouceDoesNotLoadThrowsException() throws Exception {
		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		Resource resource = mock(Resource.class);
		given(resourceLoader.getResource(anyString())).willReturn(resource);
		given(resource.getInputStream()).willThrow(IOException.class);
		AnnotationTypeResolver resolver = new AnnotationTypeResolver(resourceLoader);
		assertThatExceptionOfType(UnresolvableAnnotationTypeException.class).isThrownBy(
				() -> resolver.resolve(ExampleSimpleAnnotation.class.getName()));
	}

	@Test
	public void resolveReadsSimpleAnnotation() {
		Class<? extends Annotation> sourceClass = ExampleSimpleAnnotation.class;
		AnnotationType annotationType = this.resolver.resolve(sourceClass.getName(),
				false);
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
				DeclaredAnnotation::getType)).contains(Retention.class.getName());
	}

	@Test
	public void resolveReadsArrayAttributes() {
		Class<? extends Annotation> sourceClass = ArrayAttributesAnnotation.class;
		AnnotationType annotationType = this.resolver.resolve(sourceClass.getName(),
				false);
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
	public void resolveReadsAnnotationOnAttribute() {
		AnnotationType annotationType = this.resolver.resolve(
				AnnoatedAttributeAnnotation.class.getName(), false);
		DeclaredAnnotations attributeAnnotations = annotationType.getAttributeTypes().get(
				"value").getDeclaredAnnotations();
		DeclaredAnnotation attributeAnnotation = attributeAnnotations.find(
				ExampleSimpleAnnotation.class.getName());
		assertThat(attributeAnnotation).isNotNull();
		assertThat(attributeAnnotation.getAttributes().get("value")).isEqualTo("test");
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
	public void resolveIncludesSourceForDeclaredAnnotations() {
		AnnotationType resolved = this.resolver.resolve(
				ExampleAnnotatedAnnotation.class.getName());
		Object source = resolved.getDeclaredAnnotations().getSource();
		assertThat(source).isNotNull();
		assertThat(source.toString()).isEqualTo(
				ExampleAnnotatedAnnotation.class.getName());
	}

	@Test
	public void resolveWhenAnnotationHasStaticFinalsDoesNotIncludeInitMethod()
			throws Exception {
		AnnotationType resolved = this.resolver.resolve(
				StaticFinalsAnnotation.class.getName());
		assertThat(resolved.getAttributeTypes().attributeNames()).containsOnly("value");
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	private @interface ExampleSimpleAnnotation {

		String value() default "abc";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@ExampleSimpleAnnotation
	private @interface ExampleAnnotatedAnnotation {

		String value() default "abc";

	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface ArrayAttributesAnnotation {

		String[] value() default {};

		int[] ints() default { 1, 2, 3 };

		boolean[] bools();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@SelfAnnotatedAnnotation
	private @interface SelfAnnotatedAnnotation {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface AnnoatedAttributeAnnotation {

		@ExampleSimpleAnnotation("test")
		String value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface StaticFinalsAnnotation {

		String[] EXAMPLE = { "*" };

		String[] DEFAULT_ALLOWED_HEADERS = { "*" };

		String value();

	}

}
