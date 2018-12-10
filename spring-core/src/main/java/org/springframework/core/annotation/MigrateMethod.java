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

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Internal class used to help migrate annotation util methods to a new
 * implementation.
 *
 * @author Phillip Webb
 * @since 5.2
 */
final class MigrateMethod {

	private MigrateMethod() {
	}

	/**
	 * Create a new {@link ReplacementMethod} builder for the deprecated method.
	 * @param originalMethod the original method being migrated
	 * @return a replacement builder.
	 */
	static <T> ReplacementMethod<T> from(Supplier<T> originalMethod) {
		return new ReplacementMethod<>(originalMethod);
	}

	static ReplacementCall fromCall(Runnable originalMethod) {
		return new ReplacementCall(originalMethod);
	}

	/**
	 * Builder to complete replacement details for a deprecated annotation
	 * method.
	 * @param <T> the return type
	 */
	static class ReplacementMethod<T> {

		private final Supplier<T> originalMethod;

		private Supplier<String> description;

		ReplacementMethod(Supplier<T> deprecatedMethod) {
			this.originalMethod = deprecatedMethod;
		}

		/**
		 * Add a description for the method.
		 * @param description a description supplier
		 * @return this instance
		 */
		public ReplacementMethod<T> withDescription(Supplier<String> description) {
			this.description = description;
			return this;
		}

		/**
		 * Provide the replacement method that should be used instead of the
		 * deprecated one. The replacement method is called, and when
		 * appropriate the result is checked against the deprecated method.
		 * @param replacementMethod the replacement method
		 * @return the result of the replacement method
		 */
		public T to(Supplier<T> replacementMethod) {
			T result = tryInvoke(replacementMethod);
			T expectedResult = this.originalMethod.get();
			Assert.state(isEquivalent(result, expectedResult),
					() -> "Expected " + expectedResult + " got " + result
							+ (this.description != null
									? " [" + this.description.get() + "]"
									: ""));
			return result;
		}

		private T tryInvoke(Supplier<T> replacementMethod) {
			try {
				return replacementMethod.get();
			}
			catch (RuntimeException expected) {
				try {
					this.originalMethod.get();
					throw new Error("Expected exception not thrown", expected);
				}
				catch (RuntimeException actual) {
					if (!expected.getClass().isInstance(actual)) {
						throw new Error(
								"Exception is not " + expected.getClass().getName(),
								actual);
					}
					throw actual;
				}
			}
		}

		private boolean isEquivalent(Object result, Object expectedResult) {
			if (ObjectUtils.nullSafeEquals(result, expectedResult)) {
				return true;
			}
			if (result == null || expectedResult == null) {
				return false;
			}
			if (result instanceof Map && expectedResult instanceof Map) {
				return isEquivalentMap((Map<?, ?>) result, (Map<?, ?>) expectedResult);
			}
			if (result instanceof List && expectedResult instanceof List) {
				return isEquivalentList((List<?>) result, (List<?>) expectedResult);
			}
			if (result instanceof Object[] && expectedResult instanceof Object[]) {
				return isEquivalentArray((Object[]) result, (Object[]) expectedResult);
			}
			if (result instanceof Object[] && !(expectedResult instanceof Object[])) {
				if (isEquivalentArray((Object[]) result,
						new Object[] { expectedResult })) {
					return true;
				}
			}
			if (!(result instanceof Object[]) && expectedResult instanceof Object[]) {
				if (isEquivalentArray(new Object[] { result },
						(Object[]) expectedResult)) {
					return true;
				}
			}
			return false;
		}

		private boolean isEquivalentMap(Map<?, ?> result, Map<?, ?> expectedResult) {
			if (result.size() != expectedResult.size()) {
				return false;
			}
			for (Map.Entry<?, ?> entry : result.entrySet()) {
				if (!expectedResult.containsKey(entry.getKey())) {
					return false;
				}
				if (!isEquivalent(entry.getValue(), expectedResult.get(entry.getKey()))) {
					return false;
				}
			}
			return true;
		}

		private boolean isEquivalentList(List<?> result, List<?> expectedResult) {
			if (result.size() != expectedResult.size()) {
				return false;
			}
			for (int i = 0; i < result.size(); i++) {
				if (!isEquivalent(result.get(i), expectedResult.get(i))) {
					return false;
				}
			}
			return true;
		}

		private boolean isEquivalentArray(Object[] result, Object[] expectedResult) {
			if (result.length != expectedResult.length) {
				return false;
			}
			for (int i = 0; i < result.length; i++) {
				if (!isEquivalent(result[i], expectedResult[i])) {
					return false;
				}
			}
			return true;
		}

	}

	static class ReplacementCall {

		private final Runnable originalMethod;

		public ReplacementCall(Runnable originalMethod) {
			this.originalMethod = originalMethod;
		}

		public void to(Runnable replacementMethod) {
			tryInvoke(this.originalMethod);
			replacementMethod.run();
		}

		private void tryInvoke(Runnable replacementMethod) {
			try {
				replacementMethod.run();
			}
			catch (RuntimeException expected) {
				try {
					this.originalMethod.run();
					throw new Error("Expected exception not thrown", expected);
				}
				catch (RuntimeException actual) {
					if (!expected.getClass().isInstance(actual)) {
						throw new Error(
								"Exception is not " + expected.getClass().getName(),
								actual);
					}
					throw actual;
				}
			}
		}

	}

}
