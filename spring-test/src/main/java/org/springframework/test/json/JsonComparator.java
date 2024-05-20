/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.json;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Strategy interface used to compare JSON strings.
 *
 * @author Phillip Webb
 * @since 6.2
 */
@FunctionalInterface
public interface JsonComparator {

	/**
	 * Compare the given JSON strings.
	 * @param expectedJson the expected JSON
	 * @param actualJson the actual JSON
	 * @return the JSON comparison
	 */
	JsonComparison compare(@Nullable String expectedJson, @Nullable String actualJson);

	/**
	 * Factory method to create a {@link JsonComparator} for the given {@link JsonCompareMode}.
	 * @param compareMode the compare mode
	 * @return a {@link JsonComparator} for the mode
	 * @throws IllegalStateException if no suitable JSON assertion library can be found
	 */
	static JsonComparator forCompareMode(JsonCompareMode compareMode) {
		Assert.state(ClassUtils.isPresent("org.skyscreamer.jsonassert.JSONCompare", null),
				"Unable to use compare modes without the JSONAssert library");
		return JsonAssert.comparator(compareMode);
	}

}
