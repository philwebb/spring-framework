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

package org.springframework.mock.web;

import java.util.Map;
import java.util.Set;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRegistration;

import org.junit.Test;

import org.springframework.http.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 19.02.2006
 */
public class MockServletContextTests {

	private final MockServletContext sc = new MockServletContext("org/springframework/mock");


	@Test
	public void listFiles() {
		Set<String> paths = this.sc.getResourcePaths("/web");
		assertNotNull(paths);
		assertTrue(paths.contains("/web/MockServletContextTests.class"));
	}

	@Test
	public void listSubdirectories() {
		Set<String> paths = this.sc.getResourcePaths("/");
		assertNotNull(paths);
		assertTrue(paths.contains("/web/"));
	}

	@Test
	public void listNonDirectory() {
		Set<String> paths = this.sc.getResourcePaths("/web/MockServletContextTests.class");
		assertNull(paths);
	}

	@Test
	public void listInvalidPath() {
		Set<String> paths = this.sc.getResourcePaths("/web/invalid");
		assertNull(paths);
	}

	@Test
	public void registerContextAndGetContext() {
		MockServletContext sc2 = new MockServletContext();
		this.sc.setContextPath("/");
		this.sc.registerContext("/second", sc2);
		assertSame(this.sc, this.sc.getContext("/"));
		assertSame(sc2, this.sc.getContext("/second"));
	}

	@Test
	public void getMimeType() {
		assertEquals("text/html", this.sc.getMimeType("test.html"));
		assertEquals("image/gif", this.sc.getMimeType("test.gif"));
		assertNull(this.sc.getMimeType("test.foobar"));
	}

	/**
	 * Introduced to dispel claims in a thread on Stack Overflow:
	 * <a href="http://stackoverflow.com/questions/22986109/testing-spring-managed-servlet">Testing Spring managed servlet</a>
	 */
	@Test
	public void getMimeTypeWithCustomConfiguredType() {
		this.sc.addMimeType("enigma", new MediaType("text", "enigma"));
		assertEquals("text/enigma", this.sc.getMimeType("filename.enigma"));
	}

	@Test
	public void servletVersion() {
		assertEquals(3, this.sc.getMajorVersion());
		assertEquals(1, this.sc.getMinorVersion());
		assertEquals(3, this.sc.getEffectiveMajorVersion());
		assertEquals(1, this.sc.getEffectiveMinorVersion());

		this.sc.setMajorVersion(4);
		this.sc.setMinorVersion(0);
		this.sc.setEffectiveMajorVersion(4);
		this.sc.setEffectiveMinorVersion(0);
		assertEquals(4, this.sc.getMajorVersion());
		assertEquals(0, this.sc.getMinorVersion());
		assertEquals(4, this.sc.getEffectiveMajorVersion());
		assertEquals(0, this.sc.getEffectiveMinorVersion());
	}

	@Test
	public void registerAndUnregisterNamedDispatcher() throws Exception {
		final String name = "test-servlet";
		final String url = "/test";
		assertNull(this.sc.getNamedDispatcher(name));

		this.sc.registerNamedDispatcher(name, new MockRequestDispatcher(url));
		RequestDispatcher namedDispatcher = this.sc.getNamedDispatcher(name);
		assertNotNull(namedDispatcher);
		MockHttpServletResponse response = new MockHttpServletResponse();
		namedDispatcher.forward(new MockHttpServletRequest(this.sc), response);
		assertEquals(url, response.getForwardedUrl());

		this.sc.unregisterNamedDispatcher(name);
		assertNull(this.sc.getNamedDispatcher(name));
	}

	@Test
	public void getNamedDispatcherForDefaultServlet() throws Exception {
		final String name = "default";
		RequestDispatcher namedDispatcher = this.sc.getNamedDispatcher(name);
		assertNotNull(namedDispatcher);

		MockHttpServletResponse response = new MockHttpServletResponse();
		namedDispatcher.forward(new MockHttpServletRequest(this.sc), response);
		assertEquals(name, response.getForwardedUrl());
	}

	@Test
	public void setDefaultServletName() throws Exception {
		final String originalDefault = "default";
		final String newDefault = "test";
		assertNotNull(this.sc.getNamedDispatcher(originalDefault));

		this.sc.setDefaultServletName(newDefault);
		assertEquals(newDefault, this.sc.getDefaultServletName());
		assertNull(this.sc.getNamedDispatcher(originalDefault));

		RequestDispatcher namedDispatcher = this.sc.getNamedDispatcher(newDefault);
		assertNotNull(namedDispatcher);
		MockHttpServletResponse response = new MockHttpServletResponse();
		namedDispatcher.forward(new MockHttpServletRequest(this.sc), response);
		assertEquals(newDefault, response.getForwardedUrl());
	}

	/**
	 * @since 4.1.2
	 */
	@Test
	public void getServletRegistration() {
		assertNull(this.sc.getServletRegistration("servlet"));
	}

	/**
	 * @since 4.1.2
	 */
	@Test
	public void getServletRegistrations() {
		Map<String, ? extends ServletRegistration> servletRegistrations = this.sc.getServletRegistrations();
		assertNotNull(servletRegistrations);
		assertEquals(0, servletRegistrations.size());
	}

	/**
	 * @since 4.1.2
	 */
	@Test
	public void getFilterRegistration() {
		assertNull(this.sc.getFilterRegistration("filter"));
	}

	/**
	 * @since 4.1.2
	 */
	@Test
	public void getFilterRegistrations() {
		Map<String, ? extends FilterRegistration> filterRegistrations = this.sc.getFilterRegistrations();
		assertNotNull(filterRegistrations);
		assertEquals(0, filterRegistrations.size());
	}

}
