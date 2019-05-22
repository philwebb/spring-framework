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

package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for
 * {@link org.springframework.web.socket.config.annotation.AbstractWebSocketHandlerRegistration}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketHandlerRegistrationTests {

	private TestWebSocketHandlerRegistration registration;

	private TaskScheduler taskScheduler;


	@Before
	public void setup() {
		this.taskScheduler = Mockito.mock(TaskScheduler.class);
		this.registration = new TestWebSocketHandlerRegistration();
	}

	@Test
	public void minimal() {
		WebSocketHandler handler = new TextWebSocketHandler();
		this.registration.addHandler(handler, "/foo", "/bar");

		List<Mapping> mappings = this.registration.getMappings();
		assertThat((long) mappings.size()).isEqualTo((long) 2);

		Mapping m1 = mappings.get(0);
		assertThat(m1.webSocketHandler).isEqualTo(handler);
		assertThat(m1.path).isEqualTo("/foo");
		assertThat((Object) m1.interceptors).isNotNull();
		assertThat((long) m1.interceptors.length).isEqualTo((long) 1);
		assertThat(m1.interceptors[0].getClass()).isEqualTo(OriginHandshakeInterceptor.class);

		Mapping m2 = mappings.get(1);
		assertThat(m2.webSocketHandler).isEqualTo(handler);
		assertThat(m2.path).isEqualTo("/bar");
		assertThat((Object) m2.interceptors).isNotNull();
		assertThat((long) m2.interceptors.length).isEqualTo((long) 1);
		assertThat(m2.interceptors[0].getClass()).isEqualTo(OriginHandshakeInterceptor.class);
	}

	@Test
	public void interceptors() {
		WebSocketHandler handler = new TextWebSocketHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		this.registration.addHandler(handler, "/foo").addInterceptors(interceptor);

		List<Mapping> mappings = this.registration.getMappings();
		assertThat((long) mappings.size()).isEqualTo((long) 1);

		Mapping mapping = mappings.get(0);
		assertThat(mapping.webSocketHandler).isEqualTo(handler);
		assertThat(mapping.path).isEqualTo("/foo");
		assertThat((Object) mapping.interceptors).isNotNull();
		assertThat((long) mapping.interceptors.length).isEqualTo((long) 2);
		assertThat(mapping.interceptors[0]).isEqualTo(interceptor);
		assertThat(mapping.interceptors[1].getClass()).isEqualTo(OriginHandshakeInterceptor.class);
	}

	@Test
	public void emptyAllowedOrigin() {
		WebSocketHandler handler = new TextWebSocketHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		this.registration.addHandler(handler, "/foo").addInterceptors(interceptor).setAllowedOrigins();

		List<Mapping> mappings = this.registration.getMappings();
		assertThat((long) mappings.size()).isEqualTo((long) 1);

		Mapping mapping = mappings.get(0);
		assertThat(mapping.webSocketHandler).isEqualTo(handler);
		assertThat(mapping.path).isEqualTo("/foo");
		assertThat((Object) mapping.interceptors).isNotNull();
		assertThat((long) mapping.interceptors.length).isEqualTo((long) 2);
		assertThat(mapping.interceptors[0]).isEqualTo(interceptor);
		assertThat(mapping.interceptors[1].getClass()).isEqualTo(OriginHandshakeInterceptor.class);
	}

	@Test
	public void interceptorsWithAllowedOrigins() {
		WebSocketHandler handler = new TextWebSocketHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		this.registration.addHandler(handler, "/foo").addInterceptors(interceptor).setAllowedOrigins("https://mydomain1.com");

		List<Mapping> mappings = this.registration.getMappings();
		assertThat((long) mappings.size()).isEqualTo((long) 1);

		Mapping mapping = mappings.get(0);
		assertThat(mapping.webSocketHandler).isEqualTo(handler);
		assertThat(mapping.path).isEqualTo("/foo");
		assertThat((Object) mapping.interceptors).isNotNull();
		assertThat((long) mapping.interceptors.length).isEqualTo((long) 2);
		assertThat(mapping.interceptors[0]).isEqualTo(interceptor);
		assertThat(mapping.interceptors[1].getClass()).isEqualTo(OriginHandshakeInterceptor.class);
	}

	@Test
	public void interceptorsPassedToSockJsRegistration() {
		WebSocketHandler handler = new TextWebSocketHandler();
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();

		this.registration.addHandler(handler, "/foo")
				.addInterceptors(interceptor)
				.setAllowedOrigins("https://mydomain1.com")
				.withSockJS();

		this.registration.getSockJsServiceRegistration().setTaskScheduler(this.taskScheduler);

		List<Mapping> mappings = this.registration.getMappings();
		assertThat((long) mappings.size()).isEqualTo((long) 1);

		Mapping mapping = mappings.get(0);
		assertThat(mapping.webSocketHandler).isEqualTo(handler);
		assertThat(mapping.path).isEqualTo("/foo/**");
		assertThat((Object) mapping.sockJsService).isNotNull();
		assertThat(mapping.sockJsService.getAllowedOrigins().contains("https://mydomain1.com")).isTrue();
		List<HandshakeInterceptor> interceptors = mapping.sockJsService.getHandshakeInterceptors();
		assertThat(interceptors.get(0)).isEqualTo(interceptor);
		assertThat(interceptors.get(1).getClass()).isEqualTo(OriginHandshakeInterceptor.class);
	}

	@Test
	public void handshakeHandler() {
		WebSocketHandler handler = new TextWebSocketHandler();
		HandshakeHandler handshakeHandler = new DefaultHandshakeHandler();

		this.registration.addHandler(handler, "/foo").setHandshakeHandler(handshakeHandler);

		List<Mapping> mappings = this.registration.getMappings();
		assertThat((long) mappings.size()).isEqualTo((long) 1);

		Mapping mapping = mappings.get(0);
		assertThat(mapping.webSocketHandler).isEqualTo(handler);
		assertThat(mapping.path).isEqualTo("/foo");
		assertThat((Object) mapping.handshakeHandler).isSameAs(handshakeHandler);
	}

	@Test
	public void handshakeHandlerPassedToSockJsRegistration() {
		WebSocketHandler handler = new TextWebSocketHandler();
		HandshakeHandler handshakeHandler = new DefaultHandshakeHandler();

		this.registration.addHandler(handler, "/foo").setHandshakeHandler(handshakeHandler).withSockJS();
		this.registration.getSockJsServiceRegistration().setTaskScheduler(this.taskScheduler);

		List<Mapping> mappings = this.registration.getMappings();
		assertThat((long) mappings.size()).isEqualTo((long) 1);

		Mapping mapping = mappings.get(0);
		assertThat(mapping.webSocketHandler).isEqualTo(handler);
		assertThat(mapping.path).isEqualTo("/foo/**");
		assertThat((Object) mapping.sockJsService).isNotNull();

		WebSocketTransportHandler transportHandler =
				(WebSocketTransportHandler) mapping.sockJsService.getTransportHandlers().get(TransportType.WEBSOCKET);
		assertThat((Object) transportHandler.getHandshakeHandler()).isSameAs(handshakeHandler);
	}


	private static class TestWebSocketHandlerRegistration extends AbstractWebSocketHandlerRegistration<List<Mapping>> {

		@Override
		protected List<Mapping> createMappings() {
			return new ArrayList<>();
		}

		@Override
		protected void addSockJsServiceMapping(List<Mapping> mappings, SockJsService sockJsService,
				WebSocketHandler wsHandler, String pathPattern) {

			mappings.add(new Mapping(wsHandler, pathPattern, sockJsService));
		}

		@Override
		protected void addWebSocketHandlerMapping(List<Mapping> mappings, WebSocketHandler handler,
				HandshakeHandler handshakeHandler, HandshakeInterceptor[] interceptors, String path) {

			mappings.add(new Mapping(handler, path, handshakeHandler, interceptors));
		}
	}


	private static class Mapping {

		private final WebSocketHandler webSocketHandler;

		private final String path;

		private final HandshakeHandler handshakeHandler;

		private final HandshakeInterceptor[] interceptors;

		private final DefaultSockJsService sockJsService;

		public Mapping(WebSocketHandler handler, String path, SockJsService sockJsService) {
			this.webSocketHandler = handler;
			this.path = path;
			this.handshakeHandler = null;
			this.interceptors = null;
			this.sockJsService = (DefaultSockJsService) sockJsService;
		}

		public Mapping(WebSocketHandler h, String path, HandshakeHandler hh, HandshakeInterceptor[] interceptors) {
			this.webSocketHandler = h;
			this.path = path;
			this.handshakeHandler = hh;
			this.interceptors = interceptors;
			this.sockJsService = null;
		}
	}

}
