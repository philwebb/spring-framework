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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Test fixture with {@link ModelAndViewResolverMethodReturnValueHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class ModelAndViewResolverMethodReturnValueHandlerTests {

	private ModelAndViewResolverMethodReturnValueHandler handler;

	private List<ModelAndViewResolver> mavResolvers;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest request;


	@Before
	public void setup() {
		this.mavResolvers = new ArrayList<>();
		this.handler = new ModelAndViewResolverMethodReturnValueHandler(this.mavResolvers);
		this.mavContainer = new ModelAndViewContainer();
		this.request = new ServletWebRequest(new MockHttpServletRequest());
	}


	@Test
	public void modelAndViewResolver() throws Exception {
		MethodParameter returnType = new MethodParameter(getClass().getDeclaredMethod("testBeanReturnValue"), -1);
		this.mavResolvers.add(new TestModelAndViewResolver(TestBean.class));
		TestBean testBean = new TestBean("name");

		this.handler.handleReturnValue(testBean, returnType, this.mavContainer, this.request);

		assertEquals("viewName", this.mavContainer.getViewName());
		assertSame(testBean, this.mavContainer.getModel().get("modelAttrName"));
		assertFalse(this.mavContainer.isRequestHandled());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void modelAndViewResolverUnresolved() throws Exception {
		MethodParameter returnType = new MethodParameter(getClass().getDeclaredMethod("intReturnValue"), -1);
		this.mavResolvers.add(new TestModelAndViewResolver(TestBean.class));
		this.handler.handleReturnValue(99, returnType, this.mavContainer, this.request);
	}

	@Test
	public void handleNull() throws Exception {
		MethodParameter returnType = new MethodParameter(getClass().getDeclaredMethod("testBeanReturnValue"), -1);
		this.handler.handleReturnValue(null, returnType, this.mavContainer, this.request);

		assertNull(this.mavContainer.getView());
		assertNull(this.mavContainer.getViewName());
		assertTrue(this.mavContainer.getModel().isEmpty());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void handleSimpleType() throws Exception {
		MethodParameter returnType = new MethodParameter(getClass().getDeclaredMethod("intReturnValue"), -1);
		this.handler.handleReturnValue(55, returnType, this.mavContainer, this.request);
	}

	@Test
	public void handleNonSimpleType() throws Exception{
		MethodParameter returnType = new MethodParameter(getClass().getDeclaredMethod("testBeanReturnValue"), -1);
		this.handler.handleReturnValue(new TestBean(), returnType, this.mavContainer, this.request);

		assertTrue(this.mavContainer.containsAttribute("testBean"));
	}


	@SuppressWarnings("unused")
	private int intReturnValue() {
		return 0;
	}

	@SuppressWarnings("unused")
	private TestBean testBeanReturnValue() {
		return null;
	}


	private static class TestModelAndViewResolver implements ModelAndViewResolver {

		private final Class<?> returnValueType;

		public TestModelAndViewResolver(Class<?> returnValueType) {
			this.returnValueType = returnValueType;
		}

		@Override
		public ModelAndView resolveModelAndView(Method method, Class<?> handlerType, Object returnValue,
				ExtendedModelMap model, NativeWebRequest request) {

			if (returnValue != null && returnValue.getClass().equals(this.returnValueType)) {
				return new ModelAndView("viewName", "modelAttrName", returnValue);
			}
			else {
				return ModelAndViewResolver.UNRESOLVED;
			}
		}
	}

}
