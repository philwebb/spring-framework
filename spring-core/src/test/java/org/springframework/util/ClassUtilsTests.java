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

package org.springframework.util;

import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.springframework.tests.sample.objects.DerivedTestObject;
import org.springframework.tests.sample.objects.ITestInterface;
import org.springframework.tests.sample.objects.ITestObject;
import org.springframework.tests.sample.objects.TestObject;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;

/**
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rick Evans
 */
public class ClassUtilsTests {

	private ClassLoader classLoader = getClass().getClassLoader();


	@Before
	public void clearStatics() {
		InnerClass.noArgCalled = false;
		InnerClass.argCalled = false;
		InnerClass.overloadedCalled = false;
	}


	@Test
	public void testIsPresent() {
		assertThat(ClassUtils.isPresent("java.lang.String", classLoader)).isTrue();
		assertThat(ClassUtils.isPresent("java.lang.MySpecialString", classLoader)).isFalse();
	}

	@Test
	public void testForName() throws ClassNotFoundException {
		assertThat((Object) ClassUtils.forName("java.lang.String", classLoader)).isEqualTo(String.class);
		assertThat((Object) ClassUtils.forName("java.lang.String[]", classLoader)).isEqualTo(String[].class);
		assertThat((Object) ClassUtils.forName(String[].class.getName(), classLoader)).isEqualTo(String[].class);
		assertThat((Object) ClassUtils.forName(String[][].class.getName(), classLoader)).isEqualTo(String[][].class);
		assertThat((Object) ClassUtils.forName(String[][][].class.getName(), classLoader)).isEqualTo(String[][][].class);
		assertThat((Object) ClassUtils.forName("org.springframework.tests.sample.objects.TestObject", classLoader)).isEqualTo(TestObject.class);
		assertThat((Object) ClassUtils.forName("org.springframework.tests.sample.objects.TestObject[]", classLoader)).isEqualTo(TestObject[].class);
		assertThat((Object) ClassUtils.forName(TestObject[].class.getName(), classLoader)).isEqualTo(TestObject[].class);
		assertThat((Object) ClassUtils.forName("org.springframework.tests.sample.objects.TestObject[][]", classLoader)).isEqualTo(TestObject[][].class);
		assertThat((Object) ClassUtils.forName(TestObject[][].class.getName(), classLoader)).isEqualTo(TestObject[][].class);
		assertThat((Object) ClassUtils.forName("[[[S", classLoader)).isEqualTo(short[][][].class);
	}

	@Test
	public void testForNameWithPrimitiveClasses() throws ClassNotFoundException {
		assertThat((Object) ClassUtils.forName("boolean", classLoader)).isEqualTo(boolean.class);
		assertThat((Object) ClassUtils.forName("byte", classLoader)).isEqualTo(byte.class);
		assertThat((Object) ClassUtils.forName("char", classLoader)).isEqualTo(char.class);
		assertThat((Object) ClassUtils.forName("short", classLoader)).isEqualTo(short.class);
		assertThat((Object) ClassUtils.forName("int", classLoader)).isEqualTo(int.class);
		assertThat((Object) ClassUtils.forName("long", classLoader)).isEqualTo(long.class);
		assertThat((Object) ClassUtils.forName("float", classLoader)).isEqualTo(float.class);
		assertThat((Object) ClassUtils.forName("double", classLoader)).isEqualTo(double.class);
		assertThat((Object) ClassUtils.forName("void", classLoader)).isEqualTo(void.class);
	}

	@Test
	public void testForNameWithPrimitiveArrays() throws ClassNotFoundException {
		assertThat((Object) ClassUtils.forName("boolean[]", classLoader)).isEqualTo(boolean[].class);
		assertThat((Object) ClassUtils.forName("byte[]", classLoader)).isEqualTo(byte[].class);
		assertThat((Object) ClassUtils.forName("char[]", classLoader)).isEqualTo(char[].class);
		assertThat((Object) ClassUtils.forName("short[]", classLoader)).isEqualTo(short[].class);
		assertThat((Object) ClassUtils.forName("int[]", classLoader)).isEqualTo(int[].class);
		assertThat((Object) ClassUtils.forName("long[]", classLoader)).isEqualTo(long[].class);
		assertThat((Object) ClassUtils.forName("float[]", classLoader)).isEqualTo(float[].class);
		assertThat((Object) ClassUtils.forName("double[]", classLoader)).isEqualTo(double[].class);
	}

