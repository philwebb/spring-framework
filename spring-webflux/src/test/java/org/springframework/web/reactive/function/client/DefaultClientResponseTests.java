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

package org.springframework.web.reactive.function.client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;

/**
 * @author Arjen Poutsma
 */
public class DefaultClientResponseTests {

	private ClientHttpResponse mockResponse;

	private ExchangeStrategies mockExchangeStrategies;

	private DefaultClientResponse defaultClientResponse;


	@Before
	public void createMocks() {
		this.mockResponse = mock(ClientHttpResponse.class);
		this.mockExchangeStrategies = mock(ExchangeStrategies.class);
		this.defaultClientResponse = new DefaultClientResponse(this.mockResponse, this.mockExchangeStrategies);
	}


	@Test
	public void statusCode() throws Exception {
		HttpStatus status = HttpStatus.CONTINUE;
		when(this.mockResponse.getStatusCode()).thenReturn(status);

		assertEquals(status, this.defaultClientResponse.statusCode());
	}

	@Test
	public void header() throws Exception {
		HttpHeaders httpHeaders = new HttpHeaders();
		long contentLength = 42L;
		httpHeaders.setContentLength(contentLength);
		MediaType contentType = MediaType.TEXT_PLAIN;
		httpHeaders.setContentType(contentType);
		InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 80);
		httpHeaders.setHost(host);
		List<HttpRange> range = Collections.singletonList(HttpRange.createByteRange(0, 42));
		httpHeaders.setRange(range);

		when(this.mockResponse.getHeaders()).thenReturn(httpHeaders);

