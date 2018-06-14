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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link PathVariableMapMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class PathVariableMapMethodArgumentResolverTests {

	private PathVariableMapMethodArgumentResolver resolver;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest request;

	private MethodParameter paramMap;
	private MethodParameter paramNamedMap;
	private MethodParameter paramMapNoAnnot;


	@Before
	public void setup() throws Exception {
		this.resolver = new PathVariableMapMethodArgumentResolver();
		this.mavContainer = new ModelAndViewContainer();
		this.request = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(this.request, new MockHttpServletResponse());

		Method method = getClass().getMethod("handle", Map.class, Map.class, Map.class);
		this.paramMap = new MethodParameter(method, 0);
		this.paramNamedMap = new MethodParameter(method, 1);
		this.paramMapNoAnnot = new MethodParameter(method, 2);
	}


	@Test
	public void supportsParameter() {
		assertTrue(this.resolver.supportsParameter(this.paramMap));
		assertFalse(this.resolver.supportsParameter(this.paramNamedMap));
		assertFalse(this.resolver.supportsParameter(this.paramMapNoAnnot));
	}

	@Test
	public void resolveArgument() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name1", "value1");
		uriTemplateVars.put("name2", "value2");
		this.request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		Object result = this.resolver.resolveArgument(this.paramMap, this.mavContainer, this.webRequest, null);

		assertEquals(uriTemplateVars, result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveArgumentNoUriVars() throws Exception {
		Map<String, String> map = (Map<String, String>) this.resolver.resolveArgument(this.paramMap, this.mavContainer, this.webRequest, null);

		assertEquals(Collections.emptyMap(), map);
	}


	public void handle(
			@PathVariable Map<String, String> map,
			@PathVariable("name") Map<String, String> namedMap,
			Map<String, String> mapWithoutAnnotat) {
	}

}
