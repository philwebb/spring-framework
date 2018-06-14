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

package org.springframework.http.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.support.HttpRequestWrapper;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
public class InterceptingClientHttpRequestFactoryTests {

	private RequestFactoryMock requestFactoryMock = new RequestFactoryMock();

	private RequestMock requestMock = new RequestMock();

	private ResponseMock responseMock = new ResponseMock();

	private InterceptingClientHttpRequestFactory requestFactory;


	@Test
	public void basic() throws Exception {
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add(new NoOpInterceptor());
		interceptors.add(new NoOpInterceptor());
		interceptors.add(new NoOpInterceptor());
		this.requestFactory = new InterceptingClientHttpRequestFactory(this.requestFactoryMock, interceptors);

		ClientHttpRequest request = this.requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET);
		ClientHttpResponse response = request.execute();

		assertTrue(((NoOpInterceptor) interceptors.get(0)).invoked);
		assertTrue(((NoOpInterceptor) interceptors.get(1)).invoked);
		assertTrue(((NoOpInterceptor) interceptors.get(2)).invoked);
		assertTrue(this.requestMock.executed);
		assertSame(this.responseMock, response);
	}

	@Test
	public void noExecution() throws Exception {
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add(new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				return InterceptingClientHttpRequestFactoryTests.this.responseMock;
			}
		});

		interceptors.add(new NoOpInterceptor());
		this.requestFactory = new InterceptingClientHttpRequestFactory(this.requestFactoryMock, interceptors);

		ClientHttpRequest request = this.requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET);
		ClientHttpResponse response = request.execute();

		assertFalse(((NoOpInterceptor) interceptors.get(1)).invoked);
		assertFalse(this.requestMock.executed);
		assertSame(this.responseMock, response);
	}

	@Test
	public void changeHeaders() throws Exception {
		final String headerName = "Foo";
		final String headerValue = "Bar";
		final String otherValue = "Baz";

		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				HttpRequestWrapper wrapper = new HttpRequestWrapper(request);
				wrapper.getHeaders().add(headerName, otherValue);
				return execution.execute(wrapper, body);
			}
		};

		this.requestMock = new RequestMock() {
			@Override
			public ClientHttpResponse execute() throws IOException {
				List<String> headerValues = getHeaders().get(headerName);
				assertEquals(2, headerValues.size());
				assertEquals(headerValue, headerValues.get(0));
				assertEquals(otherValue, headerValues.get(1));
				return super.execute();
			}
		};
		this.requestMock.getHeaders().add(headerName, headerValue);

		this.requestFactory =
				new InterceptingClientHttpRequestFactory(this.requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = this.requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	public void changeURI() throws Exception {
		final URI changedUri = new URI("http://example.com/2");

		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				return execution.execute(new HttpRequestWrapper(request) {
					@Override
					public URI getURI() {
						return changedUri;
					}

				}, body);
			}
		};

		this.requestFactoryMock = new RequestFactoryMock() {
			@Override
			public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
				assertEquals(changedUri, uri);
				return super.createRequest(uri, httpMethod);
			}
		};

		this.requestFactory =
				new InterceptingClientHttpRequestFactory(this.requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = this.requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	public void changeMethod() throws Exception {
		final HttpMethod changedMethod = HttpMethod.POST;

		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				return execution.execute(new HttpRequestWrapper(request) {
					@Override
					public HttpMethod getMethod() {
						return changedMethod;
					}

				}, body);
			}
		};

		this.requestFactoryMock = new RequestFactoryMock() {
			@Override
			public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
				assertEquals(changedMethod, httpMethod);
				return super.createRequest(uri, httpMethod);
			}
		};

		this.requestFactory =
				new InterceptingClientHttpRequestFactory(this.requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = this.requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET);
		request.execute();
	}

	@Test
	public void changeBody() throws Exception {
		final byte[] changedBody = "Foo".getBytes();

		ClientHttpRequestInterceptor interceptor = new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				return execution.execute(request, changedBody);
			}
		};

		this.requestFactory =
				new InterceptingClientHttpRequestFactory(this.requestFactoryMock, Collections.singletonList(interceptor));

		ClientHttpRequest request = this.requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET);
		request.execute();
		assertTrue(Arrays.equals(changedBody, this.requestMock.body.toByteArray()));
	}


	private static class NoOpInterceptor implements ClientHttpRequestInterceptor {

		private boolean invoked = false;

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
				throws IOException {
			this.invoked = true;
			return execution.execute(request, body);
		}
	}


	private class RequestFactoryMock implements ClientHttpRequestFactory {

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
			InterceptingClientHttpRequestFactoryTests.this.requestMock.setURI(uri);
			InterceptingClientHttpRequestFactoryTests.this.requestMock.setMethod(httpMethod);
			return InterceptingClientHttpRequestFactoryTests.this.requestMock;
		}

	}


	private class RequestMock implements ClientHttpRequest {

		private URI uri;

		private HttpMethod method;

		private HttpHeaders headers = new HttpHeaders();

		private ByteArrayOutputStream body = new ByteArrayOutputStream();

		private boolean executed = false;

		private RequestMock() {
		}

		@Override
		public URI getURI() {
			return this.uri;
		}

		public void setURI(URI uri) {
			this.uri = uri;
		}

		@Override
		public HttpMethod getMethod() {
			return this.method;
		}

		@Override
		public String getMethodValue() {
			return this.method.name();
		}

		public void setMethod(HttpMethod method) {
			this.method = method;
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Override
		public OutputStream getBody() throws IOException {
			return this.body;
		}

		@Override
		public ClientHttpResponse execute() throws IOException {
			this.executed = true;
			return InterceptingClientHttpRequestFactoryTests.this.responseMock;
		}
	}


	private static class ResponseMock implements ClientHttpResponse {

		private HttpStatus statusCode = HttpStatus.OK;

		private String statusText = "";

		private HttpHeaders headers = new HttpHeaders();

		@Override
		public HttpStatus getStatusCode() throws IOException {
			return this.statusCode;
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return this.statusCode.value();
		}

		@Override
		public String getStatusText() throws IOException {
			return this.statusText;
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Override
		public InputStream getBody() throws IOException {
			return null;
		}

		@Override
		public void close() {
		}
	}

}
