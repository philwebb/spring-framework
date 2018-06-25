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

package org.springframework.aop.aspectj;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.aspectj.AfterReturningAdviceBindingTestAspect.AfterReturningAdviceBindingCollaborator;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class AfterReturningAdviceBindingTests {

	private AfterReturningAdviceBindingTestAspect afterAdviceAspect;

	private ITestBean testBeanProxy;

	private TestBean testBeanTarget;

	private AfterReturningAdviceBindingCollaborator mockCollaborator;


	public void setAfterReturningAdviceAspect(AfterReturningAdviceBindingTestAspect anAspect) {
		this.afterAdviceAspect = anAspect;
	}

	@Before
	public void setUp() throws Exception {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

		this.afterAdviceAspect = (AfterReturningAdviceBindingTestAspect) ctx.getBean("testAspect");

		this.mockCollaborator = mock(AfterReturningAdviceBindingCollaborator.class);
		this.afterAdviceAspect.setCollaborator(this.mockCollaborator);

		this.testBeanProxy = (ITestBean) ctx.getBean("testBean");
		assertTrue(AopUtils.isAopProxy(this.testBeanProxy));

		// we need the real target too, not just the proxy...
		this.testBeanTarget = (TestBean) ((Advised)this.testBeanProxy).getTargetSource().getTarget();
	}


	@Test
	public void testOneIntArg() {
		this.testBeanProxy.setAge(5);
		verify(this.mockCollaborator).oneIntArg(5);
	}

	@Test
	public void testOneObjectArg() {
		this.testBeanProxy.getAge();
		verify(this.mockCollaborator).oneObjectArg(this.testBeanProxy);
	}

	@Test
	public void testOneIntAndOneObjectArgs() {
		this.testBeanProxy.setAge(5);
		verify(this.mockCollaborator).oneIntAndOneObject(5,this.testBeanProxy);
	}

	@Test
	public void testNeedsJoinPoint() {
		this.testBeanProxy.getAge();
		verify(this.mockCollaborator).needsJoinPoint("getAge");
	}

	@Test
	public void testNeedsJoinPointStaticPart() {
		this.testBeanProxy.getAge();
		verify(this.mockCollaborator).needsJoinPointStaticPart("getAge");
	}

	@Test
	public void testReturningString() {
		this.testBeanProxy.setName("adrian");
		this.testBeanProxy.getName();
		verify(this.mockCollaborator).oneString("adrian");
	}

	@Test
	public void testReturningObject() {
		this.testBeanProxy.returnsThis();
		verify(this.mockCollaborator).oneObjectArg(this.testBeanTarget);
	}

	@Test
	public void testReturningBean() {
		this.testBeanProxy.returnsThis();
		verify(this.mockCollaborator).oneTestBeanArg(this.testBeanTarget);
	}

	@Test
	public void testReturningBeanArray() {
		this.testBeanTarget.setSpouse(new TestBean());
		ITestBean[] spouses = this.testBeanTarget.getSpouses();
		this.testBeanProxy.getSpouses();
		verify(this.mockCollaborator).testBeanArrayArg(spouses);
	}

	@Test
	public void testNoInvokeWhenReturningParameterTypeDoesNotMatch() {
		this.testBeanProxy.setSpouse(this.testBeanProxy);
		this.testBeanProxy.getSpouse();
		verifyZeroInteractions(this.mockCollaborator);
	}

	@Test
	public void testReturningByType() {
		this.testBeanProxy.returnsThis();
		verify(this.mockCollaborator).objectMatchNoArgs();
	}

	@Test
	public void testReturningPrimitive() {
		this.testBeanProxy.setAge(20);
		this.testBeanProxy.haveBirthday();
		verify(this.mockCollaborator).oneInt(20);
	}

}


final class AfterReturningAdviceBindingTestAspect extends AdviceBindingTestAspect {

	private AfterReturningAdviceBindingCollaborator getCollaborator() {
		return (AfterReturningAdviceBindingCollaborator) this.collaborator;
	}

	public void oneString(String name) {
		getCollaborator().oneString(name);
	}

	public void oneTestBeanArg(TestBean bean) {
		getCollaborator().oneTestBeanArg(bean);
	}

	public void testBeanArrayArg(ITestBean[] beans) {
		getCollaborator().testBeanArrayArg(beans);
	}

	public void objectMatchNoArgs() {
		getCollaborator().objectMatchNoArgs();
	}

	public void stringMatchNoArgs() {
		getCollaborator().stringMatchNoArgs();
	}

	public void oneInt(int result) {
		getCollaborator().oneInt(result);
	}

	interface AfterReturningAdviceBindingCollaborator extends AdviceBindingCollaborator {

		void oneString(String s);
		void oneTestBeanArg(TestBean b);
		void testBeanArrayArg(ITestBean[] b);
		void objectMatchNoArgs();
		void stringMatchNoArgs();
		void oneInt(int result);
	}

}
