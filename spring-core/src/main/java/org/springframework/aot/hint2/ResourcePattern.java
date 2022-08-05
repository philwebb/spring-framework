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

public class ResourcePattern {

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