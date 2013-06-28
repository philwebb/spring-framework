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

package org.springframework.web.socket.adapter;

import java.nio.ByteBuffer;

import javax.websocket.Decoder;

/**
 * A Binary {@link javax.websocket.Encoder.Binary javax.websocket.Encoder} that delegates
 * to Spring's conversion service. See {@link ConvertingEncoderDecoderSupport} for
 * details.
 *
 * @param <T> The type that this Decoder can convert from.
 */
public abstract class AbstractConvertingBinaryDecoder<T> extends
		ConvertingEncoderDecoderSupport<T, ByteBuffer> implements Decoder.Binary<T> {
}
