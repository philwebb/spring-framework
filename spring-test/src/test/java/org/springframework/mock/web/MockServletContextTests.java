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
		assertThat((Object) paths).isNotNull();
		assertThat(paths.contains("/web/MockServletContextTests.class")).isTrue();
	}

	@Test
	public void listSubdirectories() {
		Set<String> paths = sc.getResourcePaths("/");
		assertThat((Object) paths).isNotNull();
		assertThat(paths.contains("/web/")).isTrue();
	}

	@Test
	public void listNonDirectory() {
		Set<String> paths = sc.getResourcePaths("/web/MockServletContextTests.class");
		assertThat((Object) paths).isNull();
	}

	@Test
	public void listInvalidPath() {
		Set<String> paths = sc.getResourcePaths("/web/invalid");
		assertThat((Object) paths).isNull();
	}

	@Test
	public void registerContextAndGetContext() {
		MockServletContext sc2 = new MockServletContext();
		sc.setContextPath("/");
		sc.registerContext("/second", sc2);
		assertThat((Object) sc.getContext("/")).isSameAs(sc);
		assertThat((Object) sc.getContext("/second")).isSameAs(sc2);
	}

	@Test
	public void getMimeType() {
		assertThat(sc.getMimeType("test.html")).isEqualTo("text/html");
		assertThat(sc.getMimeType("test.gif")).isEqualTo("image/gif");
		assertThat((Object) sc.getMimeType("test.foobar")).isNull();
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
		assertThat((long) sc.getMajorVersion()).isEqualTo((long) 3);
		assertThat((long) sc.getMinorVersion()).isEqualTo((long) 1);
		assertThat((long) sc.getEffectiveMajorVersion()).isEqualTo((long) 3);
		assertThat((long) sc.getEffectiveMinorVersion()).isEqualTo((long) 1);

		sc.setMajorVersion(4);
		sc.setMinorVersion(0);
		sc.setEffectiveMajorVersion(4);
		sc.setEffectiveMinorVersion(0);
		assertThat((long) sc.getMajorVersion()).isEqualTo((long) 4);
		assertThat((long) sc.getMinorVersion()).isEqualTo((long) 0);
		assertThat((long) sc.getEffectiveMajorVersion()).isEqualTo((long) 4);
		assertThat((long) sc.getEffectiveMinorVersion()).isEqualTo((long) 0);
	}

	@Test
	public void registerAndUnregisterNamedDispatcher() throws Exception {
		final String name = "test-servlet";
		final String url = "/test";
		assertThat((Object) sc.getNamedDispatcher(name)).isNull();

		sc.registerNamedDispatcher(name, new MockRequestDispatcher(url));
		RequestDispatcher namedDispatcher = sc.getNamedDispatcher(name);
		assertThat((Object) namedDispatcher).isNotNull();
		MockHttpServletResponse response = new MockHttpServletResponse();
		namedDispatcher.forward(new MockHttpServletRequest(sc), response);
		assertThat(response.getForwardedUrl()).isEqualTo(url);

		sc.unregisterNamedDispatcher(name);
		assertThat((Object) sc.getNamedDispatcher(name)).isNull();
	}

	@Test
	public void getNamedDispatcherForDefaultServlet() throws Exception {
		final String name = "default";
		RequestDispatcher namedDispatcher = sc.getNamedDispatcher(name);
		assertThat((Object) namedDispatcher).isNotNull();

		MockHttpServletResponse response = new MockHttpServletResponse();
		namedDispatcher.forward(new MockHttpServletRequest(sc), response);
		assertThat(response.getForwardedUrl()).isEqualTo(name);
	}

	@Test
	public void setDefaultServletName() throws Exception {
		final String originalDefault = "default";
		final String newDefault = "test";
		assertThat((Object) sc.getNamedDispatcher(originalDefault)).isNotNull();

		sc.setDefaultServletName(newDefault);
		assertThat(sc.getDefaultServletName()).isEqualTo(newDefault);
		assertThat((Object) sc.getNamedDispatcher(originalDefault)).isNull();

		RequestDispatcher namedDispatcher = sc.getNamedDispatcher(newDefault);
		assertThat((Object) namedDispatcher).isNotNull();
		MockHttpServletResponse response = new MockHttpServletResponse();
		namedDispatcher.forward(new MockHttpServletRequest(sc), response);
		assertThat(response.getForwardedUrl()).isEqualTo(newDefault);
	}

	/**
	 * @since 4.1.2
	 */
	@Test
	public void getServletRegistration() {
		assertThat((Object) sc.getServletRegistration("servlet")).isNull();
	}

	/**
	 * @since 4.1.2
	 */
	@Test
	public void getServletRegistrations() {
		Map<String, ? extends ServletRegistration> servletRegistrations = sc.getServletRegistrations();
		assertThat((Object) servletRegistrations).isNotNull();
		assertThat((long) servletRegistrations.size()).isEqualTo((long) 0);
	}

	/**
	 * @since 4.1.2
	 */
	@Test
	public void getFilterRegistration() {
		assertThat((Object) sc.getFilterRegistration("filter")).isNull();
	}

	/**
	 * @since 4.1.2
	 */
	@Test
	public void getFilterRegistrations() {
		Map<String, ? extends FilterRegistration> filterRegistrations = sc.getFilterRegistrations();
		assertThat((Object) filterRegistrations).isNotNull();
		assertThat((long) filterRegistrations.size()).isEqualTo((long) 0);
	}

}