	@Test
	public void testForNameWithPrimitiveArraysInternalName() throws ClassNotFoundException {
		assertThat((Object) ClassUtils.forName(boolean[].class.getName(), classLoader)).isEqualTo(boolean[].class);
		assertThat((Object) ClassUtils.forName(byte[].class.getName(), classLoader)).isEqualTo(byte[].class);
		assertThat((Object) ClassUtils.forName(char[].class.getName(), classLoader)).isEqualTo(char[].class);
		assertThat((Object) ClassUtils.forName(short[].class.getName(), classLoader)).isEqualTo(short[].class);
		assertThat((Object) ClassUtils.forName(int[].class.getName(), classLoader)).isEqualTo(int[].class);
		assertThat((Object) ClassUtils.forName(long[].class.getName(), classLoader)).isEqualTo(long[].class);
		assertThat((Object) ClassUtils.forName(float[].class.getName(), classLoader)).isEqualTo(float[].class);
		assertThat((Object) ClassUtils.forName(double[].class.getName(), classLoader)).isEqualTo(double[].class);
	}

	@Test
	public void testIsCacheSafe() {
		ClassLoader childLoader1 = new ClassLoader(classLoader) {};
		ClassLoader childLoader2 = new ClassLoader(classLoader) {};
		ClassLoader childLoader3 = new ClassLoader(classLoader) {
			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				return childLoader1.loadClass(name);
			}
		};
		Class<?> composite = ClassUtils.createCompositeInterface(
				new Class<?>[] {Serializable.class, Externalizable.class}, childLoader1);

