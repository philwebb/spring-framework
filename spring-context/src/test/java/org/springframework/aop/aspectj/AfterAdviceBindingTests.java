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

package org.springframework.aop.aspectj;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.aspectj.AdviceBindingTestAspect.AdviceBindingCollaborator;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Chris Beams
 */
public class AfterAdviceBindingTests {

	private AdviceBindingCollaborator mockCollaborator;

	private ITestBean testBeanProxy;

	private TestBean testBeanTarget;

	@Before
	public void setUp() throws Exception {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		AdviceBindingTestAspect afterAdviceAspect = (AdviceBindingTestAspect) ctx.getBean("testAspect");

		this.testBeanProxy = (ITestBean) ctx.getBean("testBean");
		assertTrue(AopUtils.isAopProxy(this.testBeanProxy));

		// we need the real target too, not just the proxy...
		this.testBeanTarget = (TestBean) ((Advised) this.testBeanProxy).getTargetSource().getTarget();

		this.mockCollaborator = mock(AdviceBindingCollaborator.class);
		afterAdviceAspect.setCollaborator(this.mockCollaborator);
	}

	@Test
	public void testOneIntArg() {
		this.testBeanProxy.setAge(5);
		verify(this.mockCollaborator).oneIntArg(5);
	}

	@Test
	public void testOneObjectArgBindingProxyWithThis() {
		this.testBeanProxy.getAge();
		verify(this.mockCollaborator).oneObjectArg(this.testBeanProxy);
	}

	@Test
	public void testOneObjectArgBindingTarget() {
		this.testBeanProxy.getDoctor();
		verify(this.mockCollaborator).oneObjectArg(this.testBeanTarget);
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

}
