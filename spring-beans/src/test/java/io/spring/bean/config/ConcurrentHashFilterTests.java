/*
 * Copyright 2002-2021 the original author or authors.
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

package io.spring.bean.config;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import io.spring.bean.config.ConcurrentHashFilter.HashCodeConsumer;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ConcurrentHashFilter}.
 *
 * @author Phillip Webb
 */
class ConcurrentHashFilterTests {

	@Test
	void testName() {
		Set<String> strings = new LinkedHashSet<>();
		strings.add("phillip");
		strings.add("iwebb");
		ConcurrentHashFilter<String, Character> filter = new ConcurrentHashFilter<>(
				this::getChars, strings::iterator);
		strings.forEach(filter::add);
		filter.doWithCandidates('w', System.out::println);
		filter.doWithCandidates('b', System.out::println);
		filter.doWithCandidates('i', System.out::println);
	}

	@Test
	void scannyman() throws Exception {

	}

	private void getChars(String string, HashCodeConsumer<Character> consumer) {
		for (char ch : string.toCharArray()) {
			consumer.accept(ch);
		}
	}

}
