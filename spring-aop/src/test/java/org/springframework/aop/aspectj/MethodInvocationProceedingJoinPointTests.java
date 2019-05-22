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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.JoinPoint.StaticPart;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.reflect.Factory;
import org.junit.Test;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.lang.Nullable;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.*;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotSame;
import static temp.XAssert.assertSame;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @author Ramnivas Laddad
 * @since 2.0
 */
public class MethodInvocationProceedingJoinPointTests {

	@Test
	public void testingBindingWithJoinPoint() {
		assertThatIllegalStateException().isThrownBy(
				AbstractAspectJAdvice::currentJoinPoint);
	}

	@Test
	public void testingBindingWithProceedingJoinPoint() {
		assertThatIllegalStateException().isThrownBy(
				AbstractAspectJAdvice::currentJoinPoint);
	}

	@Test
	public void testCanGetMethodSignatureFromJoinPoint() {
		final Object raw = new TestBean();
		// Will be set by advice during a method call
		final int newAge = 23;

		ProxyFactory pf = new ProxyFactory(raw);
		pf.setExposeProxy(true);
		pf.addAdvisor(ExposeInvocationInterceptor.ADVISOR);
		pf.addAdvice(new MethodBeforeAdvice() {
			private int depth;

			@Override
			public void before(Method method, Object[] args, @Nullable Object target) throws Throwable {
				JoinPoint jp = AbstractAspectJAdvice.currentJoinPoint();
				assertThat(jp.toString().contains(method.getName())).as("Method named in toString").isTrue();
				// Ensure that these don't cause problems
				jp.toShortString();
				jp.toLongString();

				assertSame(target, AbstractAspectJAdvice.currentJoinPoint().getTarget());
				assertThat(AopUtils.isAopProxy(AbstractAspectJAdvice.currentJoinPoint().getTarget())).isFalse();

				ITestBean thisProxy = (ITestBean) AbstractAspectJAdvice.currentJoinPoint().getThis();
				assertThat(AopUtils.isAopProxy(AbstractAspectJAdvice.currentJoinPoint().getThis())).isTrue();

				assertNotSame(target, thisProxy);

				// Check getting again doesn't cause a problem
				assertSame(thisProxy, AbstractAspectJAdvice.currentJoinPoint().getThis());

				// Try reentrant call--will go through this advice.
				// Be sure to increment depth to avoid infinite recursion
				if (depth++ == 0) {
					// Check that toString doesn't cause a problem
					thisProxy.toString();
					// Change age, so this will be returned by invocation
					thisProxy.setAge(newAge);
					assertEquals(newAge, thisProxy.getAge());
				}

				assertSame(AopContext.currentProxy(), thisProxy);
				assertSame(target, raw);

				assertSame(method.getName(), AbstractAspectJAdvice.currentJoinPoint().getSignature().getName());
				assertEquals(method.getModifiers(), AbstractAspectJAdvice.currentJoinPoint().getSignature().getModifiers());

				MethodSignature msig = (MethodSignature) AbstractAspectJAdvice.currentJoinPoint().getSignature();
				assertSame("Return same MethodSignature repeatedly", msig, AbstractAspectJAdvice.currentJoinPoint().getSignature());
				assertSame("Return same JoinPoint repeatedly", AbstractAspectJAdvice.currentJoinPoint(), AbstractAspectJAdvice.currentJoinPoint());
				assertThat(msig.getDeclaringType()).isEqualTo(method.getDeclaringClass());
				assertThat(Arrays.equals(method.getParameterTypes(), msig.getParameterTypes())).isTrue();
				assertThat(msig.getReturnType()).isEqualTo(method.getReturnType());
				assertThat(Arrays.equals(method.getExceptionTypes(), msig.getExceptionTypes())).isTrue();
				msig.toLongString();
				msig.toShortString();
			}
		});
		ITestBean itb = (ITestBean) pf.getProxy();
		// Any call will do
		assertEquals("Advice reentrantly set age", newAge, itb.getAge());
	}

