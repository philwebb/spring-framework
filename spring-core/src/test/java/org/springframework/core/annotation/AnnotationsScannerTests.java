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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
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
	public void iteratorWhenDirectScanOnClassFindsAnnotations() {
		Class<?> source = WithExampleAndOther.class;
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("Example",
				"Other");
	}

	@Test
	public void iteratorWhenDirectScanOnMethodFindsAnnotations() throws Exception {
		Method source = WithExampleAndOther.class.getDeclaredMethod("method");
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("Example",
				"Other");
	}

	@Test
	public void iterateWhenDirectScanOnBridgeMethodFindsAnnotations() throws Exception {
		Method source = WithBridgeMethod.class.getDeclaredMethod("method", Object.class);
		assertThat(source.isBridge()).isTrue();
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("Example");
	}

	@Test
	public void iterateWhenDirectScanOnBridgedMethodFindsAnnotations() throws Exception {
		Method source = WithBridgeMethod.class.getDeclaredMethod("method", String.class);
		assertThat(source.isBridge()).isFalse();
		assertThat(scan(source, SearchStrategy.DIRECT)).containsExactly("Example");
	}

	@Test
	public void iterateWhenExhaustiveScanOnClassFindsAnnotations() {
		Class<?> source = WithSuperClassAndInterfaces.class;
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly("Example",
				"^OnSuperClass", "^OnInterface");
	}

	@Test
	public void iterateWhenExhaustiveScanOnMethodFindsAnnotations() throws Exception {
		Method source = WithSuperClassAndInterfaces.class.getDeclaredMethod("method");
		assertThat(scan(source, SearchStrategy.EXHAUSTIVE)).containsExactly("Example",
				"^OnSuperClass", "^OnInterface");
	}

	// FIXME more tests

	private Stream<String> scan(AnnotatedElement source, SearchStrategy searchStrategy) {
		AnnotationsScanner scanner = new AnnotationsScanner(
				source, searchStrategy);
		return stream(scanner).flatMap(new Mapper());
	}

	private Stream<DeclaredAnnotations> stream(
			AnnotationsScanner scanner) {
		return StreamSupport.stream(scanner.spliterator(), false);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Example {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface Other {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface OnSuperClass {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface OnInterface {

	}

	@Example
	@Other
	static abstract class WithExampleAndOther {

		@Example
		@Other
		void method() {
		}

	}

	static interface BridgeMethod<T> {

		@Other
		void method(T arg);

	}

	static class WithBridgeMethod implements BridgeMethod<String> {

		@Override
		@Example
		public void method(String arg) {
		}

	}

	@OnSuperClass
	static class WithOnSuperClass {

		@OnSuperClass
		public void method() {
		}

	}

	@OnInterface
	static interface WithOnInterface {

		@OnInterface
		public void method();

	}

	@Example
	static class WithSuperClassAndInterfaces extends WithOnSuperClass
			implements WithOnInterface {

		@Example
		public void method() {
		}

	}

	static class Mapper implements Function<DeclaredAnnotations, Stream<String>> {

		private boolean inherited = false;

		@Override
		public Stream<String> apply(DeclaredAnnotations annotations) {
			String prefix = this.inherited ? "^" : "";
			this.inherited = true;
			return StreamSupport.stream(annotations.spliterator(), false).map(
					annotation -> {
						String name = ClassUtils.getShortName(annotation.getClassName());
						name = name.substring(name.lastIndexOf(".") + 1);
						name = prefix + name;
						return name;
					});
		}

	}

}
