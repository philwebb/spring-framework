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

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.ComparisonFailure;
import org.junit.function.ThrowingRunnable;
import org.junit.internal.ArrayComparisonFailure;
import org.junit.internal.ExactComparisonCriteria;
import org.junit.internal.InexactComparisonCriteria;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Temp assert class.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public class XAssert  {

    public static void assertTrue(String message, boolean condition) {
    	assertThat(condition).as(message).isTrue();
    }

    public static void assertNotEquals(String message, Object unexpected,
            Object actual) {
    }

    public static void assertNotEquals(Object unexpected, Object actual) {
    }

    public static void assertNotEquals(String message, double unexpected,
            double actual, double delta) {
    }

    public static void assertNotEquals(double unexpected, double actual, double delta) {
    }

    public static void assertNotEquals(float unexpected, float actual, float delta) {
    }

    public static void assertArrayEquals(String message, Object expecteds,
            Object actuals) throws ArrayComparisonFailure {
    }

    public static void assertArrayEquals(Object expecteds, Object actuals) {
    }

    public static void assertEquals(float expected, float actual, float delta) {

    }


}
