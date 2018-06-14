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

package org.springframework.web.context.request;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

import static java.time.format.DateTimeFormatter.*;
import static org.junit.Assert.*;

/**
 * Parameterized tests for {@link ServletWebRequest}.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Markus Malkusch
 */
@RunWith(Parameterized.class)
public class ServletWebRequestHttpMethodsTests {

	private static final String CURRENT_TIME = "Wed, 9 Apr 2014 09:57:42 GMT";

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ServletWebRequest request;

	private Date currentDate;

	@Parameter
	public String method;

	@Parameters(name = "{0}")
	static public Iterable<Object[]> safeMethods() {
		return Arrays.asList(new Object[][] {
				{"GET"}, {"HEAD"}
		});
	}


	@Before
	public void setup() {
		this.currentDate = new Date();
		this.servletRequest = new MockHttpServletRequest(this.method, "http://example.org");
		this.servletResponse = new MockHttpServletResponse();
		this.request = new ServletWebRequest(this.servletRequest, this.servletResponse);
	}


	@Test
	public void checkNotModifiedNon2xxStatus() {
		long epochTime = this.currentDate.getTime();
		this.servletRequest.addHeader("If-Modified-Since", epochTime);
		this.servletResponse.setStatus(304);

		assertFalse(this.request.checkNotModified(epochTime));
		assertEquals(304, this.servletResponse.getStatus());
		assertNull(this.servletResponse.getHeader("Last-Modified"));
	}

	@Test  // SPR-13516
	public void checkNotModifiedInvalidStatus() {
		long epochTime = this.currentDate.getTime();
		this.servletRequest.addHeader("If-Modified-Since", epochTime);
		this.servletResponse.setStatus(0);

		assertFalse(this.request.checkNotModified(epochTime));
	}

