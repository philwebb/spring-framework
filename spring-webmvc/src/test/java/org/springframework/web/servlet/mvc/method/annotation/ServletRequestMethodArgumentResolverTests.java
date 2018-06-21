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

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.security.Principal;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.PushBuilder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockHttpSession;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Nicholas Williams
 */
public class ServletRequestMethodArgumentResolverTests {

	private ServletRequestMethodArgumentResolver resolver;

	private ModelAndViewContainer mavContainer;

	private MockHttpServletRequest servletRequest;

	private ServletWebRequest webRequest;

	private Method method;


	@Before
	public void setup() throws Exception {
		this.resolver = new ServletRequestMethodArgumentResolver();
		this.mavContainer = new ModelAndViewContainer();
		this.servletRequest = new MockHttpServletRequest("GET", "");
		this.webRequest = new ServletWebRequest(this.servletRequest, new MockHttpServletResponse());

		this.method = getClass().getMethod("supportedParams", ServletRequest.class, MultipartRequest.class,
				HttpSession.class, Principal.class, Locale.class, InputStream.class, Reader.class,
				WebRequest.class, TimeZone.class, ZoneId.class, HttpMethod.class, PushBuilder.class);
	}


	@Test
	public void servletRequest() throws Exception {
		MethodParameter servletRequestParameter = new MethodParameter(this.method, 0);
		assertTrue("ServletRequest not supported", this.resolver.supportsParameter(servletRequestParameter));

		Object result = this.resolver.resolveArgument(servletRequestParameter, this.mavContainer, this.webRequest, null);
		assertSame("Invalid result", this.servletRequest, result);
		assertFalse("The requestHandled flag shouldn't change", this.mavContainer.isRequestHandled());
	}

	@Test
	public void session() throws Exception {
		MockHttpSession session = new MockHttpSession();
		this.servletRequest.setSession(session);

		MethodParameter sessionParameter = new MethodParameter(this.method, 2);
		assertTrue("Session not supported", this.resolver.supportsParameter(sessionParameter));

		Object result = this.resolver.resolveArgument(sessionParameter, this.mavContainer, this.webRequest, null);
		assertSame("Invalid result", session, result);
		assertFalse("The requestHandled flag shouldn't change", this.mavContainer.isRequestHandled());
	}

	@Test
	public void principal() throws Exception {
		Principal principal = () -> "Foo";
		this.servletRequest.setUserPrincipal(principal);

		MethodParameter principalParameter = new MethodParameter(this.method, 3);
		assertTrue("Principal not supported", this.resolver.supportsParameter(principalParameter));

		Object result = this.resolver.resolveArgument(principalParameter, null, this.webRequest, null);
		assertSame("Invalid result", principal, result);
	}

	@Test
	public void principalAsNull() throws Exception {
		MethodParameter principalParameter = new MethodParameter(this.method, 3);
		assertTrue("Principal not supported", this.resolver.supportsParameter(principalParameter));

		Object result = this.resolver.resolveArgument(principalParameter, null, this.webRequest, null);
		assertNull("Invalid result", result);
	}

	@Test
	public void locale() throws Exception {
		Locale locale = Locale.ENGLISH;
		this.servletRequest.addPreferredLocale(locale);

		MethodParameter localeParameter = new MethodParameter(this.method, 4);
		assertTrue("Locale not supported", this.resolver.supportsParameter(localeParameter));

		Object result = this.resolver.resolveArgument(localeParameter, null, this.webRequest, null);
		assertSame("Invalid result", locale, result);
	}

	@Test
	public void localeFromResolver() throws Exception {
		Locale locale = Locale.ENGLISH;
		this.servletRequest.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,
				new FixedLocaleResolver(locale));

		MethodParameter localeParameter = new MethodParameter(this.method, 4);
		assertTrue("Locale not supported", this.resolver.supportsParameter(localeParameter));

