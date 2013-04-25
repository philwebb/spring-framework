/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.websocket;

import org.springframework.core.GenericTypeResolver;

/**
 * Utility methods for use when dealing with {@link WebSocketMessage}s.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public abstract class WebSocketMessages {

	/**
	 * Deliver messages to the specified handler, taking into account they type
	 * of message that the handler supports.
	 * @param handler the handler to receive messages
	 * @param session the WebSocketSession
	 * @param binaryPayloads the message payloads
	 * @throws Exception Any error thrown from {@link WebSocketHandler#handleMessage}
	 */
	public static void deliver(WebSocketHandler<?> handler, WebSocketSession session,
			byte[]... binaryPayloads) throws Exception {
		WebSocketMessage<?>[] messages = new WebSocketMessage<?>[binaryPayloads.length];
		for (int i = 0; i < binaryPayloads.length; i++) {
			messages[i] = new BinaryMessage(binaryPayloads[i]);
		}
		deliver(handler, session, messages);
	}

	/**
	 * Deliver messages to the specified handler, taking into account they type
	 * of message that the handler supports.
	 * @param handler the handler to receive messages
	 * @param session the WebSocketSession
	 * @param textPayloads the message payloads
	 * @throws Exception Any error thrown from {@link WebSocketHandler#handleMessage}
	 */
	public static void deliver(WebSocketHandler<?> handler, WebSocketSession session,
			String... textPayloads) throws Exception {
		WebSocketMessage<?>[] messages = new WebSocketMessage<?>[textPayloads.length];
		for (int i = 0; i < textPayloads.length; i++) {
			messages[i] = new TextMessage(textPayloads[i]);
		}
		deliver(handler, session, messages);
	}

	/**
	 * Deliver messages to the specified handler, taking into account they type
	 * of message that the handler supports.
	 * @param handler the handler to receive messages
	 * @param session the WebSocketSession
	 * @param messages the messages
	 * @throws Exception Any error thrown from {@link WebSocketHandler#handleMessage}
	 */
	@SuppressWarnings("unchecked")
	public static void deliver(WebSocketHandler<?> handler, WebSocketSession session,
			WebSocketMessage<?>... messages) throws Exception {
		Class<?> messageType = GenericTypeResolver.resolveTypeArgument(
				handler.getClass(), WebSocketHandler.class);
		for (WebSocketMessage<?> message : messages) {
			if (messageType.isInstance(message)) {
				((WebSocketHandler) handler).handleMessage(session, message);
			}
		}
	}

}