	@Test
	public void testCanGetSourceLocationFromJoinPoint() {
		final Object raw = new TestBean();
		ProxyFactory pf = new ProxyFactory(raw);
		pf.addAdvisor(ExposeInvocationInterceptor.ADVISOR);
		pf.addAdvice(new MethodBeforeAdvice() {
			@Override
			public void before(Method method, Object[] args, @Nullable Object target) throws Throwable {
				SourceLocation sloc = AbstractAspectJAdvice.currentJoinPoint().getSourceLocation();
				assertThat((Object) AbstractAspectJAdvice.currentJoinPoint().getSourceLocation()).as("Same source location must be returned on subsequent requests").isEqualTo(sloc);
				assertThat(sloc.getWithinType()).isEqualTo(TestBean.class);
				assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(sloc::getLine);
				assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(sloc::getFileName);
			}
		});
		ITestBean itb = (ITestBean) pf.getProxy();
		// Any call will do
		itb.getAge();
	}

	@Test
	public void testCanGetStaticPartFromJoinPoint() {
		final Object raw = new TestBean();
		ProxyFactory pf = new ProxyFactory(raw);
		pf.addAdvisor(ExposeInvocationInterceptor.ADVISOR);
		pf.addAdvice(new MethodBeforeAdvice() {
			@Override
			public void before(Method method, Object[] args, @Nullable Object target) throws Throwable {
				StaticPart staticPart = AbstractAspectJAdvice.currentJoinPoint().getStaticPart();
				assertThat((Object) AbstractAspectJAdvice.currentJoinPoint().getStaticPart()).as("Same static part must be returned on subsequent requests").isEqualTo(staticPart);
				assertThat((Object) staticPart.getKind()).isEqualTo(ProceedingJoinPoint.METHOD_EXECUTION);
				assertSame(AbstractAspectJAdvice.currentJoinPoint().getSignature(), staticPart.getSignature());
				assertThat((Object) staticPart.getSourceLocation()).isEqualTo(AbstractAspectJAdvice.currentJoinPoint().getSourceLocation());
			}
		});
		ITestBean itb = (ITestBean) pf.getProxy();
		// Any call will do
		itb.getAge();
	}

	@Test
	public void toShortAndLongStringFormedCorrectly() throws Exception {
		final Object raw = new TestBean();
		ProxyFactory pf = new ProxyFactory(raw);
		pf.addAdvisor(ExposeInvocationInterceptor.ADVISOR);
		pf.addAdvice(new MethodBeforeAdvice() {
			@Override
			public void before(Method method, Object[] args, @Nullable Object target) throws Throwable {
				// makeEncSJP, although meant for computing the enclosing join point,
				// it serves our purpose here
				JoinPoint.StaticPart aspectJVersionJp = Factory.makeEncSJP(method);
				JoinPoint jp = AbstractAspectJAdvice.currentJoinPoint();

				assertThat((Object) jp.getSignature().toLongString()).isEqualTo(aspectJVersionJp.getSignature().toLongString());
				assertThat((Object) jp.getSignature().toShortString()).isEqualTo(aspectJVersionJp.getSignature().toShortString());
				assertThat((Object) jp.getSignature().toString()).isEqualTo(aspectJVersionJp.getSignature().toString());

				assertThat((Object) jp.toLongString()).isEqualTo(aspectJVersionJp.toLongString());
				assertThat((Object) jp.toShortString()).isEqualTo(aspectJVersionJp.toShortString());
				assertThat((Object) jp.toString()).isEqualTo(aspectJVersionJp.toString());
			}
		});
		ITestBean itb = (ITestBean) pf.getProxy();
		itb.getAge();
		itb.setName("foo");
		itb.getDoctor();
		itb.getStringArray();
		itb.getSpouse();
		itb.setSpouse(new TestBean());
		try {
			itb.unreliableFileOperation();
		}
		catch (IOException ex) {
			// we don't really care...
		}
	}

}
