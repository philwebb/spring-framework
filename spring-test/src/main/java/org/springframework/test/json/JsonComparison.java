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

/**
 * A comparison of two JSON strings as returned from a {@link JsonComparator}.
 *
 * @author Phillip Webb
 * @since 6.2
 */
public final class JsonComparison {

	private final Result result;

	private final String expectedJson;

	private final String actualJson;

	private final String message;


	private JsonComparison(Result result, @Nullable String expectedJson,
			@Nullable String actualJson, @Nullable String message) {

		this.result = result;
		this.expectedJson = expectedJson;
		this.actualJson = actualJson;
		this.message = message;
	}


	/**
	 * Return the result of the comparison.
	 * @return the comparison result
	 */
	public Result getResult() {
		return this.result;
	}

	/**
	 * Return the expected JSON.
	 * @return the JSON that was expected
	 */
	public String getExpectedJson() {
		return expectedJson;
	}

	/**
	 * Return the actual JSON.
	 * @return the JSON that was actually supplied
	 */
	public String getActualJson() {
		return actualJson;
	}

	/**
	 * Return a message describing the comparison.
	 * @return the comparison message
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Factory method to create a new {@link JsonComparison} when the JSON
	 * strings match.
	 * @param expectedJson the expected JSON
	 * @param actualJson the actual JSON
	 * @return a new {@link JsonComparison} instance
	 */
	public static JsonComparison match(String expectedJson, String actualJson) {
		return new JsonComparison(Result.MATCH, expectedJson, actualJson, null);
	}

	/**
	 * Factory method to create a new {@link JsonComparison} when the JSON strings do not match.
	 * @param expectedJson the expected JSON
	 * @param actualJson the actual JSON
	 * @param message a message describing the mismatch
	 * @return a new {@link JsonComparison} instance
	 */
	public static JsonComparison mismatch(String expectedJson, String actualJson, @Nullable String message) {
		return new JsonComparison(Result.MISMATCH, expectedJson, actualJson, message);
	}


	/**
	 * Comparison results.
	 */
	public static enum Result {

		/**
		 * The JSON strings match when considering the comparison rules.
		 */
		MATCH,

		/**
		 * The JSON strings do not match when considering the comparison rules.
		 */
		MISMATCH
	}

}
