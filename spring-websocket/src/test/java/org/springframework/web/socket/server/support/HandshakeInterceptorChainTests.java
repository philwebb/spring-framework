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

package org.springframework.web.socket.server.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import static org.mockito.BDDMockito.*;

/**
 * Test fixture for {@link HandshakeInterceptorChain}.
 *
 * @author Rossen Stoyanchev
 */
public class HandshakeInterceptorChainTests extends AbstractHttpRequestTests {

	private HandshakeInterceptor i1;

	private HandshakeInterceptor i2;

	private HandshakeInterceptor i3;

	private List<HandshakeInterceptor> interceptors;

	private WebSocketHandler wsHandler;

	private Map<String, Object> attributes;


	@Before
	public void setup() {
		super.setup();

		this.i1 = mock(HandshakeInterceptor.class);
		this.i2 = mock(HandshakeInterceptor.class);
		this.i3 = mock(HandshakeInterceptor.class);
		this.interceptors = Arrays.asList(this.i1, this.i2, this.i3);
		this.wsHandler = mock(WebSocketHandler.class);
		this.attributes = new HashMap<>();
	}


	@Test
	public void success() throws Exception {
		given(this.i1.beforeHandshake(this.request, this.response, this.wsHandler, this.attributes)).willReturn(true);
		given(this.i2.beforeHandshake(this.request, this.response, this.wsHandler, this.attributes)).willReturn(true);
		given(this.i3.beforeHandshake(this.request, this.response, this.wsHandler, this.attributes)).willReturn(true);

		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(this.interceptors, this.wsHandler);
		chain.applyBeforeHandshake(this.request, this.response, this.attributes);

		verify(this.i1).beforeHandshake(this.request, this.response, this.wsHandler, this.attributes);
		verify(this.i2).beforeHandshake(this.request, this.response, this.wsHandler, this.attributes);
		verify(this.i3).beforeHandshake(this.request, this.response, this.wsHandler, this.attributes);
		verifyNoMoreInteractions(this.i1, this.i2, this.i3);
	}

	@Test
	public void applyBeforeHandshakeWithFalseReturnValue() throws Exception {
		given(this.i1.beforeHandshake(this.request, this.response, this.wsHandler, this.attributes)).willReturn(true);
		given(this.i2.beforeHandshake(this.request, this.response, this.wsHandler, this.attributes)).willReturn(false);

		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(this.interceptors, this.wsHandler);
		chain.applyBeforeHandshake(this.request, this.response, this.attributes);

		verify(this.i1).beforeHandshake(this.request, this.response, this.wsHandler, this.attributes);
		verify(this.i1).afterHandshake(this.request, this.response, this.wsHandler, null);
		verify(this.i2).beforeHandshake(this.request, this.response, this.wsHandler, this.attributes);
		verifyNoMoreInteractions(this.i1, this.i2, this.i3);
	}

	@Test
	public void applyAfterHandshakeOnly() {
		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(this.interceptors, this.wsHandler);
		chain.applyAfterHandshake(this.request, this.response, null);

		verifyNoMoreInteractions(this.i1, this.i2, this.i3);
	}

}
