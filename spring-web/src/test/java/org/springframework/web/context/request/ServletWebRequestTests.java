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

package org.springframework.web.context.request;

import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.multipart.MultipartRequest;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class ServletWebRequestTests {

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ServletWebRequest request;


	@Before
	public void setup() {
		this.servletRequest = new MockHttpServletRequest();
		this.servletResponse = new MockHttpServletResponse();
		this.request = new ServletWebRequest(this.servletRequest, this.servletResponse);
	}


	@Test
	public void parameters() {
		this.servletRequest.addParameter("param1", "value1");
		this.servletRequest.addParameter("param2", "value2");
		this.servletRequest.addParameter("param2", "value2a");

		assertEquals("value1", this.request.getParameter("param1"));
		assertEquals(1, this.request.getParameterValues("param1").length);
		assertEquals("value1", this.request.getParameterValues("param1")[0]);
		assertEquals("value2", this.request.getParameter("param2"));
		assertEquals(2, this.request.getParameterValues("param2").length);
		assertEquals("value2", this.request.getParameterValues("param2")[0]);
		assertEquals("value2a", this.request.getParameterValues("param2")[1]);

		Map<String, String[]> paramMap = this.request.getParameterMap();
		assertEquals(2, paramMap.size());
		assertEquals(1, paramMap.get("param1").length);
		assertEquals("value1", paramMap.get("param1")[0]);
		assertEquals(2, paramMap.get("param2").length);
		assertEquals("value2", paramMap.get("param2")[0]);
		assertEquals("value2a", paramMap.get("param2")[1]);
	}

	@Test
	public void locale() {
		this.servletRequest.addPreferredLocale(Locale.UK);

		assertEquals(Locale.UK, this.request.getLocale());
	}

	@Test
	public void nativeRequest() {
		assertSame(this.servletRequest, this.request.getNativeRequest());
		assertSame(this.servletRequest, this.request.getNativeRequest(ServletRequest.class));
		assertSame(this.servletRequest, this.request.getNativeRequest(HttpServletRequest.class));
		assertSame(this.servletRequest, this.request.getNativeRequest(MockHttpServletRequest.class));
		assertNull(this.request.getNativeRequest(MultipartRequest.class));
		assertSame(this.servletResponse, this.request.getNativeResponse());
		assertSame(this.servletResponse, this.request.getNativeResponse(ServletResponse.class));
		assertSame(this.servletResponse, this.request.getNativeResponse(HttpServletResponse.class));
		assertSame(this.servletResponse, this.request.getNativeResponse(MockHttpServletResponse.class));
		assertNull(this.request.getNativeResponse(MultipartRequest.class));
	}

	@Test
	public void decoratedNativeRequest() {
		HttpServletRequest decoratedRequest = new HttpServletRequestWrapper(this.servletRequest);
		HttpServletResponse decoratedResponse = new HttpServletResponseWrapper(this.servletResponse);
		ServletWebRequest request = new ServletWebRequest(decoratedRequest, decoratedResponse);
		assertSame(decoratedRequest, request.getNativeRequest());
		assertSame(decoratedRequest, request.getNativeRequest(ServletRequest.class));
		assertSame(decoratedRequest, request.getNativeRequest(HttpServletRequest.class));
		assertSame(this.servletRequest, request.getNativeRequest(MockHttpServletRequest.class));
		assertNull(request.getNativeRequest(MultipartRequest.class));
		assertSame(decoratedResponse, request.getNativeResponse());
		assertSame(decoratedResponse, request.getNativeResponse(ServletResponse.class));
		assertSame(decoratedResponse, request.getNativeResponse(HttpServletResponse.class));
		assertSame(this.servletResponse, request.getNativeResponse(MockHttpServletResponse.class));
		assertNull(request.getNativeResponse(MultipartRequest.class));
	}

}
