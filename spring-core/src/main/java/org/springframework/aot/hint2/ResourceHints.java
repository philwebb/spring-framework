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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * Hints for runtime resource needs.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @see RuntimeHints
 */
public class ResourceHints {

	private final Map<ResourcePattern, ResourcePatternHint> patternHints = new ConcurrentHashMap<>();

	private final Map<String, ResourcePatternHint> locationHints = new ConcurrentHashMap<>();

	private final Map<String, ResourcePatternHint> bundleHints = new ConcurrentHashMap<>();

	private final Set<TypeReference> classBytecodeHints = Collections
			.newSetFromMap(new ConcurrentHashMap<>());

	ConditionRegistration updatePattern(ResourcePattern[] patterns, UnaryOperator<ResourcePatternHint> mapper) {
		for (ResourcePattern pattern : patterns) {
			this.patternHints.compute(pattern, (key, hint) -> mapper
					.apply((hint != null) ? hint : new ResourcePatternHint(pattern)));
		}
		return new ConditionRegistration();
	}

	public ResourceRegistration registerResource() {
		return new ResourceRegistration();
	}

	public BundleRegistration registerBundle() {
		return new BundleRegistration();
	}

	public  class ResourceRegistration {

		public ConditionLocationRegistration forLocation(String... locations) {
			return null;
		}

		public ConditionRegistration forPattern(String include, String exclude) {
			return null;
		}

		public ConditionRegistration forPattern(ResourcePattern... patterns) {
			return null;
		}

		public void forClassBytecode(Class<?>... types) {
		}

	}

	public  class BundleRegistration {

		public ConditionRegistration forName(String... names) {
			return null;
		}

	}

	public class ConditionRegistration
			extends AbstractConditionalRegistration<ConditionRegistration> {

		@Override
		protected void apply(TypeReference reachableType) {
		}

	}

	public static class ConditionLocationRegistration
			extends AbstractConditionalRegistration<ConditionLocationRegistration> {

		public ConditionLocationRegistration whenPresent() {
			return this;
		}

		@Override
		protected void apply(TypeReference reachableType) {
		}

	}

}
