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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test fixture with {@link org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestHeaderMethodArgumentResolverTests {

	private RequestHeaderMethodArgumentResolver resolver;

	private MethodParameter paramNamedDefaultValueStringHeader;
	private MethodParameter paramNamedValueStringArray;
	private MethodParameter paramSystemProperty;
	private MethodParameter paramContextPath;
	private MethodParameter paramResolvedNameWithExpression;
	private MethodParameter paramResolvedNameWithPlaceholder;
	private MethodParameter paramNamedValueMap;
	private MethodParameter paramDate;
	private MethodParameter paramInstant;

	private MockHttpServletRequest servletRequest;

	private NativeWebRequest webRequest;


	@Before
	@SuppressWarnings("resource")
	public void setUp() throws Exception {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.refresh();
		this.resolver = new RequestHeaderMethodArgumentResolver(context.getBeanFactory());

		Method method = ReflectionUtils.findMethod(getClass(), "params", (Class<?>[]) null);
		this.paramNamedDefaultValueStringHeader = new SynthesizingMethodParameter(method, 0);
		this.paramNamedValueStringArray = new SynthesizingMethodParameter(method, 1);
		this.paramSystemProperty = new SynthesizingMethodParameter(method, 2);
		this.paramContextPath = new SynthesizingMethodParameter(method, 3);
		this.paramResolvedNameWithExpression = new SynthesizingMethodParameter(method, 4);
		this.paramResolvedNameWithPlaceholder = new SynthesizingMethodParameter(method, 5);
		this.paramNamedValueMap = new SynthesizingMethodParameter(method, 6);
		this.paramDate = new SynthesizingMethodParameter(method, 7);
		this.paramInstant = new SynthesizingMethodParameter(method, 8);

		this.servletRequest = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(this.servletRequest, new MockHttpServletResponse());

		// Expose request to the current thread (for SpEL expressions)
		RequestContextHolder.setRequestAttributes(this.webRequest);
	}

	@After
	public void teardown() {
		RequestContextHolder.resetRequestAttributes();
	}


	@Test
	public void supportsParameter() {
		assertTrue("String parameter not supported", this.resolver.supportsParameter(this.paramNamedDefaultValueStringHeader));
		assertTrue("String array parameter not supported", this.resolver.supportsParameter(this.paramNamedValueStringArray));
		assertFalse("non-@RequestParam parameter supported", this.resolver.supportsParameter(this.paramNamedValueMap));
	}

	@Test
	public void resolveStringArgument() throws Exception {
		String expected = "foo";
		this.servletRequest.addHeader("name", expected);

		Object result = this.resolver.resolveArgument(this.paramNamedDefaultValueStringHeader, null, this.webRequest, null);
		assertTrue(result instanceof String);
		assertEquals(expected, result);
	}

	@Test
	public void resolveStringArrayArgument() throws Exception {
		String[] expected = new String[] {"foo", "bar"};
		this.servletRequest.addHeader("name", expected);

		Object result = this.resolver.resolveArgument(this.paramNamedValueStringArray, null, this.webRequest, null);
		assertTrue(result instanceof String[]);
		assertArrayEquals(expected, (String[]) result);
	}

	@Test
	public void resolveDefaultValue() throws Exception {
		Object result = this.resolver.resolveArgument(this.paramNamedDefaultValueStringHeader, null, this.webRequest, null);
		assertTrue(result instanceof String);
		assertEquals("bar", result);
	}

	@Test
	public void resolveDefaultValueFromSystemProperty() throws Exception {
		System.setProperty("systemProperty", "bar");
		try {
			Object result = this.resolver.resolveArgument(this.paramSystemProperty, null, this.webRequest, null);
			assertTrue(result instanceof String);
			assertEquals("bar", result);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	public void resolveNameFromSystemPropertyThroughExpression() throws Exception {
		String expected = "foo";
		this.servletRequest.addHeader("bar", expected);

		System.setProperty("systemProperty", "bar");
		try {
			Object result = this.resolver.resolveArgument(this.paramResolvedNameWithExpression, null, this.webRequest, null);
			assertTrue(result instanceof String);
			assertEquals(expected, result);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	public void resolveNameFromSystemPropertyThroughPlaceholder() throws Exception {
		String expected = "foo";
		this.servletRequest.addHeader("bar", expected);

		System.setProperty("systemProperty", "bar");
		try {
			Object result = this.resolver.resolveArgument(this.paramResolvedNameWithPlaceholder, null, this.webRequest, null);
			assertTrue(result instanceof String);
			assertEquals(expected, result);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	public void resolveDefaultValueFromRequest() throws Exception {
		this.servletRequest.setContextPath("/bar");

		Object result = this.resolver.resolveArgument(this.paramContextPath, null, this.webRequest, null);
		assertTrue(result instanceof String);
		assertEquals("/bar", result);
	}

	@Test(expected = ServletRequestBindingException.class)
	public void notFound() throws Exception {
		this.resolver.resolveArgument(this.paramNamedValueStringArray, null, this.webRequest, null);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void dateConversion() throws Exception {
		String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
		this.servletRequest.addHeader("name", rfc1123val);

		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setConversionService(new DefaultFormattingConversionService());
		Object result = this.resolver.resolveArgument(this.paramDate, null, this.webRequest,
				new DefaultDataBinderFactory(bindingInitializer));

		assertTrue(result instanceof Date);
		assertEquals(new Date(rfc1123val), result);
	}

	@Test
	public void instantConversion() throws Exception {
		String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
		this.servletRequest.addHeader("name", rfc1123val);

		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setConversionService(new DefaultFormattingConversionService());
		Object result = this.resolver.resolveArgument(this.paramInstant, null, this.webRequest,
				new DefaultDataBinderFactory(bindingInitializer));

		assertTrue(result instanceof Instant);
		assertEquals(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(rfc1123val)), result);
	}


	public void params(
			@RequestHeader(name = "name", defaultValue = "bar") String param1,
			@RequestHeader("name") String[] param2,
			@RequestHeader(name = "name", defaultValue="#{systemProperties.systemProperty}") String param3,
			@RequestHeader(name = "name", defaultValue="#{request.contextPath}") String param4,
			@RequestHeader("#{systemProperties.systemProperty}") String param5,
			@RequestHeader("${systemProperty}") String param6,
			@RequestHeader("name") Map<?, ?> unsupported,
			@RequestHeader("name") Date dateParam,
			@RequestHeader("name") Instant instantParam) {
	}

}
