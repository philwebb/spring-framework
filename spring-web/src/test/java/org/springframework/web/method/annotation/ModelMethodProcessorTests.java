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

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link org.springframework.web.method.annotation.ModelMethodProcessor}.
 *
 * @author Rossen Stoyanchev
 */
public class ModelMethodProcessorTests {

	private ModelMethodProcessor processor;

	private ModelAndViewContainer mavContainer;

	private MethodParameter paramModel;

	private MethodParameter returnParamModel;

	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		this.processor = new ModelMethodProcessor();
		this.mavContainer = new ModelAndViewContainer();

		Method method = getClass().getDeclaredMethod("model", Model.class);
		this.paramModel = new MethodParameter(method, 0);
		this.returnParamModel = new MethodParameter(method, -1);

		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}

	@Test
	public void supportsParameter() {
		assertTrue(this.processor.supportsParameter(this.paramModel));
	}

	@Test
	public void supportsReturnType() {
		assertTrue(this.processor.supportsReturnType(this.returnParamModel));
	}

	@Test
	public void resolveArgumentValue() throws Exception {
		assertSame(this.mavContainer.getModel(), this.processor.resolveArgument(this.paramModel, this.mavContainer, this.webRequest, null));
	}

	@Test
	public void handleModelReturnValue() throws Exception {
		this.mavContainer.addAttribute("attr1", "value1");
		Model returnValue = new ExtendedModelMap();
		returnValue.addAttribute("attr2", "value2");

		this.processor.handleReturnValue(returnValue , this.returnParamModel, this.mavContainer, this.webRequest);

		assertEquals("value1", this.mavContainer.getModel().get("attr1"));
		assertEquals("value2", this.mavContainer.getModel().get("attr2"));
	}

	@SuppressWarnings("unused")
	private Model model(Model model) {
		return null;
	}

}
