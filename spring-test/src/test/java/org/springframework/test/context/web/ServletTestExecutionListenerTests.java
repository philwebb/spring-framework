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

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;

import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.context.web.ServletTestExecutionListener.*;

/**
 * Unit tests for {@link ServletTestExecutionListener}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.2.6
 */
public class ServletTestExecutionListenerTests {

	private static final String SET_UP_OUTSIDE_OF_STEL = "setUpOutsideOfStel";

	private final WebApplicationContext wac = mock(WebApplicationContext.class);
	private final MockServletContext mockServletContext = new MockServletContext();
	private final TestContext testContext = mock(TestContext.class);
	private final ServletTestExecutionListener listener = new ServletTestExecutionListener();


	@Before
	public void setUp() {
		given(this.wac.getServletContext()).willReturn(this.mockServletContext);
		given(this.testContext.getApplicationContext()).willReturn(this.wac);

		MockHttpServletRequest request = new MockHttpServletRequest(this.mockServletContext);
		MockHttpServletResponse response = new MockHttpServletResponse();
		ServletWebRequest servletWebRequest = new ServletWebRequest(request, response);

		request.setAttribute(SET_UP_OUTSIDE_OF_STEL, "true");

		RequestContextHolder.setRequestAttributes(servletWebRequest);
		assertSetUpOutsideOfStelAttributeExists();
	}

	@Test
	public void standardApplicationContext() throws Exception {
		BDDMockito.<Class<?>> given(this.testContext.getTestClass()).willReturn(getClass());
		given(this.testContext.getApplicationContext()).willReturn(mock(ApplicationContext.class));

		this.listener.beforeTestClass(this.testContext);
		assertSetUpOutsideOfStelAttributeExists();

		this.listener.prepareTestInstance(this.testContext);
		assertSetUpOutsideOfStelAttributeExists();

		this.listener.beforeTestMethod(this.testContext);
		assertSetUpOutsideOfStelAttributeExists();

		this.listener.afterTestMethod(this.testContext);
		assertSetUpOutsideOfStelAttributeExists();
	}

	@Test
	public void legacyWebTestCaseWithoutExistingRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(this.testContext.getTestClass()).willReturn(LegacyWebTestCase.class);

		RequestContextHolder.resetRequestAttributes();
		assertRequestAttributesDoNotExist();

		this.listener.beforeTestClass(this.testContext);

		this.listener.prepareTestInstance(this.testContext);
		assertRequestAttributesDoNotExist();
		verify(this.testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		given(this.testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(null);

		this.listener.beforeTestMethod(this.testContext);
		assertRequestAttributesDoNotExist();
		verify(this.testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

		this.listener.afterTestMethod(this.testContext);
		verify(this.testContext, times(1)).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		assertRequestAttributesDoNotExist();
	}

	@Test
	public void legacyWebTestCaseWithPresetRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(this.testContext.getTestClass()).willReturn(LegacyWebTestCase.class);

		this.listener.beforeTestClass(this.testContext);
		assertSetUpOutsideOfStelAttributeExists();

		this.listener.prepareTestInstance(this.testContext);
		assertSetUpOutsideOfStelAttributeExists();
		verify(this.testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		given(this.testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(null);

		this.listener.beforeTestMethod(this.testContext);
		assertSetUpOutsideOfStelAttributeExists();
		verify(this.testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		given(this.testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(null);

		this.listener.afterTestMethod(this.testContext);
		verify(this.testContext, times(1)).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		assertSetUpOutsideOfStelAttributeExists();
	}

	@Test
	public void atWebAppConfigTestCaseWithoutExistingRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(this.testContext.getTestClass()).willReturn(AtWebAppConfigWebTestCase.class);

		RequestContextHolder.resetRequestAttributes();
		this.listener.beforeTestClass(this.testContext);
		assertRequestAttributesDoNotExist();

		assertWebAppConfigTestCase();
	}

	@Test
	public void atWebAppConfigTestCaseWithPresetRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(this.testContext.getTestClass()).willReturn(AtWebAppConfigWebTestCase.class);

		this.listener.beforeTestClass(this.testContext);
		assertRequestAttributesExist();

		assertWebAppConfigTestCase();
	}

	/**
	 * @since 4.3
	 */
	@Test
	public void activateListenerWithoutExistingRequestAttributes() throws Exception {
		BDDMockito.<Class<?>> given(this.testContext.getTestClass()).willReturn(NoAtWebAppConfigWebTestCase.class);
		given(this.testContext.getAttribute(ServletTestExecutionListener.ACTIVATE_LISTENER)).willReturn(true);

		RequestContextHolder.resetRequestAttributes();
		this.listener.beforeTestClass(this.testContext);
		assertRequestAttributesDoNotExist();

		assertWebAppConfigTestCase();
	}


	private RequestAttributes assertRequestAttributesExist() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		assertNotNull("request attributes should exist", requestAttributes);
		return requestAttributes;
	}

	private void assertRequestAttributesDoNotExist() {
		assertNull("request attributes should not exist", RequestContextHolder.getRequestAttributes());
	}

	private void assertSetUpOutsideOfStelAttributeExists() {
		RequestAttributes requestAttributes = assertRequestAttributesExist();
		Object setUpOutsideOfStel = requestAttributes.getAttribute(SET_UP_OUTSIDE_OF_STEL,
			RequestAttributes.SCOPE_REQUEST);
		assertNotNull(SET_UP_OUTSIDE_OF_STEL + " should exist as a request attribute", setUpOutsideOfStel);
	}

	private void assertSetUpOutsideOfStelAttributeDoesNotExist() {
		RequestAttributes requestAttributes = assertRequestAttributesExist();
		Object setUpOutsideOfStel = requestAttributes.getAttribute(SET_UP_OUTSIDE_OF_STEL,
			RequestAttributes.SCOPE_REQUEST);
		assertNull(SET_UP_OUTSIDE_OF_STEL + " should NOT exist as a request attribute", setUpOutsideOfStel);
	}

	private void assertWebAppConfigTestCase() throws Exception {
		this.listener.prepareTestInstance(this.testContext);
		assertRequestAttributesExist();
		assertSetUpOutsideOfStelAttributeDoesNotExist();
		verify(this.testContext, times(1)).setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		verify(this.testContext, times(1)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		given(this.testContext.getAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(Boolean.TRUE);
		given(this.testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(Boolean.TRUE);

		this.listener.beforeTestMethod(this.testContext);
		assertRequestAttributesExist();
		assertSetUpOutsideOfStelAttributeDoesNotExist();
		verify(this.testContext, times(1)).setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
		verify(this.testContext, times(1)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

		this.listener.afterTestMethod(this.testContext);
		verify(this.testContext).removeAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		verify(this.testContext).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
		assertRequestAttributesDoNotExist();
	}


	static class LegacyWebTestCase {
	}

	@WebAppConfiguration
	static class AtWebAppConfigWebTestCase {
	}

	static class NoAtWebAppConfigWebTestCase {
	}

}
