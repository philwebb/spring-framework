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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertEquals;

/**
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class ThisAndTargetSelectionOnlyPointcutsAtAspectJTests {

	private TestInterface testBean;

	private TestInterface testAnnotatedClassBean;

	private TestInterface testAnnotatedMethodBean;

	private Counter counter;

	@org.junit.Before
	@SuppressWarnings("resource")
	public void setUp() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		this.testBean = (TestInterface) ctx.getBean("testBean");
		this.testAnnotatedClassBean = (TestInterface) ctx.getBean("testAnnotatedClassBean");
		this.testAnnotatedMethodBean = (TestInterface) ctx.getBean("testAnnotatedMethodBean");
		this.counter = (Counter) ctx.getBean("counter");
		this.counter.reset();
	}

	@Test
	public void thisAsClassDoesNotMatch() {
		this.testBean.doIt();
		assertEquals(0, this.counter.thisAsClassCounter);
	}

	@Test
	public void thisAsInterfaceMatch() {
		this.testBean.doIt();
		assertEquals(1, this.counter.thisAsInterfaceCounter);
	}

	@Test
	public void targetAsClassDoesMatch() {
		this.testBean.doIt();
		assertEquals(1, this.counter.targetAsClassCounter);
	}

	@Test
	public void targetAsInterfaceMatch() {
		this.testBean.doIt();
		assertEquals(1, this.counter.targetAsInterfaceCounter);
	}

	@Test
	public void thisAsClassAndTargetAsClassCounterNotMatch() {
		this.testBean.doIt();
		assertEquals(0, this.counter.thisAsClassAndTargetAsClassCounter);
	}

	@Test
	public void thisAsInterfaceAndTargetAsInterfaceCounterMatch() {
		this.testBean.doIt();
		assertEquals(1, this.counter.thisAsInterfaceAndTargetAsInterfaceCounter);
	}

	@Test
	public void thisAsInterfaceAndTargetAsClassCounterMatch() {
		this.testBean.doIt();
		assertEquals(1, this.counter.thisAsInterfaceAndTargetAsInterfaceCounter);
	}


	@Test
	public void atTargetClassAnnotationMatch() {
		this.testAnnotatedClassBean.doIt();
		assertEquals(1, this.counter.atTargetClassAnnotationCounter);
	}

	@Test
	public void atAnnotationMethodAnnotationMatch() {
		this.testAnnotatedMethodBean.doIt();
		assertEquals(1, this.counter.atAnnotationMethodAnnotationCounter);
	}

	public static interface TestInterface {
		public void doIt();
	}

	public static class TestImpl implements TestInterface {
		@Override
		public void doIt() {
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface TestAnnotation {

	}

	@TestAnnotation
	public static class AnnotatedClassTestImpl implements TestInterface {
		@Override
		public void doIt() {
		}
	}

	public static class AnnotatedMethodTestImpl implements TestInterface {
		@Override
		@TestAnnotation
		public void doIt() {
		}
	}

	@Aspect
	public static class Counter {
		int thisAsClassCounter;
		int thisAsInterfaceCounter;
		int targetAsClassCounter;
		int targetAsInterfaceCounter;
		int thisAsClassAndTargetAsClassCounter;
		int thisAsInterfaceAndTargetAsInterfaceCounter;
		int thisAsInterfaceAndTargetAsClassCounter;
		int atTargetClassAnnotationCounter;
		int atAnnotationMethodAnnotationCounter;

		public void reset() {
			this.thisAsClassCounter = 0;
			this.thisAsInterfaceCounter = 0;
			this.targetAsClassCounter = 0;
			this.targetAsInterfaceCounter = 0;
			this.thisAsClassAndTargetAsClassCounter = 0;
			this.thisAsInterfaceAndTargetAsInterfaceCounter = 0;
			this.thisAsInterfaceAndTargetAsClassCounter = 0;
			this.atTargetClassAnnotationCounter = 0;
			this.atAnnotationMethodAnnotationCounter = 0;
		}

		@Before("this(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl)")
		public void incrementThisAsClassCounter() {
			this.thisAsClassCounter++;
		}

		@Before("this(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface)")
		public void incrementThisAsInterfaceCounter() {
			this.thisAsInterfaceCounter++;
		}

		@Before("target(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl)")
		public void incrementTargetAsClassCounter() {
			this.targetAsClassCounter++;
		}

		@Before("target(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface)")
		public void incrementTargetAsInterfaceCounter() {
			this.targetAsInterfaceCounter++;
		}

		@Before("this(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl) " +
				"&& target(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl)")
		public void incrementThisAsClassAndTargetAsClassCounter() {
			this.thisAsClassAndTargetAsClassCounter++;
		}

		@Before("this(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface) " +
				"&& target(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface)")
		public void incrementThisAsInterfaceAndTargetAsInterfaceCounter() {
			this.thisAsInterfaceAndTargetAsInterfaceCounter++;
		}

		@Before("this(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestInterface) " +
				"&& target(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestImpl)")
		public void incrementThisAsInterfaceAndTargetAsClassCounter() {
			this.thisAsInterfaceAndTargetAsClassCounter++;
		}

		@Before("@target(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestAnnotation)")
		public void incrementAtTargetClassAnnotationCounter() {
			this.atTargetClassAnnotationCounter++;
		}

		@Before("@annotation(org.springframework.aop.aspectj.ThisAndTargetSelectionOnlyPointcutsAtAspectJTests.TestAnnotation)")
		public void incrementAtAnnotationMethodAnnotationCounter() {
			this.atAnnotationMethodAnnotationCounter++;
		}

	}
}
