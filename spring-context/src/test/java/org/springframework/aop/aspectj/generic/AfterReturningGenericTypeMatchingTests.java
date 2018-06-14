/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.aop.aspectj.generic;

import java.util.ArrayList;
import java.util.Collection;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.sample.beans.Employee;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * Tests ensuring that after-returning advice for generic parameters bound to
 * the advice and the return type follow AspectJ semantics.
 *
 * <p>See SPR-3628 for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class AfterReturningGenericTypeMatchingTests {

	private GenericReturnTypeVariationClass testBean;

	private CounterAspect counterAspect;


	@Before
	public void setUp() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		this.counterAspect = (CounterAspect) ctx.getBean("counterAspect");
		this.counterAspect.reset();

		this.testBean = (GenericReturnTypeVariationClass) ctx.getBean("testBean");
	}

	@Test
	public void testReturnTypeExactMatching() {
		this.testBean.getStrings();
		assertEquals(1, this.counterAspect.getStringsInvocationsCount);
		assertEquals(0, this.counterAspect.getIntegersInvocationsCount);

		this.counterAspect.reset();

		this.testBean.getIntegers();
		assertEquals(0, this.counterAspect.getStringsInvocationsCount);
		assertEquals(1, this.counterAspect.getIntegersInvocationsCount);
	}

	@Test
	public void testReturnTypeRawMatching() {
		this.testBean.getStrings();
		assertEquals(1, this.counterAspect.getRawsInvocationsCount);

		this.counterAspect.reset();

		this.testBean.getIntegers();
		assertEquals(1, this.counterAspect.getRawsInvocationsCount);
	}

	@Test
	public void testReturnTypeUpperBoundMatching() {
		this.testBean.getIntegers();
		assertEquals(1, this.counterAspect.getNumbersInvocationsCount);
	}

	@Test
	public void testReturnTypeLowerBoundMatching() {
		this.testBean.getTestBeans();
		assertEquals(1, this.counterAspect.getTestBeanInvocationsCount);

		this.counterAspect.reset();

		this.testBean.getEmployees();
		assertEquals(0, this.counterAspect.getTestBeanInvocationsCount);
	}

}


class GenericReturnTypeVariationClass {

	public Collection<String> getStrings() {
		return new ArrayList<>();
	}

	public Collection<Integer> getIntegers() {
		return new ArrayList<>();
	}

	public Collection<TestBean> getTestBeans() {
		return new ArrayList<>();
	}

	public Collection<Employee> getEmployees() {
		return new ArrayList<>();
	}
}


@Aspect
class CounterAspect {

	int getRawsInvocationsCount;

	int getStringsInvocationsCount;

	int getIntegersInvocationsCount;

	int getNumbersInvocationsCount;

	int getTestBeanInvocationsCount;

	@Pointcut("execution(* org.springframework.aop.aspectj.generic.GenericReturnTypeVariationClass.*(..))")
	public void anyTestMethod() {
	}

	@AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
	public void incrementGetRawsInvocationsCount(Collection<?> ret) {
		this.getRawsInvocationsCount++;
	}

	@AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
	public void incrementGetStringsInvocationsCount(Collection<String> ret) {
		this.getStringsInvocationsCount++;
	}

	@AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
	public void incrementGetIntegersInvocationsCount(Collection<Integer> ret) {
		this.getIntegersInvocationsCount++;
	}

	@AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
	public void incrementGetNumbersInvocationsCount(Collection<? extends Number> ret) {
		this.getNumbersInvocationsCount++;
	}

	@AfterReturning(pointcut = "anyTestMethod()", returning = "ret")
	public void incrementTestBeanInvocationsCount(Collection<? super TestBean> ret) {
		this.getTestBeanInvocationsCount++;
	}

	public void reset() {
		this.getRawsInvocationsCount = 0;
		this.getStringsInvocationsCount = 0;
		this.getIntegersInvocationsCount = 0;
		this.getNumbersInvocationsCount = 0;
		this.getTestBeanInvocationsCount = 0;
	}
}

