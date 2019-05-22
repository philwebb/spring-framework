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

package org.springframework.messaging;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import org.springframework.util.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNotSame;
import static temp.XAssert.assertNull;
import static temp.XAssert.assertSame;

/**
 * Test fixture for {@link MessageHeaders}.
 *
 * @author Rossen Stoyanchev
 * @author Gary Russell
 * @author Juergen Hoeller
 */
public class MessageHeadersTests {

	@Test
	public void testTimestamp() {
		MessageHeaders headers = new MessageHeaders(null);
		assertNotNull(headers.getTimestamp());
	}

	@Test
	public void testTimestampOverwritten() throws Exception {
		MessageHeaders headers1 = new MessageHeaders(null);
		Thread.sleep(50L);
		MessageHeaders headers2 = new MessageHeaders(headers1);
		assertNotSame(headers1.getTimestamp(), headers2.getTimestamp());
	}

	@Test
	public void testTimestampProvided() throws Exception {
		MessageHeaders headers = new MessageHeaders(null, null, 10L);
		assertEquals(10L, (long) headers.getTimestamp());
	}

	@Test
	public void testTimestampProvidedNullValue() throws Exception {
		Map<String, Object> input = Collections.<String, Object>singletonMap(MessageHeaders.TIMESTAMP, 1L);
		MessageHeaders headers = new MessageHeaders(input, null, null);
		assertNotNull(headers.getTimestamp());
	}

	@Test
	public void testTimestampNone() throws Exception {
		MessageHeaders headers = new MessageHeaders(null, null, -1L);
		assertNull(headers.getTimestamp());
	}

	@Test
	public void testIdOverwritten() throws Exception {
		MessageHeaders headers1 = new MessageHeaders(null);
		MessageHeaders headers2 = new MessageHeaders(headers1);
		assertNotSame(headers1.getId(), headers2.getId());
	}

	@Test
	public void testId() {
		MessageHeaders headers = new MessageHeaders(null);
		assertNotNull(headers.getId());
	}

	@Test
	public void testIdProvided() {
		UUID id = new UUID(0L, 25L);
		MessageHeaders headers = new MessageHeaders(null, id, null);
		assertThat(headers.getId()).isEqualTo(id);
	}

	@Test
	public void testIdProvidedNullValue() {
		Map<String, Object> input = Collections.<String, Object>singletonMap(MessageHeaders.ID, new UUID(0L, 25L));
		MessageHeaders headers = new MessageHeaders(input, null, null);
		assertNotNull(headers.getId());
	}

	@Test
	public void testIdNone() {
		MessageHeaders headers = new MessageHeaders(null, MessageHeaders.ID_VALUE_NONE, null);
		assertNull(headers.getId());
	}

	@Test
	public void testNonTypedAccessOfHeaderValue() {
		Integer value = new Integer(123);
		Map<String, Object> map = new HashMap<>();
		map.put("test", value);
		MessageHeaders headers = new MessageHeaders(map);
		assertThat(headers.get("test")).isEqualTo(value);
	}

	@Test
	public void testTypedAccessOfHeaderValue() {
		Integer value = new Integer(123);
		Map<String, Object> map = new HashMap<>();
		map.put("test", value);
		MessageHeaders headers = new MessageHeaders(map);
		assertThat(headers.get("test", Integer.class)).isEqualTo(value);
	}

	@Test
	public void testHeaderValueAccessWithIncorrectType() {
		Integer value = new Integer(123);
		Map<String, Object> map = new HashMap<>();
		map.put("test", value);
		MessageHeaders headers = new MessageHeaders(map);
		assertThatIllegalArgumentException().isThrownBy(() ->
				headers.get("test", String.class));
	}

	@Test
	public void testNullHeaderValue() {
		Map<String, Object> map = new HashMap<>();
		MessageHeaders headers = new MessageHeaders(map);
		assertNull(headers.get("nosuchattribute"));
	}

	@Test
	public void testNullHeaderValueWithTypedAccess() {
		Map<String, Object> map = new HashMap<>();
		MessageHeaders headers = new MessageHeaders(map);
		assertNull(headers.get("nosuchattribute", String.class));
	}

	@Test
	public void testHeaderKeys() {
		Map<String, Object> map = new HashMap<>();
		map.put("key1", "val1");
		map.put("key2", new Integer(123));
		MessageHeaders headers = new MessageHeaders(map);
		Set<String> keys = headers.keySet();
		assertThat(keys.contains("key1")).isTrue();
		assertThat(keys.contains("key2")).isTrue();
	}

	@Test
	public void serializeWithAllSerializableHeaders() throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("name", "joe");
		map.put("age", 42);
		MessageHeaders input = new MessageHeaders(map);
		MessageHeaders output = (MessageHeaders) SerializationTestUtils.serializeAndDeserialize(input);
		assertThat(output.get("name")).isEqualTo("joe");
		assertThat(output.get("age")).isEqualTo(42);
		assertThat(input.get("name")).isEqualTo("joe");
		assertThat(input.get("age")).isEqualTo(42);
	}

	@Test
	public void serializeWithNonSerializableHeader() throws Exception {
		Object address = new Object();
		Map<String, Object> map = new HashMap<>();
		map.put("name", "joe");
		map.put("address", address);
		MessageHeaders input = new MessageHeaders(map);
		MessageHeaders output = (MessageHeaders) SerializationTestUtils.serializeAndDeserialize(input);
		assertThat(output.get("name")).isEqualTo("joe");
		assertNull(output.get("address"));
		assertThat(input.get("name")).isEqualTo("joe");
		assertSame(address, input.get("address"));
	}

	@Test
	public void subclassWithCustomIdAndNoTimestamp() {
		final AtomicLong id = new AtomicLong();
		@SuppressWarnings("serial")
		class MyMH extends MessageHeaders {
			public MyMH() {
				super(null, new UUID(0, id.incrementAndGet()), -1L);
			}
		}
		MessageHeaders headers = new MyMH();
		assertThat(headers.getId().toString()).isEqualTo("00000000-0000-0000-0000-000000000001");
		assertEquals(1, headers.size());
	}

}
