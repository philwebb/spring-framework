/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.aspectj;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertEquals;

/**
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class ThisAndTargetSelectionOnlyPointcutsTests {

	private TestInterface testBean;

	private Counter thisAsClassCounter;
	private Counter thisAsInterfaceCounter;
	private Counter targetAsClassCounter;
	private Counter targetAsInterfaceCounter;
	private Counter thisAsClassAndTargetAsClassCounter;
	private Counter thisAsInterfaceAndTargetAsInterfaceCounter;
	private Counter thisAsInterfaceAndTargetAsClassCounter;

	@Before
	public void setUp() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

		this.testBean = (TestInterface) ctx.getBean("testBean");

		this.thisAsClassCounter = (Counter) ctx.getBean("thisAsClassCounter");
		this.thisAsInterfaceCounter = (Counter) ctx.getBean("thisAsInterfaceCounter");
		this.targetAsClassCounter = (Counter) ctx.getBean("targetAsClassCounter");
		this.targetAsInterfaceCounter = (Counter) ctx.getBean("targetAsInterfaceCounter");

		this.thisAsClassAndTargetAsClassCounter = (Counter) ctx.getBean("thisAsClassAndTargetAsClassCounter");
		this.thisAsInterfaceAndTargetAsInterfaceCounter = (Counter) ctx.getBean("thisAsInterfaceAndTargetAsInterfaceCounter");
		this.thisAsInterfaceAndTargetAsClassCounter = (Counter) ctx.getBean("thisAsInterfaceAndTargetAsClassCounter");

		this.thisAsClassCounter.reset();
		this.thisAsInterfaceCounter.reset();
		this.targetAsClassCounter.reset();
		this.targetAsInterfaceCounter.reset();

		this.thisAsClassAndTargetAsClassCounter.reset();
		this.thisAsInterfaceAndTargetAsInterfaceCounter.reset();
		this.thisAsInterfaceAndTargetAsClassCounter.reset();
	}

	@Test
	public void testThisAsClassDoesNotMatch() {
		this.testBean.doIt();
		assertEquals(0, this.thisAsClassCounter.getCount());
	}

	@Test
	public void testThisAsInterfaceMatch() {
		this.testBean.doIt();
		assertEquals(1, this.thisAsInterfaceCounter.getCount());
	}

	@Test
	public void testTargetAsClassDoesMatch() {
		this.testBean.doIt();
		assertEquals(1, this.targetAsClassCounter.getCount());
	}

	@Test
	public void testTargetAsInterfaceMatch() {
		this.testBean.doIt();
		assertEquals(1, this.targetAsInterfaceCounter.getCount());
	}

	@Test
	public void testThisAsClassAndTargetAsClassCounterNotMatch() {
		this.testBean.doIt();
		assertEquals(0, this.thisAsClassAndTargetAsClassCounter.getCount());
	}

	@Test
	public void testThisAsInterfaceAndTargetAsInterfaceCounterMatch() {
		this.testBean.doIt();
		assertEquals(1, this.thisAsInterfaceAndTargetAsInterfaceCounter.getCount());
	}

	@Test
	public void testThisAsInterfaceAndTargetAsClassCounterMatch() {
		this.testBean.doIt();
		assertEquals(1, this.thisAsInterfaceAndTargetAsInterfaceCounter.getCount());
	}

}


interface TestInterface {
	public void doIt();
}


class TestImpl implements TestInterface {
	@Override
	public void doIt() {
	}
}
