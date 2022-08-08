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

/**
 * FIXME
 * <p>
 * The patterns may be a simple path which has a one-to-one mapping to a
 * resource on the classpath, or alternatively may contain the special {@code *}
 * character to indicate a wildcard search. For example:
 * <ul>
 * <li>{@code file.properties}: matches just the {@code file.properties} file at
 * the root of the classpath.</li>
 * <li>{@code com/example/file.properties}: matches just the
 * {@code file.properties} file in {@code com/example/}.</li>
 * <li>{@code *.properties}: matches all the files with a {@code .properties}
 * extension anywhere in the classpath.</li>
 * <li>{@code com/example/*.properties}: matches all the files with a
 * {@code .properties} extension in {@code com/example/} and its child
 * directories at any depth.</li>
 * <li>{@code com/example/*}: matches all the files in {@code com/example/} and
 * its child directories at any depth.</li>
 * </ul>
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Phillip Webb
 * @since 6.0
 * @see PatternResourceHint
 */
public class ResourcePattern {

	// FIXME look at the old ResourcePatternHint code

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