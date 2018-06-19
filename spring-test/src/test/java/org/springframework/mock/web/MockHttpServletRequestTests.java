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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StreamUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link MockHttpServletRequest}.
 *
 * @author Rick Evans
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 * @author Jakub Narloch
 */
public class MockHttpServletRequestTests {

	private static final String HOST = "Host";

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void protocolAndScheme() {
		assertEquals(MockHttpServletRequest.DEFAULT_PROTOCOL, this.request.getProtocol());
		assertEquals(MockHttpServletRequest.DEFAULT_SCHEME, this.request.getScheme());
		this.request.setProtocol("HTTP/2.0");
		this.request.setScheme("https");
		assertEquals("HTTP/2.0", this.request.getProtocol());
		assertEquals("https", this.request.getScheme());
	}

	@Test
	public void setContentAndGetInputStream() throws IOException {
		byte[] bytes = "body".getBytes(Charset.defaultCharset());
		this.request.setContent(bytes);
		assertEquals(bytes.length, this.request.getContentLength());
		assertNotNull(this.request.getInputStream());
		assertEquals("body", StreamUtils.copyToString(this.request.getInputStream(), Charset.defaultCharset()));
	}

	@Test
	public void setContentAndGetContentAsByteArray() {
		byte[] bytes = "request body".getBytes();
		this.request.setContent(bytes);
		assertEquals(bytes.length, this.request.getContentLength());
		assertNotNull(this.request.getContentAsByteArray());
		assertEquals(bytes, this.request.getContentAsByteArray());
	}

	@Test
	public void getContentAsStringWithoutSettingCharacterEncoding() throws IOException {
		this.exception.expect(IllegalStateException.class);
		this.exception.expectMessage("Cannot get content as a String for a null character encoding");
		this.request.getContentAsString();
	}

	@Test
	public void setContentAndGetContentAsStringWithExplicitCharacterEncoding() throws IOException {
		String palindrome = "ablE was I ere I saw Elba";
		byte[] bytes = palindrome.getBytes("UTF-16");
		this.request.setCharacterEncoding("UTF-16");
		this.request.setContent(bytes);
		assertEquals(bytes.length, this.request.getContentLength());
		assertNotNull(this.request.getContentAsString());
		assertEquals(palindrome, this.request.getContentAsString());
	}

	@Test
	public void noContent() throws IOException {
		assertEquals(-1, this.request.getContentLength());
		assertNotNull(this.request.getInputStream());
		assertEquals(-1, this.request.getInputStream().read());
		assertNull(this.request.getContentAsByteArray());
	}

	@Test
	public void setContentType() {
		String contentType = "test/plain";
		this.request.setContentType(contentType);
		assertEquals(contentType, this.request.getContentType());
		assertEquals(contentType, this.request.getHeader(HttpHeaders.CONTENT_TYPE));
		assertNull(this.request.getCharacterEncoding());
	}

