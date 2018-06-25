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

package org.springframework.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Collections.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * Unit tests for the {@link Assert} class.
 *
 * @author Keith Donald
 * @author Erwin Vervaet
 * @author Rick Evans
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Juergen Hoeller
 */
public class AssertTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@Test
	public void stateWithMessage() {
		Assert.state(true, "enigma");
	}

	@Test
	public void stateWithFalseExpressionAndMessage() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("enigma");
		Assert.state(false, "enigma");
	}

	@Test
	public void stateWithMessageSupplier() {
		Assert.state(true, () -> "enigma");
	}

	@Test
	public void stateWithFalseExpressionAndMessageSupplier() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("enigma");
		Assert.state(false, () -> "enigma");
	}

	@Test
	public void stateWithFalseExpressionAndNullMessageSupplier() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage(equalTo(null));
		Assert.state(false, (Supplier<String>) null);
	}

	@Test
	public void isTrueWithMessage() {
		Assert.isTrue(true, "enigma");
	}

	@Test
	public void isTrueWithFalse() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.isTrue(false, "enigma");
	}

	@Test
	public void isTrueWithMessageSupplier() {
		Assert.isTrue(true, () -> "enigma");
	}

	@Test
	public void isTrueWithFalseAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.isTrue(false, () -> "enigma");
	}

	@Test
	public void isTrueWithFalseAndNullMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(equalTo(null));
		Assert.isTrue(false, (Supplier<String>) null);
	}

	@Test
	public void isNullWithMessage() {
		Assert.isNull(null, "Bla");
	}

	@Test
	public void isNullWithMessageSupplier() {
		Assert.isNull(null, () -> "enigma");
	}

	@Test
	public void isNullWithNonNullObjectAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.isNull("foo", () -> "enigma");
	}

	@Test
	public void isNullWithNonNullObjectAndNullMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(equalTo(null));
		Assert.isNull("foo", (Supplier<String>) null);
	}

	@Test
	public void notNullWithMessage() {
		Assert.notNull("foo", "enigma");
	}

	@Test
	public void notNullWithMessageSupplier() {
		Assert.notNull("foo", () -> "enigma");
	}

	@Test
	public void notNullWithNullAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.notNull(null, () -> "enigma");
	}

	@Test
	public void notNullWithNullAndNullMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(equalTo(null));
		Assert.notNull(null, (Supplier<String>) null);
	}

	@Test
	public void hasLength() {
		Assert.hasLength("I Heart ...", "enigma");
	}

	@Test
	public void hasLengthWithWhitespaceOnly() {
		Assert.hasLength("\t  ", "enigma");
	}

	@Test
	public void hasLengthWithEmptyString() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.hasLength("", "enigma");
	}

	@Test
	public void hasLengthWithNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.hasLength(null, "enigma");
	}

	@Test
	public void hasLengthWithMessageSupplier() {
		Assert.hasLength("foo", () -> "enigma");
	}

	@Test
	public void hasLengthWithWhitespaceOnlyAndMessageSupplier() {
		Assert.hasLength("\t", () -> "enigma");
	}

	@Test
	public void hasLengthWithEmptyStringAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.hasLength("", () -> "enigma");
	}

	@Test
	public void hasLengthWithNullAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.hasLength(null, () -> "enigma");
	}

	@Test
	public void hasLengthWithNullAndNullMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(equalTo(null));
		Assert.hasLength(null, (Supplier<String>) null);
	}

	@Test
	public void hasText() {
		Assert.hasText("foo", "enigma");
	}

	@Test
	public void hasTextWithWhitespaceOnly() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.hasText("\t ", "enigma");
	}

	@Test
	public void hasTextWithEmptyString() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.hasText("", "enigma");
	}

	@Test
	public void hasTextWithNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.hasText(null, "enigma");
	}

	@Test
	public void hasTextWithMessageSupplier() {
		Assert.hasText("foo", () -> "enigma");
	}

	@Test
	public void hasTextWithWhitespaceOnlyAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.hasText("\t ", () -> "enigma");
	}

	@Test
	public void hasTextWithEmptyStringAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.hasText("", () -> "enigma");
	}

	@Test
	public void hasTextWithNullAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.hasText(null, () -> "enigma");
	}

	@Test
	public void hasTextWithNullAndNullMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(equalTo(null));
		Assert.hasText(null, (Supplier<String>) null);
	}

	@Test
	public void doesNotContainWithNullSearchString() {
		Assert.doesNotContain(null, "rod", "enigma");
	}

	@Test
	public void doesNotContainWithNullSubstring() {
		Assert.doesNotContain("A cool chick's name is Brod.", null, "enigma");
	}

	@Test
	public void doesNotContainWithEmptySubstring() {
		Assert.doesNotContain("A cool chick's name is Brod.", "", "enigma");
	}

	@Test
	public void doesNotContainWithNullSearchStringAndNullSubstring() {
		Assert.doesNotContain(null, null, "enigma");
	}

	@Test
	public void doesNotContainWithMessageSupplier() {
		Assert.doesNotContain("foo", "bar", () -> "enigma");
	}

	@Test
	public void doesNotContainWithNullSearchStringAndMessageSupplier() {
		Assert.doesNotContain(null, "bar", () -> "enigma");
	}

	@Test
	public void doesNotContainWithNullSubstringAndMessageSupplier() {
		Assert.doesNotContain("foo", null, () -> "enigma");
	}

	@Test
	public void doesNotContainWithNullSearchStringAndNullSubstringAndMessageSupplier() {
		Assert.doesNotContain(null, null, () -> "enigma");
	}

	@Test
	public void doesNotContainWithSubstringPresentInSearchStringAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.doesNotContain("1234", "23", () -> "enigma");
	}

	@Test
	public void doesNotContainWithNullMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(equalTo(null));
		Assert.doesNotContain("1234", "23", (Supplier<String>) null);
	}

	@Test
	public void notEmptyArray() {
		Assert.notEmpty(new String[] {"1234"}, "enigma");
	}

	@Test
	public void notEmptyArrayWithEmptyArray() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.notEmpty(new String[] {}, "enigma");
	}

	@Test
	public void notEmptyArrayWithNullArray() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.notEmpty((Object[]) null, "enigma");
	}

	@Test
	public void notEmptyArrayWithMessageSupplier() {
		Assert.notEmpty(new String[] {"1234"}, () -> "enigma");
	}

	@Test
	public void notEmptyArrayWithEmptyArrayAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.notEmpty(new String[] {}, () -> "enigma");
	}

	@Test
	public void notEmptyArrayWithNullArrayAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.notEmpty((Object[]) null, () -> "enigma");
	}

	@Test
	public void notEmptyArrayWithEmptyArrayAndNullMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(equalTo(null));
		Assert.notEmpty(new String[] {}, (Supplier<String>) null);
	}

	@Test
	public void noNullElements() {
		Assert.noNullElements(new String[] { "1234" }, "enigma");
	}

	@Test
	public void noNullElementsWithEmptyArray() {
		Assert.noNullElements(new String[] {}, "enigma");
	}

	@Test
	public void noNullElementsWithMessageSupplier() {
		Assert.noNullElements(new String[] { "1234" }, () -> "enigma");
	}

	@Test
	public void noNullElementsWithEmptyArrayAndMessageSupplier() {
		Assert.noNullElements(new String[] {}, () -> "enigma");
	}

	@Test
	public void noNullElementsWithNullArrayAndMessageSupplier() {
		Assert.noNullElements((Object[]) null, () -> "enigma");
	}

	@Test
	public void noNullElementsWithNullElementsAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.noNullElements(new String[] { "foo", null, "bar" }, () -> "enigma");
	}

	@Test
	public void noNullElementsWithNullElementsAndNullMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(equalTo(null));
		Assert.noNullElements(new String[] { "foo", null, "bar" }, (Supplier<String>) null);
	}

	@Test
	public void notEmptyCollection() {
		Assert.notEmpty(singletonList("foo"), "enigma");
	}

	@Test
	public void notEmptyCollectionWithEmptyCollection() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.notEmpty(emptyList(), "enigma");
	}

	@Test
	public void notEmptyCollectionWithNullCollection() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.notEmpty((Collection<?>) null, "enigma");
	}

	@Test
	public void notEmptyCollectionWithMessageSupplier() {
		Assert.notEmpty(singletonList("foo"), () -> "enigma");
	}

	@Test
	public void notEmptyCollectionWithEmptyCollectionAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.notEmpty(emptyList(), () -> "enigma");
	}

	@Test
	public void notEmptyCollectionWithNullCollectionAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.notEmpty((Collection<?>) null, () -> "enigma");
	}

	@Test
	public void notEmptyCollectionWithEmptyCollectionAndNullMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(equalTo(null));
		Assert.notEmpty(emptyList(), (Supplier<String>) null);
	}

	@Test
	public void notEmptyMap() {
		Assert.notEmpty(singletonMap("foo", "bar"), "enigma");
	}

	@Test
	public void notEmptyMapWithNullMap() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.notEmpty((Map<?, ?>) null, "enigma");
	}

	@Test
	public void notEmptyMapWithEmptyMap() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.notEmpty(emptyMap(), "enigma");
	}

	@Test
	public void notEmptyMapWithMessageSupplier() {
		Assert.notEmpty(singletonMap("foo", "bar"), () -> "enigma");
	}

	@Test
	public void notEmptyMapWithEmptyMapAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.notEmpty(emptyMap(), () -> "enigma");
	}

	@Test
	public void notEmptyMapWithNullMapAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma");
		Assert.notEmpty((Map<?, ?>) null, () -> "enigma");
	}

	@Test
	public void notEmptyMapWithEmptyMapAndNullMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(equalTo(null));
		Assert.notEmpty(emptyMap(), (Supplier<String>) null);
	}

	@Test
	public void isInstanceOf() {
		Assert.isInstanceOf(String.class, "foo", "enigma");
	}

	@Test
	public void isInstanceOfWithNullType() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Type to check against must not be null");
		Assert.isInstanceOf(null, "foo", "enigma");
	}

	@Test
	public void isInstanceOfWithNullInstance() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma: null");
		Assert.isInstanceOf(String.class, null, "enigma");
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndNullMessage() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Object of class [java.lang.Long] must be an instance of class java.lang.String");
		Assert.isInstanceOf(String.class, 42L, (String) null);
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndCustomMessage() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Custom message: java.lang.Long");
		Assert.isInstanceOf(String.class, 42L, "Custom message");
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndCustomMessageWithSeparator() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(
				"Custom message: Object of class [java.lang.Long] must be an instance of class java.lang.String");
		Assert.isInstanceOf(String.class, 42L, "Custom message:");
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndCustomMessageWithSpace() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Custom message for java.lang.Long");
		Assert.isInstanceOf(String.class, 42L, "Custom message for ");
	}

	@Test
	public void isInstanceOfWithMessageSupplier() {
		Assert.isInstanceOf(String.class, "foo", () -> "enigma");
	}

	@Test
	public void isInstanceOfWithNullTypeAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Type to check against must not be null");
		Assert.isInstanceOf(null, "foo", () -> "enigma");
	}

	@Test
	public void isInstanceOfWithNullInstanceAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma: null");
		Assert.isInstanceOf(String.class, null, () -> "enigma");
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndNullMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Object of class [java.lang.Long] must be an instance of class java.lang.String");
		Assert.isInstanceOf(String.class, 42L, (Supplier<String>) null);
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma: java.lang.Long");
		Assert.isInstanceOf(String.class, 42L, () -> "enigma");
	}

	@Test
	public void isAssignable() {
		Assert.isAssignable(Number.class, Integer.class, "enigma");
	}

	@Test
	public void isAssignableWithNullSupertype() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Super type to check against must not be null");
		Assert.isAssignable(null, Integer.class, "enigma");
	}

	@Test
	public void isAssignableWithNullSubtype() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma: null");
		Assert.isAssignable(Integer.class, null, "enigma");
	}

	@Test
	public void isAssignableWithTypeMismatchAndNullMessage() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("class java.lang.Integer is not assignable to class java.lang.String");
		Assert.isAssignable(String.class, Integer.class, (String) null);
	}

	@Test
	public void isAssignableWithTypeMismatchAndCustomMessage() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Custom message: class java.lang.Integer");
		Assert.isAssignable(String.class, Integer.class, "Custom message");
	}

	@Test
	public void isAssignableWithTypeMismatchAndCustomMessageWithSeparator() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Custom message: class java.lang.Integer is not assignable to class java.lang.String");
		Assert.isAssignable(String.class, Integer.class, "Custom message:");
	}

	@Test
	public void isAssignableWithTypeMismatchAndCustomMessageWithSpace() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Custom message for class java.lang.Integer");
		Assert.isAssignable(String.class, Integer.class, "Custom message for ");
	}

	@Test
	public void isAssignableWithMessageSupplier() {
		Assert.isAssignable(Number.class, Integer.class, () -> "enigma");
	}

	@Test
	public void isAssignableWithNullSupertypeAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Super type to check against must not be null");
		Assert.isAssignable(null, Integer.class, () -> "enigma");
	}

	@Test
	public void isAssignableWithNullSubtypeAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma: null");
		Assert.isAssignable(Integer.class, null, () -> "enigma");
	}

	@Test
	public void isAssignableWithTypeMismatchAndNullMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("class java.lang.Integer is not assignable to class java.lang.String");
		Assert.isAssignable(String.class, Integer.class, (Supplier<String>) null);
	}

	@Test
	public void isAssignableWithTypeMismatchAndMessageSupplier() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("enigma: class java.lang.Integer");
		Assert.isAssignable(String.class, Integer.class, () -> "enigma");
	}

	@Test
	public void state() {
		Assert.state(true, "enigma");
	}

	@Test
	public void stateWithFalseExpression() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("enigma");
		Assert.state(false, "enigma");
	}

}
