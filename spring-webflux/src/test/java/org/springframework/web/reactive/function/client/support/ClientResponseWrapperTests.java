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

package org.springframework.web.reactive.function.client.support;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Arjen Poutsma
 */
public class ClientResponseWrapperTests {

	private ClientResponse mockResponse;

	private ClientResponseWrapper wrapper;

	@Before
	public void createWrapper() {
		this.mockResponse = mock(ClientResponse.class);
		this.wrapper = new ClientResponseWrapper(this.mockResponse);
	}

	@Test
	public void response() throws Exception {
		assertSame(this.mockResponse, this.wrapper.response());
	}

	@Test
	public void statusCode() throws Exception {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		when(this.mockResponse.statusCode()).thenReturn(status);

		assertSame(status, this.wrapper.statusCode());
	}

	@Test
	public void headers() throws Exception {
		ClientResponse.Headers headers = mock(ClientResponse.Headers.class);
		when(this.mockResponse.headers()).thenReturn(headers);

		assertSame(headers, this.wrapper.headers());
	}

	@Test
	public void cookies() throws Exception {
		MultiValueMap<String, ResponseCookie> cookies = mock(MultiValueMap.class);
		when(this.mockResponse.cookies()).thenReturn(cookies);

		assertSame(cookies, this.wrapper.cookies());
	}

	@Test
	public void bodyExtractor() throws Exception {
		Mono<String> result = Mono.just("foo");
		BodyExtractor<Mono<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(String.class);
		when(this.mockResponse.body(extractor)).thenReturn(result);

		assertSame(result, this.wrapper.body(extractor));
	}

	@Test
	public void bodyToMonoClass() throws Exception {
		Mono<String> result = Mono.just("foo");
		when(this.mockResponse.bodyToMono(String.class)).thenReturn(result);

		assertSame(result, this.wrapper.bodyToMono(String.class));
	}

	@Test
	public void bodyToMonoParameterizedTypeReference() throws Exception {
		Mono<String> result = Mono.just("foo");
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<String>() {};
		when(this.mockResponse.bodyToMono(reference)).thenReturn(result);

		assertSame(result, this.wrapper.bodyToMono(reference));
	}

	@Test
	public void bodyToFluxClass() throws Exception {
		Flux<String> result = Flux.just("foo");
		when(this.mockResponse.bodyToFlux(String.class)).thenReturn(result);

		assertSame(result, this.wrapper.bodyToFlux(String.class));
	}

	@Test
	public void bodyToFluxParameterizedTypeReference() throws Exception {
		Flux<String> result = Flux.just("foo");
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<String>() {};
		when(this.mockResponse.bodyToFlux(reference)).thenReturn(result);

		assertSame(result, this.wrapper.bodyToFlux(reference));
	}

	@Test
	public void toEntityClass() throws Exception {
		Mono<ResponseEntity<String>> result = Mono.just(new ResponseEntity<>("foo", HttpStatus.OK));
		when(this.mockResponse.toEntity(String.class)).thenReturn(result);

		assertSame(result, this.wrapper.toEntity(String.class));
	}

	@Test
	public void toEntityParameterizedTypeReference() throws Exception {
		Mono<ResponseEntity<String>> result = Mono.just(new ResponseEntity<>("foo", HttpStatus.OK));
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<String>() {};
		when(this.mockResponse.toEntity(reference)).thenReturn(result);

		assertSame(result, this.wrapper.toEntity(reference));
	}

	@Test
	public void toEntityListClass() throws Exception {
		Mono<ResponseEntity<List<String>>> result = Mono.just(new ResponseEntity<>(singletonList("foo"), HttpStatus.OK));
		when(this.mockResponse.toEntityList(String.class)).thenReturn(result);

		assertSame(result, this.wrapper.toEntityList(String.class));
	}

	@Test
	public void toEntityListParameterizedTypeReference() throws Exception {
		Mono<ResponseEntity<List<String>>> result = Mono.just(new ResponseEntity<>(singletonList("foo"), HttpStatus.OK));
		ParameterizedTypeReference<String> reference = new ParameterizedTypeReference<String>() {};
		when(this.mockResponse.toEntityList(reference)).thenReturn(result);

		assertSame(result, this.wrapper.toEntityList(reference));
	}



}
