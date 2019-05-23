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

package org.springframework.test.util;

import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static temp.XAssert.assertTrue;
import static org.springframework.test.util.AopTestUtils.getTargetObject;
import static org.springframework.test.util.AopTestUtils.getUltimateTargetObject;

/**
 * Unit tests for {@link AopTestUtils}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
public class AopTestUtilsTests {

	private final FooImpl foo = new FooImpl();


	@Test
	public void getTargetObjectForNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				getTargetObject(null));
	}

	@Test
	public void getTargetObjectForNonProxiedObject() {
		Foo target = getTargetObject(foo);
		assertThat((Object) target).isSameAs(foo);
	}

	@Test
	public void getTargetObjectWrappedInSingleJdkDynamicProxy() {
		Foo target = getTargetObject(jdkProxy(foo));
		assertThat((Object) target).isSameAs(foo);
	}

	@Test
	public void getTargetObjectWrappedInSingleCglibProxy() {
		Foo target = getTargetObject(cglibProxy(foo));
		assertThat((Object) target).isSameAs(foo);
	}

	@Test
	public void getTargetObjectWrappedInDoubleJdkDynamicProxy() {
		Foo target = getTargetObject(jdkProxy(jdkProxy(foo)));
		assertThat((Object) target).isNotSameAs(foo);
	}

	@Test
	public void getTargetObjectWrappedInDoubleCglibProxy() {
		Foo target = getTargetObject(cglibProxy(cglibProxy(foo)));
		assertThat((Object) target).isNotSameAs(foo);
	}

	@Test
	public void getUltimateTargetObjectForNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				getUltimateTargetObject(null));
	}

	@Test
	public void getUltimateTargetObjectForNonProxiedObject() {
		Foo target = getUltimateTargetObject(foo);
		assertThat((Object) target).isSameAs(foo);
	}

	@Test
	public void getUltimateTargetObjectWrappedInSingleJdkDynamicProxy() {
		Foo target = getUltimateTargetObject(jdkProxy(foo));
		assertThat((Object) target).isSameAs(foo);
	}

	@Test
	public void getUltimateTargetObjectWrappedInSingleCglibProxy() {
		Foo target = getUltimateTargetObject(cglibProxy(foo));
		assertThat((Object) target).isSameAs(foo);
	}

	@Test
	public void getUltimateTargetObjectWrappedInDoubleJdkDynamicProxy() {
		Foo target = getUltimateTargetObject(jdkProxy(jdkProxy(foo)));
		assertThat((Object) target).isSameAs(foo);
	}

	@Test
	public void getUltimateTargetObjectWrappedInDoubleCglibProxy() {
		Foo target = getUltimateTargetObject(cglibProxy(cglibProxy(foo)));
		assertThat((Object) target).isSameAs(foo);
	}

	@Test
	public void getUltimateTargetObjectWrappedInCglibProxyWrappedInJdkDynamicProxy() {
		Foo target = getUltimateTargetObject(jdkProxy(cglibProxy(foo)));
		assertThat((Object) target).isSameAs(foo);
	}

	@Test
	public void getUltimateTargetObjectWrappedInCglibProxyWrappedInDoubleJdkDynamicProxy() {
		Foo target = getUltimateTargetObject(jdkProxy(jdkProxy(cglibProxy(foo))));
		assertThat((Object) target).isSameAs(foo);
	}

	private Foo jdkProxy(Foo foo) {
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(foo);
		pf.addInterface(Foo.class);
		Foo proxy = (Foo) pf.getProxy();
		assertTrue("Proxy is a JDK dynamic proxy", AopUtils.isJdkDynamicProxy(proxy));
		assertThat(proxy).isInstanceOf(Foo.class);
		return proxy;
	}

	private Foo cglibProxy(Foo foo) {
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(foo);
		pf.setProxyTargetClass(true);
		Foo proxy = (Foo) pf.getProxy();
		assertTrue("Proxy is a CGLIB proxy", AopUtils.isCglibProxy(proxy));
		assertThat(proxy).isInstanceOf(FooImpl.class);
		return proxy;
	}


	static interface Foo {
	}

	static class FooImpl implements Foo {
	}

}
