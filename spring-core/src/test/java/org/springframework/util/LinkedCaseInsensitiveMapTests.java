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

package org.springframework.util;

import java.util.Iterator;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;

/**
 * Tests for {@link LinkedCaseInsensitiveMap}.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
public class LinkedCaseInsensitiveMapTests {

	private final LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<>();


	@Test
	public void putAndGet() {
		assertNull(map.put("key", "value1"));
		assertThat(map.put("key", "value2")).isEqualTo("value1");
		assertThat(map.put("key", "value3")).isEqualTo("value2");
		assertEquals(1, map.size());
		assertThat(map.get("key")).isEqualTo("value3");
		assertThat(map.get("KEY")).isEqualTo("value3");
		assertThat(map.get("Key")).isEqualTo("value3");
		assertThat(map.containsKey("key")).isTrue();
		assertThat(map.containsKey("KEY")).isTrue();
		assertThat(map.containsKey("Key")).isTrue();
		assertThat(map.keySet().contains("key")).isTrue();
		assertThat(map.keySet().contains("KEY")).isTrue();
		assertThat(map.keySet().contains("Key")).isTrue();
	}

	@Test
	public void putWithOverlappingKeys() {
		assertNull(map.put("key", "value1"));
		assertThat(map.put("KEY", "value2")).isEqualTo("value1");
		assertThat(map.put("Key", "value3")).isEqualTo("value2");
		assertEquals(1, map.size());
		assertThat(map.get("key")).isEqualTo("value3");
		assertThat(map.get("KEY")).isEqualTo("value3");
		assertThat(map.get("Key")).isEqualTo("value3");
		assertThat(map.containsKey("key")).isTrue();
		assertThat(map.containsKey("KEY")).isTrue();
		assertThat(map.containsKey("Key")).isTrue();
		assertThat(map.keySet().contains("key")).isTrue();
		assertThat(map.keySet().contains("KEY")).isTrue();
		assertThat(map.keySet().contains("Key")).isTrue();
	}

	@Test
	public void getOrDefault() {
		assertNull(map.put("key", "value1"));
		assertThat(map.put("KEY", "value2")).isEqualTo("value1");
		assertThat(map.put("Key", "value3")).isEqualTo("value2");
		assertThat(map.getOrDefault("key", "N")).isEqualTo("value3");
		assertThat(map.getOrDefault("KEY", "N")).isEqualTo("value3");
		assertThat(map.getOrDefault("Key", "N")).isEqualTo("value3");
		assertThat(map.getOrDefault("keeeey", "N")).isEqualTo("N");
		assertThat(map.getOrDefault(new Object(), "N")).isEqualTo("N");
	}

	@Test
	public void getOrDefaultWithNullValue() {
		assertNull(map.put("key", null));
		assertNull(map.put("KEY", null));
		assertNull(map.put("Key", null));
		assertNull(map.getOrDefault("key", "N"));
		assertNull(map.getOrDefault("KEY", "N"));
		assertNull(map.getOrDefault("Key", "N"));
		assertThat(map.getOrDefault("keeeey", "N")).isEqualTo("N");
		assertThat(map.getOrDefault(new Object(), "N")).isEqualTo("N");
	}

	@Test
	public void computeIfAbsentWithExistingValue() {
		assertNull(map.putIfAbsent("key", "value1"));
		assertThat(map.putIfAbsent("KEY", "value2")).isEqualTo("value1");
		assertThat(map.put("Key", "value3")).isEqualTo("value1");
		assertThat(map.computeIfAbsent("key", key2 -> "value1")).isEqualTo("value3");
		assertThat(map.computeIfAbsent("KEY", key1 -> "value2")).isEqualTo("value3");
		assertThat(map.computeIfAbsent("Key", key -> "value3")).isEqualTo("value3");
	}

	@Test
	public void computeIfAbsentWithComputedValue() {
		assertThat(map.computeIfAbsent("key", key2 -> "value1")).isEqualTo("value1");
		assertThat(map.computeIfAbsent("KEY", key1 -> "value2")).isEqualTo("value1");
		assertThat(map.computeIfAbsent("Key", key -> "value3")).isEqualTo("value1");
	}

	@Test
	public void mapClone() {
		assertNull(map.put("key", "value1"));
		LinkedCaseInsensitiveMap<String> copy = map.clone();

		assertThat(copy.getLocale()).isEqualTo(map.getLocale());
		assertThat(map.get("key")).isEqualTo("value1");
		assertThat(map.get("KEY")).isEqualTo("value1");
		assertThat(map.get("Key")).isEqualTo("value1");
		assertThat(copy.get("key")).isEqualTo("value1");
		assertThat(copy.get("KEY")).isEqualTo("value1");
		assertThat(copy.get("Key")).isEqualTo("value1");

		copy.put("Key", "value2");
		assertEquals(1, map.size());
		assertEquals(1, copy.size());
		assertThat(map.get("key")).isEqualTo("value1");
		assertThat(map.get("KEY")).isEqualTo("value1");
		assertThat(map.get("Key")).isEqualTo("value1");
		assertThat(copy.get("key")).isEqualTo("value2");
		assertThat(copy.get("KEY")).isEqualTo("value2");
		assertThat(copy.get("Key")).isEqualTo("value2");
	}


	@Test
	public void clearFromKeySet() {
		map.put("key", "value");
		map.keySet().clear();
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	public void removeFromKeySet() {
		map.put("key", "value");
		map.keySet().remove("key");
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	public void removeFromKeySetViaIterator() {
		map.put("key", "value");
		nextAndRemove(map.keySet().iterator());
		assertEquals(0, map.size());
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	public void clearFromValues() {
		map.put("key", "value");
		map.values().clear();
		assertEquals(0, map.size());
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	public void removeFromValues() {
		map.put("key", "value");
		map.values().remove("value");
		assertEquals(0, map.size());
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	public void removeFromValuesViaIterator() {
		map.put("key", "value");
		nextAndRemove(map.values().iterator());
		assertEquals(0, map.size());
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	public void clearFromEntrySet() {
		map.put("key", "value");
		map.entrySet().clear();
		assertEquals(0, map.size());
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	public void removeFromEntrySet() {
		map.put("key", "value");
		map.entrySet().remove(map.entrySet().iterator().next());
		assertEquals(0, map.size());
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	@Test
	public void removeFromEntrySetViaIterator() {
		map.put("key", "value");
		nextAndRemove(map.entrySet().iterator());
		assertEquals(0, map.size());
		map.computeIfAbsent("key", k -> "newvalue");
		assertThat(map.get("key")).isEqualTo("newvalue");
	}

	private void nextAndRemove(Iterator<?> iterator) {
		iterator.next();
		iterator.remove();
	}

}
