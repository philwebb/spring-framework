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
		assertThat(headers.getAccept()).as("Invalid Accept header").isEqualTo(mediaTypes);
		assertThat(headers.getFirst("Accept")).as("Invalid Accept header").isEqualTo("text/html, text/plain");
	}

	@Test  // SPR-9655
	public void acceptWithMultipleHeaderValues() {
		headers.add("Accept", "text/html");
		headers.add("Accept", "text/plain");
		List<MediaType> expected = Arrays.asList(new MediaType("text", "html"), new MediaType("text", "plain"));
		assertThat(headers.getAccept()).as("Invalid Accept header").isEqualTo(expected);
	}

	@Test  // SPR-14506
	public void acceptWithMultipleCommaSeparatedHeaderValues() {
		headers.add("Accept", "text/html,text/pdf");
		headers.add("Accept", "text/plain,text/csv");
		List<MediaType> expected = Arrays.asList(new MediaType("text", "html"), new MediaType("text", "pdf"),
				new MediaType("text", "plain"), new MediaType("text", "csv"));
		assertThat(headers.getAccept()).as("Invalid Accept header").isEqualTo(expected);
	}

	@Test
	public void acceptCharsets() {
		Charset charset1 = StandardCharsets.UTF_8;
		Charset charset2 = StandardCharsets.ISO_8859_1;
		List<Charset> charsets = new ArrayList<>(2);
		charsets.add(charset1);
		charsets.add(charset2);
		headers.setAcceptCharset(charsets);
		assertThat(headers.getAcceptCharset()).as("Invalid Accept header").isEqualTo(charsets);
		assertThat(headers.getFirst("Accept-Charset")).as("Invalid Accept header").isEqualTo("utf-8, iso-8859-1");
	}

	@Test
	public void acceptCharsetWildcard() {
		headers.set("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
		assertThat(headers.getAcceptCharset()).as("Invalid Accept header").isEqualTo(Arrays.asList(StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8));
	}

	@Test
	public void allow() {
		EnumSet<HttpMethod> methods = EnumSet.of(HttpMethod.GET, HttpMethod.POST);
		headers.setAllow(methods);
		assertThat(headers.getAllow()).as("Invalid Allow header").isEqualTo(methods);
		assertThat(headers.getFirst("Allow")).as("Invalid Allow header").isEqualTo("GET,POST");
	}

	@Test
	public void contentLength() {
		long length = 42L;
		headers.setContentLength(length);
		assertThat(headers.getContentLength()).as("Invalid Content-Length header").isEqualTo(length);
		assertThat(headers.getFirst("Content-Length")).as("Invalid Content-Length header").isEqualTo("42");
	}

	@Test
	public void contentType() {
		MediaType contentType = new MediaType("text", "html", StandardCharsets.UTF_8);
		headers.setContentType(contentType);
		assertThat(headers.getContentType()).as("Invalid Content-Type header").isEqualTo(contentType);
		assertThat(headers.getFirst("Content-Type")).as("Invalid Content-Type header").isEqualTo("text/html;charset=UTF-8");
	}

	@Test
	public void location() throws URISyntaxException {
		URI location = new URI("https://www.example.com/hotels");
		headers.setLocation(location);
		assertThat(headers.getLocation()).as("Invalid Location header").isEqualTo(location);
		assertThat(headers.getFirst("Location")).as("Invalid Location header").isEqualTo("https://www.example.com/hotels");
	}

	@Test
	public void eTag() {
		String eTag = "\"v2.6\"";
		headers.setETag(eTag);
		assertThat(headers.getETag()).as("Invalid ETag header").isEqualTo(eTag);
		assertThat(headers.getFirst("ETag")).as("Invalid ETag header").isEqualTo("\"v2.6\"");
	}

	@Test
	public void host() {
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 8080);
		headers.setHost(host);
		assertThat(headers.getHost()).as("Invalid Host header").isEqualTo(host);
		assertThat(headers.getFirst("Host")).as("Invalid Host header").isEqualTo("localhost:8080");
	}

	@Test
	public void hostNoPort() {
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 0);
		headers.setHost(host);
		assertThat(headers.getHost()).as("Invalid Host header").isEqualTo(host);
		assertThat(headers.getFirst("Host")).as("Invalid Host header").isEqualTo("localhost");
	}

	@Test
	public void ipv6Host() {
		InetSocketAddress host = InetSocketAddress.createUnresolved("[::1]", 0);
		headers.setHost(host);
		assertThat(headers.getHost()).as("Invalid Host header").isEqualTo(host);
		assertThat(headers.getFirst("Host")).as("Invalid Host header").isEqualTo("[::1]");
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
		assertThat(headers.getIfMatch().get(0)).as("Invalid If-Match header").isEqualTo(ifMatch);
		assertThat(headers.getFirst("If-Match")).as("Invalid If-Match header").isEqualTo("\"v2.6\"");
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
		assertThat(headers.get(HttpHeaders.IF_MATCH).get(0)).as("Invalid If-Match header").isEqualTo("\"v2,0\"");
		assertThat(headers.get(HttpHeaders.IF_MATCH).get(1)).as("Invalid If-Match header").isEqualTo("W/\"v2,1\", \"v2,2\"");
		assertThat(headers.getIfMatch()).contains("\"v2,0\"", "W/\"v2,1\"", "\"v2,2\"");
	}

	@Test
	public void ifNoneMatch() {
		String ifNoneMatch = "\"v2.6\"";
		headers.setIfNoneMatch(ifNoneMatch);
		assertThat(headers.getIfNoneMatch().get(0)).as("Invalid If-None-Match header").isEqualTo(ifNoneMatch);
		assertThat(headers.getFirst("If-None-Match")).as("Invalid If-None-Match header").isEqualTo("\"v2.6\"");
	}

	@Test
	public void ifNoneMatchWildCard() {
		String ifNoneMatch = "*";
		headers.setIfNoneMatch(ifNoneMatch);
		assertThat(headers.getIfNoneMatch().get(0)).as("Invalid If-None-Match header").isEqualTo(ifNoneMatch);
		assertThat(headers.getFirst("If-None-Match")).as("Invalid If-None-Match header").isEqualTo("*");
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
		assertThat(headers.getFirst("If-None-Match")).as("Invalid If-None-Match header").isEqualTo("\"v2.6\", \"v2.7\", \"v2.8\"");
	}

	@Test
	public void date() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		headers.setDate(date);
		assertThat(headers.getDate()).as("Invalid Date header").isEqualTo(date);
		assertThat(headers.getFirst("date")).as("Invalid Date header").isEqualTo("Thu, 18 Dec 2008 10:20:00 GMT");

		// RFC 850
		headers.set("Date", "Thu, 18 Dec 2008 10:20:00 GMT");
		assertThat(headers.getDate()).as("Invalid Date header").isEqualTo(date);
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
			assertThat(headers.getFirst("date")).as("Invalid Date header").isEqualTo("Thu, 18 Dec 2008 10:20:00 GMT");
			assertThat(headers.getDate()).as("Invalid Date header").isEqualTo(date);
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
		assertThat(headers.getLastModified()).as("Invalid Last-Modified header").isEqualTo(date);
		assertThat(headers.getFirst("last-modified")).as("Invalid Last-Modified header").isEqualTo("Thu, 18 Dec 2008 10:20:00 GMT");
	}

	@Test
	public void expiresLong() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		headers.setExpires(date);
		assertThat(headers.getExpires()).as("Invalid Expires header").isEqualTo(date);
		assertThat(headers.getFirst("expires")).as("Invalid Expires header").isEqualTo("Thu, 18 Dec 2008 10:20:00 GMT");
	}

	@Test
	public void expiresZonedDateTime() {
		ZonedDateTime zonedDateTime = ZonedDateTime.of(2008, 12, 18, 10, 20, 0, 0, ZoneId.of("GMT"));
		headers.setExpires(zonedDateTime);
		assertThat(headers.getExpires()).as("Invalid Expires header").isEqualTo(zonedDateTime.toInstant().toEpochMilli());
		assertThat(headers.getFirst("expires")).as("Invalid Expires header").isEqualTo("Thu, 18 Dec 2008 10:20:00 GMT");
	}

	@Test  // SPR-10648 (example is from INT-3063)
	public void expiresInvalidDate() {
		headers.set("Expires", "-1");
		assertThat(headers.getExpires()).isEqualTo((long) -1);
	}

	@Test
	public void ifModifiedSince() {
		Calendar calendar = new GregorianCalendar(2008, 11, 18, 11, 20);
		calendar.setTimeZone(TimeZone.getTimeZone("CET"));
		long date = calendar.getTimeInMillis();
		headers.setIfModifiedSince(date);
		assertThat(headers.getIfModifiedSince()).as("Invalid If-Modified-Since header").isEqualTo(date);
		assertThat(headers.getFirst("if-modified-since")).as("Invalid If-Modified-Since header").isEqualTo("Thu, 18 Dec 2008 10:20:00 GMT");
	}

	@Test  // SPR-14144
	public void invalidIfModifiedSinceHeader() {
		headers.set(HttpHeaders.IF_MODIFIED_SINCE, "0");
		assertThat(headers.getIfModifiedSince()).isEqualTo((long) -1);

		headers.set(HttpHeaders.IF_MODIFIED_SINCE, "-1");
		assertThat(headers.getIfModifiedSince()).isEqualTo((long) -1);

		headers.set(HttpHeaders.IF_MODIFIED_SINCE, "XXX");
		assertThat(headers.getIfModifiedSince()).isEqualTo((long) -1);
	}

	@Test
	public void pragma() {
		String pragma = "no-cache";
		headers.setPragma(pragma);
		assertThat(headers.getPragma()).as("Invalid Pragma header").isEqualTo(pragma);
		assertThat(headers.getFirst("pragma")).as("Invalid Pragma header").isEqualTo("no-cache");
	}

	@Test
	public void cacheControl() {
		headers.setCacheControl("no-cache");
		assertThat(headers.getCacheControl()).as("Invalid Cache-Control header").isEqualTo("no-cache");
		assertThat(headers.getFirst("cache-control")).as("Invalid Cache-Control header").isEqualTo("no-cache");
	}

	@Test
	public void cacheControlBuilder() {
		headers.setCacheControl(CacheControl.noCache());
		assertThat(headers.getCacheControl()).as("Invalid Cache-Control header").isEqualTo("no-cache");
		assertThat(headers.getFirst("cache-control")).as("Invalid Cache-Control header").isEqualTo("no-cache");
	}

	@Test
	public void cacheControlEmpty() {
		headers.setCacheControl(CacheControl.empty());
		assertThat(headers.getCacheControl()).as("Invalid Cache-Control header").isNull();
		assertThat(headers.getFirst("cache-control")).as("Invalid Cache-Control header").isNull();
	}

	@Test
	public void cacheControlAllValues() {
		headers.add(HttpHeaders.CACHE_CONTROL, "max-age=1000, public");
		headers.add(HttpHeaders.CACHE_CONTROL, "s-maxage=1000");
		assertThat(headers.getCacheControl()).isEqualTo("max-age=1000, public, s-maxage=1000");
	}

	@Test
	public void contentDisposition() {
		ContentDisposition disposition = headers.getContentDisposition();
		assertThat(disposition).isNotNull();
		assertThat(headers.getContentDisposition()).as("Invalid Content-Disposition header").isEqualTo(ContentDisposition.empty());

		disposition = ContentDisposition.builder("attachment").name("foo").filename("foo.txt").size(123L).build();
		headers.setContentDisposition(disposition);
		assertThat(headers.getContentDisposition()).as("Invalid Content-Disposition header").isEqualTo(disposition);
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
		assertThat(allowedHeaders).isEqualTo(Arrays.asList("header1", "header2", "header3"));
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
		assertThat(headers.getAccessControlAllowOrigin()).isNull();
		headers.setAccessControlAllowOrigin("*");
		assertThat(headers.getAccessControlAllowOrigin()).isEqualTo("*");
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
		assertThat(headers.getAccessControlMaxAge()).isEqualTo((long) -1);
		headers.setAccessControlMaxAge(3600);
		assertThat(headers.getAccessControlMaxAge()).isEqualTo(3600);
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
		assertThat(headers.getAccessControlRequestMethod()).isNull();
		headers.setAccessControlRequestMethod(HttpMethod.POST);
		assertThat(headers.getAccessControlRequestMethod()).isEqualTo(HttpMethod.POST);
	}

	@Test
	public void acceptLanguage() {
		String headerValue = "fr-ch, fr;q=0.9, en-*;q=0.8, de;q=0.7, *;q=0.5";
		headers.setAcceptLanguage(Locale.LanguageRange.parse(headerValue));
		assertThat(headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE)).isEqualTo(headerValue);

		List<Locale.LanguageRange> expectedRanges = Arrays.asList(
				new Locale.LanguageRange("fr-ch"),
				new Locale.LanguageRange("fr", 0.9),
				new Locale.LanguageRange("en-*", 0.8),
				new Locale.LanguageRange("de", 0.7),
				new Locale.LanguageRange("*", 0.5)
		);
		assertThat(headers.getAcceptLanguage()).isEqualTo(expectedRanges);
		assertThat(headers.getAcceptLanguageAsLocales().get(0)).isEqualTo(Locale.forLanguageTag("fr-ch"));

		headers.setAcceptLanguageAsLocales(Collections.singletonList(Locale.FRANCE));
		assertThat(headers.getAcceptLanguageAsLocales().get(0)).isEqualTo(Locale.FRANCE);
	}

	@Test // SPR-15603
	public void acceptLanguageWithEmptyValue() throws Exception {
		this.headers.set(HttpHeaders.ACCEPT_LANGUAGE, "");
		assertThat(this.headers.getAcceptLanguageAsLocales()).isEqualTo(Collections.emptyList());
	}

	@Test
	public void contentLanguage() {
		headers.setContentLanguage(Locale.FRANCE);
		assertThat(headers.getContentLanguage()).isEqualTo(Locale.FRANCE);
		assertThat(headers.getFirst(HttpHeaders.CONTENT_LANGUAGE)).isEqualTo("fr-FR");
	}

	@Test
	public void contentLanguageSerialized() {
		headers.set(HttpHeaders.CONTENT_LANGUAGE,  "de, en_CA");
		assertThat(headers.getContentLanguage()).as("Expected one (first) locale").isEqualTo(Locale.GERMAN);
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
		assertThat(headers.get(HttpHeaders.DATE)).isEqualTo(Arrays.asList("Fri, 02 Jun 2017 02:22:00 GMT",
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
		assertThat(authorization).isNotNull();
		assertThat(authorization.startsWith("Basic ")).isTrue();
		byte[] result = Base64.getDecoder().decode(authorization.substring(6).getBytes(StandardCharsets.ISO_8859_1));
		assertThat(new String(result, StandardCharsets.ISO_8859_1)).isEqualTo("foo:bar");
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
		assertThat(authorization).isEqualTo("Bearer foo");
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
		assertThat(headers.get(headerName).get(0)).isEqualTo(headerValue);
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
		assertThat(headers.get(headerName).get(0)).isEqualTo(headerValue);
	}

}
