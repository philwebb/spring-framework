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
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.core.annotation.MergedAnnotation.MapValues;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * Tests for {@link MergedAnnotations}.
 *
 * @author Phillip Webb
 */
@Ignore
public class MergedAnnotationsTests {
//
//	@Test
//	public void isAnnotationPresent() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.isPresent(Other.class)).isTrue();
//	}
//
//	@Test
//	public void isMetaAnnotationPresent() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.isPresent(Example.class)).isTrue();
//	}
//
//	@Test
//	public void getDirectlyDeclaredAnnotationNames() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		Stream<MergedAnnotation<?>> result = annotations.stream().filter(
//				MergedAnnotation::isDirectlyPresent);
//		assertThat(result.map(MergedAnnotation::getType)).containsExactly(
//				MetaExample.class.getName(), Other.class.getName());
//	}
//
//	@Test
//	@Ignore
//	public void getAllMetaAnnotationsOnADirectlyDeclaredAnnotation() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		MergedAnnotation<MetaExample> annotation = annotations.get(MetaExample.class);
//		Stream<MergedAnnotation<?>> result = annotations.stream().filter(
//				annotation::isParentOf);
//		assertThat(result.map(MergedAnnotation::getType)).containsExactly(
//				Example.class.getName());
//		// FIXME
//	}
//
//	@Test
//	public void hasDirectAnnitation() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.get(MetaExample.class).isDirectlyPresent()).isTrue();
//		assertThat(annotations.get(Example.class).isDirectlyPresent()).isFalse();
//		assertThat(annotations.get(Missing.class).isDirectlyPresent()).isFalse();
//	}
//
//	@Test
//	public void hasDirectOrMetaAnnotation() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.isPresent(MetaExample.class)).isTrue();
//		assertThat(annotations.isPresent(Example.class)).isTrue();
//		assertThat(annotations.isPresent(Missing.class)).isFalse();
//	}
//
//	@Test
//	public void hasMetaAnnotationOnly() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.get(MetaExample.class).isMetaPresent()).isFalse();
//		assertThat(annotations.get(Example.class).isMetaPresent()).isTrue();
//		assertThat(annotations.get(Missing.class).isMetaPresent()).isFalse();
//	}
//
//	@Test
//	public void getMapWithClassesAsStrings() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		Map<String, Object> map = annotations.get(Example.class).asMap(
//				MapValues.CLASS_TO_STRING);
//		// FIXME assert
//	}
//
//	@Test
//	public void getMultiValueMap() {
//		MultiValueMap<String, Object> multiMap = new LinkedMultiValueMap<>();
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndExample.class);
//		annotations.stream(Example.class).map(
//				annotation -> annotation.asMap(MapValues.CLASS_TO_STRING)).forEach(
//						map -> map.forEach(multiMap::add));
//		// FIXME assert
//		// FIXME do we want a collector to help with this?
//		// annotations.stream("annotationType").collect(AnnotationCollectors.toMultiValueMap());
//	}
//
//	@Test
//	public void getAnAttributeValueThatMustExist() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndExample.class);
//		assertThat(annotations.get(Example.class).getString("value")).isEqualTo("b");
//	}
//
//	@Test
//	public void getAnAttributeValueThatMightNotExist() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndExample.class);
//		assertThat(annotations.get(Example.class).getAttribute("value",
//				String.class)).contains("b");
//		assertThat(
//				annotations.get("Missing").getAttribute("value", String.class)).isEmpty();
//	}
//
//	@Test
//	public void testIfAnAnnotationHasDefaultValue() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.get(Other.class).hasDefaultValue("value")).isTrue();
//	}
//
//	@Test
//	public void filterDefaultValues() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndOther.class);
//		assertThat(annotations.get(Other.class).filterDefaultValues().getAttribute(
//				"value", String.class).orElse("-")).isEqualTo("-");
//	}
//
//	@Test
//	public void getAttributeValueWithAnWithExplicitAlaisMirror() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithExplicitAlaisMirror.class);
//		MergedAnnotation<?> annotation = annotations.get(ExplicitAlaisMirror.class);
//		assertThat(annotation.getString("one")).isEqualTo("a");
//		assertThat(annotation.getString("two")).isEqualTo("a");
//	}
//
//	@Test
//	public void getNestedAttributeValueWithAnWithExplicitAlaisMirror() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithNestedExplicitAlaisMirror.class);
//		MergedAnnotation<?> annotation = annotations.get(NestedExplicitAlaisMirror.class);
//		MergedAnnotation<?> nested = annotation.getAnnotation("mirror",
//				ExplicitAlaisMirror.class);
//		MergedAnnotation<?>[] nestedArray = annotation.getAnnotationArray("mirrors",
//				ExplicitAlaisMirror.class);
//		assertThat(nested.getString("one")).isEqualTo("b");
//		assertThat(nested.getString("two")).isEqualTo("b");
//		assertThat(nestedArray[0].getString("one")).isEqualTo("c");
//		assertThat(nestedArray[0].getString("two")).isEqualTo("c");
//	}
//
//	@Test
//	public void toStringMatches() {
//		String expected = WithNestedExplicitAlaisMirror.class.getDeclaredAnnotation(
//				NestedExplicitAlaisMirror.class).toString();
//		expected = expected.replace("two=x, one=b", "one=b, two=b");
//		expected = expected.replace("two=c, one=x", "one=c, two=c");
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithNestedExplicitAlaisMirror.class);
//		MergedAnnotation<?> annotation = annotations.get(NestedExplicitAlaisMirror.class);
//		assertThat(annotation.toString()).isEqualTo(expected);
//	}
//
//	@Test
//	public void getFavorsMoreLocallyDeclaredComposedAnnotationsOverInheritedAnnotations() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				WithMetaExampleAndInheritedExample.class);
//		MergedAnnotation<Example> annotation = annotations.get(Example.class);
//		assertThat(annotation.getString("value")).isEqualTo("b");
//	}
//
//	@Test
//	public void selfAnnotated() {
//		MergedAnnotations annotations = MergedAnnotations.from(
//				SelfAnnotatedExample.class);
//		MergedAnnotation<SelfAnnotatedExample> annotation = annotations.get(
//				SelfAnnotatedExample.class);
//		assertThat(annotation.getString("value")).isEqualTo("test");
//		assertThat(annotations.stream(SelfAnnotatedExample.class)).hasSize(1);
//	}
//
//	//
//	// // AnnotatedElementUtils.forAnnotations
//	//
//	// // Annotations.of(Annotation...)
//	//
//	// // AnnotatedElementUtils.getMetaAnnotationTypes
//	//
//	// //
//	// Annotations.from(element).stream().map(Annotation::getType).collect(..)
//	//
//	// // AnnotatedElementUtils.hasMetaAnnotationTypes
//	//
//	// //
//	// Annotations.from(element).stream("annotationType").anyMatch(Annotation::isMetaAnnotation);
//	//
//	// // AnnotatedElementUtils.isAnnotated
//	//
//	// // annotations.get("annotationType").isPresent();
//	//
//	// // AnnotatedElementUtils.getMergedAnnotationAttributes
//	//
//	// // annotation = annotations.get("annotationType");
//	// // return (annotation.isPresent ? annotation.asMap(synth+asclass) :
//	// // null;
//	// // (asMap will have different options depending on flavor of call).
//	//
//	// // AnnotatedElementUtils.getMergedAnnotation(
//	//
//	// // annotation.get("type").synthesize(); // FIXME if present
//	//
//	// // AnnotatedElementUtils.getAllMergedAnnotations
//	//
//	// // FIXME filter out repeatable
//	// // annotation.stream("type").map(Annotation::synthesize).collect(toSet);
//	//
//	// // FIXME merge ?
//	// // annotation.stream("type").map(Annotation::synthesize).collect(toSet);
//	//
//	// // getAllAnnotationAttributes (as above)
//	//
//	// // AnnotationElementUtils.hasAnnotation
//	//
//	// // Annotations.from(element,
//	// // SearchStrategy.FULL).getAnnotation("").isPresent();
//	//
//	// // AnnotationElementUtils.findMergedAnnotationAttributes
//	//
//	// // FIXME merge ?
//	// // Annotations.from(element,
//	// // SearchStrategy.FULL).get("type").asMap(...);
//	//
//	// // findAllMergedAnnotations
//	//
//	// // FIXME filter out repeatable
//	// // Annotations.from(element, SearchStrategy.FULL)
//	// // annotation.stream("type").map(Annotation::synthesize).collect(toSet);
//	//
//	// // AnnotationUtils.isAnnotationInherited
//	// // Annotations.from(element, SearchStrategy.INHERITED).get("type");
//	// // isDirectlyDeclared && !isInherited
//	//
//	// // AnnotationUtils.isAnnotationMetaPresent
//	// // Annotation.of(annotation).isMetaAnnotation();
//	//
//	// }
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface Examples {
//
//		Example[] value();
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Repeatable(Examples.class)
//	@interface Example {
//
//		String value() default "";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface Missing {
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface Other {
//
//		String value() default "default";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Example
//	@interface MetaExample {
//
//		@AliasFor(annotation = Example.class, attribute = "value")
//		String exampleValue() default "";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@Example(value = "b")
//	@interface ExampleB {
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface ExplicitAlaisMirror {
//
//		@AliasFor("two")
//		String one() default "x";
//
//		@AliasFor("one")
//		String two() default "x";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface NestedExplicitAlaisMirror {
//
//		ExplicitAlaisMirror mirror();
//
//		ExplicitAlaisMirror[] mirrors();
//
//	}
//
//	@MetaExample(exampleValue = "a")
//	@Other
//	static class WithMetaExampleAndOther {
//
//	}
//
//	@MetaExample(exampleValue = "a")
//	@Example("b")
//	static class WithMetaExampleAndExample {
//
//	}
//
//	@ExplicitAlaisMirror(one = "a")
//	static class WithExplicitAlaisMirror {
//
//	}
//
//	@NestedExplicitAlaisMirror(mirror = @ExplicitAlaisMirror(one = "b"), mirrors = @ExplicitAlaisMirror(two = "c"))
//	static class WithNestedExplicitAlaisMirror {
//
//	}
//
//	@Example("a")
//	static class InheritedWithExample {
//
//	}
//
//	@ExampleB
//	static class WithMetaExampleAndInheritedExample extends InheritedWithExample {
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@interface ExampleArray {
//
//		String[] test() default {};
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@ExampleArray
//	@interface MataArrayOverrideWithSingle {
//
//		@AliasFor(annotation = ExampleArray.class, attribute = "test")
//		String test() default "";
//
//	}
//
//	@Retention(RetentionPolicy.RUNTIME)
//	@SelfAnnotatedExample("test")
//	@interface SelfAnnotatedExample {
//
//		String value() default "";
//
//	}
//
//	// FIXME MapAnnotationAttributeExtractor.java#L136
//
//	// FIXME test repeated

}
