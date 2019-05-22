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

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotEquals;

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
		method = getClass().getMethod("method", String.class, Long.TYPE);
		stringParameter = new SynthesizingMethodParameter(method, 0);
		longParameter = new SynthesizingMethodParameter(method, 1);
		intReturnType = new SynthesizingMethodParameter(method, -1);
	}


	@Test
	public void testEquals() throws NoSuchMethodException {
		assertEquals(stringParameter, stringParameter);
		assertEquals(longParameter, longParameter);
		assertEquals(intReturnType, intReturnType);

		assertThat(stringParameter.equals(longParameter)).isFalse();
		assertThat(stringParameter.equals(intReturnType)).isFalse();
		assertThat(longParameter.equals(stringParameter)).isFalse();
		assertThat(longParameter.equals(intReturnType)).isFalse();
		assertThat(intReturnType.equals(stringParameter)).isFalse();
		assertThat(intReturnType.equals(longParameter)).isFalse();

		Method method = getClass().getMethod("method", String.class, Long.TYPE);
		MethodParameter methodParameter = new SynthesizingMethodParameter(method, 0);
		assertEquals(stringParameter, methodParameter);
		assertEquals(methodParameter, stringParameter);
		assertNotEquals(longParameter, methodParameter);
		assertNotEquals(methodParameter, longParameter);

		methodParameter = new MethodParameter(method, 0);
		assertEquals(stringParameter, methodParameter);
		assertEquals(methodParameter, stringParameter);
		assertNotEquals(longParameter, methodParameter);
		assertNotEquals(methodParameter, longParameter);
	}

	@Test
	public void testHashCode() throws NoSuchMethodException {
		assertEquals(stringParameter.hashCode(), stringParameter.hashCode());
		assertEquals(longParameter.hashCode(), longParameter.hashCode());
		assertEquals(intReturnType.hashCode(), intReturnType.hashCode());

		Method method = getClass().getMethod("method", String.class, Long.TYPE);
		SynthesizingMethodParameter methodParameter = new SynthesizingMethodParameter(method, 0);
		assertEquals(stringParameter.hashCode(), methodParameter.hashCode());
		assertNotEquals(longParameter.hashCode(), methodParameter.hashCode());
	}

	@Test
	public void testFactoryMethods() {
		assertEquals(stringParameter, SynthesizingMethodParameter.forExecutable(method, 0));
		assertEquals(longParameter, SynthesizingMethodParameter.forExecutable(method, 1));

		assertEquals(stringParameter, SynthesizingMethodParameter.forParameter(method.getParameters()[0]));
		assertEquals(longParameter, SynthesizingMethodParameter.forParameter(method.getParameters()[1]));
	}

	@Test
	public void testIndexValidation() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new SynthesizingMethodParameter(method, 2));
	}


	public int method(String p1, long p2) {
		return 42;
	}

}