		Object result = this.resolver.resolveArgument(localeParameter, null, this.webRequest, null);
		assertSame("Invalid result", locale, result);
	}

	@Test
	public void timeZone() throws Exception {
		MethodParameter timeZoneParameter = new MethodParameter(this.method, 8);
		assertTrue("TimeZone not supported", this.resolver.supportsParameter(timeZoneParameter));

		Object result = this.resolver.resolveArgument(timeZoneParameter, null, this.webRequest, null);
		assertEquals("Invalid result", TimeZone.getDefault(), result);
	}

	@Test
	public void timeZoneFromResolver() throws Exception {
		TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
		this.servletRequest.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,
				new FixedLocaleResolver(Locale.US, timeZone));

		MethodParameter timeZoneParameter = new MethodParameter(this.method, 8);
		assertTrue("TimeZone not supported", this.resolver.supportsParameter(timeZoneParameter));

		Object result = this.resolver.resolveArgument(timeZoneParameter, null, this.webRequest, null);
		assertEquals("Invalid result", timeZone, result);
	}

	@Test
	public void zoneId() throws Exception {
		MethodParameter zoneIdParameter = new MethodParameter(this.method, 9);
		assertTrue("ZoneId not supported", this.resolver.supportsParameter(zoneIdParameter));

		Object result = this.resolver.resolveArgument(zoneIdParameter, null, this.webRequest, null);
		assertEquals("Invalid result", ZoneId.systemDefault(), result);
	}

	@Test
	public void zoneIdFromResolver() throws Exception {
		TimeZone timeZone = TimeZone.getTimeZone("America/New_York");
		this.servletRequest.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,
				new FixedLocaleResolver(Locale.US, timeZone));
		MethodParameter zoneIdParameter = new MethodParameter(this.method, 9);

		assertTrue("ZoneId not supported", this.resolver.supportsParameter(zoneIdParameter));

		Object result = this.resolver.resolveArgument(zoneIdParameter, null, this.webRequest, null);
		assertEquals("Invalid result", timeZone.toZoneId(), result);
	}

	@Test
	public void inputStream() throws Exception {
		MethodParameter inputStreamParameter = new MethodParameter(this.method, 5);
		assertTrue("InputStream not supported", this.resolver.supportsParameter(inputStreamParameter));

		Object result = this.resolver.resolveArgument(inputStreamParameter, null, this.webRequest, null);
		assertSame("Invalid result", this.webRequest.getRequest().getInputStream(), result);
	}

	@Test
	public void reader() throws Exception {
		MethodParameter readerParameter = new MethodParameter(this.method, 6);
		assertTrue("Reader not supported", this.resolver.supportsParameter(readerParameter));

		Object result = this.resolver.resolveArgument(readerParameter, null, this.webRequest, null);
		assertSame("Invalid result", this.webRequest.getRequest().getReader(), result);
	}

	@Test
	public void webRequest() throws Exception {
		MethodParameter webRequestParameter = new MethodParameter(this.method, 7);
		assertTrue("WebRequest not supported", this.resolver.supportsParameter(webRequestParameter));

		Object result = this.resolver.resolveArgument(webRequestParameter, null, this.webRequest, null);
		assertSame("Invalid result", this.webRequest, result);
	}

	@Test
	public void httpMethod() throws Exception {
		MethodParameter httpMethodParameter = new MethodParameter(this.method, 10);
		assertTrue("HttpMethod not supported", this.resolver.supportsParameter(httpMethodParameter));

		Object result = this.resolver.resolveArgument(httpMethodParameter, null, this.webRequest, null);
		assertSame("Invalid result", HttpMethod.valueOf(this.webRequest.getRequest().getMethod()), result);
	}

	@Test
	public void pushBuilder() throws Exception {
		final PushBuilder pushBuilder = Mockito.mock(PushBuilder.class);
		this.servletRequest = new MockHttpServletRequest("GET", "") {
			@Override
			public PushBuilder newPushBuilder() {
				return pushBuilder;
			}
		};
		ServletWebRequest webRequest = new ServletWebRequest(this.servletRequest, new MockHttpServletResponse());

		MethodParameter pushBuilderParameter = new MethodParameter(this.method, 11);
		assertTrue("PushBuilder not supported", this.resolver.supportsParameter(pushBuilderParameter));

		Object result = this.resolver.resolveArgument(pushBuilderParameter, null, webRequest, null);
		assertSame("Invalid result", pushBuilder, result);
	}


	@SuppressWarnings("unused")
	public void supportedParams(ServletRequest p0,
								MultipartRequest p1,
								HttpSession p2,
								Principal p3,
								Locale p4,
								InputStream p5,
								Reader p6,
								WebRequest p7,
								TimeZone p8,
								ZoneId p9,
								HttpMethod p10,
								PushBuilder p11) {
	}

}
