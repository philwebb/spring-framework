/*
 * Copyright 2002-2017 the original author or authors.
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

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.Assert.assertEquals;

/**
 * Test fixture with {@link ServletCookieValueMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class ServletCookieValueMethodArgumentResolverTests {

	private ServletCookieValueMethodArgumentResolver resolver;

	private MockHttpServletRequest request;

	private ServletWebRequest webRequest;

	private MethodParameter cookieParameter;
	private MethodParameter cookieStringParameter;


	@Before
	public void setup() throws Exception {
		this.resolver = new ServletCookieValueMethodArgumentResolver(null);
		this.request = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(this.request, new MockHttpServletResponse());

		Method method = getClass().getMethod("params", Cookie.class, String.class);
		this.cookieParameter = new SynthesizingMethodParameter(method, 0);
		this.cookieStringParameter = new SynthesizingMethodParameter(method, 1);
	}


	@Test
	public void resolveCookieArgument() throws Exception {
		Cookie expected = new Cookie("name", "foo");
		this.request.setCookies(expected);

		Cookie result = (Cookie) this.resolver.resolveArgument(this.cookieParameter, null, this.webRequest, null);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveCookieStringArgument() throws Exception {
		Cookie cookie = new Cookie("name", "foo");
		this.request.setCookies(cookie);

		String result = (String) this.resolver.resolveArgument(this.cookieStringParameter, null, this.webRequest, null);
		assertEquals("Invalid result", cookie.getValue(), result);
	}


	public void params(@CookieValue("name") Cookie cookie,
			@CookieValue(name = "name", defaultValue = "bar") String cookieString) {
	}

}
