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

package org.springframework.web.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static temp.XAssert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.HTTP_VERSION_NOT_SUPPORTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

/**
 * Unit tests for {@link DefaultResponseErrorHandler} handling of specific
 * HTTP status codes.
 */
@RunWith(Parameterized.class)
public class DefaultResponseErrorHandlerHttpStatusTests {

	@Parameters(name = "error: [{0}], exception: [{1}]")
	public static Object[][] errorCodes() {
		return new Object[][]{
				// 4xx
				{BAD_REQUEST, HttpClientErrorException.BadRequest.class},
				{UNAUTHORIZED, HttpClientErrorException.Unauthorized.class},
				{FORBIDDEN, HttpClientErrorException.Forbidden.class},
				{NOT_FOUND, HttpClientErrorException.NotFound.class},
				{METHOD_NOT_ALLOWED, HttpClientErrorException.MethodNotAllowed.class},
				{NOT_ACCEPTABLE, HttpClientErrorException.NotAcceptable.class},
				{CONFLICT, HttpClientErrorException.Conflict.class},
				{TOO_MANY_REQUESTS, HttpClientErrorException.TooManyRequests.class},
				{UNPROCESSABLE_ENTITY, HttpClientErrorException.UnprocessableEntity.class},
				{I_AM_A_TEAPOT, HttpClientErrorException.class},
				// 5xx
				{INTERNAL_SERVER_ERROR, HttpServerErrorException.InternalServerError.class},
				{NOT_IMPLEMENTED, HttpServerErrorException.NotImplemented.class},
				{BAD_GATEWAY, HttpServerErrorException.BadGateway.class},
				{SERVICE_UNAVAILABLE, HttpServerErrorException.ServiceUnavailable.class},
				{GATEWAY_TIMEOUT, HttpServerErrorException.GatewayTimeout.class},
				{HTTP_VERSION_NOT_SUPPORTED, HttpServerErrorException.class}
		};
	}

	@Parameterized.Parameter
	public HttpStatus httpStatus;

	@Parameterized.Parameter(1)
	public Class<? extends Throwable> expectedExceptionClass;

	private final DefaultResponseErrorHandler handler = new DefaultResponseErrorHandler();

	private final ClientHttpResponse response = mock(ClientHttpResponse.class);


	@Test
	public void hasErrorTrue() throws Exception {
		given(this.response.getRawStatusCode()).willReturn(this.httpStatus.value());
		assertTrue(this.handler.hasError(this.response));
	}

	@Test
	public void handleErrorException() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(this.response.getRawStatusCode()).willReturn(this.httpStatus.value());
		given(this.response.getHeaders()).willReturn(headers);

		assertThatExceptionOfType(expectedExceptionClass).isThrownBy(() ->
				this.handler.handleError(this.response));
	}

}