		ClientResponse.Headers headers = this.defaultClientResponse.headers();
		assertEquals(OptionalLong.of(contentLength), headers.contentLength());
		assertEquals(Optional.of(contentType), headers.contentType());
		assertEquals(httpHeaders, headers.asHttpHeaders());
	}

	@Test
	public void cookies() throws Exception {
		ResponseCookie cookie = ResponseCookie.from("foo", "bar").build();
		MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();
		cookies.add("foo", cookie);

		when(this.mockResponse.getCookies()).thenReturn(cookies);

		assertSame(cookies, this.defaultClientResponse.cookies());
	}


	@Test
	public void body() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		when(this.mockResponse.getHeaders()).thenReturn(httpHeaders);
		when(this.mockResponse.getBody()).thenReturn(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		when(this.mockExchangeStrategies.messageReaders()).thenReturn(messageReaders);

		Mono<String> resultMono = this.defaultClientResponse.body(toMono(String.class));
		assertEquals("foo", resultMono.block());
	}

	@Test
	public void bodyToMono() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		when(this.mockResponse.getHeaders()).thenReturn(httpHeaders);
		when(this.mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
		when(this.mockResponse.getBody()).thenReturn(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		when(this.mockExchangeStrategies.messageReaders()).thenReturn(messageReaders);

		Mono<String> resultMono = this.defaultClientResponse.bodyToMono(String.class);
		assertEquals("foo", resultMono.block());
	}

	@Test
	public void bodyToMonoTypeReference() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		when(this.mockResponse.getHeaders()).thenReturn(httpHeaders);
		when(this.mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
		when(this.mockResponse.getBody()).thenReturn(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		when(this.mockExchangeStrategies.messageReaders()).thenReturn(messageReaders);

		Mono<String> resultMono =
				this.defaultClientResponse.bodyToMono(new ParameterizedTypeReference<String>() {
				});
		assertEquals("foo", resultMono.block());
	}

	@Test
	public void bodyToFlux() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		when(this.mockResponse.getHeaders()).thenReturn(httpHeaders);
		when(this.mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
		when(this.mockResponse.getBody()).thenReturn(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		when(this.mockExchangeStrategies.messageReaders()).thenReturn(messageReaders);

		Flux<String> resultFlux = this.defaultClientResponse.bodyToFlux(String.class);
		Mono<List<String>> result = resultFlux.collectList();
		assertEquals(Collections.singletonList("foo"), result.block());
	}

	@Test
	public void bodyToFluxTypeReference() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		when(this.mockResponse.getHeaders()).thenReturn(httpHeaders);
		when(this.mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
		when(this.mockResponse.getBody()).thenReturn(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		when(this.mockExchangeStrategies.messageReaders()).thenReturn(messageReaders);

		Flux<String> resultFlux =
				this.defaultClientResponse.bodyToFlux(new ParameterizedTypeReference<String>() {
				});
		Mono<List<String>> result = resultFlux.collectList();
		assertEquals(Collections.singletonList("foo"), result.block());
	}

	@Test
	public void toEntity() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		when(this.mockResponse.getHeaders()).thenReturn(httpHeaders);
		when(this.mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
		when(this.mockResponse.getBody()).thenReturn(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		when(this.mockExchangeStrategies.messageReaders()).thenReturn(messageReaders);

		ResponseEntity<String> result = this.defaultClientResponse.toEntity(String.class).block();
		assertEquals("foo", result.getBody());
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(MediaType.TEXT_PLAIN, result.getHeaders().getContentType());
	}

	@Test
	public void toEntityTypeReference() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		when(this.mockResponse.getHeaders()).thenReturn(httpHeaders);
		when(this.mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
		when(this.mockResponse.getBody()).thenReturn(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		when(this.mockExchangeStrategies.messageReaders()).thenReturn(messageReaders);

		ResponseEntity<String> result = this.defaultClientResponse.toEntity(
				new ParameterizedTypeReference<String>() {
				}).block();
		assertEquals("foo", result.getBody());
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(MediaType.TEXT_PLAIN, result.getHeaders().getContentType());
	}

	@Test
	public void toEntityList() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		when(this.mockResponse.getHeaders()).thenReturn(httpHeaders);
		when(this.mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
		when(this.mockResponse.getBody()).thenReturn(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		when(this.mockExchangeStrategies.messageReaders()).thenReturn(messageReaders);

		ResponseEntity<List<String>> result = this.defaultClientResponse.toEntityList(String.class).block();
		assertEquals(Collections.singletonList("foo"), result.getBody());
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(MediaType.TEXT_PLAIN, result.getHeaders().getContentType());
	}

	@Test
	public void toEntityListTypeReference() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		when(this.mockResponse.getHeaders()).thenReturn(httpHeaders);
		when(this.mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
		when(this.mockResponse.getBody()).thenReturn(body);

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		when(this.mockExchangeStrategies.messageReaders()).thenReturn(messageReaders);

		ResponseEntity<List<String>> result = this.defaultClientResponse.toEntityList(
				new ParameterizedTypeReference<String>() {
				}).block();
		assertEquals(Collections.singletonList("foo"), result.getBody());
		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals(MediaType.TEXT_PLAIN, result.getHeaders().getContentType());
	}

	@Test
	public void toMonoVoid() throws Exception {
		TestPublisher<DataBuffer> body = TestPublisher.create();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		when(this.mockResponse.getHeaders()).thenReturn(httpHeaders);
		when(this.mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
		when(this.mockResponse.getBody()).thenReturn(body.flux());

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		when(this.mockExchangeStrategies.messageReaders()).thenReturn(messageReaders);

		StepVerifier.create(this.defaultClientResponse.bodyToMono(Void.class))
				.then(() -> {
					body.assertWasSubscribed();
					body.complete();
				})
				.verifyComplete();
	}

	@Test
	public void toMonoVoidNonEmptyBody() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		TestPublisher<DataBuffer> body = TestPublisher.create();

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.TEXT_PLAIN);
		when(this.mockResponse.getHeaders()).thenReturn(httpHeaders);
		when(this.mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
		when(this.mockResponse.getBody()).thenReturn(body.flux());

		List<HttpMessageReader<?>> messageReaders = Collections
				.singletonList(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		when(this.mockExchangeStrategies.messageReaders()).thenReturn(messageReaders);

		StepVerifier.create(this.defaultClientResponse.bodyToMono(Void.class))
				.then(() -> {
					body.assertWasSubscribed();
					body.emit(dataBuffer);
				})
				.verifyComplete();

		body.assertCancelled();
	}

}
