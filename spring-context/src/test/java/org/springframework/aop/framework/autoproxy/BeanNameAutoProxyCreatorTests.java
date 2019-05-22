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

package org.springframework.aop.framework.autoproxy;

import org.junit.Before;
import org.junit.Test;
import test.mixin.Lockable;
import test.mixin.LockedException;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.TimeStamped;
import org.springframework.tests.aop.advice.CountingBeforeAdvice;
import org.springframework.tests.aop.interceptor.NopInterceptor;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static temp.XAssert.assertEquals;

/**
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Chris Beams
 */
public class BeanNameAutoProxyCreatorTests {

	private BeanFactory beanFactory;


	@Before
	public void setup() {
		// Note that we need an ApplicationContext, not just a BeanFactory,
		// for post-processing and hence auto-proxying to work.
		beanFactory = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
	}


	@Test
	public void testNoProxy() {
		TestBean tb = (TestBean) beanFactory.getBean("noproxy");
		assertThat(AopUtils.isAopProxy(tb)).isFalse();
		assertThat((Object) tb.getName()).isEqualTo("noproxy");
	}

	@Test
	public void testJdkProxyWithExactNameMatch() {
		ITestBean tb = (ITestBean) beanFactory.getBean("onlyJdk");
		jdkAssertions(tb, 1);
		assertThat((Object) tb.getName()).isEqualTo("onlyJdk");
	}

	@Test
	public void testJdkProxyWithDoubleProxying() {
		ITestBean tb = (ITestBean) beanFactory.getBean("doubleJdk");
		jdkAssertions(tb, 2);
		assertThat((Object) tb.getName()).isEqualTo("doubleJdk");
	}

	@Test
	public void testJdkIntroduction() {
		ITestBean tb = (ITestBean) beanFactory.getBean("introductionUsingJdk");
		NopInterceptor nop = (NopInterceptor) beanFactory.getBean("introductionNopInterceptor");
		assertEquals(0, nop.getCount());
		assertThat(AopUtils.isJdkDynamicProxy(tb)).isTrue();
		int age = 5;
		tb.setAge(age);
		assertEquals(age, tb.getAge());
		boolean condition = tb instanceof TimeStamped;
		assertThat(condition).as("Introduction was made").isTrue();
		assertEquals(0, ((TimeStamped) tb).getTimeStamp());
		assertEquals(3, nop.getCount());
		assertThat((Object) tb.getName()).isEqualTo("introductionUsingJdk");

		ITestBean tb2 = (ITestBean) beanFactory.getBean("second-introductionUsingJdk");

		// Check two per-instance mixins were distinct
		Lockable lockable1 = (Lockable) tb;
		Lockable lockable2 = (Lockable) tb2;
		assertThat(lockable1.locked()).isFalse();
		assertThat(lockable2.locked()).isFalse();
		tb.setAge(65);
		assertEquals(65, tb.getAge());
		lockable1.lock();
		assertThat(lockable1.locked()).isTrue();
		// Shouldn't affect second
		assertThat(lockable2.locked()).isFalse();
		// Can still mod second object
		tb2.setAge(12);
		// But can't mod first
		assertThatExceptionOfType(LockedException.class).as("mixin should have locked this object").isThrownBy(() ->
				tb.setAge(6));
	}

	@Test
	public void testJdkIntroductionAppliesToCreatedObjectsNotFactoryBean() {
		ITestBean tb = (ITestBean) beanFactory.getBean("factory-introductionUsingJdk");
		NopInterceptor nop = (NopInterceptor) beanFactory.getBean("introductionNopInterceptor");
		assertEquals("NOP should not have done any work yet", 0, nop.getCount());
		assertThat(AopUtils.isJdkDynamicProxy(tb)).isTrue();
		int age = 5;
		tb.setAge(age);
		assertEquals(age, tb.getAge());
		boolean condition = tb instanceof TimeStamped;
		assertThat(condition).as("Introduction was made").isTrue();
		assertEquals(0, ((TimeStamped) tb).getTimeStamp());
		assertEquals(3, nop.getCount());

		ITestBean tb2 = (ITestBean) beanFactory.getBean("second-introductionUsingJdk");

		// Check two per-instance mixins were distinct
		Lockable lockable1 = (Lockable) tb;
		Lockable lockable2 = (Lockable) tb2;
		assertThat(lockable1.locked()).isFalse();
		assertThat(lockable2.locked()).isFalse();
		tb.setAge(65);
		assertEquals(65, tb.getAge());
		lockable1.lock();
		assertThat(lockable1.locked()).isTrue();
		// Shouldn't affect second
		assertThat(lockable2.locked()).isFalse();
		// Can still mod second object
		tb2.setAge(12);
		// But can't mod first
		assertThatExceptionOfType(LockedException.class).as("mixin should have locked this object").isThrownBy(() ->
				tb.setAge(6));
	}

	@Test
	public void testJdkProxyWithWildcardMatch() {
		ITestBean tb = (ITestBean) beanFactory.getBean("jdk1");
		jdkAssertions(tb, 1);
		assertThat((Object) tb.getName()).isEqualTo("jdk1");
	}

	@Test
	public void testCglibProxyWithWildcardMatch() {
		TestBean tb = (TestBean) beanFactory.getBean("cglib1");
		cglibAssertions(tb);
		assertThat((Object) tb.getName()).isEqualTo("cglib1");
	}

	@Test
	public void testWithFrozenProxy() {
		ITestBean testBean = (ITestBean) beanFactory.getBean("frozenBean");
		assertThat(((Advised)testBean).isFrozen()).isTrue();
	}


	private void jdkAssertions(ITestBean tb, int nopInterceptorCount)  {
		NopInterceptor nop = (NopInterceptor) beanFactory.getBean("nopInterceptor");
		assertEquals(0, nop.getCount());
		assertThat(AopUtils.isJdkDynamicProxy(tb)).isTrue();
		int age = 5;
		tb.setAge(age);
		assertEquals(age, tb.getAge());
		assertEquals(2 * nopInterceptorCount, nop.getCount());
	}

	/**
	 * Also has counting before advice.
	 */
	private void cglibAssertions(TestBean tb) {
		CountingBeforeAdvice cba = (CountingBeforeAdvice) beanFactory.getBean("countingBeforeAdvice");
		NopInterceptor nop = (NopInterceptor) beanFactory.getBean("nopInterceptor");
		assertEquals(0, cba.getCalls());
		assertEquals(0, nop.getCount());
		assertThat(AopUtils.isCglibProxy(tb)).isTrue();
		int age = 5;
		tb.setAge(age);
		assertEquals(age, tb.getAge());
		assertEquals(2, nop.getCount());
		assertEquals(2, cba.getCalls());
	}

}


class CreatesTestBean implements FactoryBean<Object> {

	/**
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	@Override
	public Object getObject() throws Exception {
		return new TestBean();
	}

	/**
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {
		return TestBean.class;
	}

	/**
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return true;
	}

}
