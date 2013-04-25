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
package org.springframework.sockjs.server.transport;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.sockjs.AbstractSockJsSession;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.SockJsFrame;
import org.springframework.websocket.HandlerProvider;
import org.springframework.websocket.WebSocketHandler;


public class StreamingServerSockJsSession extends AbstractHttpServerSockJsSession {

	private int byteCount;


	public StreamingServerSockJsSession(String sessionId, SockJsConfiguration sockJsConfig,
			HandlerProvider<WebSocketHandler<?>> handler) {
		super(sessionId, sockJsConfig, handler);
	}


	protected void flushCache() throws Exception {

		cancelHeartbeat();

		do {
			String message = getMessageCache().poll();
			SockJsFrame frame = SockJsFrame.messageFrame(message);
			writeFrame(frame);

			this.byteCount += frame.getContentBytes().length + 1;
			if (logger.isTraceEnabled()) {
				logger.trace(this.byteCount + " bytes written so far, "
						+ getMessageCache().size() + " more messages not flushed");
			}
			if (this.byteCount >= getSockJsConfig().getStreamBytesLimit()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Streamed bytes limit reached. Recycling current request");
				}
				resetRequest();
				break;
			}
		} while (!getMessageCache().isEmpty());

		scheduleHeartbeat();
	}

	@Override
	protected synchronized void resetRequest() {
		super.resetRequest();
		this.byteCount = 0;
	}

	@Override
	public void writeFrame(ServerHttpResponse response, SockJsFrame frame) throws IOException {
		super.writeFrame(response, frame);
		response.flush();
	}
}

