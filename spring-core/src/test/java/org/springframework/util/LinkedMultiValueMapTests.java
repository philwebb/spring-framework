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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNull;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
public class LinkedMultiValueMapTests {

	private final LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();


	@Test
	public void add() {
		map.add("key", "value1");
		map.add("key", "value2");
		assertEquals(1, map.size());
		List<String> expected = new ArrayList<>(2);
		expected.add("value1");
		expected.add("value2");
		assertThat((Object) map.get("key")).isEqualTo(expected);
	}

	@Test
	public void set() {
		map.set("key", "value1");
		map.set("key", "value2");
		assertEquals(1, map.size());
		assertThat((Object) map.get("key")).isEqualTo(Collections.singletonList("value2"));
	}

	@Test
	public void addAll() {
		map.add("key", "value1");
		map.addAll("key", Arrays.asList("value2", "value3"));
		assertEquals(1, map.size());
		List<String> expected = new ArrayList<>(2);
		expected.add("value1");
		expected.add("value2");
		expected.add("value3");
		assertThat((Object) map.get("key")).isEqualTo(expected);
	}

	@Test
	public void addAllWithEmptyList() {
		map.addAll("key", Collections.emptyList());
		assertEquals(1, map.size());
		assertThat((Object) map.get("key")).isEqualTo(Collections.emptyList());
		assertNull(map.getFirst("key"));
	}

	@Test
	public void getFirst() {
		List<String> values = new ArrayList<>(2);
		values.add("value1");
		values.add("value2");
		map.put("key", values);
		assertThat((Object) map.getFirst("key")).isEqualTo("value1");
		assertNull(map.getFirst("other"));
	}

	@Test
	public void getFirstWithEmptyList() {
		map.put("key", Collections.emptyList());
		assertNull(map.getFirst("key"));
		assertNull(map.getFirst("other"));
	}

	@Test
	public void toSingleValueMap() {
		List<String> values = new ArrayList<>(2);
		values.add("value1");
		values.add("value2");
		map.put("key", values);
		Map<String, String> svm = map.toSingleValueMap();
		assertEquals(1, svm.size());
		assertThat((Object) svm.get("key")).isEqualTo("value1");
	}

	@Test
	public void toSingleValueMapWithEmptyList() {
		map.put("key", Collections.emptyList());
		Map<String, String> svm = map.toSingleValueMap();
		assertEquals(0, svm.size());
		assertNull(svm.get("key"));
	}

	@Test
	public void equals() {
		map.set("key1", "value1");
		assertThat((Object) map).isEqualTo(map);
		MultiValueMap<String, String> o1 = new LinkedMultiValueMap<>();
		o1.set("key1", "value1");
		assertThat((Object) o1).isEqualTo(map);
		assertThat((Object) map).isEqualTo(o1);
		Map<String, List<String>> o2 = new HashMap<>();
		o2.put("key1", Collections.singletonList("value1"));
		assertThat((Object) o2).isEqualTo(map);
		assertThat((Object) map).isEqualTo(o2);
	}

}
