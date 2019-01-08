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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SimpleAttributeTypes}.
 *
 * @author Phillip Webb
 */
public class SimpleAttributeTypesTests {

	private SimpleAttributeType type;

	private SimpleAttributeTypes types;

	@Before
	public void setup() {
		this.type = new SimpleAttributeType("test", "className", DeclaredAnnotations.NONE,
				null);
		this.types = new SimpleAttributeTypes(Arrays.asList(this.type));
	}

	@Test
	public void createFromArrayWhenTypesIsNullThrowException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new SimpleAttributeTypes((AttributeType[]) null)).withMessage(
						"Types must not be null");
	}

	@Test
	public void createFromCollectionWhenTypesIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SimpleAttributeTypes(
				(Collection<AttributeType>) null)).withMessage("Types must not be null");
	}

	@Test
	public void attributeNamesReturnsNames() {
		assertThat(this.types.attributeNames()).containsOnly("test");
	}

	@Test
	public void getReturnsAttributeType() {
		assertThat(this.types.get("test")).isSameAs(this.type);
	}

	@Test
	public void getWhenHasNoMatchingAttributeReturnsNull() {
		assertThat(this.types.get("missing")).isNull();
	}

	@Test
	public void iteratorIteratesAttributeTypes() {
		assertThat(this.types.iterator()).containsOnly(this.type);
	}

	@Test
	public void toStringReturnsString() {
		assertThat(this.types.toString()).isEqualTo("className test();");
	}

}
