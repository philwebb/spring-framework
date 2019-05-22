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

package org.springframework.core.io.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Arjen Poutsma
 */
public class DataBufferTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void byteCountsAndPositions() {
		DataBuffer buffer = createDataBuffer(2);

		assertThat((long) buffer.readPosition()).isEqualTo((long) 0);
		assertThat((long) buffer.writePosition()).isEqualTo((long) 0);
		assertThat((long) buffer.readableByteCount()).isEqualTo((long) 0);
		assertThat((long) buffer.writableByteCount()).isEqualTo((long) 2);
		assertThat((long) buffer.capacity()).isEqualTo((long) 2);

		buffer.write((byte) 'a');
		assertThat((long) buffer.readPosition()).isEqualTo((long) 0);
		assertThat((long) buffer.writePosition()).isEqualTo((long) 1);
		assertThat((long) buffer.readableByteCount()).isEqualTo((long) 1);
		assertThat((long) buffer.writableByteCount()).isEqualTo((long) 1);
		assertThat((long) buffer.capacity()).isEqualTo((long) 2);

		buffer.write((byte) 'b');
		assertThat((long) buffer.readPosition()).isEqualTo((long) 0);
		assertThat((long) buffer.writePosition()).isEqualTo((long) 2);
		assertThat((long) buffer.readableByteCount()).isEqualTo((long) 2);
		assertThat((long) buffer.writableByteCount()).isEqualTo((long) 0);
		assertThat((long) buffer.capacity()).isEqualTo((long) 2);

		buffer.read();
		assertThat((long) buffer.readPosition()).isEqualTo((long) 1);
		assertThat((long) buffer.writePosition()).isEqualTo((long) 2);
		assertThat((long) buffer.readableByteCount()).isEqualTo((long) 1);
		assertThat((long) buffer.writableByteCount()).isEqualTo((long) 0);
		assertThat((long) buffer.capacity()).isEqualTo((long) 2);

		buffer.read();
		assertThat((long) buffer.readPosition()).isEqualTo((long) 2);
		assertThat((long) buffer.writePosition()).isEqualTo((long) 2);
		assertThat((long) buffer.readableByteCount()).isEqualTo((long) 0);
		assertThat((long) buffer.writableByteCount()).isEqualTo((long) 0);
		assertThat((long) buffer.capacity()).isEqualTo((long) 2);

