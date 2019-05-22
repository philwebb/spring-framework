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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;

/**
 * Unit tests for {@link MockHttpServletResponse}.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 19.02.2006
 */
public class MockHttpServletResponseTests {

	private MockHttpServletResponse response = new MockHttpServletResponse();


	@Test
	public void setContentType() {
		String contentType = "test/plain";
		response.setContentType(contentType);
		assertThat((Object) response.getContentType()).isEqualTo(contentType);
		assertThat((Object) response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat((Object) response.getCharacterEncoding()).isEqualTo(WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Test
	public void setContentTypeUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		response.setContentType(contentType);
		assertThat((Object) response.getCharacterEncoding()).isEqualTo("UTF-8");
		assertThat((Object) response.getContentType()).isEqualTo(contentType);
		assertThat((Object) response.getHeader("Content-Type")).isEqualTo(contentType);
	}

	@Test
	public void contentTypeHeader() {
		String contentType = "test/plain";
		response.addHeader("Content-Type", contentType);
		assertThat((Object) response.getContentType()).isEqualTo(contentType);
		assertThat((Object) response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat((Object) response.getCharacterEncoding()).isEqualTo(WebUtils.DEFAULT_CHARACTER_ENCODING);

		response = new MockHttpServletResponse();
		response.setHeader("Content-Type", contentType);
		assertThat((Object) response.getContentType()).isEqualTo(contentType);
		assertThat((Object) response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat((Object) response.getCharacterEncoding()).isEqualTo(WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	@Test
	public void contentTypeHeaderUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		response.setHeader("Content-Type", contentType);
		assertThat((Object) response.getContentType()).isEqualTo(contentType);
		assertThat((Object) response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat((Object) response.getCharacterEncoding()).isEqualTo("UTF-8");

		response = new MockHttpServletResponse();
		response.addHeader("Content-Type", contentType);
		assertThat((Object) response.getContentType()).isEqualTo(contentType);
		assertThat((Object) response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat((Object) response.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test  // SPR-12677
	public void contentTypeHeaderWithMoreComplexCharsetSyntax() {
		String contentType = "test/plain;charset=\"utf-8\";foo=\"charset=bar\";foocharset=bar;foo=bar";
		response.setHeader("Content-Type", contentType);
		assertThat((Object) response.getContentType()).isEqualTo(contentType);
		assertThat((Object) response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat((Object) response.getCharacterEncoding()).isEqualTo("UTF-8");

		response = new MockHttpServletResponse();
		response.addHeader("Content-Type", contentType);
		assertThat((Object) response.getContentType()).isEqualTo(contentType);
		assertThat((Object) response.getHeader("Content-Type")).isEqualTo(contentType);
		assertThat((Object) response.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	public void setContentTypeThenCharacterEncoding() {
		response.setContentType("test/plain");
		response.setCharacterEncoding("UTF-8");
		assertThat((Object) response.getContentType()).isEqualTo("test/plain");
		assertThat((Object) response.getHeader("Content-Type")).isEqualTo("test/plain;charset=UTF-8");
		assertThat((Object) response.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	public void setCharacterEncodingThenContentType() {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("test/plain");
		assertThat((Object) response.getContentType()).isEqualTo("test/plain");
		assertThat((Object) response.getHeader("Content-Type")).isEqualTo("test/plain;charset=UTF-8");
		assertThat((Object) response.getCharacterEncoding()).isEqualTo("UTF-8");
	}

	@Test
	public void contentLength() {
		response.setContentLength(66);
		assertEquals(66, response.getContentLength());
		assertThat((Object) response.getHeader("Content-Length")).isEqualTo("66");
	}

	@Test
	public void contentLengthHeader() {
		response.addHeader("Content-Length", "66");
		assertEquals(66, response.getContentLength());
		assertThat((Object) response.getHeader("Content-Length")).isEqualTo("66");
	}

	@Test
	public void contentLengthIntHeader() {
		response.addIntHeader("Content-Length", 66);
		assertEquals(66, response.getContentLength());
		assertThat((Object) response.getHeader("Content-Length")).isEqualTo("66");
	}

	@Test
	public void httpHeaderNameCasingIsPreserved() throws Exception {
		final String headerName = "Header1";
		response.addHeader(headerName, "value1");
		Collection<String> responseHeaders = response.getHeaderNames();
		assertNotNull(responseHeaders);
		assertEquals(1, responseHeaders.size());
		assertThat((Object) responseHeaders.iterator().next()).as("HTTP header casing not being preserved").isEqualTo(headerName);
	}

	@Test
	public void cookies() {
		Cookie cookie = new Cookie("foo", "bar");
		cookie.setPath("/path");
		cookie.setDomain("example.com");
		cookie.setMaxAge(0);
		cookie.setSecure(true);
		cookie.setHttpOnly(true);

		response.addCookie(cookie);

		assertThat((Object) response.getHeader(HttpHeaders.SET_COOKIE)).isEqualTo(("foo=bar; Path=/path; Domain=example.com; " +
				"Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; " +
				"Secure; HttpOnly"));
	}

	@Test
	public void servletOutputStreamCommittedWhenBufferSizeExceeded() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getOutputStream().write('X');
		assertThat(response.isCommitted()).isFalse();
		int size = response.getBufferSize();
		response.getOutputStream().write(new byte[size]);
		assertThat(response.isCommitted()).isTrue();
		assertEquals(size + 1, response.getContentAsByteArray().length);
	}

	@Test
	public void servletOutputStreamCommittedOnFlushBuffer() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getOutputStream().write('X');
		assertThat(response.isCommitted()).isFalse();
		response.flushBuffer();
		assertThat(response.isCommitted()).isTrue();
		assertEquals(1, response.getContentAsByteArray().length);
	}

	@Test
	public void servletWriterCommittedWhenBufferSizeExceeded() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().write("X");
		assertThat(response.isCommitted()).isFalse();
		int size = response.getBufferSize();
		char[] data = new char[size];
		Arrays.fill(data, 'p');
		response.getWriter().write(data);
		assertThat(response.isCommitted()).isTrue();
		assertEquals(size + 1, response.getContentAsByteArray().length);
	}

	@Test
	public void servletOutputStreamCommittedOnOutputStreamFlush() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getOutputStream().write('X');
		assertThat(response.isCommitted()).isFalse();
		response.getOutputStream().flush();
		assertThat(response.isCommitted()).isTrue();
		assertEquals(1, response.getContentAsByteArray().length);
	}

	@Test
	public void servletWriterCommittedOnWriterFlush() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().write("X");
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().flush();
		assertThat(response.isCommitted()).isTrue();
		assertEquals(1, response.getContentAsByteArray().length);
	}

	@Test // SPR-16683
	public void servletWriterCommittedOnWriterClose() throws IOException {
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().write("X");
		assertThat(response.isCommitted()).isFalse();
		response.getWriter().close();
		assertThat(response.isCommitted()).isTrue();
		assertEquals(1, response.getContentAsByteArray().length);
	}

	@Test
	public void servletWriterAutoFlushedForString() throws IOException {
		response.getWriter().write("X");
		assertThat((Object) response.getContentAsString()).isEqualTo("X");
	}

	@Test
	public void servletWriterAutoFlushedForChar() throws IOException {
		response.getWriter().write('X');
		assertThat((Object) response.getContentAsString()).isEqualTo("X");
	}

	@Test
	public void servletWriterAutoFlushedForCharArray() throws IOException {
		response.getWriter().write("XY".toCharArray());
		assertThat((Object) response.getContentAsString()).isEqualTo("XY");
	}

	@Test
	public void sendRedirect() throws IOException {
		String redirectUrl = "/redirect";
		response.sendRedirect(redirectUrl);
		assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
		assertThat((Object) response.getHeader("Location")).isEqualTo(redirectUrl);
		assertThat((Object) response.getRedirectedUrl()).isEqualTo(redirectUrl);
		assertThat(response.isCommitted()).isTrue();
	}

	@Test
	public void locationHeaderUpdatesGetRedirectedUrl() {
		String redirectUrl = "/redirect";
		response.setHeader("Location", redirectUrl);
		assertThat((Object) response.getRedirectedUrl()).isEqualTo(redirectUrl);
	}

	@Test
	public void setDateHeader() {
		response.setDateHeader("Last-Modified", 1437472800000L);
		assertThat((Object) response.getHeader("Last-Modified")).isEqualTo("Tue, 21 Jul 2015 10:00:00 GMT");
	}

	@Test
	public void addDateHeader() {
		response.addDateHeader("Last-Modified", 1437472800000L);
		response.addDateHeader("Last-Modified", 1437472801000L);
		assertThat((Object) response.getHeaders("Last-Modified").get(0)).isEqualTo("Tue, 21 Jul 2015 10:00:00 GMT");
		assertThat((Object) response.getHeaders("Last-Modified").get(1)).isEqualTo("Tue, 21 Jul 2015 10:00:01 GMT");
	}

	@Test
	public void getDateHeader() {
		long time = 1437472800000L;
		response.setDateHeader("Last-Modified", time);
		assertThat((Object) response.getHeader("Last-Modified")).isEqualTo("Tue, 21 Jul 2015 10:00:00 GMT");
		assertEquals(time, response.getDateHeader("Last-Modified"));
	}

	@Test
	public void getInvalidDateHeader() {
		response.setHeader("Last-Modified", "invalid");
		assertThat((Object) response.getHeader("Last-Modified")).isEqualTo("invalid");
		assertThatIllegalArgumentException().isThrownBy(() ->
				response.getDateHeader("Last-Modified"));
	}

	@Test  // SPR-16160
	public void getNonExistentDateHeader() {
		assertNull(response.getHeader("Last-Modified"));
		assertEquals(-1, response.getDateHeader("Last-Modified"));
	}

	@Test  // SPR-10414
	public void modifyStatusAfterSendError() throws IOException {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
		response.setStatus(HttpServletResponse.SC_OK);
		assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
	}

	@Test  // SPR-10414
	@SuppressWarnings("deprecation")
	public void modifyStatusMessageAfterSendError() throws IOException {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Server Error");
		assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
	}

	@Test
	public void setCookieHeaderValid() {
		response.addHeader(HttpHeaders.SET_COOKIE, "SESSION=123; Path=/; Secure; HttpOnly; SameSite=Lax");
		Cookie cookie = response.getCookie("SESSION");
		assertNotNull(cookie);
		boolean condition = cookie instanceof MockCookie;
		assertThat(condition).isTrue();
		assertThat((Object) cookie.getName()).isEqualTo("SESSION");
		assertThat((Object) cookie.getValue()).isEqualTo("123");
		assertThat((Object) cookie.getPath()).isEqualTo("/");
		assertThat(cookie.getSecure()).isTrue();
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat((Object) ((MockCookie) cookie).getSameSite()).isEqualTo("Lax");
	}

	@Test
	public void addMockCookie() {
		MockCookie mockCookie = new MockCookie("SESSION", "123");
		mockCookie.setPath("/");
		mockCookie.setDomain("example.com");
		mockCookie.setMaxAge(0);
		mockCookie.setSecure(true);
		mockCookie.setHttpOnly(true);
		mockCookie.setSameSite("Lax");

		response.addCookie(mockCookie);

		assertThat((Object) response.getHeader(HttpHeaders.SET_COOKIE)).isEqualTo(("SESSION=123; Path=/; Domain=example.com; Max-Age=0; " +
				"Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=Lax"));
	}

}