	@Test  // SPR-14559
	public void checkNotModifiedInvalidIfNoneMatchHeader() {
		String etag = "\"etagvalue\"";
		this.servletRequest.addHeader("If-None-Match", "missingquotes");
		assertFalse(this.request.checkNotModified(etag));
		assertEquals(200, this.servletResponse.getStatus());
		assertEquals(etag, this.servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedHeaderAlreadySet() {
		long epochTime = this.currentDate.getTime();
		this.servletRequest.addHeader("If-Modified-Since", epochTime);
		this.servletResponse.addHeader("Last-Modified", CURRENT_TIME);

		assertTrue(this.request.checkNotModified(epochTime));
		assertEquals(304, this.servletResponse.getStatus());
		assertEquals(1, this.servletResponse.getHeaders("Last-Modified").size());
		assertEquals(CURRENT_TIME, this.servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkNotModifiedTimestamp() {
		long epochTime = this.currentDate.getTime();
		this.servletRequest.addHeader("If-Modified-Since", epochTime);

		assertTrue(this.request.checkNotModified(epochTime));
		assertEquals(304, this.servletResponse.getStatus());
		assertEquals(this.currentDate.getTime() / 1000, this.servletResponse.getDateHeader("Last-Modified") / 1000);
	}

	@Test
	public void checkModifiedTimestamp() {
		long oneMinuteAgo = this.currentDate.getTime() - (1000 * 60);
		this.servletRequest.addHeader("If-Modified-Since", oneMinuteAgo);

		assertFalse(this.request.checkNotModified(this.currentDate.getTime()));
		assertEquals(200, this.servletResponse.getStatus());
		assertEquals(this.currentDate.getTime() / 1000, this.servletResponse.getDateHeader("Last-Modified") / 1000);
	}

	@Test
	public void checkNotModifiedETag() {
		String etag = "\"Foo\"";
		this.servletRequest.addHeader("If-None-Match", etag);

		assertTrue(this.request.checkNotModified(etag));
		assertEquals(304, this.servletResponse.getStatus());
		assertEquals(etag, this.servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedETagWithSeparatorChars() {
		String etag = "\"Foo, Bar\"";
		this.servletRequest.addHeader("If-None-Match", etag);

		assertTrue(this.request.checkNotModified(etag));
		assertEquals(304, this.servletResponse.getStatus());
		assertEquals(etag, this.servletResponse.getHeader("ETag"));
	}


	@Test
	public void checkModifiedETag() {
		String currentETag = "\"Foo\"";
		String oldETag = "Bar";
		this.servletRequest.addHeader("If-None-Match", oldETag);

		assertFalse(this.request.checkNotModified(currentETag));
		assertEquals(200, this.servletResponse.getStatus());
		assertEquals(currentETag, this.servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedUnpaddedETag() {
		String etag = "Foo";
		String paddedETag = String.format("\"%s\"", etag);
		this.servletRequest.addHeader("If-None-Match", paddedETag);

		assertTrue(this.request.checkNotModified(etag));
		assertEquals(304, this.servletResponse.getStatus());
		assertEquals(paddedETag, this.servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkModifiedUnpaddedETag() {
		String currentETag = "Foo";
		String oldETag = "Bar";
		this.servletRequest.addHeader("If-None-Match", oldETag);

		assertFalse(this.request.checkNotModified(currentETag));
		assertEquals(200, this.servletResponse.getStatus());
		assertEquals(String.format("\"%s\"", currentETag), this.servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedWildcardIsIgnored() {
		String etag = "\"Foo\"";
		this.servletRequest.addHeader("If-None-Match", "*");

		assertFalse(this.request.checkNotModified(etag));
		assertEquals(200, this.servletResponse.getStatus());
		assertEquals(etag, this.servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedETagAndTimestamp() {
		String etag = "\"Foo\"";
		this.servletRequest.addHeader("If-None-Match", etag);
		this.servletRequest.addHeader("If-Modified-Since", this.currentDate.getTime());

		assertTrue(this.request.checkNotModified(etag, this.currentDate.getTime()));
		assertEquals(304, this.servletResponse.getStatus());
		assertEquals(etag, this.servletResponse.getHeader("ETag"));
		assertEquals(this.currentDate.getTime() / 1000, this.servletResponse.getDateHeader("Last-Modified") / 1000);
	}

	@Test  // SPR-14224
	public void checkNotModifiedETagAndModifiedTimestamp() {
		String etag = "\"Foo\"";
		this.servletRequest.addHeader("If-None-Match", etag);
		long currentEpoch = this.currentDate.getTime();
		long oneMinuteAgo = currentEpoch - (1000 * 60);
		this.servletRequest.addHeader("If-Modified-Since", oneMinuteAgo);

		assertTrue(this.request.checkNotModified(etag, currentEpoch));
		assertEquals(304, this.servletResponse.getStatus());
		assertEquals(etag, this.servletResponse.getHeader("ETag"));
		assertEquals(this.currentDate.getTime() / 1000, this.servletResponse.getDateHeader("Last-Modified") / 1000);
	}

	@Test
	public void checkModifiedETagAndNotModifiedTimestamp() {
		String currentETag = "\"Foo\"";
		String oldETag = "\"Bar\"";
		this.servletRequest.addHeader("If-None-Match", oldETag);
		long epochTime = this.currentDate.getTime();
		this.servletRequest.addHeader("If-Modified-Since", epochTime);

		assertFalse(this.request.checkNotModified(currentETag, epochTime));
		assertEquals(200, this.servletResponse.getStatus());
		assertEquals(currentETag, this.servletResponse.getHeader("ETag"));
		assertEquals(this.currentDate.getTime() / 1000, this.servletResponse.getDateHeader("Last-Modified") / 1000);
	}

	@Test
	public void checkNotModifiedETagWeakStrong() {
		String etag = "\"Foo\"";
		String weakETag = String.format("W/%s", etag);
		this.servletRequest.addHeader("If-None-Match", etag);

		assertTrue(this.request.checkNotModified(weakETag));
		assertEquals(304, this.servletResponse.getStatus());
		assertEquals(weakETag, this.servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedETagStrongWeak() {
		String etag = "\"Foo\"";
		this.servletRequest.addHeader("If-None-Match", String.format("W/%s", etag));

		assertTrue(this.request.checkNotModified(etag));
		assertEquals(304, this.servletResponse.getStatus());
		assertEquals(etag, this.servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedMultipleETags() {
		String etag = "\"Bar\"";
		String multipleETags = String.format("\"Foo\", %s", etag);
		this.servletRequest.addHeader("If-None-Match", multipleETags);

		assertTrue(this.request.checkNotModified(etag));
		assertEquals(304, this.servletResponse.getStatus());
		assertEquals(etag, this.servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedTimestampWithLengthPart() {
		long epochTime = ZonedDateTime.parse(CURRENT_TIME, RFC_1123_DATE_TIME).toInstant().toEpochMilli();
		this.servletRequest.setMethod("GET");
		this.servletRequest.addHeader("If-Modified-Since", "Wed, 09 Apr 2014 09:57:42 GMT; length=13774");

		assertTrue(this.request.checkNotModified(epochTime));
		assertEquals(304, this.servletResponse.getStatus());
		assertEquals(epochTime / 1000, this.servletResponse.getDateHeader("Last-Modified") / 1000);
	}

	@Test
	public void checkModifiedTimestampWithLengthPart() {
		long epochTime = ZonedDateTime.parse(CURRENT_TIME, RFC_1123_DATE_TIME).toInstant().toEpochMilli();
		this.servletRequest.setMethod("GET");
		this.servletRequest.addHeader("If-Modified-Since", "Wed, 08 Apr 2014 09:57:42 GMT; length=13774");

		assertFalse(this.request.checkNotModified(epochTime));
		assertEquals(200, this.servletResponse.getStatus());
		assertEquals(epochTime / 1000, this.servletResponse.getDateHeader("Last-Modified") / 1000);
	}

	@Test
	public void checkNotModifiedTimestampConditionalPut() {
		long currentEpoch = this.currentDate.getTime();
		long oneMinuteAgo = currentEpoch - (1000 * 60);
		this.servletRequest.setMethod("PUT");
		this.servletRequest.addHeader("If-UnModified-Since", currentEpoch);

		assertFalse(this.request.checkNotModified(oneMinuteAgo));
		assertEquals(200, this.servletResponse.getStatus());
		assertEquals(null, this.servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkNotModifiedTimestampConditionalPutConflict() {
		long currentEpoch = this.currentDate.getTime();
		long oneMinuteAgo = currentEpoch - (1000 * 60);
		this.servletRequest.setMethod("PUT");
		this.servletRequest.addHeader("If-UnModified-Since", oneMinuteAgo);

		assertTrue(this.request.checkNotModified(currentEpoch));
		assertEquals(412, this.servletResponse.getStatus());
		assertEquals(null, this.servletResponse.getHeader("Last-Modified"));
	}

}
