/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.util.WebUtils;

import static org.junit.Assert.*;

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
		this.response.setContentType(contentType);
		assertEquals(contentType, this.response.getContentType());
		assertEquals(contentType, this.response.getHeader("Content-Type"));
		assertEquals(WebUtils.DEFAULT_CHARACTER_ENCODING, this.response.getCharacterEncoding());
	}

	@Test
	public void setContentTypeUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		this.response.setContentType(contentType);
		assertEquals("UTF-8", this.response.getCharacterEncoding());
		assertEquals(contentType, this.response.getContentType());
		assertEquals(contentType, this.response.getHeader("Content-Type"));
	}

	@Test
	public void contentTypeHeader() {
		String contentType = "test/plain";
		this.response.addHeader("Content-Type", contentType);
		assertEquals(contentType, this.response.getContentType());
		assertEquals(contentType, this.response.getHeader("Content-Type"));
		assertEquals(WebUtils.DEFAULT_CHARACTER_ENCODING, this.response.getCharacterEncoding());

		this.response = new MockHttpServletResponse();
		this.response.setHeader("Content-Type", contentType);
		assertEquals(contentType, this.response.getContentType());
		assertEquals(contentType, this.response.getHeader("Content-Type"));
		assertEquals(WebUtils.DEFAULT_CHARACTER_ENCODING, this.response.getCharacterEncoding());
	}

	@Test
	public void contentTypeHeaderUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		this.response.setHeader("Content-Type", contentType);
		assertEquals(contentType, this.response.getContentType());
		assertEquals(contentType, this.response.getHeader("Content-Type"));
		assertEquals("UTF-8", this.response.getCharacterEncoding());

		this.response = new MockHttpServletResponse();
		this.response.addHeader("Content-Type", contentType);
		assertEquals(contentType, this.response.getContentType());
		assertEquals(contentType, this.response.getHeader("Content-Type"));
		assertEquals("UTF-8", this.response.getCharacterEncoding());
	}

	@Test  // SPR-12677
	public void contentTypeHeaderWithMoreComplexCharsetSyntax() {
		String contentType = "test/plain;charset=\"utf-8\";foo=\"charset=bar\";foocharset=bar;foo=bar";
		this.response.setHeader("Content-Type", contentType);
		assertEquals(contentType, this.response.getContentType());
		assertEquals(contentType, this.response.getHeader("Content-Type"));
		assertEquals("UTF-8", this.response.getCharacterEncoding());

		this.response = new MockHttpServletResponse();
		this.response.addHeader("Content-Type", contentType);
		assertEquals(contentType, this.response.getContentType());
		assertEquals(contentType, this.response.getHeader("Content-Type"));
		assertEquals("UTF-8", this.response.getCharacterEncoding());
	}

	@Test
	public void setContentTypeThenCharacterEncoding() {
		this.response.setContentType("test/plain");
		this.response.setCharacterEncoding("UTF-8");
		assertEquals("test/plain", this.response.getContentType());
		assertEquals("test/plain;charset=UTF-8", this.response.getHeader("Content-Type"));
		assertEquals("UTF-8", this.response.getCharacterEncoding());
	}

	@Test
	public void setCharacterEncodingThenContentType() {
		this.response.setCharacterEncoding("UTF-8");
		this.response.setContentType("test/plain");
		assertEquals("test/plain", this.response.getContentType());
		assertEquals("test/plain;charset=UTF-8", this.response.getHeader("Content-Type"));
		assertEquals("UTF-8", this.response.getCharacterEncoding());
	}

	@Test
	public void contentLength() {
		this.response.setContentLength(66);
		assertEquals(66, this.response.getContentLength());
		assertEquals("66", this.response.getHeader("Content-Length"));
	}

	@Test
	public void contentLengthHeader() {
		this.response.addHeader("Content-Length", "66");
		assertEquals(66, this.response.getContentLength());
		assertEquals("66", this.response.getHeader("Content-Length"));
	}

	@Test
	public void contentLengthIntHeader() {
		this.response.addIntHeader("Content-Length", 66);
		assertEquals(66, this.response.getContentLength());
		assertEquals("66", this.response.getHeader("Content-Length"));
	}

	@Test
	public void httpHeaderNameCasingIsPreserved() throws Exception {
		final String headerName = "Header1";
		this.response.addHeader(headerName, "value1");
		Collection<String> responseHeaders = this.response.getHeaderNames();
		assertNotNull(responseHeaders);
		assertEquals(1, responseHeaders.size());
		assertEquals("HTTP header casing not being preserved", headerName, responseHeaders.iterator().next());
	}

	@Test
	public void cookies() {
		Cookie cookie = new Cookie("foo", "bar");
		cookie.setPath("/path");
		cookie.setDomain("example.com");
		cookie.setMaxAge(0);
		cookie.setSecure(true);
		cookie.setHttpOnly(true);

		this.response.addCookie(cookie);

		assertEquals("foo=bar; Path=/path; Domain=example.com; " +
				"Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT; " +
				"Secure; HttpOnly", this.response.getHeader(HttpHeaders.SET_COOKIE));
	}

	@Test
	public void servletOutputStreamCommittedWhenBufferSizeExceeded() throws IOException {
		assertFalse(this.response.isCommitted());
		this.response.getOutputStream().write('X');
		assertFalse(this.response.isCommitted());
		int size = this.response.getBufferSize();
		this.response.getOutputStream().write(new byte[size]);
		assertTrue(this.response.isCommitted());
		assertEquals(size + 1, this.response.getContentAsByteArray().length);
	}

	@Test
	public void servletOutputStreamCommittedOnFlushBuffer() throws IOException {
		assertFalse(this.response.isCommitted());
		this.response.getOutputStream().write('X');
		assertFalse(this.response.isCommitted());
		this.response.flushBuffer();
		assertTrue(this.response.isCommitted());
		assertEquals(1, this.response.getContentAsByteArray().length);
	}

	@Test
	public void servletWriterCommittedWhenBufferSizeExceeded() throws IOException {
		assertFalse(this.response.isCommitted());
		this.response.getWriter().write("X");
		assertFalse(this.response.isCommitted());
		int size = this.response.getBufferSize();
		char[] data = new char[size];
		Arrays.fill(data, 'p');
		this.response.getWriter().write(data);
		assertTrue(this.response.isCommitted());
		assertEquals(size + 1, this.response.getContentAsByteArray().length);
	}

	@Test
	public void servletOutputStreamCommittedOnOutputStreamFlush() throws IOException {
		assertFalse(this.response.isCommitted());
		this.response.getOutputStream().write('X');
		assertFalse(this.response.isCommitted());
		this.response.getOutputStream().flush();
		assertTrue(this.response.isCommitted());
		assertEquals(1, this.response.getContentAsByteArray().length);
	}

	@Test
	public void servletWriterCommittedOnWriterFlush() throws IOException {
		assertFalse(this.response.isCommitted());
		this.response.getWriter().write("X");
		assertFalse(this.response.isCommitted());
		this.response.getWriter().flush();
		assertTrue(this.response.isCommitted());
		assertEquals(1, this.response.getContentAsByteArray().length);
	}

	@Test // SPR-16683
	public void servletWriterCommittedOnWriterClose() throws IOException {
		assertFalse(this.response.isCommitted());
		this.response.getWriter().write("X");
		assertFalse(this.response.isCommitted());
		this.response.getWriter().close();
		assertTrue(this.response.isCommitted());
		assertEquals(1, this.response.getContentAsByteArray().length);
	}

	@Test
	public void servletWriterAutoFlushedForString() throws IOException {
		this.response.getWriter().write("X");
		assertEquals("X", this.response.getContentAsString());
	}

	@Test
	public void servletWriterAutoFlushedForChar() throws IOException {
		this.response.getWriter().write('X');
		assertEquals("X", this.response.getContentAsString());
	}

	@Test
	public void servletWriterAutoFlushedForCharArray() throws IOException {
		this.response.getWriter().write("XY".toCharArray());
		assertEquals("XY", this.response.getContentAsString());
	}

	@Test
	public void sendRedirect() throws IOException {
		String redirectUrl = "/redirect";
		this.response.sendRedirect(redirectUrl);
		assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, this.response.getStatus());
		assertEquals(redirectUrl, this.response.getHeader("Location"));
		assertEquals(redirectUrl, this.response.getRedirectedUrl());
		assertTrue(this.response.isCommitted());
	}

	@Test
	public void locationHeaderUpdatesGetRedirectedUrl() {
		String redirectUrl = "/redirect";
		this.response.setHeader("Location", redirectUrl);
		assertEquals(redirectUrl, this.response.getRedirectedUrl());
	}

	@Test
	public void setDateHeader() {
		this.response.setDateHeader("Last-Modified", 1437472800000L);
		assertEquals("Tue, 21 Jul 2015 10:00:00 GMT", this.response.getHeader("Last-Modified"));
	}

	@Test
	public void addDateHeader() {
		this.response.addDateHeader("Last-Modified", 1437472800000L);
		this.response.addDateHeader("Last-Modified", 1437472801000L);
		assertEquals("Tue, 21 Jul 2015 10:00:00 GMT", this.response.getHeaders("Last-Modified").get(0));
		assertEquals("Tue, 21 Jul 2015 10:00:01 GMT", this.response.getHeaders("Last-Modified").get(1));
	}

	@Test
	public void getDateHeader() {
		long time = 1437472800000L;
		this.response.setDateHeader("Last-Modified", time);
		assertEquals("Tue, 21 Jul 2015 10:00:00 GMT", this.response.getHeader("Last-Modified"));
		assertEquals(time, this.response.getDateHeader("Last-Modified"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getInvalidDateHeader() {
		this.response.setHeader("Last-Modified", "invalid");
		assertEquals("invalid", this.response.getHeader("Last-Modified"));
		this.response.getDateHeader("Last-Modified");
	}

	@Test  // SPR-16160
	public void getNonExistentDateHeader() {
		assertNull(this.response.getHeader("Last-Modified"));
		assertEquals(-1, this.response.getDateHeader("Last-Modified"));
	}

	@Test  // SPR-10414
	public void modifyStatusAfterSendError() throws IOException {
		this.response.sendError(HttpServletResponse.SC_NOT_FOUND);
		this.response.setStatus(HttpServletResponse.SC_OK);
		assertEquals(HttpServletResponse.SC_NOT_FOUND, this.response.getStatus());
	}

	@Test  // SPR-10414
	@SuppressWarnings("deprecation")
	public void modifyStatusMessageAfterSendError() throws IOException {
		this.response.sendError(HttpServletResponse.SC_NOT_FOUND);
		this.response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Server Error");
		assertEquals(HttpServletResponse.SC_NOT_FOUND, this.response.getStatus());
	}

}
