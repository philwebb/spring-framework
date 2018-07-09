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

package org.springframework.core.annotation4;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation4.dunno.AnnotatedSource;
import org.springframework.core.annotation4.dunno.StandardAnnotatedSource;
import org.springframework.core.annotation4.dunno.StandardAnnotatedSource.StandardClassAnnotatedSource;
import org.springframework.core.annotation4.dunno.StandardAnnotatedSource.StandardMethodAnnotatedSource;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StandardAnnotatedSource}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class StandardAnnotatedSourceTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenSourceIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Source must not be null");
		new StandardAnnotatedSource<>(null);
	}

	@Test
	public void getDeclaredAnnotationsShouldAdaptAnnotatedElement() {
		StandardAnnotatedSource<Parameter> source = new StandardAnnotatedSource<>(
				getExampleParameterElement());
		assertHasDeclaredAnnotation(source, ExampleAnnotation.class);
	}

	@Test
	public void getRelatedSuperClassAnnotationSourceWhenNotClassOrMethodReturnsNull() {
		StandardAnnotatedSource<Parameter> source = new StandardAnnotatedSource<>(
				getExampleParameterElement());
		assertThat(source.getRelatedSuperClassAnnotationSource()).isNull();
	}

	@Test
	public void getRelatedInterfaceAnnotationSourcesWhenNotClassOrMethodReturnsEmpty() {
		StandardAnnotatedSource<Parameter> source = new StandardAnnotatedSource<>(
				getExampleParameterElement());
		assertThat(source.getRelatedInterfaceAnnotationSources()).isEmpty();
	}

	@Test
	public void getWhenClassReturnsStandardClassAnnotatedSource() {
		AnnotatedElement source = ExampleClass.class;
		assertThat(StandardAnnotatedSource.get(source)).isExactlyInstanceOf(
				StandardClassAnnotatedSource.class);
	}

	@Test
	public void getWhenMethodReturnsStandardMethodAnnotatedSource() {
		AnnotatedElement source = ReflectionUtils.findMethod(ExampleOnParameter.class,
				"myMethod", String.class);
		assertThat(StandardAnnotatedSource.get(source)).isExactlyInstanceOf(
				StandardMethodAnnotatedSource.class);
	}

	@Test
	public void getWhenOtherReturnsStandardAnnotatedSource() {
		AnnotatedElement source = getExampleParameterElement();
		assertThat(StandardAnnotatedSource.get(source)).isExactlyInstanceOf(
				StandardAnnotatedSource.class);
	}

	private Parameter getExampleParameterElement() {
		Method method = ReflectionUtils.findMethod(ExampleOnParameter.class, "myMethod",
				String.class);
		return method.getParameters()[0];
	}

	@Test
	public void standardClassAnnotationSourceGetRelatedSuperClassAnnotationSourceOnObject() {
		StandardClassAnnotatedSource source = new StandardClassAnnotatedSource(
				Object.class);
		assertThat(source.getRelatedSuperClassAnnotationSource()).isNull();
	}

	@Test
	public void standardClassAnnotationSourceGetRelatedSuperClassAnnotationSourceFindSuperClass() {
		StandardClassAnnotatedSource source = new StandardClassAnnotatedSource(
				ExampleClass.class);
		AnnotatedSource parent = source.getRelatedSuperClassAnnotationSource();
		assertHasDeclaredAnnotation(parent, ExampleAnnotationOnBase.class);
		assertThat(parent.getRelatedSuperClassAnnotationSource()).isNull();
	}

	@Test
	public void standardClassAnnotationSourceGetRelatedInterfaceAnnotationSources() {
		StandardClassAnnotatedSource source = new StandardClassAnnotatedSource(
				ExampleClass.class);
		List<AnnotatedSource> related = asList(
				source.getRelatedInterfaceAnnotationSources().iterator());
		assertThat(related).hasSize(1);
		assertHasDeclaredAnnotation(related.get(0), ExampleAnnotationOnInterface.class);
	}

	@Test
	public void standardMethodAnnotationSourceGetRelatedSuperClassAnnotationSourceOnNonOverriddenMethod() {
		StandardMethodAnnotatedSource source = new StandardMethodAnnotatedSource(
				ReflectionUtils.findMethod(Object.class, "toString"));
		assertThat(source.getRelatedSuperClassAnnotationSource()).isNull();
	}

	@Test
	public void standardMethodAnnotationSourceGetRelatedSuperClassAnnotationSourceFindsRelatedMethod() {
		StandardMethodAnnotatedSource source = new StandardMethodAnnotatedSource(
				getExampleMethodElement());
		AnnotatedSource parent = source.getRelatedSuperClassAnnotationSource();
		assertHasDeclaredAnnotation(parent, ExampleAnnotationOnBase.class);
	}

	@Test
	public void standardMethodAnnotationSourceGetRelatedInterfaceAnnotationSourcesFindsRelatedMethods() {
		StandardMethodAnnotatedSource source = new StandardMethodAnnotatedSource(
				getExampleMethodElement());
		List<AnnotatedSource> related = asList(
				source.getRelatedInterfaceAnnotationSources().iterator());
		assertThat(related).hasSize(1);
		assertHasDeclaredAnnotation(related.get(0), ExampleAnnotationOnInterface.class);
	}

	private Method getExampleMethodElement() {
		return ReflectionUtils.findMethod(ExampleMethod.class, "myMethod",
				String.class);
	}

	@Test
	public void standardMethodAnnotationSourceGetRelatedSuperClassAnnotationSourceFindsRelatedMethodWithGeneric() {
		StandardMethodAnnotatedSource source = new StandardMethodAnnotatedSource(
				ReflectionUtils.findMethod(ExampleGenericClass.class, "myMethod",
						String.class));
		AnnotatedSource parent = source.getRelatedSuperClassAnnotationSource();
		assertHasDeclaredAnnotation(parent, ExampleAnnotation.class);
	}

	@Test
	public void standardMethodAnnotationSourceGetRelatedInterfaceAnnotationSourcesFindsRelatedMethodWithGeneric() {
		StandardMethodAnnotatedSource source = new StandardMethodAnnotatedSource(
				ReflectionUtils.findMethod(ExampleGenericClass.class, "myOtherMethod",
						Integer.class));
		List<AnnotatedSource> related = asList(
				source.getRelatedInterfaceAnnotationSources().iterator());
		assertThat(related).hasSize(1);
		assertHasDeclaredAnnotation(related.get(0), ExampleAnnotationOnInterface.class);
	}

	@Test
	public void standardMethodAnnotationSourceGetDeclaredAnnotationsOnBridgeMethod()
			throws NoSuchMethodException {
		Method bridgeMethod = ExampleAnotherGenericClass.class.getMethod("myMethod", Object.class);
		assertThat(bridgeMethod.isBridge()).isTrue();
		StandardMethodAnnotatedSource source = new StandardMethodAnnotatedSource(
				bridgeMethod);
		assertHasDeclaredAnnotation(source, ExampleAnnotation.class);
	}

	private void assertHasDeclaredAnnotation(AnnotatedSource source,
			Class<?> annotationType) {
		Iterable<DeclaredAnnotation> declaredAnnotations = source.getDeclaredAnnotations();
		boolean hasAnnotation = StreamSupport.stream(declaredAnnotations.spliterator(),
				false).map(DeclaredAnnotation::getClassName).anyMatch(
				annotationType.getName()::equals);
		assertThat(hasAnnotation).as(
				source + " containing " + annotationType.getName()).isTrue();
	}

	private <T> List<T> asList(Iterator<T> iterator) {
		List<T> result = new ArrayList<>();
		iterator.forEachRemaining(result::add);
		return result;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ExampleAnnotationOnBase {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ExampleAnnotationOnInterface {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface ExampleAnnotation {

	}

	@ExampleAnnotationOnBase
	static class ExampleClassBase {

	}

	@ExampleAnnotationOnInterface
	interface ExampleInterface {

	}

	interface ExampleGenericInterface<T> {

		@ExampleAnnotationOnInterface
		void myOtherMethod(T arg);

	}

	interface ExampleAnotherGenericInterface<T> {

		void myMethod(T arg);

	}

	static abstract class ExampleGenericClassBase<T> {

		@ExampleAnnotation
		public abstract void myMethod(T arg);

	}

	@ExampleAnnotation
	static class ExampleClass extends ExampleClassBase implements ExampleInterface {

	}

	static class ExampleMethodBase {

		@ExampleAnnotationOnBase
		String myMethod(String arg) {
			return "test";
		}

	}

	interface ExampleMethodInterface {

		@ExampleAnnotationOnInterface
		String myMethod(String arg);

	}

	static class ExampleMethod extends ExampleMethodBase
			implements ExampleMethodInterface {

		@Override
		@ExampleAnnotation
		public String myMethod(String arg) {
			return super.myMethod(arg);
		}

	}

	interface ExampleOnParameter {

		String myMethod(@ExampleAnnotation String param);

	}

	static class ExampleGenericClass extends ExampleGenericClassBase<String>
			implements ExampleGenericInterface<Integer> {

		@Override
		public void myOtherMethod(Integer arg) {

		}

		@Override
		public void myMethod(String arg) {

		}

	}

	static class ExampleAnotherGenericClass
			implements ExampleAnotherGenericInterface<String> {

		@Override
		@ExampleAnnotation
		public void myMethod(String arg) {

		}
	}

}
