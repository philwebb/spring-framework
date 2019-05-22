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

package org.springframework.mock.web;

import java.util.Map;
import java.util.Set;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRegistration;

import org.junit.Test;

import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;
import static temp.XAssert.assertSame;

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
		Set<String> paths = sc.getResourcePaths("/web");
		assertNotNull(paths);
		assertThat(paths.contains("/web/MockServletContextTests.class")).isTrue();
	}

	@Test
	public void listSubdirectories() {
		Set<String> paths = sc.getResourcePaths("/");
		assertNotNull(paths);
		assertThat(paths.contains("/web/")).isTrue();
	}

	@Test
	public void listNonDirectory() {
		Set<String> paths = sc.getResourcePaths("/web/MockServletContextTests.class");
		assertNull(paths);
	}

	@Test
	public void listInvalidPath() {
		Set<String> paths = sc.getResourcePaths("/web/invalid");
		assertNull(paths);
	}

	@Test
	public void registerContextAndGetContext() {
		MockServletContext sc2 = new MockServletContext();
		sc.setContextPath("/");
		sc.registerContext("/second", sc2);
		assertSame(sc, sc.getContext("/"));
		assertSame(sc2, sc.getContext("/second"));
	}

	@Test
	public void getMimeType() {
		assertThat(sc.getMimeType("test.html")).isEqualTo("text/html");
		assertThat(sc.getMimeType("test.gif")).isEqualTo("image/gif");
		assertNull(sc.getMimeType("test.foobar"));
	}

	/**
	 * Introduced to dispel claims in a thread on Stack Overflow:
	 * <a href="https://stackoverflow.com/questions/22986109/testing-spring-managed-servlet">Testing Spring managed servlet</a>
	 */
	@Test
	public void getMimeTypeWithCustomConfiguredType() {
		sc.addMimeType("enigma", new MediaType("text", "enigma"));
		assertThat(sc.getMimeType("filename.enigma")).isEqualTo("text/enigma");
	}

	@Test
	public void servletVersion() {
		assertEquals(3, sc.getMajorVersion());
		assertEquals(1, sc.getMinorVersion());
		assertEquals(3, sc.getEffectiveMajorVersion());
		assertEquals(1, sc.getEffectiveMinorVersion());

		sc.setMajorVersion(4);
		sc.setMinorVersion(0);
		sc.setEffectiveMajorVersion(4);
		sc.setEffectiveMinorVersion(0);
		assertEquals(4, sc.getMajorVersion());
		assertEquals(0, sc.getMinorVersion());
		assertEquals(4, sc.getEffectiveMajorVersion());
		assertEquals(0, sc.getEffectiveMinorVersion());
	}

	@Test
	public void registerAndUnregisterNamedDispatcher() throws Exception {
		final String name = "test-servlet";
		final String url = "/test";
		assertNull(sc.getNamedDispatcher(name));

		sc.registerNamedDispatcher(name, new MockRequestDispatcher(url));
		RequestDispatcher namedDispatcher = sc.getNamedDispatcher(name);
		assertNotNull(namedDispatcher);
		MockHttpServletResponse response = new MockHttpServletResponse();
		namedDispatcher.forward(new MockHttpServletRequest(sc), response);
		assertThat(response.getForwardedUrl()).isEqualTo(url);

		sc.unregisterNamedDispatcher(name);
		assertNull(sc.getNamedDispatcher(name));
	}

	@Test
	public void getNamedDispatcherForDefaultServlet() throws Exception {
		final String name = "default";
		RequestDispatcher namedDispatcher = sc.getNamedDispatcher(name);
		assertNotNull(namedDispatcher);

		MockHttpServletResponse response = new MockHttpServletResponse();
		namedDispatcher.forward(new MockHttpServletRequest(sc), response);
		assertThat(response.getForwardedUrl()).isEqualTo(name);
	}

	@Test
	public void setDefaultServletName() throws Exception {
		final String originalDefault = "default";
		final String newDefault = "test";
		assertNotNull(sc.getNamedDispatcher(originalDefault));

		sc.setDefaultServletName(newDefault);
		assertThat(sc.getDefaultServletName()).isEqualTo(newDefault);
		assertNull(sc.getNamedDispatcher(originalDefault));

		RequestDispatcher namedDispatcher = sc.getNamedDispatcher(newDefault);
		assertNotNull(namedDispatcher);
		MockHttpServletResponse response = new MockHttpServletResponse();
		namedDispatcher.forward(new MockHttpServletRequest(sc), response);
		assertThat(response.getForwardedUrl()).isEqualTo(newDefault);
	}

	/**
	 * @since 4.1.2
	 */
	@Test
	public void getServletRegistration() {
		assertNull(sc.getServletRegistration("servlet"));
	}

	/**
	 * @since 4.1.2
	 */
	@Test
	public void getServletRegistrations() {
		Map<String, ? extends ServletRegistration> servletRegistrations = sc.getServletRegistrations();
		assertNotNull(servletRegistrations);
		assertEquals(0, servletRegistrations.size());
	}

	/**
	 * @since 4.1.2
	 */
	@Test
	public void getFilterRegistration() {
		assertNull(sc.getFilterRegistration("filter"));
	}

	/**
	 * @since 4.1.2
	 */
	@Test
	public void getFilterRegistrations() {
		Map<String, ? extends FilterRegistration> filterRegistrations = sc.getFilterRegistrations();
		assertNotNull(filterRegistrations);
		assertEquals(0, filterRegistrations.size());
	}

}
