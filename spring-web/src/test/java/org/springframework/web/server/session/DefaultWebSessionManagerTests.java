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
package org.springframework.web.server.session;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DefaultWebSessionManager}.
 * @author Rossen Stoyanchev
 * @author Rob Winch
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultWebSessionManagerTests {

	private DefaultWebSessionManager sessionManager;

	private ServerWebExchange exchange;

	@Mock
	private WebSessionIdResolver sessionIdResolver;

	@Mock
	private WebSessionStore sessionStore;

	@Mock
	private WebSession createSession;

	@Mock
	private WebSession updateSession;


	@Before
	public void setUp() throws Exception {

		given(this.createSession.save()).willReturn(Mono.empty());
		given(this.createSession.getId()).willReturn("create-session-id");
		given(this.updateSession.getId()).willReturn("update-session-id");

		given(this.sessionStore.createWebSession()).willReturn(Mono.just(this.createSession));
		given(this.sessionStore.retrieveSession(this.updateSession.getId())).willReturn(Mono.just(this.updateSession));

		this.sessionManager = new DefaultWebSessionManager();
		this.sessionManager.setSessionIdResolver(this.sessionIdResolver);
		this.sessionManager.setSessionStore(this.sessionStore);

		MockServerHttpRequest request = MockServerHttpRequest.get("/path").build();
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(request, response, this.sessionManager,
				ServerCodecConfigurer.create(), new AcceptHeaderLocaleContextResolver());
	}

	@Test
	public void getSessionSaveWhenCreatedAndNotStartedThenNotSaved() {

		given(this.sessionIdResolver.resolveSessionIds(this.exchange)).willReturn(Collections.emptyList());
		WebSession session = this.sessionManager.getSession(this.exchange).block();
		this.exchange.getResponse().setComplete().block();

		assertThat((Object) session).isSameAs(this.createSession);
		assertThat(session.isStarted()).isFalse();
		assertThat(session.isExpired()).isFalse();
		verify(this.createSession, never()).save();
		verify(this.sessionIdResolver, never()).setSessionId(any(), any());
	}

	@Test
	public void getSessionSaveWhenCreatedAndStartedThenSavesAndSetsId() {

		given(this.sessionIdResolver.resolveSessionIds(this.exchange)).willReturn(Collections.emptyList());
		WebSession session = this.sessionManager.getSession(this.exchange).block();
		assertThat((Object) session).isSameAs(this.createSession);
		String sessionId = this.createSession.getId();

		given(this.createSession.isStarted()).willReturn(true);
		this.exchange.getResponse().setComplete().block();

		verify(this.sessionStore).createWebSession();
		verify(this.sessionIdResolver).setSessionId(any(), eq(sessionId));
		verify(this.createSession).save();
	}

	@Test
	public void existingSession() {

		String sessionId = this.updateSession.getId();
		given(this.sessionIdResolver.resolveSessionIds(this.exchange)).willReturn(Collections.singletonList(sessionId));

		WebSession actual = this.sessionManager.getSession(this.exchange).block();
		assertThat((Object) actual).isNotNull();
		assertThat(actual.getId()).isEqualTo(sessionId);
	}

	@Test
	public void multipleSessionIds() {

		List<String> ids = Arrays.asList("not-this", "not-that", this.updateSession.getId());
		given(this.sessionStore.retrieveSession("not-this")).willReturn(Mono.empty());
		given(this.sessionStore.retrieveSession("not-that")).willReturn(Mono.empty());
		given(this.sessionIdResolver.resolveSessionIds(this.exchange)).willReturn(ids);
		WebSession actual = this.sessionManager.getSession(this.exchange).block();

		assertThat((Object) actual).isNotNull();
		assertThat(actual.getId()).isEqualTo(this.updateSession.getId());
	}
}