	@Test
	public void setContentTypeUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		this.request.setContentType(contentType);
		assertEquals(contentType, this.request.getContentType());
		assertEquals(contentType, this.request.getHeader(HttpHeaders.CONTENT_TYPE));
		assertEquals("UTF-8", this.request.getCharacterEncoding());
	}

	@Test
	public void contentTypeHeader() {
		String contentType = "test/plain";
		this.request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
		assertEquals(contentType, this.request.getContentType());
		assertEquals(contentType, this.request.getHeader(HttpHeaders.CONTENT_TYPE));
		assertNull(this.request.getCharacterEncoding());
	}

	@Test
	public void contentTypeHeaderUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		this.request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
		assertEquals(contentType, this.request.getContentType());
		assertEquals(contentType, this.request.getHeader(HttpHeaders.CONTENT_TYPE));
		assertEquals("UTF-8", this.request.getCharacterEncoding());
	}

	@Test  // SPR-12677
	public void setContentTypeHeaderWithMoreComplexCharsetSyntax() {
		String contentType = "test/plain;charset=\"utf-8\";foo=\"charset=bar\";foocharset=bar;foo=bar";
		this.request.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
		assertEquals(contentType, this.request.getContentType());
		assertEquals(contentType, this.request.getHeader(HttpHeaders.CONTENT_TYPE));
		assertEquals("UTF-8", this.request.getCharacterEncoding());
	}

	@Test
	public void setContentTypeThenCharacterEncoding() {
		this.request.setContentType("test/plain");
		this.request.setCharacterEncoding("UTF-8");
		assertEquals("test/plain", this.request.getContentType());
		assertEquals("test/plain;charset=UTF-8", this.request.getHeader(HttpHeaders.CONTENT_TYPE));
		assertEquals("UTF-8", this.request.getCharacterEncoding());
	}

	@Test
	public void setCharacterEncodingThenContentType() {
		this.request.setCharacterEncoding("UTF-8");
		this.request.setContentType("test/plain");
		assertEquals("test/plain", this.request.getContentType());
		assertEquals("test/plain;charset=UTF-8", this.request.getHeader(HttpHeaders.CONTENT_TYPE));
		assertEquals("UTF-8", this.request.getCharacterEncoding());
	}

	@Test
	public void httpHeaderNameCasingIsPreserved() {
		String headerName = "Header1";
		this.request.addHeader(headerName, "value1");
		Enumeration<String> requestHeaders = this.request.getHeaderNames();
		assertNotNull(requestHeaders);
		assertEquals("HTTP header casing not being preserved", headerName, requestHeaders.nextElement());
	}

	@Test
	public void setMultipleParameters() {
		this.request.setParameter("key1", "value1");
		this.request.setParameter("key2", "value2");
		Map<String, Object> params = new HashMap<>(2);
		params.put("key1", "newValue1");
		params.put("key3", new String[] { "value3A", "value3B" });
		this.request.setParameters(params);
		String[] values1 = this.request.getParameterValues("key1");
		assertEquals(1, values1.length);
		assertEquals("newValue1", this.request.getParameter("key1"));
		assertEquals("value2", this.request.getParameter("key2"));
		String[] values3 = this.request.getParameterValues("key3");
		assertEquals(2, values3.length);
		assertEquals("value3A", values3[0]);
		assertEquals("value3B", values3[1]);
	}

	@Test
	public void addMultipleParameters() {
		this.request.setParameter("key1", "value1");
		this.request.setParameter("key2", "value2");
		Map<String, Object> params = new HashMap<>(2);
		params.put("key1", "newValue1");
		params.put("key3", new String[] { "value3A", "value3B" });
		this.request.addParameters(params);
		String[] values1 = this.request.getParameterValues("key1");
		assertEquals(2, values1.length);
		assertEquals("value1", values1[0]);
		assertEquals("newValue1", values1[1]);
		assertEquals("value2", this.request.getParameter("key2"));
		String[] values3 = this.request.getParameterValues("key3");
		assertEquals(2, values3.length);
		assertEquals("value3A", values3[0]);
		assertEquals("value3B", values3[1]);
	}

	@Test
	public void removeAllParameters() {
		this.request.setParameter("key1", "value1");
		Map<String, Object> params = new HashMap<>(2);
		params.put("key2", "value2");
		params.put("key3", new String[] { "value3A", "value3B" });
		this.request.addParameters(params);
		assertEquals(3, this.request.getParameterMap().size());
		this.request.removeAllParameters();
		assertEquals(0, this.request.getParameterMap().size());
	}

	@Test
	public void cookies() {
		Cookie cookie1 = new Cookie("foo", "bar");
		Cookie cookie2 = new Cookie("baz", "qux");
		this.request.setCookies(cookie1, cookie2);

		Cookie[] cookies = this.request.getCookies();
		List<String> cookieHeaders = Collections.list(this.request.getHeaders("Cookie"));

		assertEquals(2, cookies.length);
		assertEquals("foo", cookies[0].getName());
		assertEquals("bar", cookies[0].getValue());
		assertEquals("baz", cookies[1].getName());
		assertEquals("qux", cookies[1].getValue());
		assertEquals(Arrays.asList("foo=bar", "baz=qux"), cookieHeaders);
	}

	@Test
	public void noCookies() {
		assertNull(this.request.getCookies());
	}

	@Test
	public void defaultLocale() {
		Locale originalDefaultLocale = Locale.getDefault();
		try {
			Locale newDefaultLocale = (originalDefaultLocale.equals(Locale.GERMANY) ? Locale.FRANCE : Locale.GERMANY);
			Locale.setDefault(newDefaultLocale);
			// Create the request after changing the default locale.
			MockHttpServletRequest request = new MockHttpServletRequest();
			assertFalse(newDefaultLocale.equals(request.getLocale()));
			assertEquals(Locale.ENGLISH, request.getLocale());
		}
		finally {
			Locale.setDefault(originalDefaultLocale);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void setPreferredLocalesWithNullList() {
		this.request.setPreferredLocales(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setPreferredLocalesWithEmptyList() {
		this.request.setPreferredLocales(new ArrayList<>());
	}

	@Test
	public void setPreferredLocales() {
		List<Locale> preferredLocales = Arrays.asList(Locale.ITALY, Locale.CHINA);
		this.request.setPreferredLocales(preferredLocales);
		assertEqualEnumerations(Collections.enumeration(preferredLocales), this.request.getLocales());
		assertEquals("it-it, zh-cn", this.request.getHeader(HttpHeaders.ACCEPT_LANGUAGE));
	}

	@Test
	public void preferredLocalesFromAcceptLanguageHeader() {
		String headerValue = "fr-ch, fr;q=0.9, en-*;q=0.8, de;q=0.7, *;q=0.5";
		this.request.addHeader("Accept-Language", headerValue);
		List<Locale> actual = Collections.list(this.request.getLocales());
		assertEquals(Arrays.asList(Locale.forLanguageTag("fr-ch"), Locale.forLanguageTag("fr"),
				Locale.forLanguageTag("en"), Locale.forLanguageTag("de")), actual);
	}

	@Test
	public void invalidAcceptLanguageHeader() {
		this.request.addHeader("Accept-Language", "en_US");
		assertEquals(Locale.ENGLISH, this.request.getLocale());
		assertEquals("en_US", this.request.getHeader("Accept-Language"));
	}

	@Test
	public void getServerNameWithDefaultName() {
		assertEquals("localhost", this.request.getServerName());
	}

	@Test
	public void getServerNameWithCustomName() {
		this.request.setServerName("example.com");
		assertEquals("example.com", this.request.getServerName());
	}

	@Test
	public void getServerNameViaHostHeaderWithoutPort() {
		String testServer = "test.server";
		this.request.addHeader(HOST, testServer);
		assertEquals(testServer, this.request.getServerName());
	}

	@Test
	public void getServerNameViaHostHeaderWithPort() {
		String testServer = "test.server";
		this.request.addHeader(HOST, testServer + ":8080");
		assertEquals(testServer, this.request.getServerName());
	}

	@Test
	public void getServerNameViaHostHeaderAsIpv6AddressWithoutPort() {
		String ipv6Address = "[2001:db8:0:1]";
		this.request.addHeader(HOST, ipv6Address);
		assertEquals("2001:db8:0:1", this.request.getServerName());
	}

	@Test
	public void getServerNameViaHostHeaderAsIpv6AddressWithPort() {
		String ipv6Address = "[2001:db8:0:1]:8081";
		this.request.addHeader(HOST, ipv6Address);
		assertEquals("2001:db8:0:1", this.request.getServerName());
	}

	@Test
	public void getServerPortWithDefaultPort() {
		assertEquals(80, this.request.getServerPort());
	}

	@Test
	public void getServerPortWithCustomPort() {
		this.request.setServerPort(8080);
		assertEquals(8080, this.request.getServerPort());
	}

	@Test
	public void getServerPortViaHostHeaderAsIpv6AddressWithoutPort() {
		String testServer = "[2001:db8:0:1]";
		this.request.addHeader(HOST, testServer);
		assertEquals(80, this.request.getServerPort());
	}

	@Test
	public void getServerPortViaHostHeaderAsIpv6AddressWithPort() {
		String testServer = "[2001:db8:0:1]";
		int testPort = 9999;
		this.request.addHeader(HOST, testServer + ":" + testPort);
		assertEquals(testPort, this.request.getServerPort());
	}

	@Test
	public void getServerPortViaHostHeaderWithoutPort() {
		String testServer = "test.server";
		this.request.addHeader(HOST, testServer);
		assertEquals(80, this.request.getServerPort());
	}

	@Test
	public void getServerPortViaHostHeaderWithPort() {
		String testServer = "test.server";
		int testPort = 9999;
		this.request.addHeader(HOST, testServer + ":" + testPort);
		assertEquals(testPort, this.request.getServerPort());
	}

	@Test
	public void getRequestURL() {
		this.request.setServerPort(8080);
		this.request.setRequestURI("/path");
		assertEquals("http://localhost:8080/path", this.request.getRequestURL().toString());

		this.request.setScheme("https");
		this.request.setServerName("example.com");
		this.request.setServerPort(8443);
		assertEquals("https://example.com:8443/path", this.request.getRequestURL().toString());
	}

	@Test
	public void getRequestURLWithDefaults() {
		StringBuffer requestURL = this.request.getRequestURL();
		assertEquals("http://localhost", requestURL.toString());
	}

	@Test  // SPR-16138
	public void getRequestURLWithHostHeader() {
		String testServer = "test.server";
		this.request.addHeader(HOST, testServer);
		StringBuffer requestURL = this.request.getRequestURL();
		assertEquals("http://" + testServer, requestURL.toString());
	}

	@Test  // SPR-16138
	public void getRequestURLWithHostHeaderAndPort() {
		String testServer = "test.server:9999";
		this.request.addHeader(HOST, testServer);
		StringBuffer requestURL = this.request.getRequestURL();
		assertEquals("http://" + testServer, requestURL.toString());
	}

	@Test
	public void getRequestURLWithNullRequestUri() {
		this.request.setRequestURI(null);
		StringBuffer requestURL = this.request.getRequestURL();
		assertEquals("http://localhost", requestURL.toString());
	}

	@Test
	public void getRequestURLWithDefaultsAndHttps() {
		this.request.setScheme("https");
		this.request.setServerPort(443);
		StringBuffer requestURL = this.request.getRequestURL();
		assertEquals("https://localhost", requestURL.toString());
	}

	@Test
	public void getRequestURLWithNegativePort() {
		this.request.setServerPort(-99);
		StringBuffer requestURL = this.request.getRequestURL();
		assertEquals("http://localhost", requestURL.toString());
	}

	@Test
	public void isSecureWithHttpSchemeAndSecureFlagIsFalse() {
		assertFalse(this.request.isSecure());
		this.request.setScheme("http");
		this.request.setSecure(false);
		assertFalse(this.request.isSecure());
	}

	@Test
	public void isSecureWithHttpSchemeAndSecureFlagIsTrue() {
		assertFalse(this.request.isSecure());
		this.request.setScheme("http");
		this.request.setSecure(true);
		assertTrue(this.request.isSecure());
	}

	@Test
	public void isSecureWithHttpsSchemeAndSecureFlagIsFalse() {
		assertFalse(this.request.isSecure());
		this.request.setScheme("https");
		this.request.setSecure(false);
		assertTrue(this.request.isSecure());
	}

	@Test
	public void isSecureWithHttpsSchemeAndSecureFlagIsTrue() {
		assertFalse(this.request.isSecure());
		this.request.setScheme("https");
		this.request.setSecure(true);
		assertTrue(this.request.isSecure());
	}

	@Test
	public void httpHeaderDate() {
		Date date = new Date();
		this.request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, date);
		assertEquals(date.getTime(), this.request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE));
	}

	@Test
	public void httpHeaderTimestamp() {
		long timestamp = new Date().getTime();
		this.request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, timestamp);
		assertEquals(timestamp, this.request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE));
	}

	@Test
	public void httpHeaderRfcFormatedDate() {
		this.request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "Tue, 21 Jul 2015 10:00:00 GMT");
		assertEquals(1437472800000L, this.request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE));
	}

	@Test
	public void httpHeaderFirstVariantFormatedDate() {
		this.request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "Tue, 21-Jul-15 10:00:00 GMT");
		assertEquals(1437472800000L, this.request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE));
	}

	@Test
	public void httpHeaderSecondVariantFormatedDate() {
		this.request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "Tue Jul 21 10:00:00 2015");
		assertEquals(1437472800000L, this.request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE));
	}

	@Test(expected = IllegalArgumentException.class)
	public void httpHeaderFormatedDateError() {
		this.request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "This is not a date");
		this.request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
	}


	private void assertEqualEnumerations(Enumeration<?> enum1, Enumeration<?> enum2) {
		assertNotNull(enum1);
		assertNotNull(enum2);
		int count = 0;
		while (enum1.hasMoreElements()) {
			assertTrue("enumerations must be equal in length", enum2.hasMoreElements());
			assertEquals("enumeration element #" + ++count, enum1.nextElement(), enum2.nextElement());
		}
	}

}
