/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.config.annotation;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with a {@link ViewControllerRegistry}.
 *
 * @author Rossen Stoyanchev
 */
public class ViewControllerRegistryTests {

	private ViewControllerRegistry registry;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@Before
	public void setup() {
		this.registry = new ViewControllerRegistry(new StaticApplicationContext());
		this.request = new MockHttpServletRequest("GET", "/");
		this.response = new MockHttpServletResponse();
	}


	@Test
	public void noViewControllers() {
		assertThat((Object) this.registry.buildHandlerMapping()).isNull();
	}

	@Test
	public void addViewController() {
		this.registry.addViewController("/path").setViewName("viewName");
		ParameterizableViewController controller = getController("/path");

		assertThat(controller.getViewName()).isEqualTo("viewName");
		assertThat((Object) controller.getStatusCode()).isNull();
		assertThat(controller.isStatusOnly()).isFalse();
		assertThat((Object) controller.getApplicationContext()).isNotNull();
	}

	@Test
	public void addViewControllerWithDefaultViewName() {
		this.registry.addViewController("/path");
		ParameterizableViewController controller = getController("/path");

		assertThat((Object) controller.getViewName()).isNull();
		assertThat((Object) controller.getStatusCode()).isNull();
		assertThat(controller.isStatusOnly()).isFalse();
		assertThat((Object) controller.getApplicationContext()).isNotNull();
	}

	@Test
	public void addRedirectViewController() throws Exception {
		this.registry.addRedirectViewController("/path", "/redirectTo");
		RedirectView redirectView = getRedirectView("/path");
		this.request.setQueryString("a=b");
		this.request.setContextPath("/context");
		redirectView.render(Collections.emptyMap(), this.request, this.response);

		assertThat((long) this.response.getStatus()).isEqualTo((long) 302);
		assertThat(this.response.getRedirectedUrl()).isEqualTo("/context/redirectTo");
		assertThat((Object) redirectView.getApplicationContext()).isNotNull();
	}

	@Test
	public void addRedirectViewControllerWithCustomSettings() throws Exception {
		this.registry.addRedirectViewController("/path", "/redirectTo")
				.setContextRelative(false)
				.setKeepQueryParams(true)
				.setStatusCode(HttpStatus.PERMANENT_REDIRECT);

		RedirectView redirectView = getRedirectView("/path");
		this.request.setQueryString("a=b");
		this.request.setContextPath("/context");
		redirectView.render(Collections.emptyMap(), this.request, this.response);

		assertThat((long) this.response.getStatus()).isEqualTo((long) 308);
		assertThat(response.getRedirectedUrl()).isEqualTo("/redirectTo?a=b");
		assertThat((Object) redirectView.getApplicationContext()).isNotNull();
	}

	@Test
	public void addStatusController() {
		this.registry.addStatusController("/path", HttpStatus.NOT_FOUND);
		ParameterizableViewController controller = getController("/path");

		assertThat((Object) controller.getViewName()).isNull();
		assertThat(controller.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(controller.isStatusOnly()).isTrue();
		assertThat((Object) controller.getApplicationContext()).isNotNull();
	}

	@Test
	public void order() {
		this.registry.addViewController("/path");
		SimpleUrlHandlerMapping handlerMapping = this.registry.buildHandlerMapping();
		assertThat((long) handlerMapping.getOrder()).isEqualTo((long) 1);

		this.registry.setOrder(2);
		handlerMapping = this.registry.buildHandlerMapping();
		assertThat((long) handlerMapping.getOrder()).isEqualTo((long) 2);
	}


	private ParameterizableViewController getController(String path) {
		Map<String, ?> urlMap = this.registry.buildHandlerMapping().getUrlMap();
		ParameterizableViewController controller = (ParameterizableViewController) urlMap.get(path);
		assertThat((Object) controller).isNotNull();
		return controller;
	}

	private RedirectView getRedirectView(String path) {
		ParameterizableViewController controller = getController(path);
		assertThat((Object) controller.getViewName()).isNull();
		assertThat((Object) controller.getView()).isNotNull();
		assertThat(controller.getView().getClass()).isEqualTo(RedirectView.class);
		return (RedirectView) controller.getView();
	}

}
