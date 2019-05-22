/*
 * Copyright 2002-2019 the original author or authors.
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

package temp;

import org.junit.internal.ArrayComparisonFailure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Temp assert class.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public class XAssert {

	public static void assertArrayEquals(String message, boolean[] expecteds,
			boolean[] actuals) throws ArrayComparisonFailure {
		assertThat(actuals).as(message).isEqualTo(expecteds);
	}

	public static void assertArrayEquals(boolean[] expecteds, boolean[] actuals) {
		assertThat(actuals).isEqualTo(expecteds);
	}

	public static void assertArrayEquals(String message, byte[] expecteds, byte[] actuals)
			throws ArrayComparisonFailure {
		assertThat(actuals).as(message).isEqualTo(expecteds);
	}

	public static void assertArrayEquals(byte[] expecteds, byte[] actuals) {
		assertThat(actuals).isEqualTo(expecteds);
	}

	public static void assertArrayEquals(String message, char[] expecteds, char[] actuals)
			throws ArrayComparisonFailure {
		assertThat(actuals).as(message).isEqualTo(expecteds);
	}

	public static void assertArrayEquals(char[] expecteds, char[] actuals) {
		assertThat(actuals).isEqualTo(expecteds);
	}

	public static void assertArrayEquals(String message, short[] expecteds,
			short[] actuals) throws ArrayComparisonFailure {
		assertThat(actuals).as(message).isEqualTo(expecteds);
	}

	public static void assertArrayEquals(short[] expecteds, short[] actuals) {
		assertThat(actuals).isEqualTo(expecteds);
	}

	public static void assertArrayEquals(String message, int[] expecteds, int[] actuals)
			throws ArrayComparisonFailure {
		assertThat(actuals).as(message).isEqualTo(expecteds);
	}

	public static void assertArrayEquals(int[] expecteds, int[] actuals) {
		assertThat(actuals).isEqualTo(expecteds);
	}

	public static void assertArrayEquals(String message, long[] expecteds, long[] actuals)
			throws ArrayComparisonFailure {
		assertThat(actuals).as(message).isEqualTo(expecteds);
	}

	public static void assertArrayEquals(long[] expecteds, long[] actuals) {
		assertThat(actuals).isEqualTo(expecteds);
	}

	public static void assertEquals(long expected, long actual) {
		assertThat(actual).isEqualTo(expected);
	}

	public static void assertEquals(String message, long expected, long actual) {
		assertThat(actual).as(message).isEqualTo(expected);
	}

	public static void assertEquals(double expected, double actual) {
		assertThat(actual).isEqualTo(expected);
	}

	public static void assertNotNull(String message, Object object) {
		assertThat(object).as(message).isNotNull();
	}

	public static void assertNotNull(Object object) {
		assertThat(object).isNotNull();
	}

	public static void assertNull(String message, Object object) {
		assertThat(object).as(message).isNull();
	}

	public static void assertNull(Object object) {
		assertThat(object).isNull();
	}

	public static void assertSame(String message, Object expected, Object actual) {
		assertThat(actual).as(message).isSameAs(expected);
	}

	public static void assertSame(Object expected, Object actual) {
		assertThat(actual).isSameAs(expected);
	}

	public static void assertNotSame(String message, Object unexpected, Object actual) {
		assertThat(actual).as(message).isNotSameAs(unexpected);
	}

	public static void assertNotSame(Object unexpected, Object actual) {
		assertThat(actual).isNotSameAs(unexpected);
	}

	public static void assertEquals(String message, double expected, double actual,
			double delta) {
		assertThat(actual).as(message).isCloseTo(expected, within(delta));
	}

}
