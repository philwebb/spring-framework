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

import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint2.ReflectionHints.ConditionRegistration;
import org.springframework.util.ResizableByteArrayOutputStream;

/**
 * Gather the need for resources available at runtime.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @see RuntimeHints
 */
public class ResourceHints {

	public ConditionLocationRegistration registerResource(String location) {
		return null;
	}

	public ConditionRegistration registerResources(String include, String exclude) {
		return null;
	}

	public ConditionRegistration registerResources(ResourcePattern pattern) {
		return null;
	}

	public ConditionRegistration registerBytecode(Class<?> type) {
		return null;
	}

	public ConditionRegistration registerBundle(String name) {
		return null;
	}

	public static class ResourcePattern {

		public ResourcePattern andInclude(String... includeRegexes) {
			return null;
		}

		public ResourcePattern andExclude(String... includeRegexes) {
			return null;
		}

		public ResourcePattern and(ResourcePattern... excludeRegexes) {
			return null;
		}


		public static ResourcePattern of(String includeRegex, String excludeRegex) {
			return null;
		}

		public static ResourcePattern include(String... includeRegexes) {
			return null;
		}

		public static ResourcePattern exclude(String... excludeRegexes) {
			return null;
		}

	}

	public static class ConditionRegistration {

		ConditionRegistration() {
		}

		public ConditionRegistration whenReachable(Class<?> type) {
			return this;
		}

	}

	public static class ConditionLocationRegistration extends ConditionRegistration {

		@Override
		public ConditionLocationRegistration whenReachable(Class<?> type) {
			return (ConditionLocationRegistration) super.whenReachable(type);
		}

		public ConditionLocationRegistration whenPresent() {
			return this;
		}

	}


	// FIXME

	// ....whenReachable(type);
	// ....whenPresent();

	// ResourcePattern(include/exclude)

	// registerType (makes a pattern from the type) this is include

	// registerBundle

	// registerLocation(...)
	// registerPattern(include, exclude)
	// registerPattern(Pattern)
	// registerClassBytecode(...)
	// registerBundle(...)



}
