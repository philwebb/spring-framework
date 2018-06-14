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

package org.springframework.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link StreamUtils}.
 *
 * @author Phillip Webb
 */
public class StreamUtilsTests {

	private byte[] bytes = new byte[StreamUtils.BUFFER_SIZE + 10];

	private String string = "";

	@Before
	public void setup() {
		new Random().nextBytes(this.bytes);
		while (this.string.length() < StreamUtils.BUFFER_SIZE + 10) {
			this.string += UUID.randomUUID().toString();
		}
	}

	@Test
	public void copyToByteArray() throws Exception {
		InputStream inputStream = spy(new ByteArrayInputStream(this.bytes));
		byte[] actual = StreamUtils.copyToByteArray(inputStream);
		assertThat(actual, equalTo(this.bytes));
		verify(inputStream, never()).close();
	}

	@Test
	public void copyToString() throws Exception {
		Charset charset = Charset.defaultCharset();
		InputStream inputStream = spy(new ByteArrayInputStream(this.string.getBytes(charset)));
		String actual = StreamUtils.copyToString(inputStream, charset);
		assertThat(actual, equalTo(this.string));
		verify(inputStream, never()).close();
	}

	@Test
	public void copyBytes() throws Exception {
		ByteArrayOutputStream out = spy(new ByteArrayOutputStream());
		StreamUtils.copy(this.bytes, out);
		assertThat(out.toByteArray(), equalTo(this.bytes));
		verify(out, never()).close();
	}

	@Test
	public void copyString() throws Exception {
		Charset charset = Charset.defaultCharset();
		ByteArrayOutputStream out = spy(new ByteArrayOutputStream());
		StreamUtils.copy(this.string, charset, out);
		assertThat(out.toByteArray(), equalTo(this.string.getBytes(charset)));
		verify(out, never()).close();
	}

	@Test
	public void copyStream() throws Exception {
		ByteArrayOutputStream out = spy(new ByteArrayOutputStream());
		StreamUtils.copy(new ByteArrayInputStream(this.bytes), out);
		assertThat(out.toByteArray(), equalTo(this.bytes));
		verify(out, never()).close();
	}

	@Test
	public void copyRange() throws Exception {
		ByteArrayOutputStream out = spy(new ByteArrayOutputStream());
		StreamUtils.copyRange(new ByteArrayInputStream(this.bytes), out, 0, 100);
		byte[] range = Arrays.copyOfRange(this.bytes, 0, 101);
		assertThat(out.toByteArray(), equalTo(range));
		verify(out, never()).close();
	}

	@Test
	public void nonClosingInputStream() throws Exception {
		InputStream source = mock(InputStream.class);
		InputStream nonClosing = StreamUtils.nonClosing(source);
		nonClosing.read();
		nonClosing.read(this.bytes);
		nonClosing.read(this.bytes, 1, 2);
		nonClosing.close();
		InOrder ordered = inOrder(source);
		ordered.verify(source).read();
		ordered.verify(source).read(this.bytes, 0, this.bytes.length);
		ordered.verify(source).read(this.bytes, 1, 2);
		ordered.verify(source, never()).close();
	}

	@Test
	public void nonClosingOutputStream() throws Exception {
		OutputStream source = mock(OutputStream.class);
		OutputStream nonClosing = StreamUtils.nonClosing(source);
		nonClosing.write(1);
		nonClosing.write(this.bytes);
		nonClosing.write(this.bytes, 1, 2);
		nonClosing.close();
		InOrder ordered = inOrder(source);
		ordered.verify(source).write(1);
		ordered.verify(source).write(this.bytes, 0, this.bytes.length);
		ordered.verify(source).write(this.bytes, 1, 2);
		ordered.verify(source, never()).close();
	}
}
