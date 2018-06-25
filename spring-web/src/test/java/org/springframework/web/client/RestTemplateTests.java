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

package org.springframework.web.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.parseMediaType;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
@SuppressWarnings("unchecked")
public class RestTemplateTests {

	private RestTemplate template;

	private ClientHttpRequestFactory requestFactory;

	private ClientHttpRequest request;

	private ClientHttpResponse response;

	private ResponseErrorHandler errorHandler;

	@SuppressWarnings("rawtypes")
	private HttpMessageConverter converter;


	@Before
	public void setup() {
		this.requestFactory = mock(ClientHttpRequestFactory.class);
		this.request = mock(ClientHttpRequest.class);
		this.response = mock(ClientHttpResponse.class);
		this.errorHandler = mock(ResponseErrorHandler.class);
		this.converter = mock(HttpMessageConverter.class);
		this.template = new RestTemplate(Collections.singletonList(this.converter));
		this.template.setRequestFactory(this.requestFactory);
		this.template.setErrorHandler(this.errorHandler);
	}


	@Test
	public void varArgsTemplateVariables() throws Exception {
		mockSentRequest(GET, "http://example.com/hotels/42/bookings/21");
		mockResponseStatus(HttpStatus.OK);

		this.template.execute("http://example.com/hotels/{hotel}/bookings/{booking}", GET,
				null, null, "42", "21");

		verify(this.response).close();
	}

	@Test
	public void varArgsNullTemplateVariable() throws Exception {
		mockSentRequest(GET, "http://example.com/-foo");
		mockResponseStatus(HttpStatus.OK);

		this.template.execute("http://example.com/{first}-{last}", GET, null, null, null, "foo");

		verify(this.response).close();
	}

	@Test
	public void mapTemplateVariables() throws Exception {
		mockSentRequest(GET, "http://example.com/hotels/42/bookings/42");
		mockResponseStatus(HttpStatus.OK);

		Map<String, String> vars = Collections.singletonMap("hotel", "42");
		this.template.execute("http://example.com/hotels/{hotel}/bookings/{hotel}", GET, null, null, vars);

		verify(this.response).close();
	}

	@Test
	public void mapNullTemplateVariable() throws Exception {
		mockSentRequest(GET, "http://example.com/-foo");
		mockResponseStatus(HttpStatus.OK);

		Map<String, String> vars = new HashMap<>(2);
		vars.put("first", null);
		vars.put("last", "foo");
		this.template.execute("http://example.com/{first}-{last}", GET, null, null, vars);

		verify(this.response).close();
	}

	@Test  // SPR-15201
	public void uriTemplateWithTrailingSlash() throws Exception {
		String url = "http://example.com/spring/";
		mockSentRequest(GET, url);
		mockResponseStatus(HttpStatus.OK);

		this.template.execute(url, GET, null, null);

		verify(this.response).close();
	}

	@Test
	public void errorHandling() throws Exception {
		String url = "http://example.com";
		mockSentRequest(GET, url);
		mockResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		willThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
				.given(this.errorHandler).handleError(new URI(url), GET, this.response);

		try {
			this.template.execute(url, GET, null, null);
			fail("HttpServerErrorException expected");
		}
		catch (HttpServerErrorException ex) {
			// expected
		}

		verify(this.response).close();
	}

	@Test
	public void getForObject() throws Exception {
		String expected = "Hello World";
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(GET, "http://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		mockTextResponseBody("Hello World");

		String result = this.template.getForObject("http://example.com", String.class);
		assertEquals("Invalid GET result", expected, result);
		assertEquals("Invalid Accept header", MediaType.TEXT_PLAIN_VALUE,
				requestHeaders.getFirst("Accept"));

		verify(this.response).close();
	}

	@Test
	public void getUnsupportedMediaType() throws Exception {
		mockSentRequest(GET, "http://example.com/resource");
		mockResponseStatus(HttpStatus.OK);

		given(this.converter.canRead(String.class, null)).willReturn(true);
		MediaType supportedMediaType = new MediaType("foo", "bar");
		given(this.converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(supportedMediaType));

		MediaType barBaz = new MediaType("bar", "baz");
		mockResponseBody("Foo", new MediaType("bar", "baz"));
		given(this.converter.canRead(String.class, barBaz)).willReturn(false);

		try {
			this.template.getForObject("http://example.com/{p}", String.class, "resource");
			fail("UnsupportedMediaTypeException expected");
		}
		catch (RestClientException ex) {
			// expected
		}

		verify(this.response).close();
	}

	@Test
	public void requestAvoidsDuplicateAcceptHeaderValues() throws Exception {
		HttpMessageConverter firstConverter = mock(HttpMessageConverter.class);
		given(firstConverter.canRead(any(), any())).willReturn(true);
		given(firstConverter.getSupportedMediaTypes())
				.willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		HttpMessageConverter secondConverter = mock(HttpMessageConverter.class);
		given(secondConverter.canRead(any(), any())).willReturn(true);
		given(secondConverter.getSupportedMediaTypes())
				.willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));

		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(GET, "http://example.com/", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		mockTextResponseBody("Hello World");

		this.template.setMessageConverters(Arrays.asList(firstConverter, secondConverter));
		this.template.getForObject("http://example.com/", String.class);

		assertEquals("Sent duplicate Accept header values", 1,
				requestHeaders.getAccept().size());
	}

