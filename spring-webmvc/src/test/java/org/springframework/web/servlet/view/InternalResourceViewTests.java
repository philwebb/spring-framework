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

package org.springframework.web.servlet.view;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockRequestDispatcher;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.servlet.View;
import org.springframework.web.util.WebUtils;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link InternalResourceView}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class InternalResourceViewTests {

	@SuppressWarnings("serial")
	private static final Map<String, Object> model = Collections.unmodifiableMap(new HashMap<String, Object>() {{
		put("foo", "bar");
		put("I", 1L);
	}});

	private static final String url = "forward-to";

	private final HttpServletRequest request = mock(HttpServletRequest.class);

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final InternalResourceView view = new InternalResourceView();


	/**
	 * If the url property isn't supplied, view initialization should fail.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullUrl() throws Exception {
		this.view.afterPropertiesSet();
	}

	@Test
	public void forward() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myservlet/handler.do");
		request.setContextPath("/mycontext");
		request.setServletPath("/myservlet");
		request.setPathInfo(";mypathinfo");
		request.setQueryString("?param1=value1");

		this.view.setUrl(url);
		this.view.setServletContext(new MockServletContext() {
			@Override
			public int getMinorVersion() {
				return 4;
			}
		});

		this.view.render(model, request, this.response);
		assertEquals(url, this.response.getForwardedUrl());

		model.forEach((key, value) -> assertEquals("Values for model key '" + key
				+ "' must match", value, request.getAttribute(key)));
	}

	@Test
	public void alwaysInclude() throws Exception {
		given(this.request.getAttribute(View.PATH_VARIABLES)).willReturn(null);
		given(this.request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

		this.view.setUrl(url);
		this.view.setAlwaysInclude(true);

		// Can now try multiple tests
		this.view.render(model, this.request, this.response);
		assertEquals(url, this.response.getIncludedUrl());

		model.forEach((key, value) -> verify(this.request).setAttribute(key, value));
	}

	@Test
	public void includeOnAttribute() throws Exception {
		given(this.request.getAttribute(View.PATH_VARIABLES)).willReturn(null);
		given(this.request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE)).willReturn("somepath");
		given(this.request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

		this.view.setUrl(url);

		// Can now try multiple tests
		this.view.render(model, this.request, this.response);
		assertEquals(url, this.response.getIncludedUrl());

		model.forEach((key, value) -> verify(this.request).setAttribute(key, value));
	}

	@Test
	public void includeOnCommitted() throws Exception {
		given(this.request.getAttribute(View.PATH_VARIABLES)).willReturn(null);
		given(this.request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(this.request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

		this.response.setCommitted(true);
		this.view.setUrl(url);

		// Can now try multiple tests
		this.view.render(model, this.request, this.response);
		assertEquals(url, this.response.getIncludedUrl());

		model.forEach((k, v) -> verify(this.request).setAttribute(k, v));
	}

}
