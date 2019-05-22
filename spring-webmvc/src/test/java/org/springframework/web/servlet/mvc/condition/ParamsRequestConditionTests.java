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

package org.springframework.web.servlet.mvc.condition;

import java.util.Collection;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition.ParamExpression;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertFalse;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNull;

/**
 * Unit tests for {@link ParamsRequestCondition}.
 * @author Arjen Poutsma
 */
public class ParamsRequestConditionTests {

	@Test
	public void paramEquals() {
		assertEquals(new ParamsRequestCondition("foo"), new ParamsRequestCondition("foo"));
		assertFalse(new ParamsRequestCondition("foo").equals(new ParamsRequestCondition("bar")));
		assertFalse(new ParamsRequestCondition("foo").equals(new ParamsRequestCondition("FOO")));
		assertEquals(new ParamsRequestCondition("foo=bar"), new ParamsRequestCondition("foo=bar"));
		assertFalse(new ParamsRequestCondition("foo=bar").equals(new ParamsRequestCondition("FOO=bar")));
	}

	@Test
	public void paramPresent() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", "");

		assertNotNull(new ParamsRequestCondition("foo").getMatchingCondition(request));
	}

	@Test // SPR-15831
	public void paramPresentNullValue() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", (String) null);

		assertNotNull(new ParamsRequestCondition("foo").getMatchingCondition(request));
	}

	@Test
	public void paramPresentNoMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("bar", "");

		assertNull(new ParamsRequestCondition("foo").getMatchingCondition(request));
	}

	@Test
	public void paramNotPresent() {
		ParamsRequestCondition condition = new ParamsRequestCondition("!foo");
		MockHttpServletRequest request = new MockHttpServletRequest();

		assertNotNull(condition.getMatchingCondition(request));
	}

	@Test
	public void paramValueMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", "bar");

		assertNotNull(new ParamsRequestCondition("foo=bar").getMatchingCondition(request));
	}

	@Test
	public void paramValueNoMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo", "bazz");

		assertNull(new ParamsRequestCondition("foo=bar").getMatchingCondition(request));
	}

	@Test
	public void compareTo() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo", "bar", "baz");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=a", "bar");

		int result = condition1.compareTo(condition2, request);
		assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();

		result = condition2.compareTo(condition1, request);
		assertThat(result > 0).as("Invalid comparison result: " + result).isTrue();
	}

	@Test // SPR-16674
	public void compareToWithMoreSpecificMatchByValue() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type=code");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

		int result = condition1.compareTo(condition2, request);
		assertThat(result < 0).as("Invalid comparison result: " + result).isTrue();
	}

	@Test
	public void compareToWithNegatedMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		ParamsRequestCondition condition1 = new ParamsRequestCondition("response_type!=code");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("response_type");

		assertEquals("Negated match should not count as more specific",
				0, condition1.compareTo(condition2, request));
	}

	@Test
	public void combine() {
		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo=bar");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=baz");

		ParamsRequestCondition result = condition1.combine(condition2);
		Collection<ParamExpression> conditions = result.getContent();
		assertEquals(2, conditions.size());
	}

}
