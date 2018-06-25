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

package org.springframework.web.reactive.function.server.support;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.ServerRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Arjen Poutsma
 */
public class ServerRequestWrapperTests {

	private ServerRequest mockRequest;

	private ServerRequestWrapper wrapper;

	@Before
	public void createWrapper() {
		this.mockRequest = mock(ServerRequest.class);
		this.wrapper = new ServerRequestWrapper(this.mockRequest);
	}

	@Test
	public void request() throws Exception {
		assertSame(this.mockRequest, this.wrapper.request());
	}

	@Test
	public void method() throws Exception {
		HttpMethod method = HttpMethod.POST;
		when(this.mockRequest.method()).thenReturn(method);

		assertSame(method, this.wrapper.method());
	}

	@Test
	public void uri() throws Exception {
		URI uri = URI.create("https://example.com");
		when(this.mockRequest.uri()).thenReturn(uri);

		assertSame(uri, this.wrapper.uri());
	}

	@Test
	public void path() throws Exception {
		String path = "/foo/bar";
		when(this.mockRequest.path()).thenReturn(path);

		assertSame(path, this.wrapper.path());
	}

	@Test
	public void headers() throws Exception {
		ServerRequest.Headers headers = mock(ServerRequest.Headers.class);
		when(this.mockRequest.headers()).thenReturn(headers);

		assertSame(headers, this.wrapper.headers());
	}

	@Test
	public void attribute() throws Exception {
		String name = "foo";
		String value = "bar";
		when(this.mockRequest.attribute(name)).thenReturn(Optional.of(value));

		assertEquals(Optional.of(value), this.wrapper.attribute(name));
	}

	@Test
	public void queryParam() throws Exception {
		String name = "foo";
		String value = "bar";
		when(this.mockRequest.queryParam(name)).thenReturn(Optional.of(value));

		assertEquals(Optional.of(value), this.wrapper.queryParam(name));
	}

	@Test
	public void queryParams() throws Exception {
		MultiValueMap<String, String> value = new LinkedMultiValueMap<>();
		value.add("foo", "bar");
		when(this.mockRequest.queryParams()).thenReturn(value);

		assertSame(value, this.wrapper.queryParams());
	}

	@Test
	public void pathVariable() throws Exception {
		String name = "foo";
		String value = "bar";
		when(this.mockRequest.pathVariable(name)).thenReturn(value);

		assertEquals(value, this.wrapper.pathVariable(name));
	}

	@Test
	public void pathVariables() throws Exception {
		Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
		when(this.mockRequest.pathVariables()).thenReturn(pathVariables);

		assertSame(pathVariables, this.wrapper.pathVariables());
	}

	@Test
	public void cookies() throws Exception {
		MultiValueMap<String, HttpCookie> cookies = mock(MultiValueMap.class);
		when(this.mockRequest.cookies()).thenReturn(cookies);

		assertSame(cookies, this.wrapper.cookies());
	}

	@Test
	public void bodyExtractor() throws Exception {
		Mono<String> result = Mono.just("foo");
		BodyExtractor<Mono<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(String.class);
		when(this.mockRequest.body(extractor)).thenReturn(result);

		assertSame(result, this.wrapper.body(extractor));
	}

	@Test
	public void bodyToMonoClass() throws Exception {
		Mono<String> result = Mono.just("foo");
		when(this.mockRequest.bodyToMono(String.class)).thenReturn(result);

		assertSame(result, this.wrapper.bodyToMono(String.class));
	}

	@Test
	public void bodyToMonoParameterizedTypeReference() throws Exception {
		Mono<String> result = Mono.just("foo");
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<String>() {};
		when(this.mockRequest.bodyToMono(reference)).thenReturn(result);

		assertSame(result, this.wrapper.bodyToMono(reference));
	}

	@Test
	public void bodyToFluxClass() throws Exception {
		Flux<String> result = Flux.just("foo");
		when(this.mockRequest.bodyToFlux(String.class)).thenReturn(result);

		assertSame(result, this.wrapper.bodyToFlux(String.class));
	}

	@Test
	public void bodyToFluxParameterizedTypeReference() throws Exception {
		Flux<String> result = Flux.just("foo");
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<String>() {};
		when(this.mockRequest.bodyToFlux(reference)).thenReturn(result);

		assertSame(result, this.wrapper.bodyToFlux(reference));
	}

}
