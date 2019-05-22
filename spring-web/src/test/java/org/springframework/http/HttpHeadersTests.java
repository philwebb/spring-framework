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

package org.springframework.http;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThat;


import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;

/**
 * Unit tests for {@link org.springframework.http.HttpHeaders}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class HttpHeadersTests {

	private final HttpHeaders headers = new HttpHeaders();


	@Test
	public void getFirst() {
		headers.add(HttpHeaders.CACHE_CONTROL, "max-age=1000, public");
		headers.add(HttpHeaders.CACHE_CONTROL, "s-maxage=1000");
		assertThat(headers.getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("max-age=1000, public");
	}

	@Test
	public void accept() {
		MediaType mediaType1 = new MediaType("text", "html");
		MediaType mediaType2 = new MediaType("text", "plain");
		List<MediaType> mediaTypes = new ArrayList<>(2);
		mediaTypes.add(mediaType1);
		mediaTypes.add(mediaType2);
		headers.setAccept(mediaTypes);
		assertThat((Object) headers.getAccept()).as("Invalid Accept header").isEqualTo(mediaTypes);
		assertThat((Object) headers.getFirst("Accept")).as("Invalid Accept header").isEqualTo("text/html, text/plain");
	}

	@Test  // SPR-9655
	public void acceptWithMultipleHeaderValues() {
		headers.add("Accept", "text/html");
		headers.add("Accept", "text/plain");
		List<MediaType> expected = Arrays.asList(new MediaType("text", "html"), new MediaType("text", "plain"));
		assertThat((Object) headers.getAccept()).as("Invalid Accept header").isEqualTo(expected);
	}

	@Test  // SPR-14506
	public void acceptWithMultipleCommaSeparatedHeaderValues() {
		headers.add("Accept", "text/html,text/pdf");
		headers.add("Accept", "text/plain,text/csv");
		List<MediaType> expected = Arrays.asList(new MediaType("text", "html"), new MediaType("text", "pdf"),
				new MediaType("text", "plain"), new MediaType("text", "csv"));
		assertThat((Object) headers.getAccept()).as("Invalid Accept header").isEqualTo(expected);
	}

	@Test
	public void acceptCharsets() {
		Charset charset1 = StandardCharsets.UTF_8;
		Charset charset2 = StandardCharsets.ISO_8859_1;
		List<Charset> charsets = new ArrayList<>(2);
		charsets.add(charset1);
		charsets.add(charset2);
		headers.setAcceptCharset(charsets);
		assertThat((Object) headers.getAcceptCharset()).as("Invalid Accept header").isEqualTo(charsets);
		assertThat((Object) headers.getFirst("Accept-Charset")).as("Invalid Accept header").isEqualTo("utf-8, iso-8859-1");
	}

	@Test
	public void acceptCharsetWildcard() {
		headers.set("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		assertThat((Object) headers.getAcceptCharset()).as("Invalid Accept header").isEqualTo(Arrays.asList(StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8));
	}

	@Test
	public void allow() {
		EnumSet<HttpMethod> methods = EnumSet.of(HttpMethod.GET, HttpMethod.POST);
		headers.setAllow(methods);
		assertThat((Object) headers.getAllow()).as("Invalid Allow header").isEqualTo(methods);
		assertThat((Object) headers.getFirst("Allow")).as("Invalid Allow header").isEqualTo("GET,POST");
	}

	@Test
	public void contentLength() {
		long length = 42L;
		headers.setContentLength(length);
		assertEquals("Invalid Content-Length header", length, headers.getContentLength());
		assertThat((Object) headers.getFirst("Content-Length")).as("Invalid Content-Length header").isEqualTo("42");
	}

	@Test
	public void contentType() {
		MediaType contentType = new MediaType("text", "html", StandardCharsets.UTF_8);
		headers.setContentType(contentType);
		assertThat((Object) headers.getContentType()).as("Invalid Content-Type header").isEqualTo(contentType);
		assertThat((Object) headers.getFirst("Content-Type")).as("Invalid Content-Type header").isEqualTo("text/html;charset=UTF-8");
	}

	@Test
	public void location() throws URISyntaxException {
		URI location = new URI("https://www.example.com/hotels");
		headers.setLocation(location);
		assertThat((Object) headers.getLocation()).as("Invalid Location header").isEqualTo(location);
		assertThat((Object) headers.getFirst("Location")).as("Invalid Location header").isEqualTo("https://www.example.com/hotels");
	}

	@Test
	public void eTag() {
		String eTag = "\"v2.6\"";
		headers.setETag(eTag);
		assertThat((Object) headers.getETag()).as("Invalid ETag header").isEqualTo(eTag);
		assertThat((Object) headers.getFirst("ETag")).as("Invalid ETag header").isEqualTo("\"v2.6\"");
	}

	@Test
	public void host() {
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 8080);
		headers.setHost(host);
		assertThat((Object) headers.getHost()).as("Invalid Host header").isEqualTo(host);
		assertThat((Object) headers.getFirst("Host")).as("Invalid Host header").isEqualTo("localhost:8080");
	}

	@Test
	public void hostNoPort() {
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 0);
		headers.setHost(host);
		assertThat((Object) headers.getHost()).as("Invalid Host header").isEqualTo(host);
		assertThat((Object) headers.getFirst("Host")).as("Invalid Host header").isEqualTo("localhost");
	}

	@Test
	public void ipv6Host() {
		InetSocketAddress host = InetSocketAddress.createUnresolved("[::1]", 0);
		headers.setHost(host);
		assertThat((Object) headers.getHost()).as("Invalid Host header").isEqualTo(host);
		assertThat((Object) headers.getFirst("Host")).as("Invalid Host header").isEqualTo("[::1]");
	}

	@Test
	public void illegalETag() {
		String eTag = "v2.6";
		assertThatIllegalArgumentException().isThrownBy(() ->
				headers.setETag(eTag));
	}

	@Test
	public void ifMatch() {
		String ifMatch = "\"v2.6\"";
		headers.setIfMatch(ifMatch);
		assertThat((Object) headers.getIfMatch().get(0)).as("Invalid If-Match header").isEqualTo(ifMatch);
		assertThat((Object) headers.getFirst("If-Match")).as("Invalid If-Match header").isEqualTo("\"v2.6\"");
	}

	@Test
	public void ifMatchIllegalHeader() {
		headers.setIfMatch("Illegal");
		assertThatIllegalArgumentException().isThrownBy(
				headers::getIfMatch);
	}

	@Test
	public void ifMatchMultipleHeaders() {
		headers.add(HttpHeaders.IF_MATCH, "\"v2,0\"");
		headers.add(HttpHeaders.IF_MATCH, "W/\"v2,1\", \"v2,2\"");
		assertThat((Object) headers.get(HttpHeaders.IF_MATCH).get(0)).as("Invalid If-Match header").isEqualTo("\"v2,0\"");
		assertThat((Object) headers.get(HttpHeaders.IF_MATCH).get(1)).as("Invalid If-Match header").isEqualTo("W/\"v2,1\", \"v2,2\"");
		assertThat(headers.getIfMatch()).contains("\"v2,0\"", "W/\"v2,1\"", "\"v2,2\"");
	}

	@Test
	public void ifNoneMatch() {
		String ifNoneMatch = "\"v2.6\"";
		headers.setIfNoneMatch(ifNoneMatch);
		assertThat((Object) headers.getIfNoneMatch().get(0)).as("Invalid If-None-Match header").isEqualTo(ifNoneMatch);
		assertThat((Object) headers.getFirst("If-None-Match")).as("Invalid If-None-Match header").isEqualTo("\"v2.6\"");
	}

	@Test
	public void ifNoneMatchWildCard() {
		String ifNoneMatch = "*";
		headers.setIfNoneMatch(ifNoneMatch);
		assertThat((Object) headers.getIfNoneMatch().get(0)).as("Invalid If-None-Match header").isEqualTo(ifNoneMatch);
		assertThat((Object) headers.getFirst("If-None-Match")).as("Invalid If-None-Match header").isEqualTo("*");
	}

	@Test
	public void ifNoneMatchList() {
		String ifNoneMatch1 = "\"v2.6\"";
		String ifNoneMatch2 = "\"v2.7\", \"v2.8\"";
		List<String> ifNoneMatchList = new ArrayList<>(2);
		ifNoneMatchList.add(ifNoneMatch1);
		ifNoneMatchList.add(ifNoneMatch2);
		headers.setIfNoneMatch(ifNoneMatchList);
		assertThat(headers.getIfNoneMatch()).contains("\"v2.6\"", "\"v2.7\"", "\"v2.8\"");
		assertThat((Object) headers.getFirst("If-None-Match")).as("Invalid If-None-Match header").isEqualTo("\"v2.6\", \"v2.7\", \"v2.8\"");
	}

	@Test
	public void date() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		headers.setDate(date);
		assertEquals("Invalid Date header", date, headers.getDate());
		assertThat((Object) headers.getFirst("date")).as("Invalid Date header").isEqualTo("Thu, 18 Dec 2008 10:20:00 GMT");

		// RFC 850
		headers.set("Date", "Thu, 18 Dec 2008 10:20:00 GMT");
		assertEquals("Invalid Date header", date, headers.getDate());
	}

	@Test
	public void dateInvalid() {
		headers.set("Date", "Foo Bar Baz");
		assertThatIllegalArgumentException().isThrownBy(
				headers::getDate);
	}

	@Test
	public void dateOtherLocale() {
		Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(new Locale("nl", "nl"));
			Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
			calendar.setTimeZone(TimeZone.getTimeZone("CET"));
			long date = calendar.getTimeInMillis();
			headers.setDate(date);
			assertThat((Object) headers.getFirst("date")).as("Invalid Date header").isEqualTo("Thu, 18 Dec 2008 10:20:00 GMT");
			assertEquals("Invalid Date header", date, headers.getDate());
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}

	@Test
	public void lastModified() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		headers.setLastModified(date);
		assertEquals("Invalid Last-Modified header", date, headers.getLastModified());
		assertThat((Object) headers.getFirst("last-modified")).as("Invalid Last-Modified header").isEqualTo("Thu, 18 Dec 2008 10:20:00 GMT");
	}

	@Test
	public void expiresLong() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		headers.setExpires(date);
		assertEquals("Invalid Expires header", date, headers.getExpires());
		assertThat((Object) headers.getFirst("expires")).as("Invalid Expires header").isEqualTo("Thu, 18 Dec 2008 10:20:00 GMT");
	}

	@Test
	public void expiresZonedDateTime() {
		ZonedDateTime zonedDateTime = ZonedDateTime.of(2008, 12, 18, 10, 20, 0, 0, ZoneId.of("GMT"));
		headers.setExpires(zonedDateTime);
		assertEquals("Invalid Expires header", zonedDateTime.toInstant().toEpochMilli(), headers.getExpires());
		assertThat((Object) headers.getFirst("expires")).as("Invalid Expires header").isEqualTo("Thu, 18 Dec 2008 10:20:00 GMT");
	}

	@Test  // SPR-10648 (example is from INT-3063)
	public void expiresInvalidDate() {
		headers.set("Expires", "-1");
		assertEquals(-1, headers.getExpires());
	}

	@Test
	public void ifModifiedSince() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		headers.setIfModifiedSince(date);
		assertEquals("Invalid If-Modified-Since header", date, headers.getIfModifiedSince());
		assertThat((Object) headers.getFirst("if-modified-since")).as("Invalid If-Modified-Since header").isEqualTo("Thu, 18 Dec 2008 10:20:00 GMT");
	}

	@Test  // SPR-14144
	public void invalidIfModifiedSinceHeader() {
		headers.set(HttpHeaders.IF_MODIFIED_SINCE, "0");
		assertEquals(-1, headers.getIfModifiedSince());

		headers.set(HttpHeaders.IF_MODIFIED_SINCE, "-1");
		assertEquals(-1, headers.getIfModifiedSince());

		headers.set(HttpHeaders.IF_MODIFIED_SINCE, "XXX");
		assertEquals(-1, headers.getIfModifiedSince());
	}

	@Test
	public void pragma() {
		String pragma = "no-cache";
		headers.setPragma(pragma);
		assertThat((Object) headers.getPragma()).as("Invalid Pragma header").isEqualTo(pragma);
		assertThat((Object) headers.getFirst("pragma")).as("Invalid Pragma header").isEqualTo("no-cache");
	}

	@Test
	public void cacheControl() {
		headers.setCacheControl("no-cache");
		assertThat((Object) headers.getCacheControl()).as("Invalid Cache-Control header").isEqualTo("no-cache");
		assertThat((Object) headers.getFirst("cache-control")).as("Invalid Cache-Control header").isEqualTo("no-cache");
	}

	@Test
	public void cacheControlBuilder() {
		headers.setCacheControl(CacheControl.noCache());
		assertThat((Object) headers.getCacheControl()).as("Invalid Cache-Control header").isEqualTo("no-cache");
		assertThat((Object) headers.getFirst("cache-control")).as("Invalid Cache-Control header").isEqualTo("no-cache");
	}

	@Test
	public void cacheControlEmpty() {
		headers.setCacheControl(CacheControl.empty());
		assertNull("Invalid Cache-Control header", headers.getCacheControl());
		assertNull("Invalid Cache-Control header", headers.getFirst("cache-control"));
	}

	@Test
	public void cacheControlAllValues() {
		headers.add(HttpHeaders.CACHE_CONTROL, "max-age=1000, public");
		headers.add(HttpHeaders.CACHE_CONTROL, "s-maxage=1000");
		assertThat((Object) headers.getCacheControl()).isEqualTo("max-age=1000, public, s-maxage=1000");
	}

	@Test
	public void contentDisposition() {
		ContentDisposition disposition = headers.getContentDisposition();
		assertNotNull(disposition);
		assertThat((Object) headers.getContentDisposition()).as("Invalid Content-Disposition header").isEqualTo(ContentDisposition.empty());

		disposition = ContentDisposition.builder("attachment").name("foo").filename("foo.txt").size(123L).build();
		headers.setContentDisposition(disposition);
		assertThat((Object) headers.getContentDisposition()).as("Invalid Content-Disposition header").isEqualTo(disposition);
	}

	@Test  // SPR-11917
	public void getAllowEmptySet() {
		headers.setAllow(Collections.emptySet());
		assertThat(headers.getAllow()).isEmpty();
	}

	@Test
	public void accessControlAllowCredentials() {
		assertThat(headers.getAccessControlAllowCredentials()).isFalse();
		headers.setAccessControlAllowCredentials(false);
		assertThat(headers.getAccessControlAllowCredentials()).isFalse();
		headers.setAccessControlAllowCredentials(true);
		assertThat(headers.getAccessControlAllowCredentials()).isTrue();
	}

	@Test
	public void accessControlAllowHeaders() {
		List<String> allowedHeaders = headers.getAccessControlAllowHeaders();
		assertThat(allowedHeaders).isEmpty();
		headers.setAccessControlAllowHeaders(Arrays.asList("header1", "header2"));
		allowedHeaders = headers.getAccessControlAllowHeaders();
		assertThat(Arrays.asList("header1", "header2")).isEqualTo(allowedHeaders);
	}

	@Test
	public void accessControlAllowHeadersMultipleValues() {
		List<String> allowedHeaders = headers.getAccessControlAllowHeaders();
		assertThat(allowedHeaders).isEmpty();
		headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "header1, header2");
		headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "header3");
		allowedHeaders = headers.getAccessControlAllowHeaders();
		assertThat((Object) allowedHeaders).isEqualTo(Arrays.asList("header1", "header2", "header3"));
	}

	@Test
	public void accessControlAllowMethods() {
		List<HttpMethod> allowedMethods = headers.getAccessControlAllowMethods();
		assertThat(allowedMethods).isEmpty();
		headers.setAccessControlAllowMethods(Arrays.asList(HttpMethod.GET, HttpMethod.POST));
		allowedMethods = headers.getAccessControlAllowMethods();
		assertThat(Arrays.asList(HttpMethod.GET, HttpMethod.POST)).isEqualTo(allowedMethods);
	}

	@Test
	public void accessControlAllowOrigin() {
		assertNull(headers.getAccessControlAllowOrigin());
		headers.setAccessControlAllowOrigin("*");
		assertThat((Object) headers.getAccessControlAllowOrigin()).isEqualTo("*");
	}

	@Test
	public void accessControlExposeHeaders() {
		List<String> exposedHeaders = headers.getAccessControlExposeHeaders();
		assertThat(exposedHeaders).isEmpty();
		headers.setAccessControlExposeHeaders(Arrays.asList("header1", "header2"));
		exposedHeaders = headers.getAccessControlExposeHeaders();
		assertThat(Arrays.asList("header1", "header2")).isEqualTo(exposedHeaders);
	}

	@Test
	public void accessControlMaxAge() {
		assertEquals(-1, headers.getAccessControlMaxAge());
		headers.setAccessControlMaxAge(3600);
		assertEquals(3600, headers.getAccessControlMaxAge());
	}

	@Test
	public void accessControlRequestHeaders() {
		List<String> requestHeaders = headers.getAccessControlRequestHeaders();
		assertThat(requestHeaders).isEmpty();
		headers.setAccessControlRequestHeaders(Arrays.asList("header1", "header2"));
		requestHeaders = headers.getAccessControlRequestHeaders();
		assertThat(Arrays.asList("header1", "header2")).isEqualTo(requestHeaders);
	}

	@Test
	public void accessControlRequestMethod() {
		assertNull(headers.getAccessControlRequestMethod());
		headers.setAccessControlRequestMethod(HttpMethod.POST);
		assertThat((Object) headers.getAccessControlRequestMethod()).isEqualTo(HttpMethod.POST);
	}

	@Test
	public void acceptLanguage() {
		String headerValue = "fr-ch, fr;q=0.9, en-*;q=0.8, de;q=0.7, *;q=0.5";
		headers.setAcceptLanguage(Locale.LanguageRange.parse(headerValue));
		assertThat((Object) headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE)).isEqualTo(headerValue);

		List<Locale.LanguageRange> expectedRanges = Arrays.asList(
				new Locale.LanguageRange("fr-ch"),
				new Locale.LanguageRange("fr", 0.9),
				new Locale.LanguageRange("en-*", 0.8),
				new Locale.LanguageRange("de", 0.7),
				new Locale.LanguageRange("*", 0.5)
		);
		assertThat((Object) headers.getAcceptLanguage()).isEqualTo(expectedRanges);
		assertThat((Object) headers.getAcceptLanguageAsLocales().get(0)).isEqualTo(Locale.forLanguageTag("fr-ch"));

		headers.setAcceptLanguageAsLocales(Collections.singletonList(Locale.FRANCE));
		assertThat((Object) headers.getAcceptLanguageAsLocales().get(0)).isEqualTo(Locale.FRANCE);
	}

	@Test // SPR-15603
	public void acceptLanguageWithEmptyValue() throws Exception {
		this.headers.set(HttpHeaders.ACCEPT_LANGUAGE, "");
		assertThat((Object) this.headers.getAcceptLanguageAsLocales()).isEqualTo(Collections.emptyList());
	}

	@Test
	public void contentLanguage() {
		headers.setContentLanguage(Locale.FRANCE);
		assertThat((Object) headers.getContentLanguage()).isEqualTo(Locale.FRANCE);
		assertThat((Object) headers.getFirst(HttpHeaders.CONTENT_LANGUAGE)).isEqualTo("fr-FR");
	}

	@Test
	public void contentLanguageSerialized() {
		headers.set(HttpHeaders.CONTENT_LANGUAGE,  "de, en_CA");
		assertThat((Object) headers.getContentLanguage()).as("Expected one (first) locale").isEqualTo(Locale.GERMAN);
	}

	@Test
	public void firstDate() {
		headers.setDate(HttpHeaders.DATE, 1496370120000L);
		assertThat(headers.getFirstDate(HttpHeaders.DATE)).isEqualTo(1496370120000L);

		headers.clear();

		headers.add(HttpHeaders.DATE, "Fri, 02 Jun 2017 02:22:00 GMT");
		headers.add(HttpHeaders.DATE, "Sat, 18 Dec 2010 10:20:00 GMT");
		assertThat(headers.getFirstDate(HttpHeaders.DATE)).isEqualTo(1496370120000L);
	}

	@Test
	public void firstZonedDateTime() {
		ZonedDateTime date = ZonedDateTime.of(2017, 6, 2, 2, 22, 0, 0, ZoneId.of("GMT"));
		headers.setZonedDateTime(HttpHeaders.DATE, date);
		assertThat(headers.getFirst(HttpHeaders.DATE)).isEqualTo("Fri, 02 Jun 2017 02:22:00 GMT");
		assertThat(headers.getFirstZonedDateTime(HttpHeaders.DATE).isEqual(date)).isTrue();

		headers.clear();
		headers.add(HttpHeaders.DATE, "Fri, 02 Jun 2017 02:22:00 GMT");
		headers.add(HttpHeaders.DATE, "Sat, 18 Dec 2010 10:20:00 GMT");
		assertThat(headers.getFirstZonedDateTime(HttpHeaders.DATE).isEqual(date)).isTrue();
		assertThat((Object) headers.get(HttpHeaders.DATE)).isEqualTo(Arrays.asList("Fri, 02 Jun 2017 02:22:00 GMT",
				"Sat, 18 Dec 2010 10:20:00 GMT"));

		// obsolete RFC 850 format
		headers.clear();
		headers.set(HttpHeaders.DATE, "Friday, 02-Jun-17 02:22:00 GMT");
		assertThat(headers.getFirstZonedDateTime(HttpHeaders.DATE).isEqual(date)).isTrue();

		// ANSI C's asctime() format
		headers.clear();
		headers.set(HttpHeaders.DATE, "Fri Jun 02 02:22:00 2017");
		assertThat(headers.getFirstZonedDateTime(HttpHeaders.DATE).isEqual(date)).isTrue();
	}

	@Test
	public void basicAuth() {
		String username = "foo";
		String password = "bar";
		headers.setBasicAuth(username, password);
		String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
		assertNotNull(authorization);
		assertThat(authorization.startsWith("Basic ")).isTrue();
		byte[] result = Base64.getDecoder().decode(authorization.substring(6).getBytes(StandardCharsets.ISO_8859_1));
		assertThat((Object) new String(result, StandardCharsets.ISO_8859_1)).isEqualTo("foo:bar");
	}

	@Test
	public void basicAuthIllegalChar() {
		String username = "foo";
		String password = "\u03BB";
		assertThatIllegalArgumentException().isThrownBy(() ->
				headers.setBasicAuth(username, password));
	}

	@Test
	public void bearerAuth() {
		String token = "foo";

		headers.setBearerAuth(token);
		String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
		assertThat((Object) authorization).isEqualTo("Bearer foo");
	}

	@Test
	public void removalFromKeySetRemovesEntryFromUnderlyingMap() {
		String headerName = "MyHeader";
		String headerValue = "value";

		assertThat(headers.isEmpty()).isTrue();
		headers.add(headerName, headerValue);
		assertThat(headers.containsKey(headerName)).isTrue();
		headers.keySet().removeIf(key -> key.equals(headerName));
		assertThat(headers.isEmpty()).isTrue();
		headers.add(headerName, headerValue);
		assertThat((Object) headers.get(headerName).get(0)).isEqualTo(headerValue);
	}

	@Test
	public void removalFromEntrySetRemovesEntryFromUnderlyingMap() {
		String headerName = "MyHeader";
		String headerValue = "value";

		assertThat(headers.isEmpty()).isTrue();
		headers.add(headerName, headerValue);
		assertThat(headers.containsKey(headerName)).isTrue();
		headers.entrySet().removeIf(entry -> entry.getKey().equals(headerName));
		assertThat(headers.isEmpty()).isTrue();
		headers.add(headerName, headerValue);
		assertThat((Object) headers.get(headerName).get(0)).isEqualTo(headerValue);
	}

}
