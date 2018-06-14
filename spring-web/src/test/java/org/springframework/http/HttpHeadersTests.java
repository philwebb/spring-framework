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

package org.springframework.http;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.hamcrest.Matchers;
import org.junit.Test;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link org.springframework.http.HttpHeaders}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Juergen Hoeller
 */
public class HttpHeadersTests {

	private final HttpHeaders headers = new HttpHeaders();


	@Test
	public void getFirst() {
		this.headers.add(HttpHeaders.CACHE_CONTROL, "max-age=1000, public");
		this.headers.add(HttpHeaders.CACHE_CONTROL, "s-maxage=1000");
		assertThat(this.headers.getFirst(HttpHeaders.CACHE_CONTROL), is("max-age=1000, public"));
	}

	@Test
	public void accept() {
		MediaType mediaType1 = new MediaType("text", "html");
		MediaType mediaType2 = new MediaType("text", "plain");
		List<MediaType> mediaTypes = new ArrayList<>(2);
		mediaTypes.add(mediaType1);
		mediaTypes.add(mediaType2);
		this.headers.setAccept(mediaTypes);
		assertEquals("Invalid Accept header", mediaTypes, this.headers.getAccept());
		assertEquals("Invalid Accept header", "text/html, text/plain", this.headers.getFirst("Accept"));
	}

	@Test  // SPR-9655
	public void acceptWithMultipleHeaderValues() {
		this.headers.add("Accept", "text/html");
		this.headers.add("Accept", "text/plain");
		List<MediaType> expected = Arrays.asList(new MediaType("text", "html"), new MediaType("text", "plain"));
		assertEquals("Invalid Accept header", expected, this.headers.getAccept());
	}

	@Test  // SPR-14506
	public void acceptWithMultipleCommaSeparatedHeaderValues() {
		this.headers.add("Accept", "text/html,text/pdf");
		this.headers.add("Accept", "text/plain,text/csv");
		List<MediaType> expected = Arrays.asList(new MediaType("text", "html"), new MediaType("text", "pdf"),
				new MediaType("text", "plain"), new MediaType("text", "csv"));
		assertEquals("Invalid Accept header", expected, this.headers.getAccept());
	}

	@Test
	public void acceptCharsets() {
		Charset charset1 = StandardCharsets.UTF_8;
		Charset charset2 = StandardCharsets.ISO_8859_1;
		List<Charset> charsets = new ArrayList<>(2);
		charsets.add(charset1);
		charsets.add(charset2);
		this.headers.setAcceptCharset(charsets);
		assertEquals("Invalid Accept header", charsets, this.headers.getAcceptCharset());
		assertEquals("Invalid Accept header", "utf-8, iso-8859-1", this.headers.getFirst("Accept-Charset"));
	}

	@Test
	public void acceptCharsetWildcard() {
		this.headers.set("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		assertEquals("Invalid Accept header", Arrays.asList(StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8),
				this.headers.getAcceptCharset());
	}

	@Test
	public void allow() {
		EnumSet<HttpMethod> methods = EnumSet.of(HttpMethod.GET, HttpMethod.POST);
		this.headers.setAllow(methods);
		assertEquals("Invalid Allow header", methods, this.headers.getAllow());
		assertEquals("Invalid Allow header", "GET,POST", this.headers.getFirst("Allow"));
	}

	@Test
	public void contentLength() {
		long length = 42L;
		this.headers.setContentLength(length);
		assertEquals("Invalid Content-Length header", length, this.headers.getContentLength());
		assertEquals("Invalid Content-Length header", "42", this.headers.getFirst("Content-Length"));
	}

	@Test
	public void contentType() {
		MediaType contentType = new MediaType("text", "html", StandardCharsets.UTF_8);
		this.headers.setContentType(contentType);
		assertEquals("Invalid Content-Type header", contentType, this.headers.getContentType());
		assertEquals("Invalid Content-Type header", "text/html;charset=UTF-8", this.headers.getFirst("Content-Type"));
	}

	@Test
	public void location() throws URISyntaxException {
		URI location = new URI("http://www.example.com/hotels");
		this.headers.setLocation(location);
		assertEquals("Invalid Location header", location, this.headers.getLocation());
		assertEquals("Invalid Location header", "http://www.example.com/hotels", this.headers.getFirst("Location"));
	}

