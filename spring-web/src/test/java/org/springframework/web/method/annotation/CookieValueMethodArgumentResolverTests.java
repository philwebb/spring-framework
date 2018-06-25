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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link org.springframework.web.method.annotation.AbstractCookieValueMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class CookieValueMethodArgumentResolverTests {

	private AbstractCookieValueMethodArgumentResolver resolver;

	private MethodParameter paramNamedCookie;

	private MethodParameter paramNamedDefaultValueString;

	private MethodParameter paramString;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest request;


	@Before
	public void setUp() throws Exception {
		this.resolver = new TestCookieValueMethodArgumentResolver();

		Method method = getClass().getMethod("params", Cookie.class, String.class, String.class);
		this.paramNamedCookie = new SynthesizingMethodParameter(method, 0);
		this.paramNamedDefaultValueString = new SynthesizingMethodParameter(method, 1);
		this.paramString = new SynthesizingMethodParameter(method, 2);

		this.request = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(this.request, new MockHttpServletResponse());
	}


	@Test
	public void supportsParameter() {
		assertTrue("Cookie parameter not supported", this.resolver.supportsParameter(this.paramNamedCookie));
		assertTrue("Cookie string parameter not supported", this.resolver.supportsParameter(this.paramNamedDefaultValueString));
		assertFalse("non-@CookieValue parameter supported", this.resolver.supportsParameter(this.paramString));
	}

	@Test
	public void resolveCookieDefaultValue() throws Exception {
		Object result = this.resolver.resolveArgument(this.paramNamedDefaultValueString, null, this.webRequest, null);

		assertTrue(result instanceof String);
		assertEquals("Invalid result", "bar", result);
	}

	@Test(expected = ServletRequestBindingException.class)
	public void notFound() throws Exception {
		this.resolver.resolveArgument(this.paramNamedCookie, null, this.webRequest, null);
		fail("Expected exception");
	}

	private static class TestCookieValueMethodArgumentResolver extends AbstractCookieValueMethodArgumentResolver {

		public TestCookieValueMethodArgumentResolver() {
			super(null);
		}

		@Override
		protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
			return null;
		}
	}


	public void params(@CookieValue("name") Cookie param1,
			@CookieValue(name = "name", defaultValue = "bar") String param2,
			String param3) {
	}

}
