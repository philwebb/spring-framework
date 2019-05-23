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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertFalse;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;
import static temp.XAssert.assertTrue;

/**
 * @author Arjen Poutsma
 * @author Marcel Overdijk
 * @author Kazuki Shimizu
 */
public class ResponseEntityTests {

	@Test
	public void normal() {
		String headerName = "My-Custom-Header";
		String headerValue1 = "HeaderValue1";
		String headerValue2 = "HeaderValue2";
		Integer entity = 42;

		ResponseEntity<Integer> responseEntity =
				ResponseEntity.status(HttpStatus.OK).header(headerName, headerValue1, headerValue2).body(entity);

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertThat(responseEntity.getHeaders().containsKey(headerName)).isTrue();
		List<String> list = responseEntity.getHeaders().get(headerName);
		assertEquals(2, list.size());
		assertEquals(headerValue1, list.get(0));
		assertEquals(headerValue2, list.get(1));
		assertEquals(entity, responseEntity.getBody());
	}

	@Test
	public void okNoBody() {
		ResponseEntity<Void> responseEntity = ResponseEntity.ok().build();

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertThat(responseEntity.getBody()).isNotNull();
	}

	@Test
	public void okEntity() {
		Integer entity = 42;
		ResponseEntity<Integer> responseEntity = ResponseEntity.ok(entity);

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(entity, responseEntity.getBody());
	}

	@Test
	public void ofOptional() {
		Integer entity = 42;
		ResponseEntity<Integer> responseEntity = ResponseEntity.of(Optional.of(entity));

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(entity, responseEntity.getBody());
	}

	@Test
	public void ofEmptyOptional() {
		ResponseEntity<Integer> responseEntity = ResponseEntity.of(Optional.empty());

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
		assertThat(responseEntity.getBody()).isNotNull();
	}

	@Test
	public void createdLocation() throws URISyntaxException {
		URI location = new URI("location");
		ResponseEntity<Void> responseEntity = ResponseEntity.created(location).build();

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
		assertThat(responseEntity.getHeaders().containsKey("Location")).isTrue();
		assertEquals(location.toString(),
				responseEntity.getHeaders().getFirst("Location"));
		assertThat(responseEntity.getBody()).isNotNull();

		ResponseEntity.created(location).header("MyResponseHeader", "MyValue").body("Hello World");
	}

