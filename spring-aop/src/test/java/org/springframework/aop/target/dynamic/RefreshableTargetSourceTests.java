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

package org.springframework.aop.target.dynamic;

import org.junit.Test;

import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNotSame;
import static temp.XAssert.assertSame;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public class RefreshableTargetSourceTests {

	/**
	 * Test what happens when checking for refresh but not refreshing object.
	 */
	@Test
	public void testRefreshCheckWithNonRefresh() throws Exception {
		CountingRefreshableTargetSource ts = new CountingRefreshableTargetSource();
		ts.setRefreshCheckDelay(0);

		Object a = ts.getTarget();
		Thread.sleep(1);
		Object b = ts.getTarget();

		assertEquals("Should be one call to freshTarget to get initial target", 1, ts.getCallCount());
		assertSame("Returned objects should be the same - no refresh should occur", a, b);
	}

	/**
	 * Test what happens when checking for refresh and refresh occurs.
	 */
	@Test
	public void testRefreshCheckWithRefresh() throws Exception {
		CountingRefreshableTargetSource ts = new CountingRefreshableTargetSource(true);
		ts.setRefreshCheckDelay(0);

		Object a = ts.getTarget();
		Thread.sleep(100);
		Object b = ts.getTarget();

		assertEquals("Should have called freshTarget twice", 2, ts.getCallCount());
		assertNotSame("Should be different objects", a, b);
	}

	/**
	 * Test what happens when no refresh occurs.
	 */
	@Test
	public void testWithNoRefreshCheck() throws Exception {
		CountingRefreshableTargetSource ts = new CountingRefreshableTargetSource(true);
		ts.setRefreshCheckDelay(-1);

		Object a = ts.getTarget();
		Object b = ts.getTarget();

		assertEquals("Refresh target should only be called once", 1, ts.getCallCount());
		assertSame("Objects should be the same - refresh check delay not elapsed", a, b);
	}

	@Test
	public void testRefreshOverTime() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);

		CountingRefreshableTargetSource ts = new CountingRefreshableTargetSource(true);
		ts.setRefreshCheckDelay(100);

		Object a = ts.getTarget();
		Object b = ts.getTarget();
		assertThat(b).as("Objects should be same").isEqualTo(a);

		Thread.sleep(50);

		Object c = ts.getTarget();
		assertThat(c).as("A and C should be same").isEqualTo(a);

		Thread.sleep(60);

		Object d = ts.getTarget();
		assertNotNull("D should not be null", d);
		assertThat(a.equals(d)).as("A and D should not be equal").isFalse();

		Object e = ts.getTarget();
		assertThat(e).as("D and E should be equal").isEqualTo(d);

		Thread.sleep(110);

		Object f = ts.getTarget();
		assertThat(e.equals(f)).as("E and F should be different").isFalse();
	}


	private static class CountingRefreshableTargetSource extends AbstractRefreshableTargetSource {

		private int callCount;

		private boolean requiresRefresh;

		public CountingRefreshableTargetSource() {
		}

		public CountingRefreshableTargetSource(boolean requiresRefresh) {
			this.requiresRefresh = requiresRefresh;
		}

		@Override
		protected Object freshTarget() {
			this.callCount++;
			return new Object();
		}

		public int getCallCount() {
			return this.callCount;
		}

		@Override
		protected boolean requiresRefresh() {
			return this.requiresRefresh;
		}
	}

}
