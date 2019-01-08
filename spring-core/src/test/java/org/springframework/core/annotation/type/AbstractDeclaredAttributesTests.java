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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractDeclaredAttributes}.
 *
 * @author Phillip Webb
 */
public abstract class AbstractDeclaredAttributesTests {

	@Test
	public void interatorIteratesDeclaredAttributes() {
		DeclaredAttributes attributes = createTestAttributes();
		assertThat(attributes).hasSize(1);
		assertThat(attributes.iterator().next().getValue()).isEqualTo("test");
	}

	@Test
	public void toStringReturnsString() {
		assertThat(createTestAttributes().toString()).isEqualTo("(value=\"test\")");
	}

	protected abstract DeclaredAttributes createTestAttributes();

}
