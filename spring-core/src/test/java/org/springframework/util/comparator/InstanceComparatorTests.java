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

package org.springframework.util.comparator;

import java.util.Comparator;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link InstanceComparator}.
 *
 * @author Phillip Webb
 */
public class InstanceComparatorTests {

	private C1 c1 = new C1();

	private C2 c2 = new C2();

	private C3 c3 = new C3();

	private C4 c4 = new C4();

	@Test
	public void shouldCompareClasses() throws Exception {
		Comparator<Object> comparator = new InstanceComparator<>(C1.class, C2.class);
		assertThat(comparator.compare(this.c1, this.c1), is(0));
		assertThat(comparator.compare(this.c1, this.c2), is(-1));
		assertThat(comparator.compare(this.c2, this.c1), is(1));
		assertThat(comparator.compare(this.c2, this.c3), is(-1));
		assertThat(comparator.compare(this.c2, this.c4), is(-1));
		assertThat(comparator.compare(this.c3, this.c4), is(0));
	}

	@Test
	public void shouldCompareInterfaces() throws Exception {
		Comparator<Object> comparator = new InstanceComparator<>(I1.class, I2.class);
		assertThat(comparator.compare(this.c1, this.c1), is(0));
		assertThat(comparator.compare(this.c1, this.c2), is(0));
		assertThat(comparator.compare(this.c2, this.c1), is(0));
		assertThat(comparator.compare(this.c1, this.c3), is(-1));
		assertThat(comparator.compare(this.c3, this.c1), is(1));
		assertThat(comparator.compare(this.c3, this.c4), is(0));
	}

	@Test
	public void shouldCompareMix() throws Exception {
		Comparator<Object> comparator = new InstanceComparator<>(I1.class, C3.class);
		assertThat(comparator.compare(this.c1, this.c1), is(0));
		assertThat(comparator.compare(this.c3, this.c4), is(-1));
		assertThat(comparator.compare(this.c3, null), is(-1));
		assertThat(comparator.compare(this.c4, null), is(0));
	}

	private static interface I1 {

	}

	private static interface I2 {

	}

	private static class C1 implements I1 {
	}

	private static class C2 implements I1 {
	}

	private static class C3 implements I2 {
	}

	private static class C4 implements I2 {
	}

}
