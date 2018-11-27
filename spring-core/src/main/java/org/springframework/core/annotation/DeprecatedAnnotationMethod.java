/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.util.function.Supplier;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Internal class used to check updated implementations of deprecated methods.
 *
 * @author Phillip Webb
 * @since 5.2
 */
final class DeprecatedAnnotationMethod {

	private DeprecatedAnnotationMethod() {
	}

	/**
	 * Create a new {@link ReplacementAnnotationMethod} builder for the
	 * deprecated method.
	 * @param deprecatedMethod the deprecated method
	 * @return a replacement builder.
	 */
	static <T> ReplacementAnnotationMethod<T> of(Supplier<T> deprecatedMethod) {
		return new ReplacementAnnotationMethod<>(deprecatedMethod);
	}

	/**
	 * Builder to complete replacement details for a deprecated annotation
	 * method.
	 * @param <T> the return type
	 */
	static class ReplacementAnnotationMethod<T> {

		private final Supplier<T> deprecatedMethod;

		private Supplier<String> description;

		ReplacementAnnotationMethod(Supplier<T> deprecatedMethod) {
			this.deprecatedMethod = deprecatedMethod;
		}

		/**
		 * Add a description for the method.
		 * @param description a description supplier
		 * @return this instance
		 */
		public ReplacementAnnotationMethod<T> withDescription(
				Supplier<String> description) {
			this.description = description;
			return this;
		}

		/**
		 * Provide the replacement method that should be used instead of the
		 * deprecated one. The replacement method is called, and when approprate
		 * the result is checked against the deprecated method.
		 * @param replacementMethod the replacement method
		 * @return the result of the replacement method
		 */
		public T isReplacedBy(Supplier<T> replacementMethod) {
			T result = replacementMethod.get();
			T expectedResult = this.deprecatedMethod.get();
			Assert.state(ObjectUtils.nullSafeEquals(result, expectedResult),
					() -> "Expected " + expectedResult + " got " + result
							+ (this.description != null ? " [" + this.description.get() + "]" : ""));
			return result;
		}

	}

}
