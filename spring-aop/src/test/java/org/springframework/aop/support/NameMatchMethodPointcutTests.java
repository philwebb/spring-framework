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

package org.springframework.aop.support;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.tests.aop.interceptor.NopInterceptor;
import org.springframework.tests.aop.interceptor.SerializableNopInterceptor;
import org.springframework.tests.sample.beans.Person;
import org.springframework.tests.sample.beans.SerializablePerson;
import org.springframework.util.SerializationTestUtils;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class NameMatchMethodPointcutTests {

	protected NameMatchMethodPointcut pc;

	protected Person proxied;

	protected SerializableNopInterceptor nop;

	/**
	 * Create an empty pointcut, populating instance variables.
	 */
	@Before
	public void setUp() {
		ProxyFactory pf = new ProxyFactory(new SerializablePerson());
		this.nop = new SerializableNopInterceptor();
		this.pc = new NameMatchMethodPointcut();
		pf.addAdvisor(new DefaultPointcutAdvisor(this.pc, this.nop));
		this.proxied = (Person) pf.getProxy();
	}

	@Test
	public void testMatchingOnly() {
		// Can't do exact matching through isMatch
		assertTrue(this.pc.isMatch("echo", "ech*"));
		assertTrue(this.pc.isMatch("setName", "setN*"));
		assertTrue(this.pc.isMatch("setName", "set*"));
		assertFalse(this.pc.isMatch("getName", "set*"));
		assertFalse(this.pc.isMatch("setName", "set"));
		assertTrue(this.pc.isMatch("testing", "*ing"));
	}

	@Test
	public void testEmpty() throws Throwable {
		assertEquals(0, this.nop.getCount());
		this.proxied.getName();
		this.proxied.setName("");
		this.proxied.echo(null);
		assertEquals(0, this.nop.getCount());
	}


	@Test
	public void testMatchOneMethod() throws Throwable {
		this.pc.addMethodName("echo");
		this.pc.addMethodName("set*");
		assertEquals(0, this.nop.getCount());
		this.proxied.getName();
		this.proxied.getName();
		assertEquals(0, this.nop.getCount());
		this.proxied.echo(null);
		assertEquals(1, this.nop.getCount());

		this.proxied.setName("");
		assertEquals(2, this.nop.getCount());
		this.proxied.setAge(25);
		assertEquals(25, this.proxied.getAge());
		assertEquals(3, this.nop.getCount());
	}

	@Test
	public void testSets() throws Throwable {
		this.pc.setMappedNames(new String[] { "set*", "echo" });
		assertEquals(0, this.nop.getCount());
		this.proxied.getName();
		this.proxied.setName("");
		assertEquals(1, this.nop.getCount());
		this.proxied.echo(null);
		assertEquals(2, this.nop.getCount());
	}

	@Test
	public void testSerializable() throws Throwable {
		testSets();
		// Count is now 2
		Person p2 = (Person) SerializationTestUtils.serializeAndDeserialize(this.proxied);
		NopInterceptor nop2 = (NopInterceptor) ((Advised) p2).getAdvisors()[0].getAdvice();
		p2.getName();
		assertEquals(2, nop2.getCount());
		p2.echo(null);
		assertEquals(3, nop2.getCount());
	}

	@Test
	public void testEqualsAndHashCode() throws Exception {
		NameMatchMethodPointcut pc1 = new NameMatchMethodPointcut();
		NameMatchMethodPointcut pc2 = new NameMatchMethodPointcut();

		String foo = "foo";

		assertEquals(pc1, pc2);
		assertEquals(pc1.hashCode(), pc2.hashCode());

		pc1.setMappedName(foo);
		assertFalse(pc1.equals(pc2));
		assertTrue(pc1.hashCode() != pc2.hashCode());

		pc2.setMappedName(foo);
		assertEquals(pc1, pc2);
		assertEquals(pc1.hashCode(), pc2.hashCode());
	}

}
