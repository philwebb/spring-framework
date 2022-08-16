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

package org.springframework.aot.hint2;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourcePatternHint}.
 *
 * @author Sebastien Deleuze
 * @author Phillip Webb
 */
public class ResourcePatternHintTests {

	@Test
	void getPatternReturnsPattern() {
		ResourcePatternHint hint = new ResourcePatternHint("test", null);
		assertThat(hint.getPattern()).isEqualTo("test");
	}

	@Test
	void getReachableTypeReturnsReachableType() {
		TypeReference reachableType = TypeReference.of(String.class);
		ResourcePatternHint hint = new ResourcePatternHint("test", reachableType);
		assertThat(hint.getReachableType()).isEqualTo(reachableType);
	}

	@Test
	void toRegexWhenFileAtRoot() {
		ResourcePatternHint hint = new ResourcePatternHint("file.properties", null);
		assertThat(hint.toRegex().asMatchPredicate()).accepts("file.properties").rejects("com/example/file.properties",
				"file.prop", "another-file.properties");
	}

	@Test
	void toRegexWhenFileInDirectory() {
		ResourcePatternHint hint = new ResourcePatternHint("com/example/file.properties", null);
		assertThat(hint.toRegex().asMatchPredicate()).accepts("com/example/file.properties").rejects("file.properties",
				"com/file.properties", "com/example/another-file.properties");
	}

	@Test
	void toRegexWhenWildcardExtension() {
		ResourcePatternHint hint = new ResourcePatternHint("*.properties", null);
		assertThat(hint.toRegex().asMatchPredicate()).accepts("file.properties", "com/example/file.properties")
				.rejects("file.prop", "com/example/file.prop");
	}

	@Test
	void toRegexWhenExtensionInDirectoryAtAnyDepth() {
		ResourcePatternHint hint = new ResourcePatternHint("com/example/*.properties", null);
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("com/example/file.properties", "com/example/another/file.properties")
				.rejects("file.properties", "com/file.properties");
	}

	@Test
	void toRegexWhenAnyFileInDirectoryAtAnyDepth() {
		ResourcePatternHint hint = new ResourcePatternHint("com/example/*", null);
		assertThat(hint.toRegex().asMatchPredicate())
				.accepts("com/example/file.properties", "com/example/another/file.properties", "com/example/another")
				.rejects("file.properties", "com/file.properties");
	}

	@Test
	void equalsAndHashCode() {
		ResourcePatternHint h1 = new ResourcePatternHint("*.txt", null);
		ResourcePatternHint h2 = new ResourcePatternHint("*.txt", null);
		ResourcePatternHint h3 = new ResourcePatternHint("*.txt", TypeReference.of(Integer.class));
		assertThat(h1.hashCode()).isEqualTo(h2.hashCode());
		assertThat(h1).isEqualTo(h1).isEqualTo(h2).isNotEqualTo(h3);
	}

}
