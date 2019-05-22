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

package org.springframework.aop.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.context.SimpleMapScope;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ScopedProxyTests {

	private static final Class<?> CLASS = ScopedProxyTests.class;
	private static final String CLASSNAME = CLASS.getSimpleName();

	private static final ClassPathResource LIST_CONTEXT = new ClassPathResource(CLASSNAME + "-list.xml", CLASS);
	private static final ClassPathResource MAP_CONTEXT = new ClassPathResource(CLASSNAME + "-map.xml", CLASS);
	private static final ClassPathResource OVERRIDE_CONTEXT = new ClassPathResource(CLASSNAME + "-override.xml", CLASS);
	private static final ClassPathResource TESTBEAN_CONTEXT = new ClassPathResource(CLASSNAME + "-testbean.xml", CLASS);


	@Test  // SPR-2108
	public void testProxyAssignable() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(MAP_CONTEXT);
		Object baseMap = bf.getBean("singletonMap");
		boolean condition = baseMap instanceof Map;
		assertThat(condition).isTrue();
	}

	@Test
	public void testSimpleProxy() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(MAP_CONTEXT);
		Object simpleMap = bf.getBean("simpleMap");
		boolean condition1 = simpleMap instanceof Map;
		assertThat(condition1).isTrue();
		boolean condition = simpleMap instanceof HashMap;
		assertThat(condition).isTrue();
	}

	@Test
	public void testScopedOverride() throws Exception {
		GenericApplicationContext ctx = new GenericApplicationContext();
		new XmlBeanDefinitionReader(ctx).loadBeanDefinitions(OVERRIDE_CONTEXT);
		SimpleMapScope scope = new SimpleMapScope();
		ctx.getBeanFactory().registerScope("request", scope);
		ctx.refresh();

		ITestBean bean = (ITestBean) ctx.getBean("testBean");
		assertEquals("male", bean.getName());
		assertEquals(99, bean.getAge());

		assertThat(scope.getMap().containsKey("scopedTarget.testBean")).isTrue();
		assertEquals(TestBean.class, scope.getMap().get("scopedTarget.testBean").getClass());
	}

	@Test
	public void testJdkScopedProxy() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(TESTBEAN_CONTEXT);
		bf.setSerializationId("X");
		SimpleMapScope scope = new SimpleMapScope();
		bf.registerScope("request", scope);

		ITestBean bean = (ITestBean) bf.getBean("testBean");
		assertNotNull(bean);
		assertThat(AopUtils.isJdkDynamicProxy(bean)).isTrue();
		boolean condition1 = bean instanceof ScopedObject;
		assertThat(condition1).isTrue();
		ScopedObject scoped = (ScopedObject) bean;
		assertEquals(TestBean.class, scoped.getTargetObject().getClass());
		bean.setAge(101);

		assertThat(scope.getMap().containsKey("testBeanTarget")).isTrue();
		assertEquals(TestBean.class, scope.getMap().get("testBeanTarget").getClass());

		ITestBean deserialized = (ITestBean) SerializationTestUtils.serializeAndDeserialize(bean);
		assertNotNull(deserialized);
		assertThat(AopUtils.isJdkDynamicProxy(deserialized)).isTrue();
		assertEquals(101, bean.getAge());
		boolean condition = deserialized instanceof ScopedObject;
		assertThat(condition).isTrue();
		ScopedObject scopedDeserialized = (ScopedObject) deserialized;
		assertEquals(TestBean.class, scopedDeserialized.getTargetObject().getClass());

		bf.setSerializationId(null);
	}

	@Test
	public void testCglibScopedProxy() throws Exception {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(LIST_CONTEXT);
		bf.setSerializationId("Y");
		SimpleMapScope scope = new SimpleMapScope();
		bf.registerScope("request", scope);

		TestBean tb = (TestBean) bf.getBean("testBean");
		assertThat(AopUtils.isCglibProxy(tb.getFriends())).isTrue();
		boolean condition1 = tb.getFriends() instanceof ScopedObject;
		assertThat(condition1).isTrue();
		ScopedObject scoped = (ScopedObject) tb.getFriends();
		assertEquals(ArrayList.class, scoped.getTargetObject().getClass());
		tb.getFriends().add("myFriend");

		assertThat(scope.getMap().containsKey("scopedTarget.scopedList")).isTrue();
		assertEquals(ArrayList.class, scope.getMap().get("scopedTarget.scopedList").getClass());

		ArrayList<?> deserialized = (ArrayList<?>) SerializationTestUtils.serializeAndDeserialize(tb.getFriends());
		assertNotNull(deserialized);
		assertThat(AopUtils.isCglibProxy(deserialized)).isTrue();
		assertThat(deserialized.contains("myFriend")).isTrue();
		boolean condition = deserialized instanceof ScopedObject;
		assertThat(condition).isTrue();
		ScopedObject scopedDeserialized = (ScopedObject) deserialized;
		assertEquals(ArrayList.class, scopedDeserialized.getTargetObject().getClass());

		bf.setSerializationId(null);
	}

}
