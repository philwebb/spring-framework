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

package org.springframework.web.socket.sockjs.frame;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;

/**
 * Unit tests for {@link org.springframework.web.socket.sockjs.frame.SockJsFrame}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class SockJsFrameTests {


	@Test
	public void openFrame() {
		SockJsFrame frame = SockJsFrame.openFrame();

		assertThat((Object) frame.getContent()).isEqualTo("o");
		assertThat((Object) frame.getType()).isEqualTo(SockJsFrameType.OPEN);
		assertNull(frame.getFrameData());
	}

	@Test
	public void heartbeatFrame() {
		SockJsFrame frame = SockJsFrame.heartbeatFrame();

		assertThat((Object) frame.getContent()).isEqualTo("h");
		assertThat((Object) frame.getType()).isEqualTo(SockJsFrameType.HEARTBEAT);
		assertNull(frame.getFrameData());
	}

	@Test
	public void messageArrayFrame() {
		SockJsFrame frame = SockJsFrame.messageFrame(new Jackson2SockJsMessageCodec(), "m1", "m2");

		assertThat((Object) frame.getContent()).isEqualTo("a[\"m1\",\"m2\"]");
		assertThat((Object) frame.getType()).isEqualTo(SockJsFrameType.MESSAGE);
		assertThat((Object) frame.getFrameData()).isEqualTo("[\"m1\",\"m2\"]");
	}

	@Test
	public void messageArrayFrameEmpty() {
		SockJsFrame frame = new SockJsFrame("a");

		assertThat((Object) frame.getContent()).isEqualTo("a[]");
		assertThat((Object) frame.getType()).isEqualTo(SockJsFrameType.MESSAGE);
		assertThat((Object) frame.getFrameData()).isEqualTo("[]");

		frame = new SockJsFrame("a[]");

		assertThat((Object) frame.getContent()).isEqualTo("a[]");
		assertThat((Object) frame.getType()).isEqualTo(SockJsFrameType.MESSAGE);
		assertThat((Object) frame.getFrameData()).isEqualTo("[]");
	}

	@Test
	public void closeFrame() {
		SockJsFrame frame = SockJsFrame.closeFrame(3000, "Go Away!");

		assertThat((Object) frame.getContent()).isEqualTo("c[3000,\"Go Away!\"]");
		assertThat((Object) frame.getType()).isEqualTo(SockJsFrameType.CLOSE);
		assertThat((Object) frame.getFrameData()).isEqualTo("[3000,\"Go Away!\"]");
	}

	@Test
	public void closeFrameEmpty() {
		SockJsFrame frame = new SockJsFrame("c");

		assertThat((Object) frame.getContent()).isEqualTo("c[]");
		assertThat((Object) frame.getType()).isEqualTo(SockJsFrameType.CLOSE);
		assertThat((Object) frame.getFrameData()).isEqualTo("[]");

		frame = new SockJsFrame("c[]");

		assertThat((Object) frame.getContent()).isEqualTo("c[]");
		assertThat((Object) frame.getType()).isEqualTo(SockJsFrameType.CLOSE);
		assertThat((Object) frame.getFrameData()).isEqualTo("[]");
	}

}
