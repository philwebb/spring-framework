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

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import org.junit.Test;

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AnnotationTypeResolver;
import org.springframework.core.annotation.type.AttributeTypes;
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MappableAnnotation}.
 *
 * @author Phillip Webb
 */
public class MappableAnnotationTests {

	private final AnnotationTypeResolver resolver = AnnotationTypeResolver.get(
			ClassUtils.getDefaultClassLoader());

	@Test
	public void getAnnotationTypeReturnsType() {
		AnnotationType type = createMockType();
		MappableAnnotation mappable = new MappableAnnotation(this.resolver, RepeatableContainers.standardRepeatables(),
				type, DeclaredAttributes.NONE,null);
		assertThat(mappable.getAnnotationType()).isSameAs(type);
	}

	@Test
	public void getAttributesReturnsAttributes() {
		DeclaredAttributes attributes = DeclaredAttributes.of("value", "test");
		MappableAnnotation mappable = new MappableAnnotation(this.resolver,
				RepeatableContainers.standardRepeatables(), createMockType(), attributes,null);
		assertThat(mappable.getAttributes()).isSameAs(attributes);
	}

	@Test
	public void fromAnnotationsIncludesAllAnnotations() {
		DeclaredAnnotation exampleOne = DeclaredAnnotation.of(ExampleOne.class.getName(),
				DeclaredAttributes.NONE);
		DeclaredAnnotation exampleTwo = DeclaredAnnotation.of(ExampleTwo.class.getName(),
				DeclaredAttributes.NONE);
		DeclaredAnnotations annotations = DeclaredAnnotations.of(exampleOne, exampleTwo);
		Stream<String> mapped = MappableAnnotation.from(resolver, RepeatableContainers.standardRepeatables(),
				annotations).map(
						MappableAnnotation::getAnnotationType).map(
								AnnotationType::getClassName);
		assertThat(mapped).containsExactly(ExampleOne.class.getName(),
				ExampleTwo.class.getName());
	}

	@Test
	public void fromAnnotationsExpandsRepatables() {
		DeclaredAttributes exampleOneA = DeclaredAttributes.of("value", "1a");
		DeclaredAttributes exampleOneB = DeclaredAttributes.of("value", "1b");
		DeclaredAttributes exampleTwoA = DeclaredAttributes.of("value", "2a");
		DeclaredAttributes exampleTwoB = DeclaredAttributes.of("value", "2b");
		DeclaredAnnotation ones = DeclaredAnnotation.of(ExampleOnes.class.getName(),
				DeclaredAttributes.of("value",
						new DeclaredAttributes[] { exampleOneA, exampleOneB }));
		DeclaredAnnotation twos = DeclaredAnnotation.of(ExampleTwos.class.getName(),
				DeclaredAttributes.of("value",
						new DeclaredAttributes[] { exampleTwoA, exampleTwoB }));
		DeclaredAnnotations annotations = DeclaredAnnotations.of(ones, twos);
		Stream<String> mapped = MappableAnnotation.from(resolver, RepeatableContainers.standardRepeatables(),
				annotations).map(
						m -> (String) m.getAttributes().get("value"));
		assertThat(mapped).containsExactly("1a", "1b", "2a", "2b");
	}

	@Test
	public void fromAnnotationContainsAnnotation() {

	}

	@Test
	public void fromAnnotationExpandsRepeatables() {

	}

	private AnnotationType createMockType() {
		return AnnotationType.of("com.example", DeclaredAnnotations.NONE,
				AttributeTypes.of());
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(ExampleOnes.class)
	@interface ExampleOne {

		String value() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Repeatable(ExampleTwos.class)
	@interface ExampleTwo {

		String value() default "";

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ExampleOnes {

		ExampleOne[] value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ExampleTwos {

		ExampleTwo[] value();

	}

}
