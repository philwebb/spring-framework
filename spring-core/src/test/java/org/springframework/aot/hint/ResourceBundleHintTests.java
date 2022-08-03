/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.hint;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ResourceBundleHint;
import org.springframework.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourceBundleHint}.
 *
 * @author Phillip Webb
 */
class ResourceBundleHintTests {

	@Test
	void getBaseNameReturnsBaseName() {
		ResourceBundleHint hint = new ResourceBundleHint("test", null);
		assertThat(hint.getBaseName()).isEqualTo("test");
	}

	@Test
	void getReachableTypeReturnsReachableType() {
		TypeReference reachableType = TypeReference.of(Integer.class);
		ResourceBundleHint hint = new ResourceBundleHint("test", reachableType);
		assertThat(hint.getReachableType()).isEqualTo(reachableType);
	}

	@Test
	void equalsAndHashCode() {
		ResourceBundleHint h1 = new ResourceBundleHint("test", null);
		ResourceBundleHint h2 = new ResourceBundleHint("test", null);
		ResourceBundleHint h3 = new ResourceBundleHint("other", TypeReference.of(Integer.class));
		ResourceBundleHint h4 = new ResourceBundleHint("other", TypeReference.of(Integer.class.getName()));
		assertThat(h1.hashCode()).isEqualTo(h2.hashCode());
		assertThat(h1).isEqualTo(h1).isEqualTo(h2).isNotEqualTo(h3);
		assertThat(h3).isEqualTo(h4).isNotEqualTo(h1);
	}

}
