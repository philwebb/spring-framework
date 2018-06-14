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

package org.springframework.web.reactive.function.server;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.support.ServerRequestWrapper;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Arjen Poutsma
 */
public class HeadersWrapperTests {

	private ServerRequest.Headers mockHeaders;

	private ServerRequestWrapper.HeadersWrapper wrapper;


	@Before
	public void createWrapper() {
		this.mockHeaders = mock(ServerRequest.Headers.class);
		this.wrapper = new ServerRequestWrapper.HeadersWrapper(this.mockHeaders);
	}


	@Test
	public void accept() throws Exception {
		List<MediaType> accept = Collections.singletonList(MediaType.APPLICATION_JSON);
		when(this.mockHeaders.accept()).thenReturn(accept);

		assertSame(accept, this.wrapper.accept());
	}

	@Test
	public void acceptCharset() throws Exception {
		List<Charset> acceptCharset = Collections.singletonList(StandardCharsets.UTF_8);
		when(this.mockHeaders.acceptCharset()).thenReturn(acceptCharset);

		assertSame(acceptCharset, this.wrapper.acceptCharset());
	}

	@Test
	public void contentLength() throws Exception {
		OptionalLong contentLength = OptionalLong.of(42L);
		when(this.mockHeaders.contentLength()).thenReturn(contentLength);

		assertSame(contentLength, this.wrapper.contentLength());
	}

	@Test
	public void contentType() throws Exception {
		Optional<MediaType> contentType = Optional.of(MediaType.APPLICATION_JSON);
		when(this.mockHeaders.contentType()).thenReturn(contentType);

		assertSame(contentType, this.wrapper.contentType());
	}

	@Test
	public void host() throws Exception {
		InetSocketAddress host = InetSocketAddress.createUnresolved("example.com", 42);
		when(this.mockHeaders.host()).thenReturn(host);

		assertSame(host, this.wrapper.host());
	}

	@Test
	public void range() throws Exception {
		List<HttpRange> range = Collections.singletonList(HttpRange.createByteRange(42));
		when(this.mockHeaders.range()).thenReturn(range);

		assertSame(range, this.wrapper.range());
	}

	@Test
	public void header() throws Exception {
		String name = "foo";
		List<String> value = Collections.singletonList("bar");
		when(this.mockHeaders.header(name)).thenReturn(value);

		assertSame(value, this.wrapper.header(name));
	}

	@Test
	public void asHttpHeaders() throws Exception {
		HttpHeaders httpHeaders = new HttpHeaders();
		when(this.mockHeaders.asHttpHeaders()).thenReturn(httpHeaders);

		assertSame(httpHeaders, this.wrapper.asHttpHeaders());
	}

}
