/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.aop.config;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
public class MethodLocatingFactoryBeanTests {

	private static final String BEAN_NAME = "string";
	private MethodLocatingFactoryBean factory;
	private BeanFactory beanFactory;

	@Before
	public void setUp() {
		this.factory = new MethodLocatingFactoryBean();
		this.beanFactory = mock(BeanFactory.class);
	}

	@Test
	public void testIsSingleton() {
		assertTrue(this.factory.isSingleton());
	}

	@Test
	public void testGetObjectType() {
		assertEquals(Method.class, this.factory.getObjectType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWithNullTargetBeanName() {
		this.factory.setMethodName("toString()");
		this.factory.setBeanFactory(this.beanFactory);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWithEmptyTargetBeanName() {
		this.factory.setTargetBeanName("");
		this.factory.setMethodName("toString()");
		this.factory.setBeanFactory(this.beanFactory);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWithNullTargetMethodName() {
		this.factory.setTargetBeanName(BEAN_NAME);
		this.factory.setBeanFactory(this.beanFactory);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWithEmptyTargetMethodName() {
		this.factory.setTargetBeanName(BEAN_NAME);
		this.factory.setMethodName("");
		this.factory.setBeanFactory(this.beanFactory);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWhenTargetBeanClassCannotBeResolved() {
		this.factory.setTargetBeanName(BEAN_NAME);
		this.factory.setMethodName("toString()");
		this.factory.setBeanFactory(this.beanFactory);
		verify(this.beanFactory).getType(BEAN_NAME);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSunnyDayPath() throws Exception {
		given(this.beanFactory.getType(BEAN_NAME)).willReturn((Class)String.class);
		this.factory.setTargetBeanName(BEAN_NAME);
		this.factory.setMethodName("toString()");
		this.factory.setBeanFactory(this.beanFactory);
		Object result = this.factory.getObject();
		assertNotNull(result);
		assertTrue(result instanceof Method);
		Method method = (Method) result;
		assertEquals("Bingo", method.invoke("Bingo"));
	}

	@Test(expected = IllegalArgumentException.class)
	@SuppressWarnings("unchecked")
	public void testWhereMethodCannotBeResolved() {
		given(this.beanFactory.getType(BEAN_NAME)).willReturn((Class)String.class);
		this.factory.setTargetBeanName(BEAN_NAME);
		this.factory.setMethodName("loadOfOld()");
		this.factory.setBeanFactory(this.beanFactory);
	}

}