	@Test
	public void eTag() {
		String eTag = "\"v2.6\"";
		this.headers.setETag(eTag);
		assertEquals("Invalid ETag header", eTag, this.headers.getETag());
		assertEquals("Invalid ETag header", "\"v2.6\"", this.headers.getFirst("ETag"));
	}

	@Test
	public void host() {
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 8080);
		this.headers.setHost(host);
		assertEquals("Invalid Host header", host, this.headers.getHost());
		assertEquals("Invalid Host header", "localhost:8080", this.headers.getFirst("Host"));
	}

	@Test
	public void hostNoPort() {
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 0);
		this.headers.setHost(host);
		assertEquals("Invalid Host header", host, this.headers.getHost());
		assertEquals("Invalid Host header", "localhost", this.headers.getFirst("Host"));
	}

	@Test
	public void ipv6Host() {
		InetSocketAddress host = InetSocketAddress.createUnresolved("[::1]", 0);
		this.headers.setHost(host);
		assertEquals("Invalid Host header", host, this.headers.getHost());
		assertEquals("Invalid Host header", "[::1]", this.headers.getFirst("Host"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void illegalETag() {
		String eTag = "v2.6";
		this.headers.setETag(eTag);
		assertEquals("Invalid ETag header", eTag, this.headers.getETag());
		assertEquals("Invalid ETag header", "\"v2.6\"", this.headers.getFirst("ETag"));
	}

	@Test
	public void ifMatch() {
		String ifMatch = "\"v2.6\"";
		this.headers.setIfMatch(ifMatch);
		assertEquals("Invalid If-Match header", ifMatch, this.headers.getIfMatch().get(0));
		assertEquals("Invalid If-Match header", "\"v2.6\"", this.headers.getFirst("If-Match"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void ifMatchIllegalHeader() {
		this.headers.setIfMatch("Illegal");
		this.headers.getIfMatch();
	}

	@Test
	public void ifMatchMultipleHeaders() {
		this.headers.add(HttpHeaders.IF_MATCH, "\"v2,0\"");
		this.headers.add(HttpHeaders.IF_MATCH, "W/\"v2,1\", \"v2,2\"");
		assertEquals("Invalid If-Match header", "\"v2,0\"", this.headers.get(HttpHeaders.IF_MATCH).get(0));
		assertEquals("Invalid If-Match header", "W/\"v2,1\", \"v2,2\"", this.headers.get(HttpHeaders.IF_MATCH).get(1));
		assertThat(this.headers.getIfMatch(), Matchers.contains("\"v2,0\"", "W/\"v2,1\"", "\"v2,2\""));
	}

	@Test
	public void ifNoneMatch() {
		String ifNoneMatch = "\"v2.6\"";
		this.headers.setIfNoneMatch(ifNoneMatch);
		assertEquals("Invalid If-None-Match header", ifNoneMatch, this.headers.getIfNoneMatch().get(0));
		assertEquals("Invalid If-None-Match header", "\"v2.6\"", this.headers.getFirst("If-None-Match"));
	}

	@Test
	public void ifNoneMatchWildCard() {
		String ifNoneMatch = "*";
		this.headers.setIfNoneMatch(ifNoneMatch);
		assertEquals("Invalid If-None-Match header", ifNoneMatch, this.headers.getIfNoneMatch().get(0));
		assertEquals("Invalid If-None-Match header", "*", this.headers.getFirst("If-None-Match"));
	}

	@Test
	public void ifNoneMatchList() {
		String ifNoneMatch1 = "\"v2.6\"";
		String ifNoneMatch2 = "\"v2.7\", \"v2.8\"";
		List<String> ifNoneMatchList = new ArrayList<>(2);
		ifNoneMatchList.add(ifNoneMatch1);
		ifNoneMatchList.add(ifNoneMatch2);
		this.headers.setIfNoneMatch(ifNoneMatchList);
		assertThat(this.headers.getIfNoneMatch(), Matchers.contains("\"v2.6\"", "\"v2.7\"", "\"v2.8\""));
		assertEquals("Invalid If-None-Match header", "\"v2.6\", \"v2.7\", \"v2.8\"", this.headers.getFirst("If-None-Match"));
	}

	@Test
	public void date() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		this.headers.setDate(date);
		assertEquals("Invalid Date header", date, this.headers.getDate());
		assertEquals("Invalid Date header", "Thu, 18 Dec 2008 10:20:00 GMT", this.headers.getFirst("date"));

		// RFC 850
		this.headers.set("Date", "Thu, 18 Dec 2008 10:20:00 GMT");
		assertEquals("Invalid Date header", date, this.headers.getDate());
	}

	@Test(expected = IllegalArgumentException.class)
	public void dateInvalid() {
		this.headers.set("Date", "Foo Bar Baz");
		this.headers.getDate();
	}

	@Test
	public void dateOtherLocale() {
		Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(new Locale("nl", "nl"));
			Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
			calendar.setTimeZone(TimeZone.getTimeZone("CET"));
			long date = calendar.getTimeInMillis();
			this.headers.setDate(date);
			assertEquals("Invalid Date header", "Thu, 18 Dec 2008 10:20:00 GMT", this.headers.getFirst("date"));
			assertEquals("Invalid Date header", date, this.headers.getDate());
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
		this.headers.setLastModified(date);
		assertEquals("Invalid Last-Modified header", date, this.headers.getLastModified());
		assertEquals("Invalid Last-Modified header", "Thu, 18 Dec 2008 10:20:00 GMT",
				this.headers.getFirst("last-modified"));
	}

	@Test
	public void expiresLong() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		this.headers.setExpires(date);
		assertEquals("Invalid Expires header", date, this.headers.getExpires());
		assertEquals("Invalid Expires header", "Thu, 18 Dec 2008 10:20:00 GMT", this.headers.getFirst("expires"));
	}

	@Test
	public void expiresZonedDateTime() {
		ZonedDateTime zonedDateTime = ZonedDateTime.of(2008, 12, 18, 10, 20, 0, 0, ZoneId.of("GMT"));
		this.headers.setExpires(zonedDateTime);
		assertEquals("Invalid Expires header", zonedDateTime.toInstant().toEpochMilli(), this.headers.getExpires());
		assertEquals("Invalid Expires header", "Thu, 18 Dec 2008 10:20:00 GMT", this.headers.getFirst("expires"));
	}

	@Test(expected = DateTimeException.class)  // SPR-16560
	public void expiresLargeDate() {
		this.headers.setExpires(Long.MAX_VALUE);
	}

	@Test  // SPR-10648 (example is from INT-3063)
	public void expiresInvalidDate() {
		this.headers.set("Expires", "-1");
		assertEquals(-1, this.headers.getExpires());
	}

	@Test
	public void ifModifiedSince() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		this.headers.setIfModifiedSince(date);
		assertEquals("Invalid If-Modified-Since header", date, this.headers.getIfModifiedSince());
		assertEquals("Invalid If-Modified-Since header", "Thu, 18 Dec 2008 10:20:00 GMT",
				this.headers.getFirst("if-modified-since"));
	}

	@Test  // SPR-14144
	public void invalidIfModifiedSinceHeader() {
		this.headers.set(HttpHeaders.IF_MODIFIED_SINCE, "0");
		assertEquals(-1, this.headers.getIfModifiedSince());

		this.headers.set(HttpHeaders.IF_MODIFIED_SINCE, "-1");
		assertEquals(-1, this.headers.getIfModifiedSince());

		this.headers.set(HttpHeaders.IF_MODIFIED_SINCE, "XXX");
		assertEquals(-1, this.headers.getIfModifiedSince());
	}

	@Test
	public void pragma() {
		String pragma = "no-cache";
		this.headers.setPragma(pragma);
		assertEquals("Invalid Pragma header", pragma, this.headers.getPragma());
		assertEquals("Invalid Pragma header", "no-cache", this.headers.getFirst("pragma"));
	}

	@Test
	public void cacheControl() {
		this.headers.setCacheControl("no-cache");
		assertEquals("Invalid Cache-Control header", "no-cache", this.headers.getCacheControl());
		assertEquals("Invalid Cache-Control header", "no-cache", this.headers.getFirst("cache-control"));
	}

	@Test
	public void cacheControlBuilder() {
		this.headers.setCacheControl(CacheControl.noCache());
		assertEquals("Invalid Cache-Control header", "no-cache", this.headers.getCacheControl());
		assertEquals("Invalid Cache-Control header", "no-cache", this.headers.getFirst("cache-control"));
	}

	@Test
	public void cacheControlAllValues() {
		this.headers.add(HttpHeaders.CACHE_CONTROL, "max-age=1000, public");
		this.headers.add(HttpHeaders.CACHE_CONTROL, "s-maxage=1000");
		assertThat(this.headers.getCacheControl(), is("max-age=1000, public, s-maxage=1000"));
	}

	@Test
	public void contentDisposition() {
		ContentDisposition disposition = this.headers.getContentDisposition();
		assertNotNull(disposition);
		assertEquals("Invalid Content-Disposition header", ContentDisposition.empty(), this.headers.getContentDisposition());

		disposition = ContentDisposition.builder("attachment").name("foo").filename("foo.txt").size(123L).build();
		this.headers.setContentDisposition(disposition);
		assertEquals("Invalid Content-Disposition header", disposition, this.headers.getContentDisposition());
	}

	@Test  // SPR-11917
	public void getAllowEmptySet() {
		this.headers.setAllow(Collections.<HttpMethod> emptySet());
		assertThat(this.headers.getAllow(), Matchers.emptyCollectionOf(HttpMethod.class));
	}

	@Test
	public void accessControlAllowCredentials() {
		assertFalse(this.headers.getAccessControlAllowCredentials());
		this.headers.setAccessControlAllowCredentials(false);
		assertFalse(this.headers.getAccessControlAllowCredentials());
		this.headers.setAccessControlAllowCredentials(true);
		assertTrue(this.headers.getAccessControlAllowCredentials());
	}

	@Test
	public void accessControlAllowHeaders() {
		List<String> allowedHeaders = this.headers.getAccessControlAllowHeaders();
		assertThat(allowedHeaders, Matchers.emptyCollectionOf(String.class));
		this.headers.setAccessControlAllowHeaders(Arrays.asList("header1", "header2"));
		allowedHeaders = this.headers.getAccessControlAllowHeaders();
		assertEquals(allowedHeaders, Arrays.asList("header1", "header2"));
	}

	@Test
	public void accessControlAllowHeadersMultipleValues() {
		List<String> allowedHeaders = this.headers.getAccessControlAllowHeaders();
		assertThat(allowedHeaders, Matchers.emptyCollectionOf(String.class));
		this.headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "header1, header2");
		this.headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "header3");
		allowedHeaders = this.headers.getAccessControlAllowHeaders();
		assertEquals(Arrays.asList("header1", "header2", "header3"), allowedHeaders);
	}

	@Test
	public void accessControlAllowMethods() {
		List<HttpMethod> allowedMethods = this.headers.getAccessControlAllowMethods();
		assertThat(allowedMethods, Matchers.emptyCollectionOf(HttpMethod.class));
		this.headers.setAccessControlAllowMethods(Arrays.asList(HttpMethod.GET, HttpMethod.POST));
		allowedMethods = this.headers.getAccessControlAllowMethods();
		assertEquals(allowedMethods, Arrays.asList(HttpMethod.GET, HttpMethod.POST));
	}

	@Test
	public void accessControlAllowOrigin() {
		assertNull(this.headers.getAccessControlAllowOrigin());
		this.headers.setAccessControlAllowOrigin("*");
		assertEquals("*", this.headers.getAccessControlAllowOrigin());
	}

	@Test
	public void accessControlExposeHeaders() {
		List<String> exposedHeaders = this.headers.getAccessControlExposeHeaders();
		assertThat(exposedHeaders, Matchers.emptyCollectionOf(String.class));
		this.headers.setAccessControlExposeHeaders(Arrays.asList("header1", "header2"));
		exposedHeaders = this.headers.getAccessControlExposeHeaders();
		assertEquals(exposedHeaders, Arrays.asList("header1", "header2"));
	}

	@Test
	public void accessControlMaxAge() {
		assertEquals(-1, this.headers.getAccessControlMaxAge());
		this.headers.setAccessControlMaxAge(3600);
		assertEquals(3600, this.headers.getAccessControlMaxAge());
	}

	@Test
	public void accessControlRequestHeaders() {
		List<String> requestHeaders = this.headers.getAccessControlRequestHeaders();
		assertThat(requestHeaders, Matchers.emptyCollectionOf(String.class));
		this.headers.setAccessControlRequestHeaders(Arrays.asList("header1", "header2"));
		requestHeaders = this.headers.getAccessControlRequestHeaders();
		assertEquals(requestHeaders, Arrays.asList("header1", "header2"));
	}

	@Test
	public void accessControlRequestMethod() {
		assertNull(this.headers.getAccessControlRequestMethod());
		this.headers.setAccessControlRequestMethod(HttpMethod.POST);
		assertEquals(HttpMethod.POST, this.headers.getAccessControlRequestMethod());
	}

	@Test
	public void acceptLanguage() {
		String headerValue = "fr-ch, fr;q=0.9, en-*;q=0.8, de;q=0.7, *;q=0.5";
		this.headers.setAcceptLanguage(Locale.LanguageRange.parse(headerValue));
		assertEquals(headerValue, this.headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE));

		List<Locale.LanguageRange> expectedRanges = Arrays.asList(
				new Locale.LanguageRange("fr-ch"),
				new Locale.LanguageRange("fr", 0.9),
				new Locale.LanguageRange("en-*", 0.8),
				new Locale.LanguageRange("de", 0.7),
				new Locale.LanguageRange("*", 0.5)
		);
		assertEquals(expectedRanges, this.headers.getAcceptLanguage());
		assertEquals(Locale.forLanguageTag("fr-ch"), this.headers.getAcceptLanguageAsLocales().get(0));

		this.headers.setAcceptLanguageAsLocales(Collections.singletonList(Locale.FRANCE));
		assertEquals(Locale.FRANCE, this.headers.getAcceptLanguageAsLocales().get(0));
	}

	@Test // SPR-15603
	public void acceptLanguageWithEmptyValue() throws Exception {
		this.headers.set(HttpHeaders.ACCEPT_LANGUAGE, "");
		assertEquals(Collections.emptyList(), this.headers.getAcceptLanguageAsLocales());
	}

	@Test
	public void contentLanguage() {
		this.headers.setContentLanguage(Locale.FRANCE);
		assertEquals(Locale.FRANCE, this.headers.getContentLanguage());
		assertEquals("fr-FR", this.headers.getFirst(HttpHeaders.CONTENT_LANGUAGE));
	}

	@Test
	public void contentLanguageSerialized() {
		this.headers.set(HttpHeaders.CONTENT_LANGUAGE,  "de, en_CA");
		assertEquals("Expected one (first) locale", Locale.GERMAN, this.headers.getContentLanguage());
	}

	@Test
	public void firstDate() {
		this.headers.setDate(HttpHeaders.DATE, 1229595600000L);
		assertThat(this.headers.getFirstDate(HttpHeaders.DATE), is(1229595600000L));

		this.headers.clear();

		this.headers.add(HttpHeaders.DATE, "Thu, 18 Dec 2008 10:20:00 GMT");
		this.headers.add(HttpHeaders.DATE, "Sat, 18 Dec 2010 10:20:00 GMT");
		assertThat(this.headers.getFirstDate(HttpHeaders.DATE), is(1229595600000L));
	}

	@Test
	public void firstZonedDateTime() {
		ZonedDateTime date = ZonedDateTime.of(2017, 6, 22, 22, 22, 0, 0, ZoneId.of("GMT"));
		this.headers.setZonedDateTime(HttpHeaders.DATE, date);
		assertThat(this.headers.getFirst(HttpHeaders.DATE), is("Thu, 22 Jun 2017 22:22:00 GMT"));
		assertTrue(this.headers.getFirstZonedDateTime(HttpHeaders.DATE).isEqual(date));

		this.headers.clear();
		ZonedDateTime otherDate = ZonedDateTime.of(2010, 12, 18, 10, 20, 0, 0, ZoneId.of("GMT"));
		this.headers.add(HttpHeaders.DATE, RFC_1123_DATE_TIME.format(date));
		this.headers.add(HttpHeaders.DATE, RFC_1123_DATE_TIME.format(otherDate));
		assertTrue(this.headers.getFirstZonedDateTime(HttpHeaders.DATE).isEqual(date));

		// obsolete RFC 850 format
		this.headers.clear();
		this.headers.set(HttpHeaders.DATE, "Thursday, 22-Jun-17 22:22:00 GMT");
		assertTrue(this.headers.getFirstZonedDateTime(HttpHeaders.DATE).isEqual(date));

		// ANSI C's asctime() format
		this.headers.clear();
		this.headers.set(HttpHeaders.DATE, "Thu Jun 22 22:22:00 2017");
		assertTrue(this.headers.getFirstZonedDateTime(HttpHeaders.DATE).isEqual(date));
	}

}
