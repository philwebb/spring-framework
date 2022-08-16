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

package org.springframework.aot.hint2;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint2.ResourceHintsTests.Nested.Inner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResourceHints}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ResourceHintsTests {

	private final ResourceHints hints = new ResourceHints();

	@Test
	void registerIncludeForPatternRegistersHint() {
		this.hints.registerInclude().forPattern("com/example/*.properties");
		assertThat(this.hints.includeResourcePatterns()).singleElement()
				.satisfies(patternOf("com/example/*.properties"));
	}

	@Test
	void registerIncludeForPatternWhenMultipleCallsRegistersHint() {
		this.hints.registerInclude().forPattern("com/example/test.properties");
		this.hints.registerInclude().forPattern("com/example/another.properties");
		assertThat(this.hints.includeResourcePatterns()).anySatisfy(patternOf("com/example/test.properties"))
				.anySatisfy(patternOf("com/example/another.properties")).hasSize(2);
	}

	@Test
	void registerIncludeForPatternWhenMultiplePatternsRegistersHint() {
		this.hints.registerInclude().forPattern("com/example/test.properties", "com/example/another.properties");
		assertThat(this.hints.includeResourcePatterns()).anySatisfy(patternOf("com/example/test.properties"))
				.anySatisfy(patternOf("com/example/another.properties")).hasSize(2);
	}

	@Test
	void registerIncludeForClassBytecodeRegistersHint() {
		this.hints.registerInclude().forClassBytecode(String.class);
		assertThat(this.hints.includeResourcePatterns()).singleElement().satisfies(patternOf("java/lang/String.class"));
	}

	@Test
	void registerIncludeForClassBytecodeWhenNestedTypeRegistersHint() {
		this.hints.registerInclude().forClassBytecode(Nested.class);
		assertThat(this.hints.includeResourcePatterns()).singleElement()
				.satisfies(patternOf("org/springframework/aot/hint2/ResourceHintsTests$Nested.class"));
	}

	@Test
	void registerIncludeForClassBytecodeWhenInnerNestedTypeRegistersHint() {
		this.hints.registerInclude().forClassBytecode(Inner.class);
		assertThat(this.hints.includeResourcePatterns()).singleElement()
				.satisfies(patternOf("org/springframework/aot/hint2/ResourceHintsTests$Nested$Inner.class"));
	}

	@Test
	void registerIncludeForClassBytecodeSeveralTimesAddsOnlyOneEntry() {
		this.hints.registerInclude().forClassBytecode(String.class);
		this.hints.registerInclude().forClassBytecode(TypeReference.of(String.class));
		assertThat(this.hints.includeResourcePatterns()).singleElement().satisfies(patternOf("java/lang/String.class"));
	}

	@Test
	void registerIncludeForPatternWhenResourceIsPresentWhenPresentRegistersHint() {
		this.hints.registerInclude().whenResourceIsPresent("META-INF/").forPattern("com/example/*.properties");
		assertThat(this.hints.includeResourcePatterns()).singleElement()
				.satisfies(patternOf("com/example/*.properties"));
	}

	@Test
	void registerIncludeForPatternWhenResourceIsPresentWhenMissingSkipsHint() {
		this.hints.registerInclude().whenResourceIsPresent("location/does-not-exist/")
				.forPattern("com/example/*.properties");
		assertThat(this.hints.includeResourcePatterns()).isEmpty();
	}

	@Test
	void registerExcludeForPatternRegistersHint() {
		this.hints.registerExclude().forPattern("com/example/*.properties");
		assertThat(this.hints.excludeResourcePatterns()).singleElement()
				.satisfies(patternOf("com/example/*.properties"));
	}

	@Test
	void registerBundleForBaseNameRegistersHint() {
		this.hints.registerBundle().forBaseName("com.example.message");
		assertThat(this.hints.resourceBundles()).singleElement().satisfies(bundleOf("com.example.message"));
	}

	@Test
	void registerBundleForBaseNameWhenCalledSeveralTimesRegistersSingleHint() {
		this.hints.registerBundle().forBaseName("com.example.message", "com.example.message");
		this.hints.registerBundle().forBaseName("com.example.message");
		assertThat(this.hints.resourceBundles()).singleElement().satisfies(bundleOf("com.example.message"));
	}

	private static Consumer<ResourcePatternHint> patternOf(String expected) {
		return patternHint -> assertThat(patternHint.getPattern()).isEqualTo(expected);
	}

	private static Consumer<ResourceBundleHint> bundleOf(String expected) {
		return patternHint -> assertThat(patternHint.getBaseName()).isEqualTo(expected);
	}

	static class Nested {

		static class Inner {

		}
	}

}
