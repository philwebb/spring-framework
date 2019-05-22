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

package org.springframework.aop.aspectj;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.tests.sample.beans.CountingTestBean;
import org.springframework.tests.sample.beans.IOther;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.tests.sample.beans.subpkg.DeepBean;

import static org.assertj.core.api.Assertions.*;
import static temp.XAssert.assertFalse;

/**
 * Unit tests for the {@link TypePatternClassFilter} class.
 *
 * @author Rod Johnson
 * @author Rick Evans
 * @author Chris Beams
 */
public class TypePatternClassFilterTests {

	@Test
	public void testInvalidPattern() {
		// should throw - pattern must be recognized as invalid
		assertThatIllegalArgumentException().isThrownBy(() ->
				new TypePatternClassFilter("-"));
	}

	@Test
	public void testValidPatternMatching() {
		TypePatternClassFilter tpcf = new TypePatternClassFilter("org.springframework.tests.sample.beans.*");
		assertThat(tpcf.matches(TestBean.class)).as("Must match: in package").isTrue();
		assertThat(tpcf.matches(ITestBean.class)).as("Must match: in package").isTrue();
		assertThat(tpcf.matches(IOther.class)).as("Must match: in package").isTrue();
		assertFalse("Must be excluded: in wrong package", tpcf.matches(DeepBean.class));
		assertFalse("Must be excluded: in wrong package", tpcf.matches(BeanFactory.class));
		assertFalse("Must be excluded: in wrong package", tpcf.matches(DefaultListableBeanFactory.class));
	}

	@Test
	public void testSubclassMatching() {
		TypePatternClassFilter tpcf = new TypePatternClassFilter("org.springframework.tests.sample.beans.ITestBean+");
		assertThat(tpcf.matches(TestBean.class)).as("Must match: in package").isTrue();
		assertThat(tpcf.matches(ITestBean.class)).as("Must match: in package").isTrue();
		assertThat(tpcf.matches(CountingTestBean.class)).as("Must match: in package").isTrue();
		assertFalse("Must be excluded: not subclass", tpcf.matches(IOther.class));
		assertFalse("Must be excluded: not subclass", tpcf.matches(DefaultListableBeanFactory.class));
	}

	@Test
	public void testAndOrNotReplacement() {
		TypePatternClassFilter tpcf = new TypePatternClassFilter("java.lang.Object or java.lang.String");
		assertFalse("matches Number",tpcf.matches(Number.class));
		assertThat(tpcf.matches(Object.class)).as("matches Object").isTrue();
		assertThat(tpcf.matches(String.class)).as("matchesString").isTrue();
		tpcf = new TypePatternClassFilter("java.lang.Number+ and java.lang.Float");
		assertThat(tpcf.matches(Float.class)).as("matches Float").isTrue();
		assertFalse("matches Double",tpcf.matches(Double.class));
		tpcf = new TypePatternClassFilter("java.lang.Number+ and not java.lang.Float");
		assertFalse("matches Float",tpcf.matches(Float.class));
		assertThat(tpcf.matches(Double.class)).as("matches Double").isTrue();
	}

	@Test
	public void testSetTypePatternWithNullArgument() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new TypePatternClassFilter(null));
	}

	@Test
	public void testInvocationOfMatchesMethodBlowsUpWhenNoTypePatternHasBeenSet() throws Exception {
		assertThatIllegalStateException().isThrownBy(() ->
				new TypePatternClassFilter().matches(String.class));
	}

}