		assertThat(ClassUtils.isCacheSafe(String.class, null)).isTrue();
		assertThat(ClassUtils.isCacheSafe(String.class, classLoader)).isTrue();
		assertThat(ClassUtils.isCacheSafe(String.class, childLoader1)).isTrue();
		assertThat(ClassUtils.isCacheSafe(String.class, childLoader2)).isTrue();
		assertThat(ClassUtils.isCacheSafe(String.class, childLoader3)).isTrue();
		assertThat(ClassUtils.isCacheSafe(InnerClass.class, null)).isFalse();
		assertThat(ClassUtils.isCacheSafe(InnerClass.class, classLoader)).isTrue();
		assertThat(ClassUtils.isCacheSafe(InnerClass.class, childLoader1)).isTrue();
		assertThat(ClassUtils.isCacheSafe(InnerClass.class, childLoader2)).isTrue();
		assertThat(ClassUtils.isCacheSafe(InnerClass.class, childLoader3)).isTrue();
		assertThat(ClassUtils.isCacheSafe(composite, null)).isFalse();
		assertThat(ClassUtils.isCacheSafe(composite, classLoader)).isFalse();
		assertThat(ClassUtils.isCacheSafe(composite, childLoader1)).isTrue();
		assertThat(ClassUtils.isCacheSafe(composite, childLoader2)).isFalse();
		assertThat(ClassUtils.isCacheSafe(composite, childLoader3)).isTrue();
	}

	@Test
	public void testGetShortName() {
		String className = ClassUtils.getShortName(getClass());
		assertThat((Object) className).as("Class name did not match").isEqualTo("ClassUtilsTests");
	}

	@Test
	public void testGetShortNameForObjectArrayClass() {
		String className = ClassUtils.getShortName(Object[].class);
		assertThat((Object) className).as("Class name did not match").isEqualTo("Object[]");
	}

	@Test
	public void testGetShortNameForMultiDimensionalObjectArrayClass() {
		String className = ClassUtils.getShortName(Object[][].class);
		assertThat((Object) className).as("Class name did not match").isEqualTo("Object[][]");
	}

	@Test
	public void testGetShortNameForPrimitiveArrayClass() {
		String className = ClassUtils.getShortName(byte[].class);
		assertThat((Object) className).as("Class name did not match").isEqualTo("byte[]");
	}

	@Test
	public void testGetShortNameForMultiDimensionalPrimitiveArrayClass() {
		String className = ClassUtils.getShortName(byte[][][].class);
		assertThat((Object) className).as("Class name did not match").isEqualTo("byte[][][]");
	}

	@Test
	public void testGetShortNameForInnerClass() {
		String className = ClassUtils.getShortName(InnerClass.class);
		assertThat((Object) className).as("Class name did not match").isEqualTo("ClassUtilsTests.InnerClass");
	}

	@Test
	public void testGetShortNameAsProperty() {
		String shortName = ClassUtils.getShortNameAsProperty(this.getClass());
		assertThat((Object) shortName).as("Class name did not match").isEqualTo("classUtilsTests");
	}

	@Test
	public void testGetClassFileName() {
		assertThat((Object) ClassUtils.getClassFileName(String.class)).isEqualTo("String.class");
		assertThat((Object) ClassUtils.getClassFileName(getClass())).isEqualTo("ClassUtilsTests.class");
	}

	@Test
	public void testGetPackageName() {
		assertThat((Object) ClassUtils.getPackageName(String.class)).isEqualTo("java.lang");
		assertThat((Object) ClassUtils.getPackageName(getClass())).isEqualTo(getClass().getPackage().getName());
	}

	@Test
	public void testGetQualifiedName() {
		String className = ClassUtils.getQualifiedName(getClass());
		assertThat((Object) className).as("Class name did not match").isEqualTo("org.springframework.util.ClassUtilsTests");
	}

	@Test
	public void testGetQualifiedNameForObjectArrayClass() {
		String className = ClassUtils.getQualifiedName(Object[].class);
		assertThat((Object) className).as("Class name did not match").isEqualTo("java.lang.Object[]");
	}

	@Test
	public void testGetQualifiedNameForMultiDimensionalObjectArrayClass() {
		String className = ClassUtils.getQualifiedName(Object[][].class);
		assertThat((Object) className).as("Class name did not match").isEqualTo("java.lang.Object[][]");
	}

	@Test
	public void testGetQualifiedNameForPrimitiveArrayClass() {
		String className = ClassUtils.getQualifiedName(byte[].class);
		assertThat((Object) className).as("Class name did not match").isEqualTo("byte[]");
	}

	@Test
	public void testGetQualifiedNameForMultiDimensionalPrimitiveArrayClass() {
		String className = ClassUtils.getQualifiedName(byte[][].class);
		assertThat((Object) className).as("Class name did not match").isEqualTo("byte[][]");
	}

	@Test
	public void testHasMethod() {
		assertThat(ClassUtils.hasMethod(Collection.class, "size")).isTrue();
		assertThat(ClassUtils.hasMethod(Collection.class, "remove", Object.class)).isTrue();
		assertThat(ClassUtils.hasMethod(Collection.class, "remove")).isFalse();
		assertThat(ClassUtils.hasMethod(Collection.class, "someOtherMethod")).isFalse();
	}

	@Test
	public void testGetMethodIfAvailable() {
		Method method = ClassUtils.getMethodIfAvailable(Collection.class, "size");
		assertNotNull(method);
		assertThat((Object) method.getName()).isEqualTo("size");

		method = ClassUtils.getMethodIfAvailable(Collection.class, "remove", Object.class);
		assertNotNull(method);
		assertThat((Object) method.getName()).isEqualTo("remove");

		assertNull(ClassUtils.getMethodIfAvailable(Collection.class, "remove"));
		assertNull(ClassUtils.getMethodIfAvailable(Collection.class, "someOtherMethod"));
	}

	@Test
	public void testGetMethodCountForName() {
		assertEquals("Verifying number of overloaded 'print' methods for OverloadedMethodsClass.", 2,
				ClassUtils.getMethodCountForName(OverloadedMethodsClass.class, "print"));
		assertEquals("Verifying number of overloaded 'print' methods for SubOverloadedMethodsClass.", 4,
				ClassUtils.getMethodCountForName(SubOverloadedMethodsClass.class, "print"));
	}

	@Test
	public void testCountOverloadedMethods() {
		assertThat(ClassUtils.hasAtLeastOneMethodWithName(TestObject.class, "foobar")).isFalse();
		// no args
		assertThat(ClassUtils.hasAtLeastOneMethodWithName(TestObject.class, "hashCode")).isTrue();
		// matches although it takes an arg
		assertThat(ClassUtils.hasAtLeastOneMethodWithName(TestObject.class, "setAge")).isTrue();
	}

	@Test
	public void testNoArgsStaticMethod() throws IllegalAccessException, InvocationTargetException {
		Method method = ClassUtils.getStaticMethod(InnerClass.class, "staticMethod");
		method.invoke(null, (Object[]) null);
		assertThat(InnerClass.noArgCalled).as("no argument method was not invoked.").isTrue();
	}

	@Test
	public void testArgsStaticMethod() throws IllegalAccessException, InvocationTargetException {
		Method method = ClassUtils.getStaticMethod(InnerClass.class, "argStaticMethod", String.class);
		method.invoke(null, "test");
		assertThat(InnerClass.argCalled).as("argument method was not invoked.").isTrue();
	}

	@Test
	public void testOverloadedStaticMethod() throws IllegalAccessException, InvocationTargetException {
		Method method = ClassUtils.getStaticMethod(InnerClass.class, "staticMethod", String.class);
		method.invoke(null, "test");
		assertThat(InnerClass.overloadedCalled).as("argument method was not invoked.").isTrue();
	}

	@Test
	public void testIsAssignable() {
		assertThat(ClassUtils.isAssignable(Object.class, Object.class)).isTrue();
		assertThat(ClassUtils.isAssignable(String.class, String.class)).isTrue();
		assertThat(ClassUtils.isAssignable(Object.class, String.class)).isTrue();
		assertThat(ClassUtils.isAssignable(Object.class, Integer.class)).isTrue();
		assertThat(ClassUtils.isAssignable(Number.class, Integer.class)).isTrue();
		assertThat(ClassUtils.isAssignable(Number.class, int.class)).isTrue();
		assertThat(ClassUtils.isAssignable(Integer.class, int.class)).isTrue();
		assertThat(ClassUtils.isAssignable(int.class, Integer.class)).isTrue();
		assertThat(ClassUtils.isAssignable(String.class, Object.class)).isFalse();
		assertThat(ClassUtils.isAssignable(Integer.class, Number.class)).isFalse();
		assertThat(ClassUtils.isAssignable(Integer.class, double.class)).isFalse();
		assertThat(ClassUtils.isAssignable(double.class, Integer.class)).isFalse();
	}

	@Test
	public void testClassPackageAsResourcePath() {
		String result = ClassUtils.classPackageAsResourcePath(Proxy.class);
		assertThat((Object) result).isEqualTo("java/lang/reflect");
	}

	@Test
	public void testAddResourcePathToPackagePath() {
		String result = "java/lang/reflect/xyzabc.xml";
		assertThat((Object) ClassUtils.addResourcePathToPackagePath(Proxy.class, "xyzabc.xml")).isEqualTo(result);
		assertThat((Object) ClassUtils.addResourcePathToPackagePath(Proxy.class, "/xyzabc.xml")).isEqualTo(result);

		assertThat((Object) ClassUtils.addResourcePathToPackagePath(Proxy.class, "a/b/c/d.xml")).isEqualTo("java/lang/reflect/a/b/c/d.xml");
	}

	@Test
	public void testGetAllInterfaces() {
		DerivedTestObject testBean = new DerivedTestObject();
		List<Class<?>> ifcs = Arrays.asList(ClassUtils.getAllInterfaces(testBean));
		assertEquals("Correct number of interfaces", 4, ifcs.size());
		assertThat(ifcs.contains(Serializable.class)).as("Contains Serializable").isTrue();
		assertThat(ifcs.contains(ITestObject.class)).as("Contains ITestBean").isTrue();
		assertThat(ifcs.contains(ITestInterface.class)).as("Contains IOther").isTrue();
	}

	@Test
	public void testClassNamesToString() {
		List<Class<?>> ifcs = new LinkedList<>();
		ifcs.add(Serializable.class);
		ifcs.add(Runnable.class);
		assertThat((Object) ifcs.toString()).isEqualTo("[interface java.io.Serializable, interface java.lang.Runnable]");
		assertThat((Object) ClassUtils.classNamesToString(ifcs)).isEqualTo("[java.io.Serializable, java.lang.Runnable]");

		List<Class<?>> classes = new LinkedList<>();
		classes.add(LinkedList.class);
		classes.add(Integer.class);
		assertThat((Object) classes.toString()).isEqualTo("[class java.util.LinkedList, class java.lang.Integer]");
		assertThat((Object) ClassUtils.classNamesToString(classes)).isEqualTo("[java.util.LinkedList, java.lang.Integer]");

		assertThat((Object) Collections.singletonList(List.class).toString()).isEqualTo("[interface java.util.List]");
		assertThat((Object) ClassUtils.classNamesToString(List.class)).isEqualTo("[java.util.List]");

		assertThat((Object) Collections.EMPTY_LIST.toString()).isEqualTo("[]");
		assertThat((Object) ClassUtils.classNamesToString(Collections.emptyList())).isEqualTo("[]");
	}

	@Test
	public void testDetermineCommonAncestor() {
		assertThat((Object) ClassUtils.determineCommonAncestor(Integer.class, Number.class)).isEqualTo(Number.class);
		assertThat((Object) ClassUtils.determineCommonAncestor(Number.class, Integer.class)).isEqualTo(Number.class);
		assertThat((Object) ClassUtils.determineCommonAncestor(Number.class, null)).isEqualTo(Number.class);
		assertThat((Object) ClassUtils.determineCommonAncestor(null, Integer.class)).isEqualTo(Integer.class);
		assertThat((Object) ClassUtils.determineCommonAncestor(Integer.class, Integer.class)).isEqualTo(Integer.class);

		assertThat((Object) ClassUtils.determineCommonAncestor(Integer.class, Float.class)).isEqualTo(Number.class);
		assertThat((Object) ClassUtils.determineCommonAncestor(Float.class, Integer.class)).isEqualTo(Number.class);
		assertNull(ClassUtils.determineCommonAncestor(Integer.class, String.class));
		assertNull(ClassUtils.determineCommonAncestor(String.class, Integer.class));

		assertThat((Object) ClassUtils.determineCommonAncestor(List.class, Collection.class)).isEqualTo(Collection.class);
		assertThat((Object) ClassUtils.determineCommonAncestor(Collection.class, List.class)).isEqualTo(Collection.class);
		assertThat((Object) ClassUtils.determineCommonAncestor(Collection.class, null)).isEqualTo(Collection.class);
		assertThat((Object) ClassUtils.determineCommonAncestor(null, List.class)).isEqualTo(List.class);
		assertThat((Object) ClassUtils.determineCommonAncestor(List.class, List.class)).isEqualTo(List.class);

		assertNull(ClassUtils.determineCommonAncestor(List.class, Set.class));
		assertNull(ClassUtils.determineCommonAncestor(Set.class, List.class));
		assertNull(ClassUtils.determineCommonAncestor(List.class, Runnable.class));
		assertNull(ClassUtils.determineCommonAncestor(Runnable.class, List.class));

		assertThat((Object) ClassUtils.determineCommonAncestor(List.class, ArrayList.class)).isEqualTo(List.class);
		assertThat((Object) ClassUtils.determineCommonAncestor(ArrayList.class, List.class)).isEqualTo(List.class);
		assertNull(ClassUtils.determineCommonAncestor(List.class, String.class));
		assertNull(ClassUtils.determineCommonAncestor(String.class, List.class));
	}


	public static class InnerClass {

		static boolean noArgCalled;
		static boolean argCalled;
		static boolean overloadedCalled;

		public static void staticMethod() {
			noArgCalled = true;
		}

		public static void staticMethod(String anArg) {
			overloadedCalled = true;
		}

		public static void argStaticMethod(String anArg) {
			argCalled = true;
		}
	}

	@SuppressWarnings("unused")
	private static class OverloadedMethodsClass {

		public void print(String messages) {
			/* no-op */
		}

		public void print(String[] messages) {
			/* no-op */
		}
	}

	@SuppressWarnings("unused")
	private static class SubOverloadedMethodsClass extends OverloadedMethodsClass {

		public void print(String header, String[] messages) {
			/* no-op */
		}

		void print(String header, String[] messages, String footer) {
			/* no-op */
		}
	}

}