	@Test
	public void getForEntity() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(GET, "http://example.com", requestHeaders);
		mockTextPlainHttpMessageConverter();
		mockResponseStatus(HttpStatus.OK);
		String expected = "Hello World";
		mockTextResponseBody(expected);

		ResponseEntity<String> result = this.template.getForEntity("http://example.com", String.class);
		assertEquals("Invalid GET result", expected, result.getBody());
		assertEquals("Invalid Accept header", MediaType.TEXT_PLAIN_VALUE, requestHeaders.getFirst("Accept"));
		assertEquals("Invalid Content-Type header", MediaType.TEXT_PLAIN, result.getHeaders().getContentType());
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verify(this.response).close();
	}

	@Test
	public void getForObjectWithCustomUriTemplateHandler() throws Exception {
		DefaultUriBuilderFactory uriTemplateHandler = new DefaultUriBuilderFactory();
		this.template.setUriTemplateHandler(uriTemplateHandler);
		mockSentRequest(GET, "http://example.com/hotels/1/pic/pics%2Flogo.png/size/150x150");
		mockResponseStatus(HttpStatus.OK);
		given(this.response.getHeaders()).willReturn(new HttpHeaders());
		given(this.response.getBody()).willReturn(StreamUtils.emptyInput());

		Map<String, String> uriVariables = new HashMap<>(2);
		uriVariables.put("hotel", "1");
		uriVariables.put("publicpath", "pics/logo.png");
		uriVariables.put("scale", "150x150");

		String url = "http://example.com/hotels/{hotel}/pic/{publicpath}/size/{scale}";
		this.template.getForObject(url, String.class, uriVariables);

		verify(this.response).close();
	}

	@Test
	public void headForHeaders() throws Exception {
		mockSentRequest(HEAD, "http://example.com");
		mockResponseStatus(HttpStatus.OK);
		HttpHeaders responseHeaders = new HttpHeaders();
		given(this.response.getHeaders()).willReturn(responseHeaders);

		HttpHeaders result = this.template.headForHeaders("http://example.com");

		assertSame("Invalid headers returned", responseHeaders, result);

		verify(this.response).close();
	}

	@Test
	public void postForLocation() throws Exception {
		mockSentRequest(POST, "http://example.com");
		mockTextPlainHttpMessageConverter();
		mockResponseStatus(HttpStatus.OK);
		String helloWorld = "Hello World";
		HttpHeaders responseHeaders = new HttpHeaders();
		URI expected = new URI("http://example.com/hotels");
		responseHeaders.setLocation(expected);
		given(this.response.getHeaders()).willReturn(responseHeaders);

		URI result = this.template.postForLocation("http://example.com", helloWorld);
		assertEquals("Invalid POST result", expected, result);

		verify(this.response).close();
	}

	@Test
	public void postForLocationEntityContentType() throws Exception {
		mockSentRequest(POST, "http://example.com");
		mockTextPlainHttpMessageConverter();
		mockResponseStatus(HttpStatus.OK);

		String helloWorld = "Hello World";
		HttpHeaders responseHeaders = new HttpHeaders();
		URI expected = new URI("http://example.com/hotels");
		responseHeaders.setLocation(expected);
		given(this.response.getHeaders()).willReturn(responseHeaders);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(MediaType.TEXT_PLAIN);
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);

		URI result = this.template.postForLocation("http://example.com", entity);
		assertEquals("Invalid POST result", expected, result);

		verify(this.response).close();
	}

	@Test
	public void postForLocationEntityCustomHeader() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "http://example.com", requestHeaders);
		mockTextPlainHttpMessageConverter();
		mockResponseStatus(HttpStatus.OK);
		HttpHeaders responseHeaders = new HttpHeaders();
		URI expected = new URI("http://example.com/hotels");
		responseHeaders.setLocation(expected);
		given(this.response.getHeaders()).willReturn(responseHeaders);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> entity = new HttpEntity<>("Hello World", entityHeaders);

		URI result = this.template.postForLocation("http://example.com", entity);
		assertEquals("Invalid POST result", expected, result);
		assertEquals("No custom header set", "MyValue", requestHeaders.getFirst("MyHeader"));

		verify(this.response).close();
	}

	@Test
	public void postForLocationNoLocation() throws Exception {
		mockSentRequest(POST, "http://example.com");
		mockTextPlainHttpMessageConverter();
		mockResponseStatus(HttpStatus.OK);

		URI result = this.template.postForLocation("http://example.com", "Hello World");
		assertNull("Invalid POST result", result);

		verify(this.response).close();
	}

	@Test
	public void postForLocationNull() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "http://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);

		this.template.postForLocation("http://example.com", null);
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());

		verify(this.response).close();
	}

	@Test
	public void postForObject() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "http://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		String expected = "42";
		mockResponseBody(expected, MediaType.TEXT_PLAIN);

		String result = this.template.postForObject("http://example.com", "Hello World", String.class);
		assertEquals("Invalid POST result", expected, result);
		assertEquals("Invalid Accept header", MediaType.TEXT_PLAIN_VALUE, requestHeaders.getFirst("Accept"));

		verify(this.response).close();
	}

	@Test
	public void postForEntity() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "http://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		String expected = "42";
		mockResponseBody(expected, MediaType.TEXT_PLAIN);

		ResponseEntity<String> result = this.template.postForEntity("http://example.com", "Hello World", String.class);
		assertEquals("Invalid POST result", expected, result.getBody());
		assertEquals("Invalid Content-Type", MediaType.TEXT_PLAIN, result.getHeaders().getContentType());
		assertEquals("Invalid Accept header", MediaType.TEXT_PLAIN_VALUE, requestHeaders.getFirst("Accept"));
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verify(this.response).close();
	}

	@Test
	public void postForObjectNull() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "http://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		responseHeaders.setContentLength(10);
		given(this.response.getHeaders()).willReturn(responseHeaders);
		given(this.response.getBody()).willReturn(StreamUtils.emptyInput());
		given(this.converter.read(String.class, this.response)).willReturn(null);

		String result = this.template.postForObject("http://example.com", null, String.class);
		assertNull("Invalid POST result", result);
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());

		verify(this.response).close();
	}

	@Test
	public void postForEntityNull() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "http://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		responseHeaders.setContentLength(10);
		given(this.response.getHeaders()).willReturn(responseHeaders);
		given(this.response.getBody()).willReturn(StreamUtils.emptyInput());
		given(this.converter.read(String.class, this.response)).willReturn(null);

		ResponseEntity<String> result = this.template.postForEntity("http://example.com", null, String.class);
		assertFalse("Invalid POST result", result.hasBody());
		assertEquals("Invalid Content-Type", MediaType.TEXT_PLAIN, result.getHeaders().getContentType());
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verify(this.response).close();
	}

	@Test
	public void put() throws Exception {
		mockTextPlainHttpMessageConverter();
		mockSentRequest(PUT, "http://example.com");
		mockResponseStatus(HttpStatus.OK);

		this.template.put("http://example.com", "Hello World");

		verify(this.response).close();
	}

	@Test
	public void putNull() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(PUT, "http://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);

		this.template.put("http://example.com", null);
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());

		verify(this.response).close();
	}

	@Test
	public void patchForObject() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(PATCH, "http://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		String expected = "42";
		mockResponseBody("42", MediaType.TEXT_PLAIN);

		String result = this.template.patchForObject("http://example.com", "Hello World", String.class);
		assertEquals("Invalid POST result", expected, result);
		assertEquals("Invalid Accept header", MediaType.TEXT_PLAIN_VALUE, requestHeaders.getFirst("Accept"));

		verify(this.response).close();
	}

	@Test
	public void patchForObjectNull() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(PATCH, "http://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		responseHeaders.setContentLength(10);
		given(this.response.getHeaders()).willReturn(responseHeaders);
		given(this.response.getBody()).willReturn(StreamUtils.emptyInput());

		String result = this.template.patchForObject("http://example.com", null, String.class);
		assertNull("Invalid POST result", result);
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());

		verify(this.response).close();
	}


	@Test
	public void delete() throws Exception {
		mockSentRequest(DELETE, "http://example.com");
		mockResponseStatus(HttpStatus.OK);

		this.template.delete("http://example.com");

		verify(this.response).close();
	}

	@Test
	public void optionsForAllow() throws Exception {
		mockSentRequest(OPTIONS, "http://example.com");
		mockResponseStatus(HttpStatus.OK);
		HttpHeaders responseHeaders = new HttpHeaders();
		EnumSet<HttpMethod> expected = EnumSet.of(GET, POST);
		responseHeaders.setAllow(expected);
		given(this.response.getHeaders()).willReturn(responseHeaders);

		Set<HttpMethod> result = this.template.optionsForAllow("http://example.com");
		assertEquals("Invalid OPTIONS result", expected, result);

		verify(this.response).close();
	}

	@Test  // SPR-9325, SPR-13860
	public void ioException() throws Exception {
		String url = "http://example.com/resource?access_token=123";
		mockSentRequest(GET, url);
		mockHttpMessageConverter(new MediaType("foo", "bar"), String.class);
		given(this.request.execute()).willThrow(new IOException("Socket failure"));

		try {
			this.template.getForObject(url, String.class);
			fail("RestClientException expected");
		}
		catch (ResourceAccessException ex) {
			assertEquals("I/O error on GET request for \"http://example.com/resource\": " +
							"Socket failure; nested exception is java.io.IOException: Socket failure",
					ex.getMessage());
		}
	}

	@Test  // SPR-15900
	public void ioExceptionWithEmptyQueryString() throws Exception {

		// http://example.com/resource?
		URI uri = new URI("http", "example.com", "/resource", "", null);

		given(this.converter.canRead(String.class, null)).willReturn(true);
		given(this.converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(parseMediaType("foo/bar")));
		given(this.requestFactory.createRequest(uri, GET)).willReturn(this.request);
		given(this.request.getHeaders()).willReturn(new HttpHeaders());
		given(this.request.execute()).willThrow(new IOException("Socket failure"));

		try {
			this.template.getForObject(uri, String.class);
			fail("RestClientException expected");
		}
		catch (ResourceAccessException ex) {
			assertEquals("I/O error on GET request for \"http://example.com/resource\": " +
					"Socket failure; nested exception is java.io.IOException: Socket failure",
					ex.getMessage());
		}
	}

	@Test
	public void exchange() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "http://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		String expected = "42";
		mockResponseBody(expected, MediaType.TEXT_PLAIN);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> entity = new HttpEntity<>("Hello World", entityHeaders);
		ResponseEntity<String> result = this.template.exchange("http://example.com", POST, entity, String.class);
		assertEquals("Invalid POST result", expected, result.getBody());
		assertEquals("Invalid Content-Type", MediaType.TEXT_PLAIN, result.getHeaders().getContentType());
		assertEquals("Invalid Accept header", MediaType.TEXT_PLAIN_VALUE, requestHeaders.getFirst("Accept"));
		assertEquals("Invalid custom header", "MyValue", requestHeaders.getFirst("MyHeader"));
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verify(this.response).close();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void exchangeParameterizedType() throws Exception {
		GenericHttpMessageConverter converter = mock(GenericHttpMessageConverter.class);
		this.template.setMessageConverters(Collections.<HttpMessageConverter<?>>singletonList(converter));
		ParameterizedTypeReference<List<Integer>> intList = new ParameterizedTypeReference<List<Integer>>() {};
		given(converter.canRead(intList.getType(), null, null)).willReturn(true);
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		given(converter.canWrite(String.class, String.class, null)).willReturn(true);

		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "http://example.com", requestHeaders);
		List<Integer> expected = Collections.singletonList(42);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		responseHeaders.setContentLength(10);
		mockResponseStatus(HttpStatus.OK);
		given(this.response.getHeaders()).willReturn(responseHeaders);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(Integer.toString(42).getBytes()));
		given(converter.canRead(intList.getType(), null, MediaType.TEXT_PLAIN)).willReturn(true);
		given(converter.read(eq(intList.getType()), eq(null), any(HttpInputMessage.class))).willReturn(expected);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> requestEntity = new HttpEntity<>("Hello World", entityHeaders);
		ResponseEntity<List<Integer>> result = this.template.exchange("http://example.com", POST, requestEntity, intList);
		assertEquals("Invalid POST result", expected, result.getBody());
		assertEquals("Invalid Content-Type", MediaType.TEXT_PLAIN, result.getHeaders().getContentType());
		assertEquals("Invalid Accept header", MediaType.TEXT_PLAIN_VALUE, requestHeaders.getFirst("Accept"));
		assertEquals("Invalid custom header", "MyValue", requestHeaders.getFirst("MyHeader"));
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verify(this.response).close();
	}

	@Test  // SPR-15066
	public void requestInterceptorCanAddExistingHeaderValueWithoutBody() throws Exception {
		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
			request.getHeaders().add("MyHeader", "MyInterceptorValue");
			return execution.execute(request, body);
		};
		this.template.setInterceptors(Collections.singletonList(interceptor));

		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "http://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.add("MyHeader", "MyEntityValue");
		HttpEntity<Void> entity = new HttpEntity<>(null, entityHeaders);
		this.template.exchange("http://example.com", POST, entity, Void.class);
		assertThat(requestHeaders.get("MyHeader"), contains("MyEntityValue", "MyInterceptorValue"));

		verify(this.response).close();
	}

	@Test  // SPR-15066
	public void requestInterceptorCanAddExistingHeaderValueWithBody() throws Exception {
		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
			request.getHeaders().add("MyHeader", "MyInterceptorValue");
			return execution.execute(request, body);
		};
		this.template.setInterceptors(Collections.singletonList(interceptor));

		MediaType contentType = MediaType.TEXT_PLAIN;
		given(this.converter.canWrite(String.class, contentType)).willReturn(true);
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "http://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(contentType);
		entityHeaders.add("MyHeader", "MyEntityValue");
		HttpEntity<String> entity = new HttpEntity<>("Hello World", entityHeaders);
		this.template.exchange("http://example.com", POST, entity, Void.class);
		assertThat(requestHeaders.get("MyHeader"), contains("MyEntityValue", "MyInterceptorValue"));

		verify(this.response).close();
	}

	private void mockSentRequest(HttpMethod method, String uri) throws Exception {
		mockSentRequest(method, uri, new HttpHeaders());
	}

	private void mockSentRequest(HttpMethod method, String uri, HttpHeaders requestHeaders) throws Exception {
		given(this.requestFactory.createRequest(new URI(uri), method)).willReturn(this.request);
		given(this.request.getHeaders()).willReturn(requestHeaders);
	}

	private void mockResponseStatus(HttpStatus responseStatus) throws Exception {
		given(this.request.execute()).willReturn(this.response);
		given(this.errorHandler.hasError(this.response)).willReturn(responseStatus.isError());
		given(this.response.getStatusCode()).willReturn(responseStatus);
		given(this.response.getRawStatusCode()).willReturn(responseStatus.value());
		given(this.response.getStatusText()).willReturn(responseStatus.getReasonPhrase());
	}

	private void mockTextPlainHttpMessageConverter() {
		mockHttpMessageConverter(MediaType.TEXT_PLAIN, String.class);
	}

	private void mockHttpMessageConverter(MediaType mediaType, Class type) {
		given(this.converter.canRead(type, null)).willReturn(true);
		given(this.converter.canRead(type, mediaType)).willReturn(true);
		given(this.converter.getSupportedMediaTypes())
				.willReturn(Collections.singletonList(mediaType));
		given(this.converter.canRead(type, mediaType)).willReturn(true);
		given(this.converter.canWrite(type, null)).willReturn(true);
		given(this.converter.canWrite(type, mediaType)).willReturn(true);
	}

	private void mockTextResponseBody(String expectedBody) throws Exception {
		mockResponseBody(expectedBody, MediaType.TEXT_PLAIN);
	}

	private void mockResponseBody(String expectedBody, MediaType mediaType) throws Exception {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(mediaType);
		responseHeaders.setContentLength(expectedBody.length());
		given(this.response.getHeaders()).willReturn(responseHeaders);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(expectedBody.getBytes()));
		given(this.converter.read(eq(String.class), any(HttpInputMessage.class))).willReturn(expectedBody);
	}

}
