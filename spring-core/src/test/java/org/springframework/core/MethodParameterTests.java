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

package org.springframework.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class MethodParameterTests {

	private Method method;

	private MethodParameter stringParameter;

	private MethodParameter longParameter;

	private MethodParameter intReturnType;


	@Before
	public void setup() throws NoSuchMethodException {
		this.method = getClass().getMethod("method", String.class, Long.TYPE);
		this.stringParameter = new MethodParameter(this.method, 0);
		this.longParameter = new MethodParameter(this.method, 1);
		this.intReturnType = new MethodParameter(this.method, -1);
	}


	@Test
	public void testEquals() throws NoSuchMethodException {
		assertEquals(this.stringParameter, this.stringParameter);
		assertEquals(this.longParameter, this.longParameter);
		assertEquals(this.intReturnType, this.intReturnType);

		assertFalse(this.stringParameter.equals(this.longParameter));
		assertFalse(this.stringParameter.equals(this.intReturnType));
		assertFalse(this.longParameter.equals(this.stringParameter));
		assertFalse(this.longParameter.equals(this.intReturnType));
		assertFalse(this.intReturnType.equals(this.stringParameter));
		assertFalse(this.intReturnType.equals(this.longParameter));

		Method method = getClass().getMethod("method", String.class, Long.TYPE);
		MethodParameter methodParameter = new MethodParameter(method, 0);
		assertEquals(this.stringParameter, methodParameter);
		assertEquals(methodParameter, this.stringParameter);
		assertNotEquals(this.longParameter, methodParameter);
		assertNotEquals(methodParameter, this.longParameter);
	}

	@Test
	public void testHashCode() throws NoSuchMethodException {
		assertEquals(this.stringParameter.hashCode(), this.stringParameter.hashCode());
		assertEquals(this.longParameter.hashCode(), this.longParameter.hashCode());
		assertEquals(this.intReturnType.hashCode(), this.intReturnType.hashCode());

		Method method = getClass().getMethod("method", String.class, Long.TYPE);
		MethodParameter methodParameter = new MethodParameter(method, 0);
		assertEquals(this.stringParameter.hashCode(), methodParameter.hashCode());
		assertNotEquals(this.longParameter.hashCode(), methodParameter.hashCode());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testFactoryMethods() {
		assertEquals(this.stringParameter, MethodParameter.forMethodOrConstructor(this.method, 0));
		assertEquals(this.longParameter, MethodParameter.forMethodOrConstructor(this.method, 1));

		assertEquals(this.stringParameter, MethodParameter.forExecutable(this.method, 0));
		assertEquals(this.longParameter, MethodParameter.forExecutable(this.method, 1));

		assertEquals(this.stringParameter, MethodParameter.forParameter(this.method.getParameters()[0]));
		assertEquals(this.longParameter, MethodParameter.forParameter(this.method.getParameters()[1]));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIndexValidation() {
		new MethodParameter(this.method, 2);
	}

	@Test
	public void annotatedConstructorParameterInStaticNestedClass() throws Exception {
		Constructor<?> constructor = NestedClass.class.getDeclaredConstructor(String.class);
		MethodParameter methodParameter = MethodParameter.forExecutable(constructor, 0);
		assertEquals(String.class, methodParameter.getParameterType());
		assertNotNull("Failed to find @Param annotation", methodParameter.getParameterAnnotation(Param.class));
	}

	@Test  // SPR-16652
	public void annotatedConstructorParameterInInnerClass() throws Exception {
		Constructor<?> constructor = InnerClass.class.getConstructor(getClass(), String.class, Callable.class);

		MethodParameter methodParameter = MethodParameter.forExecutable(constructor, 0);
		assertEquals(getClass(), methodParameter.getParameterType());
		assertNull(methodParameter.getParameterAnnotation(Param.class));

		methodParameter = MethodParameter.forExecutable(constructor, 1);
		assertEquals(String.class, methodParameter.getParameterType());
		assertNotNull("Failed to find @Param annotation", methodParameter.getParameterAnnotation(Param.class));

		methodParameter = MethodParameter.forExecutable(constructor, 2);
		assertEquals(Callable.class, methodParameter.getParameterType());
		assertNull(methodParameter.getParameterAnnotation(Param.class));
	}

	@Test  // SPR-16734
	public void genericConstructorParameterInInnerClass() throws Exception {
		Constructor<?> constructor = InnerClass.class.getConstructor(getClass(), String.class, Callable.class);

		MethodParameter methodParameter = MethodParameter.forExecutable(constructor, 0);
		assertEquals(getClass(), methodParameter.getParameterType());
		assertEquals(getClass(), methodParameter.getGenericParameterType());

		methodParameter = MethodParameter.forExecutable(constructor, 1);
		assertEquals(String.class, methodParameter.getParameterType());
		assertEquals(String.class, methodParameter.getGenericParameterType());

		methodParameter = MethodParameter.forExecutable(constructor, 2);
		assertEquals(Callable.class, methodParameter.getParameterType());
		assertEquals(ResolvableType.forClassWithGenerics(Callable.class, Integer.class).getType(),
				methodParameter.getGenericParameterType());
	}


	public int method(String p1, long p2) {
		return 42;
	}

	@SuppressWarnings("unused")
	private static class NestedClass {

		NestedClass(@Param String s) {
		}
	}

	@SuppressWarnings("unused")
	private class InnerClass {

		public InnerClass(@Param String s, Callable<Integer> i) {
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	private @interface Param {
	}

}
