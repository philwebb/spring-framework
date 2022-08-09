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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

/**
 * Hints for runtime resource needs.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @see ResourceLocationHint
 * @see ResourcePatternHint
 * @see ResourceBundleHint
 * @see RuntimeHints
 */
public class ResourceHints {

	private final Map<ResourcePattern, ResourcePatternHint> resourcePatternHints = new ConcurrentHashMap<>();

	private final Map<String, ResourceBundleHint> resourceBundleHints = new ConcurrentHashMap<>();

	public PatternRegistration registerResourcePattern() {
		return new PatternRegistration();
	}

	public BundleRegistration registerResourceBundle() {
		return new BundleRegistration();
	}

	/**
	 * Return the resources that should be made available at runtime.
	 * @return a stream of {@link ResourcePatternsHint}
	 */
	public Stream<ResourcePatternHint> resourcePatterns() {
		return this.resourcePatternHints.values().stream();
	}

	/**
	 * Return the resource bundles that should be made available at runtime.
	 * @return a stream of {@link ResourceBundleHint}
	 */
	public Stream<ResourceBundleHint> resourceBundles() {
		return this.resourceBundleHints.values().stream();
	}

	public class PatternRegistration extends ReachableTypeRegistration<PatternRegistration> {

		public PatternRegistration whenResourceIsPresent(String location) {
			return whenResourceIsPresent(null, location);
		}

		public PatternRegistration whenResourceIsPresent(@Nullable ClassLoader classLoader, String location) {
			return this;
		}

		public void forPattern(String... includeRegexes) {
			forPattern(ResourcePattern.include(includeRegexes));
		}

		public void forPattern(ResourcePattern... patterns) {
		}

		public void forClassBytecode(Class<?>... types) {
			forClassBytecode(TypeReference.arrayOf(types));
		}

		public void forClassBytecode(String... types) {
			forClassBytecode(TypeReference.arrayOf(types));
		}

		public void forClassBytecode(TypeReference... types) {
		}

	}

	public class BundleRegistration extends ReachableTypeRegistration<PatternRegistration> {

		public void forName(String... names) {
		}

	}

}
