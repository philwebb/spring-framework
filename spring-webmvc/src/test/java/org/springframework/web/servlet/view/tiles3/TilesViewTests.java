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
package org.springframework.web.servlet.view.tiles3;

import java.util.HashMap;
import java.util.Map;

import org.apache.tiles.request.AbstractRequest;
import org.apache.tiles.request.Request;
import org.apache.tiles.request.render.Renderer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link TilesView}.
 *
 * @author mick semb wever
 * @author Sebastien Deleuze
 */
public class TilesViewTests {

	private static final String VIEW_PATH = "template.test";

	private TilesView view;

	private Renderer renderer;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@Before
	public void setUp() throws Exception {
		MockServletContext servletContext = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(servletContext);
		wac.refresh();

		this.request = new MockHttpServletRequest();
		this.request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

		this.response = new MockHttpServletResponse();

		this.renderer = mock(Renderer.class);

		this.view = new TilesView();
		this.view.setServletContext(servletContext);
		this.view.setRenderer(this.renderer);
		this.view.setUrl(VIEW_PATH);
		this.view.afterPropertiesSet();
	}

	@Test
	public void render() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("modelAttribute", "modelValue");
		this.view.render(model, this.request, this.response);
		assertEquals("modelValue", this.request.getAttribute("modelAttribute"));
		verify(this.renderer).render(eq(VIEW_PATH), isA(Request.class));
	}

	@Test
	public void alwaysIncludeDefaults() throws Exception {
		this.view.render(new HashMap<>(), this.request, this.response);
		assertNull(this.request.getAttribute(AbstractRequest.FORCE_INCLUDE_ATTRIBUTE_NAME));
	}

	@Test
	public void alwaysIncludeEnabled() throws Exception {
		this.view.setAlwaysInclude(true);
		this.view.render(new HashMap<>(), this.request, this.response);
		assertTrue((Boolean)this.request.getAttribute(AbstractRequest.FORCE_INCLUDE_ATTRIBUTE_NAME));
	}

}