	@Test
	public void acceptedNoBody() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.accepted().build();

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
		assertThat(responseEntity.getBody()).isNotNull();
	}

	@Test // SPR-14939
	public void acceptedNoBodyWithAlternativeBodyType() throws URISyntaxException {
		ResponseEntity<String> responseEntity = ResponseEntity.accepted().build();

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
		assertThat(responseEntity.getBody()).isNotNull();
	}

	@Test
	public void noContent() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.noContent().build();

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
		assertThat(responseEntity.getBody()).isNotNull();
	}

	@Test
	public void badRequest() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.badRequest().build();

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertThat(responseEntity.getBody()).isNotNull();
	}

	@Test
	public void notFound() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.notFound().build();

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
		assertThat(responseEntity.getBody()).isNotNull();
	}

	@Test
	public void unprocessableEntity() throws URISyntaxException {
		ResponseEntity<String> responseEntity = ResponseEntity.unprocessableEntity().body("error");

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
		assertEquals("error", responseEntity.getBody());
	}

	@Test
	public void headers() throws URISyntaxException {
		URI location = new URI("location");
		long contentLength = 67890;
		MediaType contentType = MediaType.TEXT_PLAIN;

		ResponseEntity<Void> responseEntity = ResponseEntity.ok().
				allow(HttpMethod.GET).
				lastModified(12345L).
				location(location).
				contentLength(contentLength).
				contentType(contentType).
				build();

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		HttpHeaders responseHeaders = responseEntity.getHeaders();

		assertEquals("GET", responseHeaders.getFirst("Allow"));
		assertEquals("Thu, 01 Jan 1970 00:00:12 GMT",
				responseHeaders.getFirst("Last-Modified"));
		assertEquals(location.toASCIIString(),
				responseHeaders.getFirst("Location"));
		assertEquals(String.valueOf(contentLength), responseHeaders.getFirst("Content-Length"));
		assertEquals(contentType.toString(), responseHeaders.getFirst("Content-Type"));

		assertThat(responseEntity.getBody()).isNotNull();
	}

	@Test
	public void Etagheader() throws URISyntaxException {

		ResponseEntity<Void> responseEntity = ResponseEntity.ok().eTag("\"foo\"").build();
		assertEquals("\"foo\"", responseEntity.getHeaders().getETag());

		responseEntity = ResponseEntity.ok().eTag("foo").build();
		assertEquals("\"foo\"", responseEntity.getHeaders().getETag());

		responseEntity = ResponseEntity.ok().eTag("W/\"foo\"").build();
		assertEquals("W/\"foo\"", responseEntity.getHeaders().getETag());
	}

	@Test
	public void headersCopy() {
		HttpHeaders customHeaders = new HttpHeaders();
		customHeaders.set("X-CustomHeader", "vale");

		ResponseEntity<Void> responseEntity = ResponseEntity.ok().headers(customHeaders).build();
		HttpHeaders responseHeaders = responseEntity.getHeaders();

		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(1, responseHeaders.size());
		assertEquals(1, responseHeaders.get("X-CustomHeader").size());
		assertEquals("vale", responseHeaders.getFirst("X-CustomHeader"));

	}

	@Test  // SPR-12792
	public void headersCopyWithEmptyAndNull() {
		ResponseEntity<Void> responseEntityWithEmptyHeaders =
				ResponseEntity.ok().headers(new HttpHeaders()).build();
		ResponseEntity<Void> responseEntityWithNullHeaders =
				ResponseEntity.ok().headers(null).build();

		assertEquals(HttpStatus.OK, responseEntityWithEmptyHeaders.getStatusCode());
		assertThat(responseEntityWithEmptyHeaders.getHeaders().isEmpty()).isTrue();
		assertEquals(responseEntityWithEmptyHeaders.toString(), responseEntityWithNullHeaders.toString());
	}

	@Test
	public void emptyCacheControl() {
		Integer entity = 42;

		ResponseEntity<Integer> responseEntity =
				ResponseEntity.status(HttpStatus.OK)
						.cacheControl(CacheControl.empty())
						.body(entity);

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertThat(responseEntity.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL)).isFalse();
		assertEquals(entity, responseEntity.getBody());
	}

	@Test
	public void cacheControl() {
		Integer entity = 42;

		ResponseEntity<Integer> responseEntity =
				ResponseEntity.status(HttpStatus.OK)
						.cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate().
								mustRevalidate().proxyRevalidate().sMaxAge(30, TimeUnit.MINUTES))
						.body(entity);

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertThat(responseEntity.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL)).isTrue();
		assertEquals(entity, responseEntity.getBody());
		String cacheControlHeader = responseEntity.getHeaders().getCacheControl();
		assertThat(cacheControlHeader).isEqualTo(
				"max-age=3600, must-revalidate, private, proxy-revalidate, s-maxage=1800");
	}

	@Test
	public void cacheControlNoCache() {
		Integer entity = 42;

		ResponseEntity<Integer> responseEntity =
				ResponseEntity.status(HttpStatus.OK)
						.cacheControl(CacheControl.noStore())
						.body(entity);

		assertThat(responseEntity).isNotNull();
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertThat(responseEntity.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL)).isTrue();
		assertEquals(entity, responseEntity.getBody());

		String cacheControlHeader = responseEntity.getHeaders().getCacheControl();
		assertThat(cacheControlHeader).isEqualTo("no-store");
	}

	@Test
	public void statusCodeAsInt() {
		Integer entity = 42;
		ResponseEntity<Integer> responseEntity = ResponseEntity.status(200).body(entity);

		assertEquals(200, responseEntity.getStatusCode().value());
		assertEquals(entity, responseEntity.getBody());
	}

	@Test
	public void customStatusCode() {
		Integer entity = 42;
		ResponseEntity<Integer> responseEntity = ResponseEntity.status(299).body(entity);

		assertEquals(299, responseEntity.getStatusCodeValue());
		assertEquals(entity, responseEntity.getBody());
	}

}
