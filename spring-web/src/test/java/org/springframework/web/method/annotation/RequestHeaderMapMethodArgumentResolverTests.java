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
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.Assert.*;

/**
 * Text fixture with {@link RequestHeaderMapMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestHeaderMapMethodArgumentResolverTests {

	private RequestHeaderMapMethodArgumentResolver resolver;

	private MethodParameter paramMap;

	private MethodParameter paramMultiValueMap;

	private MethodParameter paramHttpHeaders;

	private MethodParameter paramUnsupported;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest request;


	@Before
	public void setUp() throws Exception {
		this.resolver = new RequestHeaderMapMethodArgumentResolver();

		Method method = getClass().getMethod("params", Map.class, MultiValueMap.class, HttpHeaders.class, Map.class);
		this.paramMap = new SynthesizingMethodParameter(method, 0);
		this.paramMultiValueMap = new SynthesizingMethodParameter(method, 1);
		this.paramHttpHeaders = new SynthesizingMethodParameter(method, 2);
		this.paramUnsupported = new SynthesizingMethodParameter(method, 3);

		this.request = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(this.request, new MockHttpServletResponse());
	}


	@Test
	public void supportsParameter() {
		assertTrue("Map parameter not supported", this.resolver.supportsParameter(this.paramMap));
		assertTrue("MultiValueMap parameter not supported", this.resolver.supportsParameter(this.paramMultiValueMap));
		assertTrue("HttpHeaders parameter not supported", this.resolver.supportsParameter(this.paramHttpHeaders));
		assertFalse("non-@RequestParam map supported", this.resolver.supportsParameter(this.paramUnsupported));
	}

	@Test
	public void resolveMapArgument() throws Exception {
		String name = "foo";
		String value = "bar";
		Map<String, String> expected = Collections.singletonMap(name, value);
		this.request.addHeader(name, value);

		Object result = this.resolver.resolveArgument(this.paramMap, null, this.webRequest, null);

		assertTrue(result instanceof Map);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveMultiValueMapArgument() throws Exception {
		String name = "foo";
		String value1 = "bar";
		String value2 = "baz";

		this.request.addHeader(name, value1);
		this.request.addHeader(name, value2);

		MultiValueMap<String, String> expected = new LinkedMultiValueMap<>(1);
		expected.add(name, value1);
		expected.add(name, value2);

		Object result = this.resolver.resolveArgument(this.paramMultiValueMap, null, this.webRequest, null);

		assertTrue(result instanceof MultiValueMap);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveHttpHeadersArgument() throws Exception {
		String name = "foo";
		String value1 = "bar";
		String value2 = "baz";

		this.request.addHeader(name, value1);
		this.request.addHeader(name, value2);

		HttpHeaders expected = new HttpHeaders();
		expected.add(name, value1);
		expected.add(name, value2);

		Object result = this.resolver.resolveArgument(this.paramHttpHeaders, null, this.webRequest, null);

		assertTrue(result instanceof HttpHeaders);
		assertEquals("Invalid result", expected, result);
	}


	public void params(@RequestHeader Map<?, ?> param1,
			@RequestHeader MultiValueMap<?, ?> param2, @RequestHeader HttpHeaders param3,
			Map<?, ?> unsupported) {
	}

}
