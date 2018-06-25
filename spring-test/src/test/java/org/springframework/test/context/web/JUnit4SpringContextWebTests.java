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

package org.springframework.test.context.web;

import java.io.File;
import javax.servlet.ServletContext;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * JUnit-based integration tests that verify support for loading a
 * {@link WebApplicationContext} when extending {@link AbstractJUnit4SpringContextTests}.
 *
 * @author Sam Brannen
 * @since 3.2.7
 */
@ContextConfiguration
@WebAppConfiguration
public class JUnit4SpringContextWebTests extends AbstractJUnit4SpringContextTests implements ServletContextAware {

	@Configuration
	static class Config {

		@Bean
		public String foo() {
			return "enigma";
		}
	}


	protected ServletContext servletContext;

	@Autowired
	protected WebApplicationContext wac;

	@Autowired
	protected MockServletContext mockServletContext;

	@Autowired
	protected MockHttpServletRequest request;

	@Autowired
	protected MockHttpServletResponse response;

	@Autowired
	protected MockHttpSession session;

	@Autowired
	protected ServletWebRequest webRequest;

	@Autowired
	protected String foo;


	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Test
	public void basicWacFeatures() throws Exception {
		assertNotNull("ServletContext should be set in the WAC.", this.wac.getServletContext());

		assertNotNull("ServletContext should have been set via ServletContextAware.", this.servletContext);

		assertNotNull("ServletContext should have been autowired from the WAC.", this.mockServletContext);
		assertNotNull("MockHttpServletRequest should have been autowired from the WAC.", this.request);
		assertNotNull("MockHttpServletResponse should have been autowired from the WAC.", this.response);
		assertNotNull("MockHttpSession should have been autowired from the WAC.", this.session);
		assertNotNull("ServletWebRequest should have been autowired from the WAC.", this.webRequest);

		Object rootWac = this.mockServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		assertNotNull("Root WAC must be stored in the ServletContext as: "
				+ WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootWac);
		assertSame("test WAC and Root WAC in ServletContext must be the same object.", this.wac, rootWac);
		assertSame("ServletContext instances must be the same object.", this.mockServletContext, this.wac.getServletContext());
		assertSame("ServletContext in the WAC and in the mock request", this.mockServletContext, this.request.getServletContext());

		assertEquals("Getting real path for ServletContext resource.",
			new File("src/main/webapp/index.jsp").getCanonicalPath(), this.mockServletContext.getRealPath("index.jsp"));

	}

	@Test
	public void fooEnigmaAutowired() {
		assertEquals("enigma", this.foo);
	}

}
