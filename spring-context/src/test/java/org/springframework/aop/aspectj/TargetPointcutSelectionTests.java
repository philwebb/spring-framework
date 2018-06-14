/*
 * Copyright 2002-2015 the original author or authors.
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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.*;

/**
 * Tests for target selection matching (see SPR-3783).
 * <p>Thanks to Tomasz Blachowicz for the bug report!
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class TargetPointcutSelectionTests {

	public TestInterface testImpl1;

	public TestInterface testImpl2;

	public TestAspect testAspectForTestImpl1;

	public TestAspect testAspectForAbstractTestImpl;

	public TestInterceptor testInterceptor;


	@Before
	@SuppressWarnings("resource")
	public void setUp() {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

		this.testImpl1 = (TestInterface) ctx.getBean("testImpl1");
		this.testImpl2 = (TestInterface) ctx.getBean("testImpl2");
		this.testAspectForTestImpl1 = (TestAspect) ctx.getBean("testAspectForTestImpl1");
		this.testAspectForAbstractTestImpl = (TestAspect) ctx.getBean("testAspectForAbstractTestImpl");
		this.testInterceptor = (TestInterceptor) ctx.getBean("testInterceptor");

		this.testAspectForTestImpl1.count = 0;
		this.testAspectForAbstractTestImpl.count = 0;
		this.testInterceptor.count = 0;
	}


	@Test
	public void targetSelectionForMatchedType() {
		this.testImpl1.interfaceMethod();
		assertEquals("Should have been advised by POJO advice for impl", 1, this.testAspectForTestImpl1.count);
		assertEquals("Should have been advised by POJO advice for base type", 1, this.testAspectForAbstractTestImpl.count);
		assertEquals("Should have been advised by advisor", 1, this.testInterceptor.count);
	}

	@Test
	public void targetNonSelectionForMismatchedType() {
		this.testImpl2.interfaceMethod();
		assertEquals("Shouldn't have been advised by POJO advice for impl", 0, this.testAspectForTestImpl1.count);
		assertEquals("Should have been advised by POJO advice for base type", 1, this.testAspectForAbstractTestImpl.count);
		assertEquals("Shouldn't have been advised by advisor", 0, this.testInterceptor.count);
	}


	public static interface TestInterface {

		public void interfaceMethod();
	}


	// Reproducing bug requires that the class specified in target() pointcut doesn't
	// include the advised method's implementation (instead a base class should include it)
	public static abstract class AbstractTestImpl implements TestInterface {

		@Override
		public void interfaceMethod() {
		}
	}


	public static class TestImpl1 extends AbstractTestImpl {
	}


	public static class TestImpl2 extends AbstractTestImpl {
	}


	public static class TestAspect {

		public int count;

		public void increment() {
			this.count++;
		}
	}


	public static class TestInterceptor extends TestAspect implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation mi) throws Throwable {
			increment();
			return mi.proceed();
		}
	}

}
