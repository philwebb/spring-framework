/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.agent;

import java.lang.reflect.Proxy;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InstrumentedMethod}.
 *
 * @author Brian Clozel
 */
class InstrumentedMethodTests {

	private RuntimeHints hints = new RuntimeHints();


	@Nested
	class ClassReflectionInstrumentationTests {

		RecordedInvocation stringGetClasses = RecordedInvocation.of(InstrumentedMethod.CLASS_GETCLASSES)
				.onInstance(String.class).returnValue(String.class.getClasses()).build();

		RecordedInvocation stringGetDeclaredClasses = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDCLASSES)
				.onInstance(String.class).returnValue(String.class.getDeclaredClasses()).build();

		@Test
		void classForNameShouldMatchReflectionOnType() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_FORNAME)
					.withArgument("java.lang.String").returnValue(String.class).build();
			hints.reflection().register().forType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_FORNAME, invocation);
		}

		@Test
		void classGetClassesShouldNotMatchReflectionOnType() {
			hints.reflection().register().forType(String.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETCLASSES, this.stringGetClasses);
		}

		@Test
		void classGetClassesShouldMatchPublicClasses() {
			hints.reflection().registerPublicClasses().forType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCLASSES, this.stringGetClasses);
		}

		@Test
		void classGetClassesShouldMatchDeclaredClasses() {
			hints.reflection().registerDeclaredClasses().forType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCLASSES, this.stringGetClasses);
		}

		@Test
		void classGetDeclaredClassesShouldMatchDeclaredClassesHint() {
			hints.reflection().registerDeclaredClasses().forType(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCLASSES, this.stringGetDeclaredClasses);
		}

		@Test
		void classGetDeclaredClassesShouldNotMatchPublicClassesHint() {
			hints.reflection().registerPublicClasses().forType(String.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDCLASSES, this.stringGetDeclaredClasses);
		}

		@Test
		void classLoaderLoadClassShouldMatchReflectionHintOnType() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASSLOADER_LOADCLASS)
					.onInstance(ClassLoader.getSystemClassLoader())
					.withArgument(PublicField.class.getCanonicalName()).returnValue(PublicField.class).build();
			hints.reflection().register().forType(PublicField.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASSLOADER_LOADCLASS, invocation);
		}

	}

	@Nested
	class ConstructorReflectionInstrumentationTests {

		RecordedInvocation stringGetConstructor;

		RecordedInvocation stringGetConstructors = RecordedInvocation.of(InstrumentedMethod.CLASS_GETCONSTRUCTORS)
				.onInstance(String.class).returnValue(String.class.getConstructors()).build();

		RecordedInvocation stringGetDeclaredConstructor;

		RecordedInvocation stringGetDeclaredConstructors = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS)
				.onInstance(String.class).returnValue(String.class.getDeclaredConstructors()).build();

		@BeforeEach
		public void setup() throws Exception {
			this.stringGetConstructor = RecordedInvocation.of(InstrumentedMethod.CLASS_GETCONSTRUCTOR)
					.onInstance(String.class).withArgument(new Class[0]).returnValue(String.class.getConstructor()).build();
			this.stringGetDeclaredConstructor = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR)
					.onInstance(String.class).withArgument(new Class[] {byte[].class, byte.class})
					.returnValue(String.class.getDeclaredConstructor(byte[].class, byte.class)).build();
		}

		@Test
		void classGetConstructorShouldMatchInstrospectPublicConstructorsHint() {
			hints.reflection().registerIntrospect().forPublicConstructorsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
		}

		@Test
		void classGetConstructorShouldMatchInvokePublicConstructorsHint() {
			hints.reflection().registerInvoke().forPublicConstructorsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
		}

		@Test
		void classGetConstructorShouldMatchIntrospectDeclaredConstructorsHint() {
			hints.reflection().registerIntrospect().forDeclaredConstructorsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
		}

		@Test
		void classGetConstructorShouldMatchInvokeDeclaredConstructorsHint() {
			hints.reflection().registerInvoke().forDeclaredConstructorsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
		}

		@Test
		void classGetConstructorShouldMatchInstrospectConstructorHint() {
			hints.reflection().registerIntrospect().forConstructor(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
		}

		@Test
		void classGetConstructorShouldMatchInvokeConstructorHint() {
			hints.reflection().registerInvoke().forConstructor(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
		}

		@Test
		void classGetConstructorsShouldMatchIntrospectPublicConstructorsHint() {
			hints.reflection().registerIntrospect().forPublicConstructorsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
		}

		@Test
		void classGetConstructorsShouldMatchInvokePublicConstructorsHint() {
			hints.reflection().registerInvoke().forPublicConstructorsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
		}

		@Test
		void classGetConstructorsShouldMatchIntrospectDeclaredConstructorsHint() {
			hints.reflection().registerIntrospect().forDeclaredConstructorsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
		}

		@Test
		void classGetConstructorsShouldMatchInvokeDeclaredConstructorsHint() {
			hints.reflection().registerInvoke().forDeclaredConstructorsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
		}

		@Test
		void classGetDeclaredConstructorShouldMatchIntrospectDeclaredConstructorsHint() {
			hints.reflection().registerIntrospect().forDeclaredConstructorsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR, this.stringGetDeclaredConstructor);
		}

		@Test
		void classGetDeclaredConstructorShouldNotMatchIntrospectPublicConstructorsHint() {
			hints.reflection().registerIntrospect().forPublicConstructorsIn(String.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR, this.stringGetDeclaredConstructor);
		}

		@Test
		void classGetDeclaredConstructorShouldMatchInstrospectConstructorHint() {
			hints.reflection().registerIntrospect().forConstructor(String.class, byte[].class, byte.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR, this.stringGetDeclaredConstructor);
		}

		@Test
		void classGetDeclaredConstructorShouldMatchInvokeConstructorHint() {
			hints.reflection().registerInvoke().forConstructor(String.class, byte[].class, byte.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR, this.stringGetDeclaredConstructor);
		}

		@Test
		void classGetDeclaredConstructorsShouldMatchIntrospectDeclaredConstructorsHint() {
			hints.reflection().registerIntrospect().forDeclaredConstructorsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS, this.stringGetDeclaredConstructors);
		}

		@Test
		void classGetDeclaredConstructorsShouldMatchInvokeDeclaredConstructorsHint() {
			hints.reflection().registerInvoke().forDeclaredConstructorsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS, this.stringGetDeclaredConstructors);
		}

		@Test
		void classGetDeclaredConstructorsShouldNotMatchIntrospectPublicConstructorsHint() {
			hints.reflection().registerIntrospect().forPublicConstructorsIn(String.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS, this.stringGetDeclaredConstructors);
		}


		@Test
		void constructorNewInstanceShouldMatchInvokeHintOnConstructor() throws NoSuchMethodException {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CONSTRUCTOR_NEWINSTANCE)
					.onInstance(String.class.getConstructor()).returnValue("").build();
			hints.reflection().registerInvoke().forConstructor(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CONSTRUCTOR_NEWINSTANCE, invocation);
		}

		@Test
		void constructorNewInstanceShouldNotMatchIntrospectHintOnConstructor() throws NoSuchMethodException {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CONSTRUCTOR_NEWINSTANCE)
					.onInstance(String.class.getConstructor()).returnValue("").build();
			hints.reflection().registerIntrospect().forConstructor(String.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CONSTRUCTOR_NEWINSTANCE, invocation);
		}

	}

	@Nested
	class MethodReflectionInstrumentationTests {

		RecordedInvocation stringGetToStringMethod;

		RecordedInvocation stringGetScaleMethod;

		@BeforeEach
		void setup() throws Exception {
			this.stringGetToStringMethod = RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHOD)
					.onInstance(String.class).withArguments("toString", new Class[0])
					.returnValue(String.class.getMethod("toString")).build();
			this.stringGetScaleMethod = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDMETHOD)
					.onInstance(String.class).withArguments("scale", new Class[] {int.class, float.class})
					.returnValue(String.class.getDeclaredMethod("scale", int.class, float.class)).build();
		}

		@Test
		void classGetDeclaredMethodShouldMatchIntrospectDeclaredMethodsHint() throws NoSuchMethodException {
			hints.reflection().registerIntrospect().forDeclaredMethodsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDMETHOD, this.stringGetScaleMethod);
		}

		@Test
		void classGetDeclaredMethodShouldNotMatchIntrospectPublicMethodsHint() throws NoSuchMethodException {
			hints.reflection().registerIntrospect().forPublicMethodsIn(String.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDMETHOD, this.stringGetScaleMethod);
		}

		@Test
		void classGetDeclaredMethodShouldMatchIntrospectMethodHint() throws NoSuchMethodException {
			hints.reflection().registerIntrospect().forMethod(String.class, "scale", int.class, float.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDMETHOD, this.stringGetScaleMethod);
		}

		@Test
		void classGetDeclaredMethodShouldMatchInvokeMethodHint() throws NoSuchMethodException {
			hints.reflection().registerInvoke().forMethod(String.class, "scale", int.class, float.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDMETHOD, this.stringGetScaleMethod);
		}

		@Test
		void classGetDeclaredMethodsShouldMatchIntrospectDeclaredMethodsHint() {
			hints.reflection().registerIntrospect().forDeclaredMethodsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDMETHODS, this.stringGetScaleMethod);
		}

		@Test
		void classGetDeclaredMethodsShouldMatchInvokeDeclaredMethodsHint() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDMETHODS).onInstance(String.class).build();
			hints.reflection().registerInvoke().forDeclaredMethodsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDMETHODS, invocation);
		}

		@Test
		void classGetDeclaredMethodsShouldMatchIntrospectPublicMethodsHint() {
			hints.reflection().registerIntrospect().forPublicMethodsIn(String.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDMETHODS, this.stringGetScaleMethod);
		}

		@Test
		void classGetMethodsShouldMatchInstrospectDeclaredMethodsHint() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHODS).onInstance(String.class).build();
			hints.reflection().registerIntrospect().forDeclaredMethodsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHODS, invocation);
		}

		@Test
		void classGetMethodsShouldMatchInstrospectPublicMethodsHint() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHODS).onInstance(String.class).build();
			hints.reflection().registerIntrospect().forPublicMethodsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHODS, invocation);
		}

		@Test
		void classGetMethodsShouldMatchInvokeDeclaredMethodsHint() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHODS).onInstance(String.class).build();
			hints.reflection().registerInvoke().forDeclaredMethodsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHODS, invocation);
		}

		@Test
		void classGetMethodsShouldMatchInvokePublicMethodsHint() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHODS).onInstance(String.class).build();
			hints.reflection().registerInvoke().forPublicMethodsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHODS, invocation);
		}

		@Test
		void classGetMethodsShouldNotMatchForWrongType() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHODS).onInstance(String.class).build();
			hints.reflection().registerIntrospect().forPublicMethodsIn(Integer.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETMETHODS, invocation);
		}

		@Test
		void classGetMethodShouldMatchInstrospectPublicMethodsHint() throws NoSuchMethodException {
			hints.reflection().registerIntrospect().forPublicMethodsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
		}

		@Test
		void classGetMethodShouldMatchInvokePublicMethodsHint() throws NoSuchMethodException {
			hints.reflection().registerInvoke().forPublicMethodsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
		}

		@Test
		void classGetMethodShouldMatchInstrospectDeclaredMethodsHint() throws NoSuchMethodException {
			hints.reflection().registerIntrospect().forDeclaredMethodsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
		}

		@Test
		void classGetMethodShouldMatchInvokeDeclaredMethodsHint() {
			hints.reflection().registerInvoke().forDeclaredMethodsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
		}

		@Test
		void classGetMethodShouldMatchIntrospectMethodHint() {
			hints.reflection().registerIntrospect().forMethod(String.class, "toString");
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
		}

		@Test
		void classGetMethodShouldMatchInvokeMethodHint() throws Exception {
			hints.reflection().registerInvoke().forMethod(String.class, "toString");
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
		}

		@Test
		void classGetMethodShouldNotMatchInstrospectPublicMethodsHintWhenPrivate() throws Exception {
			hints.reflection().registerIntrospect().forPublicMethodsIn(String.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetScaleMethod);
		}

		@Test
		void classGetMethodShouldMatchInstrospectDeclaredMethodsHintWhenPrivate() {
			hints.reflection().registerIntrospect().forDeclaredMethodsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetScaleMethod);
		}

		@Test
		void classGetMethodShouldNotMatchForWrongType() {
			hints.reflection().registerIntrospect().forPublicMethodsIn(Integer.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
		}

		@Test
		void methodInvokeShouldMatchInvokeHintOnMethod() throws NoSuchMethodException {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.METHOD_INVOKE)
					.onInstance(String.class.getMethod("startsWith", String.class)).withArguments("testString", new Object[] {"test"}).build();
			hints.reflection().registerInvoke().forMethod(String.class, "startsWith", String.class);
			assertThatInvocationMatches(InstrumentedMethod.METHOD_INVOKE, invocation);
		}

		@Test
		void methodInvokeShouldNotMatchIntrospectHintOnMethod() throws NoSuchMethodException {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.METHOD_INVOKE)
					.onInstance(String.class.getMethod("toString")).withArguments("", new Object[0]).build();
			hints.reflection().registerIntrospect().forMethod(String.class, "toString");
			assertThatInvocationDoesNotMatch(InstrumentedMethod.METHOD_INVOKE, invocation);
		}

	}

	@Nested
	class FieldReflectionInstrumentationTests {

		RecordedInvocation getPublicField;

		RecordedInvocation stringGetDeclaredField;

		@BeforeEach
		void setup() throws Exception {
			this.getPublicField = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELD)
					.onInstance(PublicField.class).withArgument("field")
					.returnValue(PublicField.class.getField("field")).build();
			this.stringGetDeclaredField = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDFIELD)
					.onInstance(String.class).withArgument("value").returnValue(String.class.getDeclaredField("value")).build();
		}

		@Test
		void classGetDeclaredFieldShouldMatchDeclaredFieldsHint() {
			hints.reflection().registerRead().forDeclaredFieldsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDFIELD, this.stringGetDeclaredField);
		}

		@Test
		void classGetDeclaredFieldShouldNotMatchPublicFieldsHint() {
			hints.reflection().registerRead().forPublicFieldsIn(String.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDFIELD, this.stringGetDeclaredField);
		}

		@Test
		void classGetDeclaredFieldShouldMatchFieldHint() {
			hints.reflection().registerRead().forField(String.class, "value");
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDFIELD, this.stringGetDeclaredField);
		}

		@Test
		void classGetDeclaredFieldsShouldMatchDeclaredFieldsHint() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDFIELDS).onInstance(String.class).build();
			hints.reflection().registerRead().forDeclaredFieldsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDFIELDS, invocation);
		}

		@Test
		void classGetDeclaredFieldsShouldNotMatchPublicFieldsHint() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDFIELDS).onInstance(String.class).build();
			hints.reflection().registerRead().forPublicFieldsIn(String.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDFIELDS, invocation);
		}

		@Test
		void classGetFieldShouldMatchPublicFieldsHint() {
			hints.reflection().registerRead().forPublicFieldsIn(PublicField.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELD, this.getPublicField);
		}

		@Test
		void classGetFieldShouldMatchDeclaredFieldsHint() {
			hints.reflection().registerRead().forDeclaredFieldsIn(PublicField.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELD, this.getPublicField);
		}

		@Test
		void classGetFieldShouldMatchFieldHint() {
			hints.reflection().registerRead().forField(PublicField.class, "field");
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELD, this.getPublicField);
		}

		@Test
		void classGetFieldShouldNotMatchPublicFieldsHintWhenPrivate() throws NoSuchFieldException {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELD)
					.onInstance(String.class).withArgument("value").returnValue(null).build();
			hints.reflection().registerRead().forPublicFieldsIn(String.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETFIELD, invocation);
		}

		@Test
		void classGetFieldShouldMatchDeclaredFieldsHintWhenPrivate() throws NoSuchFieldException {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELD)
					.onInstance(String.class).withArgument("value").returnValue(String.class.getDeclaredField("value")).build();
			hints.reflection().registerRead().forDeclaredFieldsIn(String.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELD, invocation);
		}

		@Test
		void classGetFieldShouldNotMatchForWrongType() throws Exception {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELD)
					.onInstance(String.class).withArgument("value").returnValue(null).build();
			hints.reflection().registerRead().forDeclaredFieldsIn(Integer.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETFIELD, invocation);
		}

		@Test
		void classGetFieldsShouldMatchPublicFieldsHint() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELDS)
					.onInstance(PublicField.class).build();
			hints.reflection().registerRead().forPublicFieldsIn(PublicField.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELDS, invocation);
		}

		@Test
		void classGetFieldsShouldMatchDeclaredFieldsHint() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELDS)
					.onInstance(PublicField.class).build();
			hints.reflection().registerRead().forDeclaredFieldsIn(PublicField.class);
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELDS, invocation);
		}

	}


	@Nested
	class ResourcesInstrumentationTests {

		@Test
		void resourceBundleGetBundleShouldMatchBundleNameHint() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE)
					.withArgument("bundleName").build();
			hints.resources().registerBundle().forBaseName("bundleName");
			assertThatInvocationMatches(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE, invocation);
		}

		@Test
		void resourceBundleGetBundleShouldNotMatchBundleNameHintWhenWrongName() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE)
					.withArgument("bundleName").build();
			hints.resources().registerBundle().forBaseName("wrongBundleName");
			assertThatInvocationDoesNotMatch(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE, invocation);
		}

		@Test
		void classGetResourceShouldMatchResourcePatternWhenAbsolute() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCE)
					.onInstance(InstrumentedMethodTests.class).withArgument("/some/path/resource.txt").build();
			hints.resources().registerInclude().forPattern("/some/*");
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETRESOURCE, invocation);
		}

		@Test
		void classGetResourceShouldMatchResourcePatternWhenRelative() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCE)
					.onInstance(InstrumentedMethodTests.class).withArgument("resource.txt").build();
			hints.resources().registerInclude().forPattern("/org/springframework/aot/agent/*");
			assertThatInvocationMatches(InstrumentedMethod.CLASS_GETRESOURCE, invocation);
		}

		@Test
		void classGetResourceShouldNotMatchResourcePatternWhenInvalid() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCE)
					.onInstance(InstrumentedMethodTests.class).withArgument("/some/path/resource.txt").build();
			hints.resources().registerInclude().forPattern("/other/*");
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETRESOURCE, invocation);
		}

		@Test
		void classGetResourceShouldNotMatchResourcePatternWhenExcluded() {
			RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCE)
					.onInstance(InstrumentedMethodTests.class).withArgument("/some/path/resource.txt").build();
			hints.resources().registerInclude().forPattern("/some/*");
			hints.resources().registerExclude().forPattern("/some/path/*");
			assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETRESOURCE, invocation);
		}

	}


	@Nested
	class ProxiesInstrumentationTests {

		RecordedInvocation newProxyInstance;

		@BeforeEach
		void setup() {
			this.newProxyInstance = RecordedInvocation.of(InstrumentedMethod.PROXY_NEWPROXYINSTANCE)
					.withArguments(ClassLoader.getSystemClassLoader(), new Class[] {AutoCloseable.class, Comparator.class}, null)
					.returnValue(Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[] {AutoCloseable.class, Comparator.class}, (proxy, method, args) -> null))
					.build();
		}

		@Test
		void proxyNewProxyInstanceShouldMatchWhenInterfacesMatch() {
			hints.proxies().registerJavaProxy().forInterfaces(AutoCloseable.class, Comparator.class);
			assertThatInvocationMatches(InstrumentedMethod.PROXY_NEWPROXYINSTANCE, this.newProxyInstance);
		}

		@Test
		void proxyNewProxyInstanceShouldNotMatchWhenInterfacesDoNotMatch() {
			hints.proxies().registerJavaProxy().forInterfaces(Comparator.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.PROXY_NEWPROXYINSTANCE, this.newProxyInstance);
		}

		@Test
		void proxyNewProxyInstanceShouldNotMatchWhenWrongOrder() {
			hints.proxies().registerJavaProxy().forInterfaces(Comparator.class, AutoCloseable.class);
			assertThatInvocationDoesNotMatch(InstrumentedMethod.PROXY_NEWPROXYINSTANCE, this.newProxyInstance);
		}
	}


	private void assertThatInvocationMatches(InstrumentedMethod method, RecordedInvocation invocation) {
		assertThat(method.matcher(invocation)).accepts(this.hints);
	}

	private void assertThatInvocationDoesNotMatch(InstrumentedMethod method, RecordedInvocation invocation) {
		assertThat(method.matcher(invocation)).rejects(this.hints);
	}


	static class PublicField {

		public String field;

	}

}
