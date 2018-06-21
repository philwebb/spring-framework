/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.mock.web;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link MockHttpSession}.
 *
 * @author Sam Brannen
 * @since 3.2
 */
public class MockHttpSessionTests {

	private final MockHttpSession session = new MockHttpSession();


	@Test
	public void invalidateOnce() {
		assertFalse(this.session.isInvalid());
		this.session.invalidate();
		assertTrue(this.session.isInvalid());
	}

	@Test(expected = IllegalStateException.class)
	public void invalidateTwice() {
		this.session.invalidate();
		this.session.invalidate();
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void getCreationTimeOnInvalidatedSession() {
		this.session.invalidate();
		this.session.getCreationTime();
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void getLastAccessedTimeOnInvalidatedSession() {
		this.session.invalidate();
		this.session.getLastAccessedTime();
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void getAttributeOnInvalidatedSession() {
		this.session.invalidate();
		this.session.getAttribute("foo");
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void getAttributeNamesOnInvalidatedSession() {
		this.session.invalidate();
		this.session.getAttributeNames();
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void getValueOnInvalidatedSession() {
		this.session.invalidate();
		this.session.getValue("foo");
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void getValueNamesOnInvalidatedSession() {
		this.session.invalidate();
		this.session.getValueNames();
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void setAttributeOnInvalidatedSession() {
		this.session.invalidate();
		this.session.setAttribute("name", "value");
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void putValueOnInvalidatedSession() {
		this.session.invalidate();
		this.session.putValue("name", "value");
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void removeAttributeOnInvalidatedSession() {
		this.session.invalidate();
		this.session.removeAttribute("name");
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void removeValueOnInvalidatedSession() {
		this.session.invalidate();
		this.session.removeValue("name");
	}

	/**
	 * @since 4.0
	 */
	@Test(expected = IllegalStateException.class)
	public void isNewOnInvalidatedSession() {
		this.session.invalidate();
		this.session.isNew();
	}

}
