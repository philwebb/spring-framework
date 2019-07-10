/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationRegistries}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public class AnnotationRegistriesTests {

	@Before
	@After
	public void reset() {
		AnnotationRegistries.clear();
	}

	@Test
	public void requiresIntrospectionWhenNotJavaAnnotationAndPlainAnnotationsOnlyReturnsFalse() {
		AnnotationRegistries.add(throwingAnnotationRegistry());
		assertThat(AnnotationRegistries.requiresIntrospection(InputStream.class,
				Order.class)).isFalse();
	}

	@Test
	public void requiresIntrospectionWhenJavaAnnotationAndPlainAnnotationsOnlyReturnsTrue() {
		assertThat(AnnotationRegistries.requiresIntrospection(InputStream.class,
				Retention.class)).isTrue();
	}

	@Test
	public void requiresIntrospectionWhenNoRegistriesHaveBeenAddedReturnsTrue() {
		assertThat(AnnotationRegistries.requiresIntrospection(ExampleClass.class,
				Order.class)).isTrue();
	}

	@Test
	public void requiresIntrospectionWhenARegistryCanSkipReturnsFalse() {
		AnnotationRegistries.add(annotationRegistry(false));
		AnnotationRegistries.add(annotationRegistry(ExampleClass.class));
		assertThat(AnnotationRegistries.requiresIntrospection(ExampleClass.class,
				Order.class)).isFalse();
	}

	@Test
	public void requiresIntrospectionWhenNoRegistryCanSkipReturnsTrue() {
		AnnotationRegistries.add(annotationRegistry(false));
		AnnotationRegistries.add(annotationRegistry(false));
		assertThat(AnnotationRegistries.requiresIntrospection(ExampleClass.class,
				Order.class)).isTrue();
	}

	@Test
	public void requiresIntrospectionWhenRegistrySkipsClassAndInterfaceReturnsFalse() {
		AnnotationRegistries.add(annotationRegistry(ExampleClassWithInterface.class,
				ExampleInterface.class));
		assertThat(AnnotationRegistries.requiresIntrospection(
				ExampleClassWithInterface.class, Order.class)).isFalse();
	}

	@Test
	public void requiresIntrospectionWhenRegistrySkipsClassButNotInterfaceReturnsTrue() {
		AnnotationRegistries.add(annotationRegistry(ExampleClassWithInterface.class));
		assertThat(AnnotationRegistries.requiresIntrospection(
				ExampleClassWithInterface.class, Order.class)).isTrue();
	}

	@Test
	public void requiresIntrospectionWhenRegistrySkipsInterfaceButNotClassReturnsTrue() {
		AnnotationRegistries.add(annotationRegistry(ExampleInterface.class));
		assertThat(AnnotationRegistries.requiresIntrospection(
				ExampleClassWithInterface.class, Order.class)).isTrue();
	}

	@Test
	public void requiresIntrospectionWhenRegistrySkipsClassAndSubclassReturnsFalse() {
		AnnotationRegistries.add(
				annotationRegistry(ExampleSubClass.class, ExampleClass.class));
		assertThat(AnnotationRegistries.requiresIntrospection(ExampleSubClass.class,
				Order.class)).isFalse();
	}

	@Test
	public void requiresIntrospectionWhenRegistrySkipsSubclassOnlyReturnsTrue() {
		AnnotationRegistries.add(annotationRegistry(ExampleSubClass.class));
		assertThat(AnnotationRegistries.requiresIntrospection(ExampleSubClass.class,
				Order.class)).isTrue();
	}

	@Test
	public void requiresIntrospectionWhenRegistrySkipsClassAndNotSubclassReturnsFalse() {
		AnnotationRegistries.add(annotationRegistry(ExampleClass.class));
		assertThat(AnnotationRegistries.requiresIntrospection(ExampleSubClass.class,
				Order.class)).isTrue();
	}

	@Test
	public void requiresIntrospectionWhenRegistrySkipsClassSubclassAndInheritedInterfaceReturnsFalse() {
		AnnotationRegistries.add(
				annotationRegistry(ExampleSubClassWithInhertedInterface.class,
						ExampleClassWithInterface.class, ExampleInterface.class));
		assertThat(AnnotationRegistries.requiresIntrospection(
				ExampleSubClassWithInhertedInterface.class, Order.class)).isFalse();
	}

	@Test
	public void requiresIntrospectionWhenRegistrySkipsClassSubclassOnlyReturnsTrue() {
		AnnotationRegistries.add(
				annotationRegistry(ExampleSubClassWithInhertedInterface.class));
		assertThat(AnnotationRegistries.requiresIntrospection(
				ExampleSubClassWithInhertedInterface.class, Order.class)).isTrue();
	}

	private AnnotationRegistry throwingAnnotationRegistry() {
		return (annotationName, clazz) -> {
			throw new AssertionError("Unexpected call");
		};
	}

	private AnnotationRegistry annotationRegistry(boolean canSkip) {
		return (annotationName, clazz) -> canSkip;
	}

	public AnnotationRegistry annotationRegistry(Class<?>... skip) {
		return annotationRegistry(Arrays.asList(skip));
	}

	public AnnotationRegistry annotationRegistry(Collection<Class<?>> skip) {
		return (clazz, annotationName) -> skip.contains(clazz);
	}

	private static class ExampleClass {

	}

	private static interface ExampleInterface {

	}

	private static class ExampleClassWithInterface implements ExampleInterface {

	}

	private static class ExampleSubClass extends ExampleClass {

	}

	private static class ExampleSubClassWithInhertedInterface
			extends ExampleClassWithInterface {

	}

}
