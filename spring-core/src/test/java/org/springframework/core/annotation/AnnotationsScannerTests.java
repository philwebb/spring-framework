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

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Test;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationsScanner}.
 *
 * @author Phillip Webb
 */
public class AnnotationsScannerTests {

	@Test
	public void directStrategyOnClassWhenNotAnnoatedScansNone() {
		Class<?> source = WithNoAnnotations.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).isEmpty();
	}

	@Test
	public void directStrategyOnClassScansAnnotations() {
		Class<?> source = WithSingleAnnotation.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void directStrategyOnClassWhenMultipleAnnotationsScansAnnotations() {
		Class<?> source = WithMultipleAnnotations.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	public void directStrategyOnClassWhenHasSuperclassScansOnlyDirect() {
		Class<?> source = WithSingleSuperclass.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void directStrategyOnClassWhenHasInterfaceScansOnlyDirect() {
		Class<?> source = WithSingleInterface.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void directStrategyOnHierarchyScansInCorrectOrder() {
		Class<?> source = WithHierarchy.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void inheritedAnnotationsStrategyOnClassWhenNotAnnoatedScansNone() {
		Class<?> source = WithNoAnnotations.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).isEmpty();
	}

	@Test
	public void inheritedAnnotationsStrategyOnClassScansAnnotations() {
		Class<?> source = WithSingleAnnotation.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void inheritedAnnotationsStrategyOnClassWhenMultipleAnnotationsScansAnnotations() {
		Class<?> source = WithMultipleAnnotations.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	public void inheritedAnnotationsOnClassWhenHasSuperclassScansOnlyInherited() {
		Class<?> source = WithSingleSuperclass.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1", "1:TestInheritedAnnotation2");
	}

	@Test
	public void inheritedAnnotationsStrategyOnClassWhenHasInterfaceDoesNotIncludeInterfaces() {
		Class<?> source = WithSingleInterface.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void inheritedAnnotationsStrategyOnHierarchyScansInCorrectOrder() {
		Class<?> source = WithHierarchy.class;
		assertThat(scan(source, SearchStrategy.INHERITED_ANNOTATIONS)).containsExactly(
				"0:TestAnnotation1", "1:TestInheritedAnnotation2");
	}

	@Test
	public void superclassStrategyOnClassWhenNotAnnoatedScansNone() {
		Class<?> source = WithNoAnnotations.class;
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).isEmpty();
	}

	@Test
	public void superclassStrategyOnClassScansAnnotations() {
		Class<?> source = WithSingleAnnotation.class;
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void superclassStrategyOnClassWhenMultipleAnnotationsScansAnnotations() {
		Class<?> source = WithMultipleAnnotations.class;
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	public void superclassStrategyOnClassWhenHasSuperclassScansSuperclass() {
		Class<?> source = WithSingleSuperclass.class;
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	public void superclassStrategyOnClassWhenHasInterfaceDoesNotIncludeInterfaces() {
		Class<?> source = WithSingleInterface.class;
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void superclassStrategyOnHierarchyScansInCorrectOrder() {
		Class<?> source = WithHierarchy.class;
		assertThat(scan(source, SearchStrategy.SUPER_CLASS)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2",
				"2:TestAnnotation3");
	}

	@Test
	public void exhaustiveStrategyOnClassWhenNotAnnoatedScansNone() {
		Class<?> source = WithNoAnnotations.class;
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).isEmpty();
	}

	@Test
	public void exhaustiveStrategyOnClassScansAnnotations() {
		Class<?> source = WithSingleAnnotation.class;
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1");
	}

	@Test
	public void exhaustiveStrategyOnClassWhenMultipleAnnotationsScansAnnotations() {
		Class<?> source = WithMultipleAnnotations.class;
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "0:TestAnnotation2");
	}

	@Test
	public void exhaustiveStrategyOnClassWhenHasSuperclassScansSuperclass() {
		Class<?> source = WithSingleSuperclass.class;
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	public void exhaustiveStrategyOnClassWhenHasInterfaceDoesNotIncludeInterfaces() {
		Class<?> source = WithSingleInterface.class;
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2");
	}

	@Test
	public void exhaustiveStrategyOnHierarchyScansInCorrectOrder() {
		Class<?> source = WithHierarchy.class;
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly(
				"0:TestAnnotation1", "1:TestAnnotation2", "1:TestInheritedAnnotation2",
				"2:TestAnnotation3", "3:TestAnnotation4", "4:TestAnnotation5",
				"4:TestInheritedAnnotation5", "5:TestAnnotation6");
	}

	// FIXME revisit again
	//
	// @Test
	// public void iteratorWhenDirectScanOnClassFindsAnnotations() {
	// Class<?> source = WithMultipleAnnotations.class;
	// assertThat(scan(source,
	// SearchStrategy.DIRECT)).containsExactly("Example",
	// "Other");
	// }
	//
	// @Test
	// public void iteratorWhenDirectScanOnMethodFindsAnnotations() throws
	// Exception {
	// Method source =
	// WithMultipleAnnotations.class.getDeclaredMethod("method");
	// assertThat(scan(source,
	// SearchStrategy.DIRECT)).containsExactly("Example",
	// "Other");
	// }
	//
	// @Test
	// public void iterateWhenDirectScanOnBridgeMethodFindsAnnotations() throws
	// Exception {
	// Method source = WithBridgeMethod.class.getDeclaredMethod("method",
	// Object.class);
	// assertThat(source.isBridge()).isTrue();
	// assertThat(scan(source,
	// SearchStrategy.DIRECT)).containsExactly("Example");
	// }
	//
	// @Test
	// public void iterateWhenDirectScanOnBridgedMethodFindsAnnotations() throws
	// Exception {
	// Method source = WithBridgeMethod.class.getDeclaredMethod("method",
	// String.class);
	// assertThat(source.isBridge()).isFalse();
	// assertThat(scan(source,
	// SearchStrategy.DIRECT)).containsExactly("Example");
	// }
	//
	// @Test
	// public void iterateWhenExhaustiveScanOnClassFindsAnnotations() {
	// Class<?> source = WithSuperClassAndInterfaces.class;
	// assertThat(scan(source,
	// SearchStrategy.EXHAUSTIVE)).containsExactly("Example",
	// "^OnSuperClass", "^OnInterface");
	// }
	//
	// @Test
	// public void iterateWhenExhaustiveScanOnMethodFindsAnnotations() throws
	// Exception {
	// Method source =
	// WithSuperClassAndInterfaces.class.getDeclaredMethod("method");
	// assertThat(scan(source,
	// SearchStrategy.EXHAUSTIVE)).containsExactly("Example",
	// "^OnSuperClass", "^OnInterface");
	// }

	// FIXME more tests

	private Stream<String> scan(AnnotatedElement source, SearchStrategy searchStrategy) {
		AnnotationsScanner scanner = new AnnotationsScanner(source, searchStrategy);
		return stream(scanner).flatMap(new Mapper());
	}

	private static class Mapper implements Function<DeclaredAnnotations, Stream<String>> {

		private int aggregateIndex = 0;

		@Override
		public Stream<String> apply(DeclaredAnnotations annotations) {
			String prefix = this.aggregateIndex + ":";
			this.aggregateIndex++;
			return StreamSupport.stream(annotations.spliterator(), false).map(
					annotation -> {
						String name = ClassUtils.getShortName(annotation.getType());
						name = name.substring(name.lastIndexOf(".") + 1);
						name = prefix + name;
						return name;
					});
		}

	}

	private Stream<DeclaredAnnotations> stream(AnnotationsScanner scanner) {
		return StreamSupport.stream(scanner.spliterator(), false);
	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface TestAnnotation1 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface TestAnnotation2 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface TestAnnotation3 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface TestAnnotation4 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface TestAnnotation5 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface TestAnnotation6 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	private static @interface TestInheritedAnnotation1 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	private static @interface TestInheritedAnnotation2 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	private static @interface TestInheritedAnnotation3 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	private static @interface TestInheritedAnnotation4 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	private static @interface TestInheritedAnnotation5 {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface OnSuperClass {

	}

	@Retention(RetentionPolicy.RUNTIME)
	private static @interface OnInterface {

	}

	private static class WithNoAnnotations {

	}

	@TestAnnotation1
	private static class WithSingleAnnotation {

	}

	@TestAnnotation1
	@TestAnnotation2
	private static class WithMultipleAnnotations {

		@TestAnnotation1
		@TestAnnotation2
		void method() {
		}

	}

	@TestAnnotation2
	@TestInheritedAnnotation2
	private static class SingleSuperclass {

	}

	@TestAnnotation1
	private static class WithSingleSuperclass extends SingleSuperclass {

	}

	@TestAnnotation1
	private static class WithSingleInterface implements SingleInterface {

	}

	@TestAnnotation2
	@TestInheritedAnnotation2
	private static interface SingleInterface {

	}

	@TestAnnotation1
	private static class WithHierarchy extends HierarchySuperclass
			implements HierarchyInterface {

	}

	@TestAnnotation2
	@TestInheritedAnnotation2
	private static class HierarchySuperclass extends HierarchySuperSuperclass {

	}

	@TestAnnotation3
	private static class HierarchySuperSuperclass
			implements HierarchySuperSuperclassInterface {

	}

	@TestAnnotation4
	private static interface HierarchySuperSuperclassInterface {

	}

	@TestAnnotation5
	@TestInheritedAnnotation5
	private static interface HierarchyInterface extends HierarchyInterfaceInterface {

	}

	@TestAnnotation6
	private static interface HierarchyInterfaceInterface {

	}

	private static interface BridgeMethod<T> {

		@TestAnnotation2
		void method(T arg);

	}

	private static class WithBridgeMethod implements BridgeMethod<String> {

		@Override
		@TestAnnotation1
		public void method(String arg) {
		}

	}

	@OnSuperClass
	private static class WithOnSuperClass {

		@OnSuperClass
		public void method() {
		}

	}

	@OnInterface
	private static interface WithOnInterface {

		@OnInterface
		public void method();

	}

	@TestAnnotation1
	private static class WithSuperClassAndInterfaces extends WithOnSuperClass
			implements WithOnInterface {

		@TestAnnotation1
		public void method() {
		}

	}

}
