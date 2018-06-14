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

package org.springframework.web.servlet.handler;

import java.util.Collections;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import static org.junit.Assert.*;

/**
 * @author Seth Ladd
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 */
public class SimpleMappingExceptionResolverTests {

	private SimpleMappingExceptionResolver exceptionResolver;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private Object handler1;
	private Object handler2;
	private Exception genericException;

	@Before
	public void setUp() throws Exception {
		this.exceptionResolver = new SimpleMappingExceptionResolver();
		this.handler1 = new String();
		this.handler2 = new Object();
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
		this.request.setMethod("GET");
		this.genericException = new Exception();
	}

	@Test
	public void setOrder() {
		this.exceptionResolver.setOrder(2);
		assertEquals(2, this.exceptionResolver.getOrder());
	}

	@Test
	public void defaultErrorView() {
		this.exceptionResolver.setDefaultErrorView("default-view");
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertEquals("default-view", mav.getViewName());
		assertEquals(this.genericException, mav.getModel().get(SimpleMappingExceptionResolver.DEFAULT_EXCEPTION_ATTRIBUTE));
	}

	@Test
	public void defaultErrorViewDifferentHandler() {
		this.exceptionResolver.setDefaultErrorView("default-view");
		this.exceptionResolver.setMappedHandlers(Collections.singleton(this.handler1));
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler2, this.genericException);
		assertNull(mav);
	}

	@Test
	public void defaultErrorViewDifferentHandlerClass() {
		this.exceptionResolver.setDefaultErrorView("default-view");
		this.exceptionResolver.setMappedHandlerClasses(String.class);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler2, this.genericException);
		assertNull(mav);
	}

	@Test
	public void nullExceptionAttribute() {
		this.exceptionResolver.setDefaultErrorView("default-view");
		this.exceptionResolver.setExceptionAttribute(null);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertEquals("default-view", mav.getViewName());
		assertNull(mav.getModel().get(SimpleMappingExceptionResolver.DEFAULT_EXCEPTION_ATTRIBUTE));
	}

	@Test
	public void nullExceptionMappings() {
		this.exceptionResolver.setExceptionMappings(null);
		this.exceptionResolver.setDefaultErrorView("default-view");
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertEquals("default-view", mav.getViewName());
	}

	@Test
	public void noDefaultStatusCode() {
		this.exceptionResolver.setDefaultErrorView("default-view");
		this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void setDefaultStatusCode() {
		this.exceptionResolver.setDefaultErrorView("default-view");
		this.exceptionResolver.setDefaultStatusCode(HttpServletResponse.SC_BAD_REQUEST);
		this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertEquals(HttpServletResponse.SC_BAD_REQUEST, this.response.getStatus());
	}

	@Test
	public void noDefaultStatusCodeInInclude() {
		this.exceptionResolver.setDefaultErrorView("default-view");
		this.exceptionResolver.setDefaultStatusCode(HttpServletResponse.SC_BAD_REQUEST);
		this.request.setAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE, "some path");
		this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertEquals(HttpServletResponse.SC_OK, this.response.getStatus());
	}

	@Test
	public void specificStatusCode() {
		this.exceptionResolver.setDefaultErrorView("default-view");
		this.exceptionResolver.setDefaultStatusCode(HttpServletResponse.SC_BAD_REQUEST);
		Properties statusCodes = new Properties();
		statusCodes.setProperty("default-view", "406");
		this.exceptionResolver.setStatusCodes(statusCodes);
		this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertEquals(HttpServletResponse.SC_NOT_ACCEPTABLE, this.response.getStatus());
	}

	@Test
	public void simpleExceptionMapping() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		this.exceptionResolver.setWarnLogCategory("HANDLER_EXCEPTION");
		this.exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertEquals("error", mav.getViewName());
	}

	@Test
	public void exactExceptionMappingWithHandlerSpecified() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		this.exceptionResolver.setMappedHandlers(Collections.singleton(this.handler1));
		this.exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertEquals("error", mav.getViewName());
	}

	@Test
	public void exactExceptionMappingWithHandlerClassSpecified() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		this.exceptionResolver.setMappedHandlerClasses(String.class);
		this.exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertEquals("error", mav.getViewName());
	}

	@Test
	public void exactExceptionMappingWithHandlerInterfaceSpecified() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		this.exceptionResolver.setMappedHandlerClasses(Comparable.class);
		this.exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertEquals("error", mav.getViewName());
	}

	@Test
	public void simpleExceptionMappingWithHandlerSpecifiedButWrongHandler() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		this.exceptionResolver.setMappedHandlers(Collections.singleton(this.handler1));
		this.exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler2, this.genericException);
		assertNull(mav);
	}

	@Test
	public void simpleExceptionMappingWithHandlerClassSpecifiedButWrongHandler() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		this.exceptionResolver.setMappedHandlerClasses(String.class);
		this.exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler2, this.genericException);
		assertNull(mav);
	}

	@Test
	public void simpleExceptionMappingWithExclusion() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		this.exceptionResolver.setExceptionMappings(props);
		this.exceptionResolver.setExcludedExceptions(IllegalArgumentException.class);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, new IllegalArgumentException());
		assertNull(mav);
	}

	@Test
	public void missingExceptionInMapping() {
		Properties props = new Properties();
		props.setProperty("SomeFooThrowable", "error");
		this.exceptionResolver.setWarnLogCategory("HANDLER_EXCEPTION");
		this.exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertNull(mav);
	}

	@Test
	public void twoMappings() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("AnotherException", "another-error");
		this.exceptionResolver.setMappedHandlers(Collections.singleton(this.handler1));
		this.exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertEquals("error", mav.getViewName());
	}

	@Test
	public void twoMappingsOneShortOneLong() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		props.setProperty("AnotherException", "another-error");
		this.exceptionResolver.setMappedHandlers(Collections.singleton(this.handler1));
		this.exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, this.genericException);
		assertEquals("error", mav.getViewName());
	}

	@Test
	public void twoMappingsOneShortOneLongThrowOddException() {
		Exception oddException = new SomeOddException();
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		props.setProperty("SomeOddException", "another-error");
		this.exceptionResolver.setMappedHandlers(Collections.singleton(this.handler1));
		this.exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, oddException);
		assertEquals("another-error", mav.getViewName());
	}

	@Test
	public void twoMappingsThrowOddExceptionUseLongExceptionMapping() {
		Exception oddException = new SomeOddException();
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("SomeOddException", "another-error");
		this.exceptionResolver.setMappedHandlers(Collections.singleton(this.handler1));
		this.exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, oddException);
		assertEquals("another-error", mav.getViewName());
	}

	@Test
	public void threeMappings() {
		Exception oddException = new AnotherOddException();
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("SomeOddException", "another-error");
		props.setProperty("AnotherOddException", "another-some-error");
		this.exceptionResolver.setMappedHandlers(Collections.singleton(this.handler1));
		this.exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = this.exceptionResolver.resolveException(this.request, this.response, this.handler1, oddException);
		assertEquals("another-some-error", mav.getViewName());
	}


	@SuppressWarnings("serial")
	private static class SomeOddException extends Exception {

	}


	@SuppressWarnings("serial")
	private static class AnotherOddException extends Exception {

	}

}
