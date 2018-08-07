/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.annotation.type;

import java.util.Collections;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimpleDeclaredAttributes}.
 *
 * @author Phillip Webb
 */
public class SimpleDeclaredAttributesTests {

	@Test
	public void getShouldReturnValue() {
		assertThat(getAttributes("test").get("value")).isEqualTo("test");
	}

	@Test
	public void getWhenMissingReturnsNull() {
		assertThat(getAttributes("test").get("missing")).isNull();
	}

	@Test
	public void getWhenArrayReturnsClonedInstance() {
		assertCloned(new boolean[] { true });
		assertCloned(new byte[] { 1 });
		assertCloned(new char[] { 'c' });
		assertCloned(new double[] { 0.1 });
		assertCloned(new float[] { 0.1f });
		assertCloned(new int[] { 1 });
		assertCloned(new long[] { 1L });
		assertCloned(new short[] { 1 });
		assertCloned(new String[] { "s" });
		assertCloned(new DeclaredAttributes[] { getAttributes("test") });
		assertCloned(new ClassReference[] { ClassReference.of("test") });
		assertCloned(new EnumValueReference[] { EnumValueReference.of("test", "ONE") });
	}

	private void assertCloned(Object value) {
		SimpleDeclaredAttributes attributes = getAttributes(value);
		Object first = attributes.get("value");
		Object second = attributes.get("value");
		assertThat(first).isNotSameAs(second);
	}

	private SimpleDeclaredAttributes getAttributes(Object value) {
		return new SimpleDeclaredAttributes(Collections.singletonMap("value", value));
	}

}
