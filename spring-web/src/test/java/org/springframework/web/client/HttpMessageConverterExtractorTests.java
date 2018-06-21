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

package org.springframework.web.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for {@link HttpMessageConverter}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 */
public class HttpMessageConverterExtractorTests {

	private HttpMessageConverterExtractor<?> extractor;

	private final ClientHttpResponse response = mock(ClientHttpResponse.class);

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void noContent() throws IOException {
		HttpMessageConverter<?> converter = mock(HttpMessageConverter.class);
		this.extractor = new HttpMessageConverterExtractor<>(String.class, createConverterList(converter));
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.NO_CONTENT.value());

		Object result = this.extractor.extractData(this.response);
		assertNull(result);
	}

	@Test
	public void notModified() throws IOException {
		HttpMessageConverter<?> converter = mock(HttpMessageConverter.class);
		this.extractor = new HttpMessageConverterExtractor<>(String.class, createConverterList(converter));
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.NOT_MODIFIED.value());

		Object result = this.extractor.extractData(this.response);
		assertNull(result);
	}

	@Test
	public void informational() throws IOException {
		HttpMessageConverter<?> converter = mock(HttpMessageConverter.class);
		this.extractor = new HttpMessageConverterExtractor<>(String.class, createConverterList(converter));
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.CONTINUE.value());

		Object result = this.extractor.extractData(this.response);
		assertNull(result);
	}

	@Test
	public void zeroContentLength() throws IOException {
		HttpMessageConverter<?> converter = mock(HttpMessageConverter.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentLength(0);
		this.extractor = new HttpMessageConverterExtractor<>(String.class, createConverterList(converter));
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(this.response.getHeaders()).willReturn(responseHeaders);

		Object result = this.extractor.extractData(this.response);
		assertNull(result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void emptyMessageBody() throws IOException {
		HttpMessageConverter<String> converter = mock(HttpMessageConverter.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		this.extractor = new HttpMessageConverterExtractor<>(String.class, createConverterList(converter));
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(this.response.getHeaders()).willReturn(responseHeaders);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream("".getBytes()));

		Object result = this.extractor.extractData(this.response);
		assertNull(result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void normal() throws IOException {
		HttpMessageConverter<String> converter = mock(HttpMessageConverter.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType contentType = MediaType.TEXT_PLAIN;
		responseHeaders.setContentType(contentType);
		String expected = "Foo";
		this.extractor = new HttpMessageConverterExtractor<>(String.class, createConverterList(converter));
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(this.response.getHeaders()).willReturn(responseHeaders);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(expected.getBytes()));
		given(converter.canRead(String.class, contentType)).willReturn(true);
		given(converter.read(eq(String.class), any(HttpInputMessage.class))).willReturn(expected);

		Object result = this.extractor.extractData(this.response);
		assertEquals(expected, result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void cannotRead() throws IOException {
		HttpMessageConverter<String> converter = mock(HttpMessageConverter.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType contentType = MediaType.TEXT_PLAIN;
		responseHeaders.setContentType(contentType);
		this.extractor = new HttpMessageConverterExtractor<>(String.class, createConverterList(converter));
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(this.response.getHeaders()).willReturn(responseHeaders);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream("Foobar".getBytes()));
		given(converter.canRead(String.class, contentType)).willReturn(false);
		this.exception.expect(RestClientException.class);

		this.extractor.extractData(this.response);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void generics() throws IOException {
		GenericHttpMessageConverter<String> converter = mock(GenericHttpMessageConverter.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType contentType = MediaType.TEXT_PLAIN;
		responseHeaders.setContentType(contentType);
		String expected = "Foo";
		ParameterizedTypeReference<List<String>> reference = new ParameterizedTypeReference<List<String>>() {};
		Type type = reference.getType();
		this.extractor = new HttpMessageConverterExtractor<List<String>>(type, createConverterList(converter));
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(this.response.getHeaders()).willReturn(responseHeaders);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(expected.getBytes()));
		given(converter.canRead(type, null, contentType)).willReturn(true);
		given(converter.read(eq(type), eq(null), any(HttpInputMessage.class))).willReturn(expected);

		Object result = this.extractor.extractData(this.response);
		assertEquals(expected, result);
	}

	@Test  // SPR-13592
	@SuppressWarnings("unchecked")
	public void converterThrowsIOException() throws IOException {
		HttpMessageConverter<String> converter = mock(HttpMessageConverter.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType contentType = MediaType.TEXT_PLAIN;
		responseHeaders.setContentType(contentType);
		this.extractor = new HttpMessageConverterExtractor<>(String.class, createConverterList(converter));
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(this.response.getHeaders()).willReturn(responseHeaders);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream("Foobar".getBytes()));
		given(converter.canRead(String.class, contentType)).willReturn(true);
		given(converter.read(eq(String.class), any(HttpInputMessage.class))).willThrow(IOException.class);
		this.exception.expect(RestClientException.class);
		this.exception.expectMessage("Error while extracting response for type " +
				"[class java.lang.String] and content type [text/plain]");
		this.exception.expectCause(Matchers.instanceOf(IOException.class));

		this.extractor.extractData(this.response);
	}

	@Test  // SPR-13592
	@SuppressWarnings("unchecked")
	public void converterThrowsHttpMessageNotReadableException() throws IOException {
		HttpMessageConverter<String> converter = mock(HttpMessageConverter.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType contentType = MediaType.TEXT_PLAIN;
		responseHeaders.setContentType(contentType);
		this.extractor = new HttpMessageConverterExtractor<>(String.class, createConverterList(converter));
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(this.response.getHeaders()).willReturn(responseHeaders);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream("Foobar".getBytes()));
		given(converter.canRead(String.class, contentType)).willThrow(HttpMessageNotReadableException.class);
		this.exception.expect(RestClientException.class);
		this.exception.expectMessage("Error while extracting response for type " +
				"[class java.lang.String] and content type [text/plain]");
		this.exception.expectCause(Matchers.instanceOf(HttpMessageNotReadableException.class));

		this.extractor.extractData(this.response);
	}

	private List<HttpMessageConverter<?>> createConverterList(HttpMessageConverter<?> converter) {
		List<HttpMessageConverter<?>> converters = new ArrayList<>(1);
		converters.add(converter);
		return converters;
	}


}
