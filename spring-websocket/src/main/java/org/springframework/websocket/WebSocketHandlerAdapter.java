/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.websocket;

/**
 * A {@link WebSocketHandler} with empty methods.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see WebSocketHandler
 */
public abstract class WebSocketHandlerAdapter<T extends WebSocketMessage<?>> implements
		WebSocketHandler<T> {

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus)
			throws Exception {
	}

	@Override
	public void handleError(WebSocketSession session, Throwable exception) {
	}

	@Override
	public void handleMessage(WebSocketSession session, T message) throws Exception {
	}


	/**
	 * A {@link WebSocketHandlerAdapter} that handles {@link TextMessage}s.
	 */
	public static class Text extends WebSocketHandlerAdapter<TextMessage> {
	}


	/**
	 * A {@link WebSocketHandlerAdapter} that handles {@link BinaryMessage}s.
	 */
	public static class Binary extends WebSocketHandlerAdapter<BinaryMessage> {
	}


	/**
	 * A {@link WebSocketHandlerAdapter} that handles {@link TextMessage}s or
	 * {@link BinaryMessage}s.
	 * @see #handleTextMessage
	 * @see #handlBinaryeMessage
	 */
	public static class TextAndBinary extends WebSocketHandlerAdapter<WebSocketMessage<?>> {

		@Override
		public void handleMessage(WebSocketSession session, WebSocketMessage<?> message)
				throws Exception {
			if (message instanceof TextMessage) {
				handleTextMessage(session, message);
			}
			else if (message instanceof BinaryMessage) {
				handlBinaryeMessage(session, message);
			}
		}

		public void handleTextMessage(WebSocketSession session, WebSocketMessage<?> message)
				throws Exception {
		}

		public void handlBinaryeMessage(WebSocketSession session, WebSocketMessage<?> message)
				throws Exception {
		}

	}

}
