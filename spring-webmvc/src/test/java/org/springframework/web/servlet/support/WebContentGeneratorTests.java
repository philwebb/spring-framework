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
package org.springframework.web.servlet.support;

import java.util.Arrays;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;

/**
 * Unit tests for {@link WebContentGenerator}.
 * @author Rossen Stoyanchev
 */
public class WebContentGeneratorTests {

	@Test
	public void getAllowHeaderWithConstructorTrue() throws Exception {
		WebContentGenerator generator = new TestWebContentGenerator(true);
		assertThat((Object) generator.getAllowHeader()).isEqualTo("GET,HEAD,POST,OPTIONS");
	}

	@Test
	public void getAllowHeaderWithConstructorFalse() throws Exception {
		WebContentGenerator generator = new TestWebContentGenerator(false);
		assertThat((Object) generator.getAllowHeader()).isEqualTo("GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS");
	}

	@Test
	public void getAllowHeaderWithSupportedMethodsConstructor() throws Exception {
		WebContentGenerator generator = new TestWebContentGenerator("POST");
		assertThat((Object) generator.getAllowHeader()).isEqualTo("POST,OPTIONS");
	}

	@Test
	public void getAllowHeaderWithSupportedMethodsSetter() throws Exception {
		WebContentGenerator generator = new TestWebContentGenerator();
		generator.setSupportedMethods("POST");
		assertThat((Object) generator.getAllowHeader()).isEqualTo("POST,OPTIONS");
	}

	@Test
	public void getAllowHeaderWithSupportedMethodsSetterEmpty() throws Exception {
		WebContentGenerator generator = new TestWebContentGenerator();
		generator.setSupportedMethods();
		assertThat((Object) generator.getAllowHeader()).as("Effectively \"no restriction\" on supported methods").isEqualTo("GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS");
	}

	@Test
	public void varyHeaderNone() throws Exception {
		WebContentGenerator generator = new TestWebContentGenerator();
		MockHttpServletResponse response = new MockHttpServletResponse();
		generator.prepareResponse(response);

		assertNull(response.getHeader("Vary"));
	}

	@Test
	public void varyHeader() throws Exception {
		String[] configuredValues = {"Accept-Language", "User-Agent"};
		String[] responseValues = {};
		String[] expected = {"Accept-Language", "User-Agent"};
		testVaryHeader(configuredValues, responseValues, expected);
	}

	@Test
	public void varyHeaderWithExistingWildcard() throws Exception {
		String[] configuredValues = {"Accept-Language"};
		String[] responseValues = {"*"};
		String[] expected = {"*"};
		testVaryHeader(configuredValues, responseValues, expected);
	}

	@Test
	public void varyHeaderWithExistingCommaValues() throws Exception {
		String[] configuredValues = {"Accept-Language", "User-Agent"};
		String[] responseValues = {"Accept-Encoding", "Accept-Language"};
		String[] expected = {"Accept-Encoding", "Accept-Language", "User-Agent"};
		testVaryHeader(configuredValues, responseValues, expected);
	}

	@Test
	public void varyHeaderWithExistingCommaSeparatedValues() throws Exception {
		String[] configuredValues = {"Accept-Language", "User-Agent"};
		String[] responseValues = {"Accept-Encoding, Accept-Language"};
		String[] expected = {"Accept-Encoding, Accept-Language", "User-Agent"};
		testVaryHeader(configuredValues, responseValues, expected);
	}

	private void testVaryHeader(String[] configuredValues, String[] responseValues, String[] expected) {
		WebContentGenerator generator = new TestWebContentGenerator();
		generator.setVaryByRequestHeaders(configuredValues);
		MockHttpServletResponse response = new MockHttpServletResponse();
		for (String value : responseValues) {
			response.addHeader("Vary", value);
		}
		generator.prepareResponse(response);
		assertThat((Object) response.getHeaderValues("Vary")).isEqualTo(Arrays.asList(expected));
	}


	private static class TestWebContentGenerator extends WebContentGenerator {

		public TestWebContentGenerator() {
		}

		public TestWebContentGenerator(boolean restrictDefaultSupportedMethods) {
			super(restrictDefaultSupportedMethods);
		}

		public TestWebContentGenerator(String... supportedMethods) {
			super(supportedMethods);
		}
	}
}
