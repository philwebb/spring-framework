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

package org.springframework.web.server.adapter;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.get;

/**
 * "checkNotModified" unit tests for {@link DefaultServerWebExchange}.
 *
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public class DefaultServerWebExchangeCheckNotModifiedTests {

	private static final String CURRENT_TIME = "Wed, 09 Apr 2014 09:57:42 GMT";


	private SimpleDateFormat dateFormat;

	private Instant currentDate;

	@Parameter
	public HttpMethod method;

	@Parameters(name = "{0}")
	static public Iterable<Object[]> safeMethods() {
		return Arrays.asList(new Object[][] {
				{HttpMethod.GET},
				{HttpMethod.HEAD}
		});
	}


	@Before
	public void setup() throws URISyntaxException {
		this.currentDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		this.dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		this.dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}


	@Test
	public void checkNotModifiedNon2xxStatus() {
		MockServerHttpRequest request = get("/").ifModifiedSince(this.currentDate.toEpochMilli()).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		exchange.getResponse().setStatusCode(HttpStatus.NOT_MODIFIED);

		assertThat(exchange.checkNotModified(this.currentDate)).isFalse();
		assertEquals(304, exchange.getResponse().getStatusCode().value());
		assertEquals(-1, exchange.getResponse().getHeaders().getLastModified());
	}

	@Test // SPR-14559
	public void checkNotModifiedInvalidIfNoneMatchHeader() {
		String eTag = "\"etagvalue\"";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch("missingquotes"));
		assertThat(exchange.checkNotModified(eTag)).isFalse();
		assertNull(exchange.getResponse().getStatusCode());
		assertThat((Object) exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
	}

	@Test
	public void checkNotModifiedHeaderAlreadySet() {
		MockServerHttpRequest request = get("/").ifModifiedSince(currentDate.toEpochMilli()).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		exchange.getResponse().getHeaders().add("Last-Modified", CURRENT_TIME);

		assertThat(exchange.checkNotModified(currentDate)).isTrue();
		assertEquals(304, exchange.getResponse().getStatusCode().value());
		assertEquals(1, exchange.getResponse().getHeaders().get("Last-Modified").size());
		assertThat((Object) exchange.getResponse().getHeaders().getFirst("Last-Modified")).isEqualTo(CURRENT_TIME);
	}

	@Test
	public void checkNotModifiedTimestamp() throws Exception {
		MockServerHttpRequest request = get("/").ifModifiedSince(currentDate.toEpochMilli()).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(currentDate)).isTrue();

		assertEquals(304, exchange.getResponse().getStatusCode().value());
		assertEquals(currentDate.toEpochMilli(), exchange.getResponse().getHeaders().getLastModified());
	}

	@Test
	public void checkModifiedTimestamp() {
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		MockServerHttpRequest request = get("/").ifModifiedSince(oneMinuteAgo.toEpochMilli()).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(currentDate)).isFalse();

		assertNull(exchange.getResponse().getStatusCode());
		assertEquals(currentDate.toEpochMilli(), exchange.getResponse().getHeaders().getLastModified());
	}

	@Test
	public void checkNotModifiedETag() {
		String eTag = "\"Foo\"";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(eTag));

		assertThat(exchange.checkNotModified(eTag)).isTrue();

		assertEquals(304, exchange.getResponse().getStatusCode().value());
		assertThat((Object) exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
	}

	@Test
	public void checkNotModifiedETagWithSeparatorChars() {
		String eTag = "\"Foo, Bar\"";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(eTag));

		assertThat(exchange.checkNotModified(eTag)).isTrue();

		assertEquals(304, exchange.getResponse().getStatusCode().value());
		assertThat((Object) exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
	}


	@Test
	public void checkModifiedETag() {
		String currentETag = "\"Foo\"";
		String oldEtag = "Bar";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(oldEtag));

		assertThat(exchange.checkNotModified(currentETag)).isFalse();

		assertNull(exchange.getResponse().getStatusCode());
		assertThat((Object) exchange.getResponse().getHeaders().getETag()).isEqualTo(currentETag);
	}

	@Test
	public void checkNotModifiedUnpaddedETag() {
		String eTag = "Foo";
		String paddedEtag = String.format("\"%s\"", eTag);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(paddedEtag));

		assertThat(exchange.checkNotModified(eTag)).isTrue();

		assertEquals(304, exchange.getResponse().getStatusCode().value());
		assertThat((Object) exchange.getResponse().getHeaders().getETag()).isEqualTo(paddedEtag);
	}

	@Test
	public void checkModifiedUnpaddedETag() {
		String currentETag = "Foo";
		String oldEtag = "Bar";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(oldEtag));

		assertThat(exchange.checkNotModified(currentETag)).isFalse();

		assertNull(exchange.getResponse().getStatusCode());
		assertThat((Object) exchange.getResponse().getHeaders().getETag()).isEqualTo(String.format("\"%s\"", currentETag));
	}

	@Test
	public void checkNotModifiedWildcardIsIgnored() {
		String eTag = "\"Foo\"";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch("*"));
		assertThat(exchange.checkNotModified(eTag)).isFalse();

		assertNull(exchange.getResponse().getStatusCode());
		assertThat((Object) exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
	}

	@Test
	public void checkNotModifiedETagAndTimestamp() {
		String eTag = "\"Foo\"";
		long time = currentDate.toEpochMilli();
		MockServerHttpRequest request = get("/").ifNoneMatch(eTag).ifModifiedSince(time).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(eTag, currentDate)).isTrue();

		assertEquals(304, exchange.getResponse().getStatusCode().value());
		assertThat((Object) exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
		assertEquals(time, exchange.getResponse().getHeaders().getLastModified());
	}

	// SPR-14224
	@Test
	public void checkNotModifiedETagAndModifiedTimestamp() {
		String eTag = "\"Foo\"";
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/")
				.ifNoneMatch(eTag)
				.ifModifiedSince(oneMinuteAgo.toEpochMilli())
				);

		assertThat(exchange.checkNotModified(eTag, currentDate)).isTrue();

		assertEquals(304, exchange.getResponse().getStatusCode().value());
		assertThat((Object) exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
		assertEquals(currentDate.toEpochMilli(), exchange.getResponse().getHeaders().getLastModified());
	}

	@Test
	public void checkModifiedETagAndNotModifiedTimestamp() throws Exception {
		String currentETag = "\"Foo\"";
		String oldEtag = "\"Bar\"";
		long time = currentDate.toEpochMilli();
		MockServerHttpRequest request = get("/").ifNoneMatch(oldEtag).ifModifiedSince(time).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(currentETag, currentDate)).isFalse();

		assertNull(exchange.getResponse().getStatusCode());
		assertThat((Object) exchange.getResponse().getHeaders().getETag()).isEqualTo(currentETag);
		assertEquals(time, exchange.getResponse().getHeaders().getLastModified());
	}

	@Test
	public void checkNotModifiedETagWeakStrong() {
		String eTag = "\"Foo\"";
		String weakEtag = String.format("W/%s", eTag);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(eTag));

		assertThat(exchange.checkNotModified(weakEtag)).isTrue();

		assertEquals(304, exchange.getResponse().getStatusCode().value());
		assertThat((Object) exchange.getResponse().getHeaders().getETag()).isEqualTo(weakEtag);
	}

	@Test
	public void checkNotModifiedETagStrongWeak() {
		String eTag = "\"Foo\"";
		MockServerHttpRequest request = get("/").ifNoneMatch(String.format("W/%s", eTag)).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(eTag)).isTrue();

		assertEquals(304, exchange.getResponse().getStatusCode().value());
		assertThat((Object) exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
	}

	@Test
	public void checkNotModifiedMultipleETags() {
		String eTag = "\"Bar\"";
		String multipleETags = String.format("\"Foo\", %s", eTag);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(multipleETags));

		assertThat(exchange.checkNotModified(eTag)).isTrue();

		assertEquals(304, exchange.getResponse().getStatusCode().value());
		assertThat((Object) exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
	}

	@Test
	public void checkNotModifiedTimestampWithLengthPart() throws Exception {
		long epochTime = dateFormat.parse(CURRENT_TIME).getTime();
		String header = "Wed, 09 Apr 2014 09:57:42 GMT; length=13774";
		MockServerHttpRequest request = get("/").header("If-Modified-Since", header).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(Instant.ofEpochMilli(epochTime))).isTrue();

		assertEquals(304, exchange.getResponse().getStatusCode().value());
		assertEquals(epochTime, exchange.getResponse().getHeaders().getLastModified());
	}

	@Test
	public void checkModifiedTimestampWithLengthPart() throws Exception {
		long epochTime = dateFormat.parse(CURRENT_TIME).getTime();
		String header = "Tue, 08 Apr 2014 09:57:42 GMT; length=13774";
		MockServerHttpRequest request = get("/").header("If-Modified-Since", header).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(Instant.ofEpochMilli(epochTime))).isFalse();

		assertNull(exchange.getResponse().getStatusCode());
		assertEquals(epochTime, exchange.getResponse().getHeaders().getLastModified());
	}

	@Test
	public void checkNotModifiedTimestampConditionalPut() throws Exception {
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		long millis = currentDate.toEpochMilli();
		MockServerHttpRequest request = MockServerHttpRequest.put("/").ifUnmodifiedSince(millis).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(oneMinuteAgo)).isFalse();
		assertNull(exchange.getResponse().getStatusCode());
		assertEquals(-1, exchange.getResponse().getHeaders().getLastModified());
	}

	@Test
	public void checkNotModifiedTimestampConditionalPutConflict() throws Exception {
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		long millis = oneMinuteAgo.toEpochMilli();
		MockServerHttpRequest request = MockServerHttpRequest.put("/").ifUnmodifiedSince(millis).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(currentDate)).isTrue();
		assertEquals(412, exchange.getResponse().getStatusCode().value());
		assertEquals(-1, exchange.getResponse().getHeaders().getLastModified());
	}

}
