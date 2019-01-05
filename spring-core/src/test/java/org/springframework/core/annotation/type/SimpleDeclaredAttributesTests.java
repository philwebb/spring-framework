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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SimpleDeclaredAttributes}.
 *
 * @author Phillip Webb
 */
public class SimpleDeclaredAttributesTests extends AbstractDeclaredAttributesTests {

	@Before
	public void setup() {

	}

	@Test
	public void createFromMapWhenValuesIsNullThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SimpleDeclaredAttributes((Map<String, ?>) null)).withMessage(
						"Attributes must not be null");
	}

	@Test
	public void createFromAttributeArrayWhenAttributesIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SimpleDeclaredAttributes(
						(DeclaredAttribute[]) null)).withMessage(
								"Attributes must not be null");
	}

	@Test
	public void createFromPairsWhenPairsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SimpleDeclaredAttributes((Object[]) null)).withMessage(
						"Pairs must not be null");

	}

	@Test
	public void createFromPairsWhenPairsIsOddThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SimpleDeclaredAttributes("one", "two", "three")).withMessage(
						"Pairs must contain an even number of elements");
	}

	@Test
	public void createFromMapHasAttributes() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("a", "b");
		map.put("c", "d");
		SimpleDeclaredAttributes attributes = new SimpleDeclaredAttributes(map);
		assertThat(attributes.get("a")).isEqualTo("b");
		assertThat(attributes.get("c")).isEqualTo("d");
		assertThat(attributes.names()).containsExactly("a", "c");
	}

	@Test
	public void createFromAttributesArrayHasAttributes() {
		SimpleDeclaredAttributes attributes = new SimpleDeclaredAttributes(
				DeclaredAttribute.of("a", "b"), DeclaredAttribute.of("c", "d"));
		assertThat(attributes.get("a")).isEqualTo("b");
		assertThat(attributes.get("c")).isEqualTo("d");
		assertThat(attributes.names()).containsExactly("a", "c");
	}

	@Test
	public void createFromPairsHasAttributes() {
		SimpleDeclaredAttributes attributes = new SimpleDeclaredAttributes("a", "b", "c",
				"d");
		assertThat(attributes.get("a")).isEqualTo("b");
		assertThat(attributes.get("c")).isEqualTo("d");
		assertThat(attributes.names()).containsExactly("a", "c");
	}

	@Test
	public void namesReturnsNames() {
		SimpleDeclaredAttributes attributes = new SimpleDeclaredAttributes("a", "b", "c",
				"d");
		assertThat(attributes.names()).containsExactly("a", "c");
	}

	@Test
	public void getShouldReturnValue() {
		assertThat(createWithSingleValue("test").get("value")).isEqualTo("test");
	}

	@Test
	public void getWhenMissingReturnsNull() {
		assertThat(createWithSingleValue("test").get("missing")).isNull();
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
		assertCloned(new DeclaredAttributes[] { createWithSingleValue("test") });
		assertCloned(new ClassReference[] { ClassReference.of("test") });
		assertCloned(new EnumValueReference[] { EnumValueReference.of("test", "ONE") });
	}

	private void assertCloned(Object value) {
		SimpleDeclaredAttributes attributes = createWithSingleValue(value);
		Object first = attributes.get("value");
		Object second = attributes.get("value");
		assertThat(first).isNotSameAs(second);
	}

	@Override
	protected DeclaredAttributes createTestAttributes() {
		return createWithSingleValue("test");
	}

	private SimpleDeclaredAttributes createWithSingleValue(Object value) {
		return new SimpleDeclaredAttributes(Collections.singletonMap("value", value));
	}

}
