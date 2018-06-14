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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import static java.time.Instant.*;
import static java.time.format.DateTimeFormatter.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.servlet.HandlerMapping.*;

/**
 * Test fixture for {@link HttpEntityMethodProcessor} delegating to a mock
 * {@link HttpMessageConverter}.
 *
 * <p>Also see {@link HttpEntityMethodProcessorTests}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class HttpEntityMethodProcessorMockTests {

	private static final ZoneId GMT = ZoneId.of("GMT");


	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private HttpEntityMethodProcessor processor;

	private HttpMessageConverter<String> stringHttpMessageConverter;

	private HttpMessageConverter<Resource> resourceMessageConverter;

	private HttpMessageConverter<Object> resourceRegionMessageConverter;

	private MethodParameter paramHttpEntity;

	private MethodParameter paramRequestEntity;

	private MethodParameter paramResponseEntity;

	private MethodParameter paramInt;

	private MethodParameter returnTypeResponseEntity;

	private MethodParameter returnTypeResponseEntityProduces;

	private MethodParameter returnTypeResponseEntityResource;

	private MethodParameter returnTypeHttpEntity;

	private MethodParameter returnTypeHttpEntitySubclass;

	private MethodParameter returnTypeInt;

	private ModelAndViewContainer mavContainer;

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ServletWebRequest webRequest;


	@Before
	@SuppressWarnings("unchecked")
	public void setup() throws Exception {

		this.stringHttpMessageConverter = mock(HttpMessageConverter.class);
		given(this.stringHttpMessageConverter.getSupportedMediaTypes())
				.willReturn(Collections.singletonList(TEXT_PLAIN));

		this.resourceMessageConverter = mock(HttpMessageConverter.class);
		given(this.resourceMessageConverter.getSupportedMediaTypes())
				.willReturn(Collections.singletonList(MediaType.ALL));

		this.resourceRegionMessageConverter = mock(HttpMessageConverter.class);
		given(this.resourceRegionMessageConverter.getSupportedMediaTypes())
				.willReturn(Collections.singletonList(MediaType.ALL));

		this.processor = new HttpEntityMethodProcessor(Arrays.asList(
				this.stringHttpMessageConverter, this.resourceMessageConverter, this.resourceRegionMessageConverter));

		Method handle1 = getClass().getMethod("handle1", HttpEntity.class, ResponseEntity.class,
				Integer.TYPE, RequestEntity.class);

		this.paramHttpEntity = new MethodParameter(handle1, 0);
		this.paramRequestEntity = new MethodParameter(handle1, 3);
		this.paramResponseEntity = new MethodParameter(handle1, 1);
		this.paramInt = new MethodParameter(handle1, 2);
		this.returnTypeResponseEntity = new MethodParameter(handle1, -1);
		this.returnTypeResponseEntityProduces = new MethodParameter(getClass().getMethod("handle4"), -1);
		this.returnTypeHttpEntity = new MethodParameter(getClass().getMethod("handle2", HttpEntity.class), -1);
		this.returnTypeHttpEntitySubclass = new MethodParameter(getClass().getMethod("handle2x", HttpEntity.class), -1);
		this.returnTypeInt = new MethodParameter(getClass().getMethod("handle3"), -1);
		this.returnTypeResponseEntityResource = new MethodParameter(getClass().getMethod("handle5"), -1);

		this.mavContainer = new ModelAndViewContainer();
		this.servletRequest = new MockHttpServletRequest("GET", "/foo");
		this.servletResponse = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(this.servletRequest, this.servletResponse);
	}


	@Test
	public void supportsParameter() {
		assertTrue("HttpEntity parameter not supported", this.processor.supportsParameter(this.paramHttpEntity));
		assertTrue("RequestEntity parameter not supported", this.processor.supportsParameter(this.paramRequestEntity));
		assertFalse("ResponseEntity parameter supported", this.processor.supportsParameter(this.paramResponseEntity));
		assertFalse("non-entity parameter supported", this.processor.supportsParameter(this.paramInt));
	}

	@Test
	public void supportsReturnType() {
		assertTrue("ResponseEntity return type not supported", this.processor.supportsReturnType(this.returnTypeResponseEntity));
		assertTrue("HttpEntity return type not supported", this.processor.supportsReturnType(this.returnTypeHttpEntity));
		assertTrue("Custom HttpEntity subclass not supported", this.processor.supportsReturnType(this.returnTypeHttpEntitySubclass));
		assertFalse("RequestEntity parameter supported",
				this.processor.supportsReturnType(this.paramRequestEntity));
		assertFalse("non-ResponseBody return type supported", this.processor.supportsReturnType(this.returnTypeInt));
	}

	@Test
	public void shouldResolveHttpEntityArgument() throws Exception {
		String body = "Foo";

		MediaType contentType = TEXT_PLAIN;
		this.servletRequest.addHeader("Content-Type", contentType.toString());
		this.servletRequest.setContent(body.getBytes(StandardCharsets.UTF_8));

		given(this.stringHttpMessageConverter.canRead(String.class, contentType)).willReturn(true);
		given(this.stringHttpMessageConverter.read(eq(String.class), isA(HttpInputMessage.class))).willReturn(body);

		Object result = this.processor.resolveArgument(this.paramHttpEntity, this.mavContainer, this.webRequest, null);

		assertTrue(result instanceof HttpEntity);
		assertFalse("The requestHandled flag shouldn't change", this.mavContainer.isRequestHandled());
		assertEquals("Invalid argument", body, ((HttpEntity<?>) result).getBody());
	}

	@Test
	public void shouldResolveRequestEntityArgument() throws Exception {
		String body = "Foo";

		MediaType contentType = TEXT_PLAIN;
		this.servletRequest.addHeader("Content-Type", contentType.toString());
		this.servletRequest.setMethod("GET");
		this.servletRequest.setServerName("www.example.com");
		this.servletRequest.setServerPort(80);
		this.servletRequest.setRequestURI("/path");
		this.servletRequest.setContent(body.getBytes(StandardCharsets.UTF_8));

		given(this.stringHttpMessageConverter.canRead(String.class, contentType)).willReturn(true);
		given(this.stringHttpMessageConverter.read(eq(String.class), isA(HttpInputMessage.class))).willReturn(body);

		Object result = this.processor.resolveArgument(this.paramRequestEntity, this.mavContainer, this.webRequest, null);

		assertTrue(result instanceof RequestEntity);
		assertFalse("The requestHandled flag shouldn't change", this.mavContainer.isRequestHandled());
		RequestEntity<?> requestEntity = (RequestEntity<?>) result;
		assertEquals("Invalid method", HttpMethod.GET, requestEntity.getMethod());
		// using default port (which is 80), so do not need to append the port (-1 means ignore)
		URI uri = new URI("http", null, "www.example.com", -1, "/path", null, null);
		assertEquals("Invalid url", uri, requestEntity.getUrl());
		assertEquals("Invalid argument", body, requestEntity.getBody());
	}

	@Test
	public void shouldFailResolvingWhenConverterCannotRead() throws Exception {
		MediaType contentType = TEXT_PLAIN;
		this.servletRequest.setMethod("POST");
		this.servletRequest.addHeader("Content-Type", contentType.toString());

		given(this.stringHttpMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(contentType));
		given(this.stringHttpMessageConverter.canRead(String.class, contentType)).willReturn(false);

		this.thrown.expect(HttpMediaTypeNotSupportedException.class);
		this.processor.resolveArgument(this.paramHttpEntity, this.mavContainer, this.webRequest, null);
	}

	@Test
	public void shouldFailResolvingWhenContentTypeNotSupported() throws Exception {
		this.servletRequest.setMethod("POST");
		this.servletRequest.setContent("some content".getBytes(StandardCharsets.UTF_8));
		this.thrown.expect(HttpMediaTypeNotSupportedException.class);
		this.processor.resolveArgument(this.paramHttpEntity, this.mavContainer, this.webRequest, null);
	}

	@Test
	public void shouldHandleReturnValue() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);
		MediaType accepted = TEXT_PLAIN;
		this.servletRequest.addHeader("Accept", accepted.toString());
		initStringMessageConversion(accepted);

		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertTrue(this.mavContainer.isRequestHandled());
		verify(this.stringHttpMessageConverter).write(eq(body), eq(accepted), isA(HttpOutputMessage.class));
	}

	@Test
	public void shouldHandleReturnValueWithProducibleMediaType() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);
		this.servletRequest.addHeader("Accept", "text/*");
		this.servletRequest.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(MediaType.TEXT_HTML));
		given(this.stringHttpMessageConverter.canWrite(String.class, MediaType.TEXT_HTML)).willReturn(true);

		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntityProduces, this.mavContainer, this.webRequest);

		assertTrue(this.mavContainer.isRequestHandled());
		verify(this.stringHttpMessageConverter).write(eq(body), eq(MediaType.TEXT_HTML), isA(HttpOutputMessage.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldHandleReturnValueWithResponseBodyAdvice() throws Exception {
		this.servletRequest.addHeader("Accept", "text/*");
		this.servletRequest.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(MediaType.TEXT_HTML));
		ResponseEntity<String> returnValue = new ResponseEntity<>(HttpStatus.OK);
		ResponseBodyAdvice<String> advice = mock(ResponseBodyAdvice.class);
		given(advice.supports(any(), any())).willReturn(true);
		given(advice.beforeBodyWrite(any(), any(), any(), any(), any(), any())).willReturn("Foo");

		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(
				Collections.singletonList(this.stringHttpMessageConverter), null, Collections.singletonList(advice));

		reset(this.stringHttpMessageConverter);
		given(this.stringHttpMessageConverter.canWrite(String.class, MediaType.TEXT_HTML)).willReturn(true);

		processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertTrue(this.mavContainer.isRequestHandled());
		verify(this.stringHttpMessageConverter).write(eq("Foo"), eq(MediaType.TEXT_HTML), isA(HttpOutputMessage.class));
	}

	@Test
	public void shouldFailHandlingWhenContentTypeNotSupported() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);
		MediaType accepted = MediaType.APPLICATION_ATOM_XML;
		this.servletRequest.addHeader("Accept", accepted.toString());

		given(this.stringHttpMessageConverter.canWrite(String.class, null)).willReturn(true);
		given(this.stringHttpMessageConverter.getSupportedMediaTypes())
				.willReturn(Collections.singletonList(TEXT_PLAIN));

		this.thrown.expect(HttpMediaTypeNotAcceptableException.class);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);
	}

	@Test
	public void shouldFailHandlingWhenConverterCannotWrite() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);
		MediaType accepted = TEXT_PLAIN;
		this.servletRequest.addHeader("Accept", accepted.toString());

		given(this.stringHttpMessageConverter.canWrite(String.class, null)).willReturn(true);
		given(this.stringHttpMessageConverter.getSupportedMediaTypes())
				.willReturn(Collections.singletonList(TEXT_PLAIN));
		given(this.stringHttpMessageConverter.canWrite(String.class, accepted)).willReturn(false);

		this.thrown.expect(HttpMediaTypeNotAcceptableException.class);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntityProduces, this.mavContainer, this.webRequest);
	}

	@Test  // SPR-9142
	public void shouldFailHandlingWhenAcceptHeaderIllegal() throws Exception {
		ResponseEntity<String> returnValue = new ResponseEntity<>("Body", HttpStatus.ACCEPTED);
		this.servletRequest.addHeader("Accept", "01");

		this.thrown.expect(HttpMediaTypeNotAcceptableException.class);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);
	}

	@Test
	public void shouldHandleResponseHeaderNoBody() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set("headerName", "headerValue");
		ResponseEntity<String> returnValue = new ResponseEntity<>(headers, HttpStatus.ACCEPTED);

		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertTrue(this.mavContainer.isRequestHandled());
		assertEquals("headerValue", this.servletResponse.getHeader("headerName"));
	}

	@Test
	public void shouldHandleResponseHeaderAndBody() throws Exception {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("header", "headerValue");
		ResponseEntity<String> returnValue = new ResponseEntity<>("body", responseHeaders, HttpStatus.ACCEPTED);

		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		ArgumentCaptor<HttpOutputMessage> outputMessage = ArgumentCaptor.forClass(HttpOutputMessage.class);
		verify(this.stringHttpMessageConverter).write(eq("body"), eq(TEXT_PLAIN), outputMessage.capture());
		assertTrue(this.mavContainer.isRequestHandled());
		assertEquals("headerValue", outputMessage.getValue().getHeaders().get("header").get(0));
	}

	@Test
	public void shouldHandleLastModifiedWithHttp304() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		ZonedDateTime dateTime = ofEpochMilli(currentTime).atZone(GMT);
		this.servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, RFC_1123_DATE_TIME.format(dateTime));
		ResponseEntity<String> returnValue = ResponseEntity.ok().lastModified(oneMinuteAgo).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, null, oneMinuteAgo);
	}

	@Test
	public void handleEtagWithHttp304() throws Exception {
		String etagValue = "\"deadb33f8badf00d\"";
		this.servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, -1);
	}

	@Test  // SPR-14559
	public void shouldHandleInvalidIfNoneMatchWithHttp200() throws Exception {
		String etagValue = "\"deadb33f8badf00d\"";
		this.servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "unquoted");
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertConditionalResponse(HttpStatus.OK, "body", etagValue, -1);
	}

	@Test
	public void shouldHandleETagAndLastModifiedWithHttp304() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		String etagValue = "\"deadb33f8badf00d\"";
		ZonedDateTime dateTime = ofEpochMilli(currentTime).atZone(GMT);
		this.servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, RFC_1123_DATE_TIME.format(dateTime));
		this.servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok()
				.eTag(etagValue).lastModified(oneMinuteAgo).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, oneMinuteAgo);
	}

	@Test
	public void shouldHandleNotModifiedResponse() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		String etagValue = "\"deadb33f8badf00d\"";
		ResponseEntity<String> returnValue = ResponseEntity.status(HttpStatus.NOT_MODIFIED)
				.eTag(etagValue).lastModified(oneMinuteAgo).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, oneMinuteAgo);
	}

	@Test
	public void shouldHandleChangedETagAndLastModified() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		String etagValue = "\"deadb33f8badf00d\"";
		String changedEtagValue = "\"changed-etag-value\"";
		ZonedDateTime dateTime = ofEpochMilli(currentTime).atZone(GMT);
		this.servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, RFC_1123_DATE_TIME.format(dateTime));
		this.servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok()
				.eTag(changedEtagValue).lastModified(oneMinuteAgo).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertConditionalResponse(HttpStatus.OK, null, changedEtagValue, oneMinuteAgo);
	}

	@Test  // SPR-13496
	public void shouldHandleConditionalRequestIfNoneMatchWildcard() throws Exception {
		String wildcardValue = "*";
		String etagValue = "\"some-etag\"";
		this.servletRequest.setMethod("POST");
		this.servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, wildcardValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertConditionalResponse(HttpStatus.OK, "body", etagValue, -1);
	}

	@Test  // SPR-13626
	public void shouldHandleGetIfNoneMatchWildcard() throws Exception {
		String wildcardValue = "*";
		String etagValue = "\"some-etag\"";
		this.servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, wildcardValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertConditionalResponse(HttpStatus.OK, "body", etagValue, -1);
	}

	@Test  // SPR-13626
	public void shouldHandleIfNoneMatchIfMatch() throws Exception {
		String etagValue = "\"some-etag\"";
		this.servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		this.servletRequest.addHeader(HttpHeaders.IF_MATCH, "ifmatch");
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, -1);
	}

	@Test  // SPR-13626
	public void shouldHandleIfNoneMatchIfUnmodifiedSince() throws Exception {
		String etagValue = "\"some-etag\"";
		this.servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		ZonedDateTime dateTime = ofEpochMilli(new Date().getTime()).atZone(GMT);
		this.servletRequest.addHeader(HttpHeaders.IF_UNMODIFIED_SINCE, RFC_1123_DATE_TIME.format(dateTime));
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, -1);
	}

	@Test
	public void shouldHandleResource() throws Exception {
		ResponseEntity<Resource> returnValue = ResponseEntity
				.ok(new ByteArrayResource("Content".getBytes(StandardCharsets.UTF_8)));

		given(this.resourceMessageConverter.canWrite(ByteArrayResource.class, null)).willReturn(true);
		given(this.resourceMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.ALL));
		given(this.resourceMessageConverter.canWrite(ByteArrayResource.class, APPLICATION_OCTET_STREAM)).willReturn(true);

		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntityResource, this.mavContainer, this.webRequest);

		then(this.resourceMessageConverter).should(times(1)).write(
				any(ByteArrayResource.class), eq(APPLICATION_OCTET_STREAM), any(HttpOutputMessage.class));
		assertEquals(200, this.servletResponse.getStatus());
	}

	@Test
	public void shouldHandleResourceByteRange() throws Exception {
		ResponseEntity<Resource> returnValue = ResponseEntity
				.ok(new ByteArrayResource("Content".getBytes(StandardCharsets.UTF_8)));
		this.servletRequest.addHeader("Range", "bytes=0-5");

		given(this.resourceRegionMessageConverter.canWrite(any(), eq(null))).willReturn(true);
		given(this.resourceRegionMessageConverter.canWrite(any(), eq(APPLICATION_OCTET_STREAM))).willReturn(true);

		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntityResource, this.mavContainer, this.webRequest);

		then(this.resourceRegionMessageConverter).should(times(1)).write(
				anyCollection(), eq(APPLICATION_OCTET_STREAM),
				argThat(outputMessage -> "bytes".equals(outputMessage.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES))));
		assertEquals(206, this.servletResponse.getStatus());
	}

	@Test
	public void handleReturnTypeResourceIllegalByteRange() throws Exception {
		ResponseEntity<Resource> returnValue = ResponseEntity
				.ok(new ByteArrayResource("Content".getBytes(StandardCharsets.UTF_8)));
		this.servletRequest.addHeader("Range", "illegal");

		given(this.resourceRegionMessageConverter.canWrite(any(), eq(null))).willReturn(true);
		given(this.resourceRegionMessageConverter.canWrite(any(), eq(APPLICATION_OCTET_STREAM))).willReturn(true);

		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntityResource, this.mavContainer, this.webRequest);

		then(this.resourceRegionMessageConverter).should(never()).write(
				anyCollection(), eq(APPLICATION_OCTET_STREAM), any(HttpOutputMessage.class));
		assertEquals(416, this.servletResponse.getStatus());
	}

	@Test //SPR-16754
	public void disableRangeSupportForStreamingResponses() throws Exception {
		InputStream is = new ByteArrayInputStream("Content".getBytes(StandardCharsets.UTF_8));
		InputStreamResource resource = new InputStreamResource(is, "test");
		ResponseEntity<Resource> returnValue = ResponseEntity.ok(resource);
		this.servletRequest.addHeader("Range", "bytes=0-5");

		given(this.resourceMessageConverter.canWrite(any(), eq(null))).willReturn(true);
		given(this.resourceMessageConverter.canWrite(any(), eq(APPLICATION_OCTET_STREAM))).willReturn(true);

		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntityResource, this.mavContainer, this.webRequest);
		then(this.resourceMessageConverter).should(times(1)).write(
				any(InputStreamResource.class), eq(APPLICATION_OCTET_STREAM), any(HttpOutputMessage.class));
		assertEquals(200, this.servletResponse.getStatus());
		assertThat(this.servletResponse.getHeader(HttpHeaders.ACCEPT_RANGES), Matchers.isEmptyOrNullString());
	}

	@Test  //SPR-14767
	public void shouldHandleValidatorHeadersInPutResponses() throws Exception {
		this.servletRequest.setMethod("PUT");
		String etagValue = "\"some-etag\"";
		ResponseEntity<String> returnValue = ResponseEntity.ok().header(HttpHeaders.ETAG, etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertConditionalResponse(HttpStatus.OK, "body", etagValue, -1);
	}

	@Test
	public void shouldNotFailPreconditionForPutRequests() throws Exception {
		this.servletRequest.setMethod("PUT");
		ZonedDateTime dateTime = ofEpochMilli(new Date().getTime()).atZone(GMT);
		this.servletRequest.addHeader(HttpHeaders.IF_UNMODIFIED_SINCE, RFC_1123_DATE_TIME.format(dateTime));

		long justModified = dateTime.plus(1, ChronoUnit.SECONDS).toEpochSecond() * 1000;
		ResponseEntity<String> returnValue = ResponseEntity.ok()
				.lastModified(justModified).body("body");
		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertConditionalResponse(HttpStatus.OK, null, null, justModified);
	}

	@Test
	public void varyHeader() throws Exception {
		String[] entityValues = {"Accept-Language", "User-Agent"};
		String[] existingValues = {};
		String[] expected = {"Accept-Language, User-Agent"};
		testVaryHeader(entityValues, existingValues, expected);
	}

	@Test
	public void varyHeaderWithExistingWildcard() throws Exception {
		String[] entityValues = {"Accept-Language"};
		String[] existingValues = {"*"};
		String[] expected = {"*"};
		testVaryHeader(entityValues, existingValues, expected);
	}

	@Test
	public void varyHeaderWithExistingCommaValues() throws Exception {
		String[] entityValues = {"Accept-Language", "User-Agent"};
		String[] existingValues = {"Accept-Encoding", "Accept-Language"};
		String[] expected = {"Accept-Encoding", "Accept-Language", "User-Agent"};
		testVaryHeader(entityValues, existingValues, expected);
	}

	@Test
	public void varyHeaderWithExistingCommaSeparatedValues() throws Exception {
		String[] entityValues = {"Accept-Language", "User-Agent"};
		String[] existingValues = {"Accept-Encoding, Accept-Language"};
		String[] expected = {"Accept-Encoding, Accept-Language", "User-Agent"};
		testVaryHeader(entityValues, existingValues, expected);
	}

	@Test
	public void handleReturnValueVaryHeader() throws Exception {
		String[] entityValues = {"Accept-Language", "User-Agent"};
		String[] existingValues = {"Accept-Encoding, Accept-Language"};
		String[] expected = {"Accept-Encoding, Accept-Language", "User-Agent"};
		testVaryHeader(entityValues, existingValues, expected);
	}


	private void testVaryHeader(String[] entityValues, String[] existingValues, String[] expected) throws Exception {
		ResponseEntity<String> returnValue = ResponseEntity.ok().varyBy(entityValues).body("Foo");
		for (String value : existingValues) {
			this.servletResponse.addHeader("Vary", value);
		}
		initStringMessageConversion(TEXT_PLAIN);
		this.processor.handleReturnValue(returnValue, this.returnTypeResponseEntity, this.mavContainer, this.webRequest);

		assertTrue(this.mavContainer.isRequestHandled());
		assertEquals(Arrays.asList(expected), this.servletResponse.getHeaders("Vary"));
		verify(this.stringHttpMessageConverter).write(eq("Foo"), eq(TEXT_PLAIN), isA(HttpOutputMessage.class));
	}

	private void initStringMessageConversion(MediaType accepted) {
		given(this.stringHttpMessageConverter.canWrite(String.class, null)).willReturn(true);
		given(this.stringHttpMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(TEXT_PLAIN));
		given(this.stringHttpMessageConverter.canWrite(String.class, accepted)).willReturn(true);
	}

	private void assertResponseBody(String body) throws IOException {
		ArgumentCaptor<HttpOutputMessage> outputMessage = ArgumentCaptor.forClass(HttpOutputMessage.class);
		verify(this.stringHttpMessageConverter).write(eq(body), eq(TEXT_PLAIN), outputMessage.capture());
	}

	private void assertConditionalResponse(HttpStatus status, String body, String etag, long lastModified)
			throws IOException {

		assertEquals(status.value(), this.servletResponse.getStatus());
		assertTrue(this.mavContainer.isRequestHandled());
		if (body != null) {
			assertResponseBody(body);
		}
		else {
			assertEquals(0, this.servletResponse.getContentAsByteArray().length);
		}
		if (etag != null) {
			assertEquals(1, this.servletResponse.getHeaderValues(HttpHeaders.ETAG).size());
			assertEquals(etag, this.servletResponse.getHeader(HttpHeaders.ETAG));
		}
		if (lastModified != -1) {
			assertEquals(1, this.servletResponse.getHeaderValues(HttpHeaders.LAST_MODIFIED).size());
			assertEquals(lastModified / 1000, this.servletResponse.getDateHeader(HttpHeaders.LAST_MODIFIED) / 1000);
		}
	}


	@SuppressWarnings("unused")
	public ResponseEntity<String> handle1(HttpEntity<String> httpEntity, ResponseEntity<String> entity,
			int i, RequestEntity<String> requestEntity) {

		return entity;
	}

	@SuppressWarnings("unused")
	public HttpEntity<?> handle2(HttpEntity<?> entity) {
		return entity;
	}

	@SuppressWarnings("unused")
	public CustomHttpEntity handle2x(HttpEntity<?> entity) {
		return new CustomHttpEntity();
	}

	@SuppressWarnings("unused")
	public int handle3() {
		return 42;
	}

	@SuppressWarnings("unused")
	@RequestMapping(produces = {"text/html", "application/xhtml+xml"})
	public ResponseEntity<String> handle4() {
		return null;
	}

	@SuppressWarnings("unused")
	public ResponseEntity<Resource> handle5() {
		return null;
	}

	@SuppressWarnings("unused")
	public static class CustomHttpEntity extends HttpEntity<Object> {
	}

}
