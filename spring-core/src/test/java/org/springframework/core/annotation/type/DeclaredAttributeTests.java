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
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DeclaredAttribute}.
 *
 * @author Phillip Webb
 */
public class DeclaredAttributeTests {

	@Test
	public void fromMapEntryWhenNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> DeclaredAttribute.from(null)).withMessage("Entry must not be null");
	}

	@Test
	public void ofMapReturnsSimpleDeclaredAttribute() {
		Map<String, Object> map = Collections.singletonMap("key", "value");
		DeclaredAttribute declaredAttribute = DeclaredAttribute.from(
				map.entrySet().iterator().next());
		assertThat(declaredAttribute).isInstanceOf(SimpleDeclaredAttribute.class);
		assertThat(declaredAttribute.getName()).isEqualTo("key");
		assertThat(declaredAttribute.getValue()).isEqualTo("value");
	}

	@Test
	public void ofNameAndValueReturnsSimpleDeclaredAttribute() {
		DeclaredAttribute declaredAttribute = DeclaredAttribute.of("name", "value");
		assertThat(declaredAttribute).isInstanceOf(SimpleDeclaredAttribute.class);
		assertThat(declaredAttribute.getName()).isEqualTo("name");
		assertThat(declaredAttribute.getValue()).isEqualTo("value");
	}

}
