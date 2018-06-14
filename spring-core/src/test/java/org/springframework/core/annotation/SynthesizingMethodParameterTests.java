/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @since 5.0
 */
public class SynthesizingMethodParameterTests {

	private Method method;

	private SynthesizingMethodParameter stringParameter;

	private SynthesizingMethodParameter longParameter;

	private SynthesizingMethodParameter intReturnType;


	@Before
	public void setUp() throws NoSuchMethodException {
		this.method = getClass().getMethod("method", String.class, Long.TYPE);
		this.stringParameter = new SynthesizingMethodParameter(this.method, 0);
		this.longParameter = new SynthesizingMethodParameter(this.method, 1);
		this.intReturnType = new SynthesizingMethodParameter(this.method, -1);
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
		MethodParameter methodParameter = new SynthesizingMethodParameter(method, 0);
		assertEquals(this.stringParameter, methodParameter);
		assertEquals(methodParameter, this.stringParameter);
		assertNotEquals(this.longParameter, methodParameter);
		assertNotEquals(methodParameter, this.longParameter);

		methodParameter = new MethodParameter(method, 0);
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
		SynthesizingMethodParameter methodParameter = new SynthesizingMethodParameter(method, 0);
		assertEquals(this.stringParameter.hashCode(), methodParameter.hashCode());
		assertNotEquals(this.longParameter.hashCode(), methodParameter.hashCode());
	}

	@Test
	public void testFactoryMethods() {
		assertEquals(this.stringParameter, SynthesizingMethodParameter.forExecutable(this.method, 0));
		assertEquals(this.longParameter, SynthesizingMethodParameter.forExecutable(this.method, 1));

		assertEquals(this.stringParameter, SynthesizingMethodParameter.forParameter(this.method.getParameters()[0]));
		assertEquals(this.longParameter, SynthesizingMethodParameter.forParameter(this.method.getParameters()[1]));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIndexValidation() {
		new SynthesizingMethodParameter(this.method, 2);
	}


	public int method(String p1, long p2) {
		return 42;
	}

}