		release(buffer);
	}

	@Test
	public void readPositionSmallerThanZero() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() ->
					buffer.readPosition(-1));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	public void readPositionGreaterThanWritePosition() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() ->
					buffer.readPosition(1));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	public void writePositionSmallerThanReadPosition() {
		DataBuffer buffer = createDataBuffer(2);
		try {
			buffer.write((byte) 'a');
			buffer.read();
			assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() ->
					buffer.writePosition(0));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	public void writePositionGreaterThanCapacity() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() ->
					buffer.writePosition(2));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	public void writeAndRead() {
		DataBuffer buffer = createDataBuffer(5);
		buffer.write(new byte[]{'a', 'b', 'c'});

		int ch = buffer.read();
		assertThat((long) ch).isEqualTo((long) 'a');

		buffer.write((byte) 'd');
		buffer.write((byte) 'e');

		byte[] result = new byte[4];
		buffer.read(result);

		assertThat(result).isEqualTo(new byte[]{'b', 'c', 'd', 'e'});

		release(buffer);
	}

	@Test
	public void writeNullString() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			assertThatIllegalArgumentException().isThrownBy(() ->
					buffer.write(null, StandardCharsets.UTF_8));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	public void writeNullCharset() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			assertThatIllegalArgumentException().isThrownBy(() ->
					buffer.write("test", null));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	public void writeEmptyString() {
		DataBuffer buffer = createDataBuffer(1);
		buffer.write("", StandardCharsets.UTF_8);

		assertThat((long) buffer.readableByteCount()).isEqualTo((long) 0);

		release(buffer);
	}

	@Test
	public void writeUtf8String() {
		DataBuffer buffer = createDataBuffer(6);
		buffer.write("Spring", StandardCharsets.UTF_8);

		byte[] result = new byte[6];
		buffer.read(result);

		assertThat(result).isEqualTo("Spring".getBytes(StandardCharsets.UTF_8));
		release(buffer);
	}

	@Test
	public void writeUtf8StringOutGrowsCapacity() {
		DataBuffer buffer = createDataBuffer(5);
		buffer.write("Spring €", StandardCharsets.UTF_8);

		byte[] result = new byte[10];
		buffer.read(result);

		assertThat(result).isEqualTo("Spring €".getBytes(StandardCharsets.UTF_8));
		release(buffer);
	}

	@Test
	public void writeIsoString() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write("\u00A3", StandardCharsets.ISO_8859_1);

		byte[] result = new byte[1];
		buffer.read(result);

		assertThat(result).isEqualTo("\u00A3".getBytes(StandardCharsets.ISO_8859_1));
		release(buffer);
	}

	@Test
	public void writeMultipleUtf8String() {

		DataBuffer buffer = createDataBuffer(1);
		buffer.write("abc", StandardCharsets.UTF_8);
		assertThat((long) buffer.readableByteCount()).isEqualTo((long) 3);

		buffer.write("def", StandardCharsets.UTF_8);
		assertThat((long) buffer.readableByteCount()).isEqualTo((long) 6);

		buffer.write("ghi", StandardCharsets.UTF_8);
		assertThat((long) buffer.readableByteCount()).isEqualTo((long) 9);

		byte[] result = new byte[9];
		buffer.read(result);

		assertThat(result).isEqualTo("abcdefghi".getBytes());

		release(buffer);
	}

	@Test
	public void inputStream() throws IOException {
		DataBuffer buffer = createDataBuffer(4);
		buffer.write(new byte[]{'a', 'b', 'c', 'd', 'e'});
		buffer.readPosition(1);

		InputStream inputStream = buffer.asInputStream();

		assertThat((long) inputStream.available()).isEqualTo((long) 4);

		int result = inputStream.read();
		assertThat((long) result).isEqualTo((long) 'b');
		assertThat((long) inputStream.available()).isEqualTo((long) 3);

		byte[] bytes = new byte[2];
		int len = inputStream.read(bytes);
		assertThat((long) len).isEqualTo((long) 2);
		assertThat(bytes).isEqualTo(new byte[]{'c', 'd'});
		assertThat((long) inputStream.available()).isEqualTo((long) 1);

		Arrays.fill(bytes, (byte) 0);
		len = inputStream.read(bytes);
		assertThat((long) len).isEqualTo((long) 1);
		assertThat(bytes).isEqualTo(new byte[]{'e', (byte) 0});
		assertThat((long) inputStream.available()).isEqualTo((long) 0);

		assertThat((long) inputStream.read()).isEqualTo((long) -1);
		assertThat((long) inputStream.read(bytes)).isEqualTo((long) -1);

		release(buffer);
	}

	@Test
	public void inputStreamReleaseOnClose() throws IOException {
		DataBuffer buffer = createDataBuffer(3);
		byte[] bytes = {'a', 'b', 'c'};
		buffer.write(bytes);

		InputStream inputStream = buffer.asInputStream(true);

		try {
			byte[] result = new byte[3];
			int len = inputStream.read(result);
			assertThat((long) len).isEqualTo((long) 3);
			assertThat(result).isEqualTo(bytes);
		}
		finally {
			inputStream.close();
		}

		// AbstractDataBufferAllocatingTestCase.LeakDetector will verify the buffer's release

	}

	@Test
	public void outputStream() throws IOException {
		DataBuffer buffer = createDataBuffer(4);
		buffer.write((byte) 'a');

		OutputStream outputStream = buffer.asOutputStream();
		outputStream.write('b');
		outputStream.write(new byte[]{'c', 'd'});

		buffer.write((byte) 'e');

		byte[] bytes = new byte[5];
		buffer.read(bytes);
		assertThat(bytes).isEqualTo(new byte[]{'a', 'b', 'c', 'd', 'e'});

		release(buffer);
	}

	@Test
	public void expand() {
		DataBuffer buffer = createDataBuffer(1);
		buffer.write((byte) 'a');
		assertThat((long) buffer.capacity()).isEqualTo((long) 1);
		buffer.write((byte) 'b');

		assertThat(buffer.capacity() > 1).isTrue();

		release(buffer);
	}

	@Test
	public void increaseCapacity() {
		DataBuffer buffer = createDataBuffer(1);
		assertThat((long) buffer.capacity()).isEqualTo((long) 1);

		buffer.capacity(2);
		assertThat((long) buffer.capacity()).isEqualTo((long) 2);

		release(buffer);
	}

	@Test
	public void decreaseCapacityLowReadPosition() {
		DataBuffer buffer = createDataBuffer(2);
		buffer.writePosition(2);
		buffer.capacity(1);
		assertThat((long) buffer.capacity()).isEqualTo((long) 1);

		release(buffer);
	}

	@Test
	public void decreaseCapacityHighReadPosition() {
		DataBuffer buffer = createDataBuffer(2);
		buffer.writePosition(2);
		buffer.readPosition(2);
		buffer.capacity(1);
		assertThat((long) buffer.capacity()).isEqualTo((long) 1);

		release(buffer);
	}

	@Test
	public void capacityLessThanZero() {
		DataBuffer buffer = createDataBuffer(1);
		try {
			assertThatIllegalArgumentException().isThrownBy(() ->
					buffer.capacity(-1));
		}
		finally {
			release(buffer);
		}
	}

	@Test
	public void writeByteBuffer() {
		DataBuffer buffer1 = createDataBuffer(1);
		buffer1.write((byte) 'a');
		ByteBuffer buffer2 = createByteBuffer(2);
		buffer2.put((byte) 'b');
		buffer2.flip();
		ByteBuffer buffer3 = createByteBuffer(3);
		buffer3.put((byte) 'c');
		buffer3.flip();

		buffer1.write(buffer2, buffer3);
		buffer1.write((byte) 'd'); // make sure the write index is correctly set

		assertThat((long) buffer1.readableByteCount()).isEqualTo((long) 4);
		byte[] result = new byte[4];
		buffer1.read(result);

		assertThat(result).isEqualTo(new byte[]{'a', 'b', 'c', 'd'});

		release(buffer1);
	}

	private ByteBuffer createByteBuffer(int capacity) {
		return ByteBuffer.allocate(capacity);
	}

	@Test
	public void writeDataBuffer() {
		DataBuffer buffer1 = createDataBuffer(1);
		buffer1.write((byte) 'a');
		DataBuffer buffer2 = createDataBuffer(2);
		buffer2.write((byte) 'b');
		DataBuffer buffer3 = createDataBuffer(3);
		buffer3.write((byte) 'c');

		buffer1.write(buffer2, buffer3);
		buffer1.write((byte) 'd'); // make sure the write index is correctly set

		assertThat((long) buffer1.readableByteCount()).isEqualTo((long) 4);
		byte[] result = new byte[4];
		buffer1.read(result);

		assertThat(result).isEqualTo(new byte[]{'a', 'b', 'c', 'd'});

		release(buffer1, buffer2, buffer3);
	}

	@Test
	public void asByteBuffer() {
		DataBuffer buffer = createDataBuffer(4);
		buffer.write(new byte[]{'a', 'b', 'c'});
		buffer.read(); // skip a

		ByteBuffer result = buffer.asByteBuffer();
		assertThat((long) result.capacity()).isEqualTo((long) 2);

		buffer.write((byte) 'd');
		assertThat((long) result.remaining()).isEqualTo((long) 2);

		byte[] resultBytes = new byte[2];
		result.get(resultBytes);
		assertThat(resultBytes).isEqualTo(new byte[]{'b', 'c'});

		release(buffer);
	}

	@Test
	public void asByteBufferIndexLength() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b'});

		ByteBuffer result = buffer.asByteBuffer(1, 2);
		assertThat((long) result.capacity()).isEqualTo((long) 2);

		buffer.write((byte) 'c');
		assertThat((long) result.remaining()).isEqualTo((long) 2);

		byte[] resultBytes = new byte[2];
		result.get(resultBytes);
		assertThat(resultBytes).isEqualTo(new byte[]{'b', 'c'});

		release(buffer);
	}

	@Test
	public void byteBufferContainsDataBufferChanges() {
		DataBuffer dataBuffer = createDataBuffer(1);
		ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, 1);

		dataBuffer.write((byte) 'a');

		assertThat((long) byteBuffer.limit()).isEqualTo((long) 1);
		byte b = byteBuffer.get();
		assertThat((long) b).isEqualTo((long) 'a');

		release(dataBuffer);
	}

	@Test
	public void dataBufferContainsByteBufferChanges() {
		DataBuffer dataBuffer = createDataBuffer(1);
		ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, 1);

		byteBuffer.put((byte) 'a');
		dataBuffer.writePosition(1);

		byte b = dataBuffer.read();
		assertThat((long) b).isEqualTo((long) 'a');

		release(dataBuffer);
	}

	@Test
	public void emptyAsByteBuffer() {
		DataBuffer buffer = createDataBuffer(1);

		ByteBuffer result = buffer.asByteBuffer();
		assertThat((long) result.capacity()).isEqualTo((long) 0);

		release(buffer);
	}

	@Test
	public void indexOf() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b', 'c'});

		int result = buffer.indexOf(b -> b == 'c', 0);
		assertThat((long) result).isEqualTo((long) 2);

		result = buffer.indexOf(b -> b == 'c', Integer.MIN_VALUE);
		assertThat((long) result).isEqualTo((long) 2);

		result = buffer.indexOf(b -> b == 'c', Integer.MAX_VALUE);
		assertThat((long) result).isEqualTo((long) -1);

		result = buffer.indexOf(b -> b == 'z', 0);
		assertThat((long) result).isEqualTo((long) -1);

		release(buffer);
	}

	@Test
	public void lastIndexOf() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b', 'c'});

		int result = buffer.lastIndexOf(b -> b == 'b', 2);
		assertThat((long) result).isEqualTo((long) 1);

		result = buffer.lastIndexOf(b -> b == 'c', 2);
		assertThat((long) result).isEqualTo((long) 2);

		result = buffer.lastIndexOf(b -> b == 'b', Integer.MAX_VALUE);
		assertThat((long) result).isEqualTo((long) 1);

		result = buffer.lastIndexOf(b -> b == 'c', Integer.MAX_VALUE);
		assertThat((long) result).isEqualTo((long) 2);

		result = buffer.lastIndexOf(b -> b == 'b', Integer.MIN_VALUE);
		assertThat((long) result).isEqualTo((long) -1);

		result = buffer.lastIndexOf(b -> b == 'c', Integer.MIN_VALUE);
		assertThat((long) result).isEqualTo((long) -1);

		result = buffer.lastIndexOf(b -> b == 'z', 0);
		assertThat((long) result).isEqualTo((long) -1);

		release(buffer);
	}

	@Test
	public void slice() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b'});

		DataBuffer slice = buffer.slice(1, 2);
		assertThat((long) slice.readableByteCount()).isEqualTo((long) 2);
		assertThatExceptionOfType(Exception.class).isThrownBy(() ->
				slice.write((byte) 0));
		buffer.write((byte) 'c');

		assertThat((long) buffer.readableByteCount()).isEqualTo((long) 3);
		byte[] result = new byte[3];
		buffer.read(result);

		assertThat(result).isEqualTo(new byte[]{'a', 'b', 'c'});

		assertThat((long) slice.readableByteCount()).isEqualTo((long) 2);
		result = new byte[2];
		slice.read(result);

		assertThat(result).isEqualTo(new byte[]{'b', 'c'});


		release(buffer);
	}

	@Test
	public void retainedSlice() {
		DataBuffer buffer = createDataBuffer(3);
		buffer.write(new byte[]{'a', 'b'});

		DataBuffer slice = buffer.retainedSlice(1, 2);
		assertThat((long) slice.readableByteCount()).isEqualTo((long) 2);
		assertThatExceptionOfType(Exception.class).isThrownBy(() ->
				slice.write((byte) 0));
		buffer.write((byte) 'c');

		assertThat((long) buffer.readableByteCount()).isEqualTo((long) 3);
		byte[] result = new byte[3];
		buffer.read(result);

		assertThat(result).isEqualTo(new byte[]{'a', 'b', 'c'});

		assertThat((long) slice.readableByteCount()).isEqualTo((long) 2);
		result = new byte[2];
		slice.read(result);

		assertThat(result).isEqualTo(new byte[]{'b', 'c'});


		release(buffer, slice);
	}

	@Test
	public void spr16351() {
		DataBuffer buffer = createDataBuffer(6);
		byte[] bytes = {'a', 'b', 'c', 'd', 'e', 'f'};
		buffer.write(bytes);
		DataBuffer slice = buffer.slice(3, 3);
		buffer.writePosition(3);
		buffer.write(slice);

		assertThat((long) buffer.readableByteCount()).isEqualTo((long) 6);
		byte[] result = new byte[6];
		buffer.read(result);

		assertThat(result).isEqualTo(bytes);

		release(buffer);
	}

	@Test
	public void join() {
		DataBuffer composite = this.bufferFactory.join(Arrays.asList(stringBuffer("a"),
				stringBuffer("b"), stringBuffer("c")));
		assertThat((long) composite.readableByteCount()).isEqualTo((long) 3);
		byte[] bytes = new byte[3];
		composite.read(bytes);

		assertThat(bytes).isEqualTo(new byte[] {'a','b','c'});

		release(composite);
	}

	@Test
	public void getByte() {
		DataBuffer buffer = stringBuffer("abc");

		assertThat((long) buffer.getByte(0)).isEqualTo((long) 'a');
		assertThat((long) buffer.getByte(1)).isEqualTo((long) 'b');
		assertThat((long) buffer.getByte(2)).isEqualTo((long) 'c');
		assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() ->
				buffer.getByte(-1));

		assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() ->
			buffer.getByte(3));

		release(buffer);
	}

}
