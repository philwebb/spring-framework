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
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;

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

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getHeaders().containsKey(headerName)).isTrue();
		List<String> list = responseEntity.getHeaders().get(headerName);
		assertEquals(2, list.size());
		assertThat((Object) list.get(0)).isEqualTo(headerValue1);
		assertThat((Object) list.get(1)).isEqualTo(headerValue2);
		assertThat((Object) responseEntity.getBody()).isEqualTo(entity);
	}

	@Test
	public void okNoBody() {
		ResponseEntity<Void> responseEntity = ResponseEntity.ok().build();

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertNull(responseEntity.getBody());
	}

	@Test
	public void okEntity() {
		Integer entity = 42;
		ResponseEntity<Integer> responseEntity = ResponseEntity.ok(entity);

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat((Object) responseEntity.getBody()).isEqualTo(entity);
	}

	@Test
	public void ofOptional() {
		Integer entity = 42;
		ResponseEntity<Integer> responseEntity = ResponseEntity.of(Optional.of(entity));

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat((Object) responseEntity.getBody()).isEqualTo(entity);
	}

	@Test
	public void ofEmptyOptional() {
		ResponseEntity<Integer> responseEntity = ResponseEntity.of(Optional.empty());

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertNull(responseEntity.getBody());
	}

	@Test
	public void createdLocation() throws URISyntaxException {
		URI location = new URI("location");
		ResponseEntity<Void> responseEntity = ResponseEntity.created(location).build();

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(responseEntity.getHeaders().containsKey("Location")).isTrue();
		assertThat((Object) responseEntity.getHeaders().getFirst("Location")).isEqualTo(location.toString());
		assertNull(responseEntity.getBody());

		ResponseEntity.created(location).header("MyResponseHeader", "MyValue").body("Hello World");
	}

	@Test
	public void acceptedNoBody() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.accepted().build();

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		assertNull(responseEntity.getBody());
	}

	@Test // SPR-14939
	public void acceptedNoBodyWithAlternativeBodyType() throws URISyntaxException {
		ResponseEntity<String> responseEntity = ResponseEntity.accepted().build();

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		assertNull(responseEntity.getBody());
	}

	@Test
	public void noContent() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.noContent().build();

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertNull(responseEntity.getBody());
	}

	@Test
	public void badRequest() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.badRequest().build();

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertNull(responseEntity.getBody());
	}

	@Test
	public void notFound() throws URISyntaxException {
		ResponseEntity<Void> responseEntity = ResponseEntity.notFound().build();

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertNull(responseEntity.getBody());
	}

	@Test
	public void unprocessableEntity() throws URISyntaxException {
		ResponseEntity<String> responseEntity = ResponseEntity.unprocessableEntity().body("error");

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat((Object) responseEntity.getBody()).isEqualTo("error");
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

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		HttpHeaders responseHeaders = responseEntity.getHeaders();

		assertThat((Object) responseHeaders.getFirst("Allow")).isEqualTo("GET");
		assertThat((Object) responseHeaders.getFirst("Last-Modified")).isEqualTo("Thu, 01 Jan 1970 00:00:12 GMT");
		assertThat((Object) responseHeaders.getFirst("Location")).isEqualTo(location.toASCIIString());
		assertThat((Object) responseHeaders.getFirst("Content-Length")).isEqualTo(String.valueOf(contentLength));
		assertThat((Object) responseHeaders.getFirst("Content-Type")).isEqualTo(contentType.toString());

		assertNull(responseEntity.getBody());
	}

	@Test
	public void Etagheader() throws URISyntaxException {

		ResponseEntity<Void> responseEntity = ResponseEntity.ok().eTag("\"foo\"").build();
		assertThat((Object) responseEntity.getHeaders().getETag()).isEqualTo("\"foo\"");

		responseEntity = ResponseEntity.ok().eTag("foo").build();
		assertThat((Object) responseEntity.getHeaders().getETag()).isEqualTo("\"foo\"");

		responseEntity = ResponseEntity.ok().eTag("W/\"foo\"").build();
		assertThat((Object) responseEntity.getHeaders().getETag()).isEqualTo("W/\"foo\"");
	}

	@Test
	public void headersCopy() {
		HttpHeaders customHeaders = new HttpHeaders();
		customHeaders.set("X-CustomHeader", "vale");

		ResponseEntity<Void> responseEntity = ResponseEntity.ok().headers(customHeaders).build();
		HttpHeaders responseHeaders = responseEntity.getHeaders();

		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertEquals(1, responseHeaders.size());
		assertEquals(1, responseHeaders.get("X-CustomHeader").size());
		assertThat((Object) responseHeaders.getFirst("X-CustomHeader")).isEqualTo("vale");

	}

	@Test  // SPR-12792
	public void headersCopyWithEmptyAndNull() {
		ResponseEntity<Void> responseEntityWithEmptyHeaders =
				ResponseEntity.ok().headers(new HttpHeaders()).build();
		ResponseEntity<Void> responseEntityWithNullHeaders =
				ResponseEntity.ok().headers(null).build();

		assertThat((Object) responseEntityWithEmptyHeaders.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntityWithEmptyHeaders.getHeaders().isEmpty()).isTrue();
		assertThat((Object) responseEntityWithNullHeaders.toString()).isEqualTo(responseEntityWithEmptyHeaders.toString());
	}

	@Test
	public void emptyCacheControl() {
		Integer entity = 42;

		ResponseEntity<Integer> responseEntity =
				ResponseEntity.status(HttpStatus.OK)
						.cacheControl(CacheControl.empty())
						.body(entity);

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL)).isFalse();
		assertThat((Object) responseEntity.getBody()).isEqualTo(entity);
	}

	@Test
	public void cacheControl() {
		Integer entity = 42;

		ResponseEntity<Integer> responseEntity =
				ResponseEntity.status(HttpStatus.OK)
						.cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate().
								mustRevalidate().proxyRevalidate().sMaxAge(30, TimeUnit.MINUTES))
						.body(entity);

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL)).isTrue();
		assertThat((Object) responseEntity.getBody()).isEqualTo(entity);
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

		assertNotNull(responseEntity);
		assertThat((Object) responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL)).isTrue();
		assertThat((Object) responseEntity.getBody()).isEqualTo(entity);

		String cacheControlHeader = responseEntity.getHeaders().getCacheControl();
		assertThat(cacheControlHeader).isEqualTo("no-store");
	}

	@Test
	public void statusCodeAsInt() {
		Integer entity = 42;
		ResponseEntity<Integer> responseEntity = ResponseEntity.status(200).body(entity);

		assertEquals(200, responseEntity.getStatusCode().value());
		assertThat((Object) responseEntity.getBody()).isEqualTo(entity);
	}

	@Test
	public void customStatusCode() {
		Integer entity = 42;
		ResponseEntity<Integer> responseEntity = ResponseEntity.status(299).body(entity);

		assertEquals(299, responseEntity.getStatusCodeValue());
		assertThat((Object) responseEntity.getBody()).isEqualTo(entity);
	}

}
