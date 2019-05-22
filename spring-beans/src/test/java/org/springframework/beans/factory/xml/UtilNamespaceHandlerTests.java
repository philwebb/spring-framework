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

package org.springframework.beans.factory.xml;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.FieldRetrievingFactoryBean;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.beans.CollectingReaderEventListener;
import org.springframework.tests.sample.beans.CustomEnum;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.LinkedCaseInsensitiveMap;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 */
@SuppressWarnings("rawtypes")
public class UtilNamespaceHandlerTests {

	private DefaultListableBeanFactory beanFactory;

	private CollectingReaderEventListener listener = new CollectingReaderEventListener();


	@Before
	public void setUp() {
		this.beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.setEventListener(this.listener);
		reader.loadBeanDefinitions(new ClassPathResource("testUtilNamespace.xml", getClass()));
	}


	@Test
	public void testConstant() {
		Integer min = (Integer) this.beanFactory.getBean("min");
		assertEquals(Integer.MIN_VALUE, min.intValue());
	}

	@Test
	public void testConstantWithDefaultName() {
		Integer max = (Integer) this.beanFactory.getBean("java.lang.Integer.MAX_VALUE");
		assertEquals(Integer.MAX_VALUE, max.intValue());
	}

	@Test
	public void testEvents() {
		ComponentDefinition propertiesComponent = this.listener.getComponentDefinition("myProperties");
		assertNotNull("Event for 'myProperties' not sent", propertiesComponent);
		AbstractBeanDefinition propertiesBean = (AbstractBeanDefinition) propertiesComponent.getBeanDefinitions()[0];
		assertEquals("Incorrect BeanDefinition", PropertiesFactoryBean.class, propertiesBean.getBeanClass());

		ComponentDefinition constantComponent = this.listener.getComponentDefinition("min");
		assertNotNull("Event for 'min' not sent", propertiesComponent);
		AbstractBeanDefinition constantBean = (AbstractBeanDefinition) constantComponent.getBeanDefinitions()[0];
		assertEquals("Incorrect BeanDefinition", FieldRetrievingFactoryBean.class, constantBean.getBeanClass());
	}

	@Test
	public void testNestedProperties() {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		Properties props = bean.getSomeProperties();
		assertEquals("Incorrect property value", "bar", props.get("foo"));
	}

	@Test
	public void testPropertyPath() {
		String name = (String) this.beanFactory.getBean("name");
		assertEquals("Rob Harrop", name);
	}

	@Test
	public void testNestedPropertyPath() {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		assertEquals("Rob Harrop", bean.getName());
	}

	@Test
	public void testSimpleMap() {
		Map map = (Map) this.beanFactory.getBean("simpleMap");
		assertEquals("bar", map.get("foo"));
		Map map2 = (Map) this.beanFactory.getBean("simpleMap");
		assertThat(map == map2).isTrue();
	}

	@Test
	public void testScopedMap() {
		Map map = (Map) this.beanFactory.getBean("scopedMap");
		assertEquals("bar", map.get("foo"));
		Map map2 = (Map) this.beanFactory.getBean("scopedMap");
		assertEquals("bar", map2.get("foo"));
		assertThat(map != map2).isTrue();
	}

	@Test
	public void testSimpleList() {
		List list = (List) this.beanFactory.getBean("simpleList");
		assertEquals("Rob Harrop", list.get(0));
		List list2 = (List) this.beanFactory.getBean("simpleList");
		assertThat(list == list2).isTrue();
	}

	@Test
	public void testScopedList() {
		List list = (List) this.beanFactory.getBean("scopedList");
		assertEquals("Rob Harrop", list.get(0));
		List list2 = (List) this.beanFactory.getBean("scopedList");
		assertEquals("Rob Harrop", list2.get(0));
		assertThat(list != list2).isTrue();
	}

	@Test
	public void testSimpleSet() {
		Set set = (Set) this.beanFactory.getBean("simpleSet");
		assertThat(set.contains("Rob Harrop")).isTrue();
		Set set2 = (Set) this.beanFactory.getBean("simpleSet");
		assertThat(set == set2).isTrue();
	}

	@Test
	public void testScopedSet() {
		Set set = (Set) this.beanFactory.getBean("scopedSet");
		assertThat(set.contains("Rob Harrop")).isTrue();
		Set set2 = (Set) this.beanFactory.getBean("scopedSet");
		assertThat(set2.contains("Rob Harrop")).isTrue();
		assertThat(set != set2).isTrue();
	}

	@Test
	public void testMapWithRef() {
		Map map = (Map) this.beanFactory.getBean("mapWithRef");
		boolean condition = map instanceof TreeMap;
		assertThat(condition).isTrue();
		assertEquals(this.beanFactory.getBean("testBean"), map.get("bean"));
	}

	@Test
	public void testMapWithTypes() {
		Map map = (Map) this.beanFactory.getBean("mapWithTypes");
		boolean condition = map instanceof LinkedCaseInsensitiveMap;
		assertThat(condition).isTrue();
		assertEquals(this.beanFactory.getBean("testBean"), map.get("bean"));
	}

	@Test
	public void testNestedCollections() {
		TestBean bean = (TestBean) this.beanFactory.getBean("nestedCollectionsBean");

		List list = bean.getSomeList();
		assertEquals(1, list.size());
		assertEquals("foo", list.get(0));

		Set set = bean.getSomeSet();
		assertEquals(1, set.size());
		assertThat(set.contains("bar")).isTrue();

		Map map = bean.getSomeMap();
		assertEquals(1, map.size());
		boolean condition = map.get("foo") instanceof Set;
		assertThat(condition).isTrue();
		Set innerSet = (Set) map.get("foo");
		assertEquals(1, innerSet.size());
		assertThat(innerSet.contains("bar")).isTrue();

		TestBean bean2 = (TestBean) this.beanFactory.getBean("nestedCollectionsBean");
		assertEquals(list, bean2.getSomeList());
		assertEquals(set, bean2.getSomeSet());
		assertEquals(map, bean2.getSomeMap());
		assertThat(list == bean2.getSomeList()).isFalse();
		assertThat(set == bean2.getSomeSet()).isFalse();
		assertThat(map == bean2.getSomeMap()).isFalse();
	}

	@Test
	public void testNestedShortcutCollections() {
		TestBean bean = (TestBean) this.beanFactory.getBean("nestedShortcutCollections");

		assertEquals(1, bean.getStringArray().length);
		assertEquals("fooStr", bean.getStringArray()[0]);

		List list = bean.getSomeList();
		assertEquals(1, list.size());
		assertEquals("foo", list.get(0));

		Set set = bean.getSomeSet();
		assertEquals(1, set.size());
		assertThat(set.contains("bar")).isTrue();

		TestBean bean2 = (TestBean) this.beanFactory.getBean("nestedShortcutCollections");
		assertThat(Arrays.equals(bean.getStringArray(), bean2.getStringArray())).isTrue();
		assertThat(bean.getStringArray() == bean2.getStringArray()).isFalse();
		assertEquals(list, bean2.getSomeList());
		assertEquals(set, bean2.getSomeSet());
		assertThat(list == bean2.getSomeList()).isFalse();
		assertThat(set == bean2.getSomeSet()).isFalse();
	}

	@Test
	public void testNestedInCollections() {
		TestBean bean = (TestBean) this.beanFactory.getBean("nestedCustomTagBean");

		List list = bean.getSomeList();
		assertEquals(1, list.size());
		assertEquals(Integer.MIN_VALUE, list.get(0));

		Set set = bean.getSomeSet();
		assertEquals(2, set.size());
		assertThat(set.contains(Thread.State.NEW)).isTrue();
		assertThat(set.contains(Thread.State.RUNNABLE)).isTrue();

		Map map = bean.getSomeMap();
		assertEquals(1, map.size());
		assertEquals(CustomEnum.VALUE_1, map.get("min"));

		TestBean bean2 = (TestBean) this.beanFactory.getBean("nestedCustomTagBean");
		assertEquals(list, bean2.getSomeList());
		assertEquals(set, bean2.getSomeSet());
		assertEquals(map, bean2.getSomeMap());
		assertThat(list == bean2.getSomeList()).isFalse();
		assertThat(set == bean2.getSomeSet()).isFalse();
		assertThat(map == bean2.getSomeMap()).isFalse();
	}

	@Test
	public void testCircularCollections() {
		TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionsBean");

		List list = bean.getSomeList();
		assertEquals(1, list.size());
		assertEquals(bean, list.get(0));

		Set set = bean.getSomeSet();
		assertEquals(1, set.size());
		assertThat(set.contains(bean)).isTrue();

		Map map = bean.getSomeMap();
		assertEquals(1, map.size());
		assertEquals(bean, map.get("foo"));
	}

	@Test
	public void testCircularCollectionBeansStartingWithList() {
		this.beanFactory.getBean("circularList");
		TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionBeansBean");

		List list = bean.getSomeList();
		assertThat(Proxy.isProxyClass(list.getClass())).isTrue();
		assertEquals(1, list.size());
		assertEquals(bean, list.get(0));

		Set set = bean.getSomeSet();
		assertThat(Proxy.isProxyClass(set.getClass())).isFalse();
		assertEquals(1, set.size());
		assertThat(set.contains(bean)).isTrue();

		Map map = bean.getSomeMap();
		assertThat(Proxy.isProxyClass(map.getClass())).isFalse();
		assertEquals(1, map.size());
		assertEquals(bean, map.get("foo"));
	}

	@Test
	public void testCircularCollectionBeansStartingWithSet() {
		this.beanFactory.getBean("circularSet");
		TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionBeansBean");

		List list = bean.getSomeList();
		assertThat(Proxy.isProxyClass(list.getClass())).isFalse();
		assertEquals(1, list.size());
		assertEquals(bean, list.get(0));

		Set set = bean.getSomeSet();
		assertThat(Proxy.isProxyClass(set.getClass())).isTrue();
		assertEquals(1, set.size());
		assertThat(set.contains(bean)).isTrue();

		Map map = bean.getSomeMap();
		assertThat(Proxy.isProxyClass(map.getClass())).isFalse();
		assertEquals(1, map.size());
		assertEquals(bean, map.get("foo"));
	}

	@Test
	public void testCircularCollectionBeansStartingWithMap() {
		this.beanFactory.getBean("circularMap");
		TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionBeansBean");

		List list = bean.getSomeList();
		assertThat(Proxy.isProxyClass(list.getClass())).isFalse();
		assertEquals(1, list.size());
		assertEquals(bean, list.get(0));

		Set set = bean.getSomeSet();
		assertThat(Proxy.isProxyClass(set.getClass())).isFalse();
		assertEquals(1, set.size());
		assertThat(set.contains(bean)).isTrue();

		Map map = bean.getSomeMap();
		assertThat(Proxy.isProxyClass(map.getClass())).isTrue();
		assertEquals(1, map.size());
		assertEquals(bean, map.get("foo"));
	}

	@Test
	public void testNestedInConstructor() {
		TestBean bean = (TestBean) this.beanFactory.getBean("constructedTestBean");
		assertEquals("Rob Harrop", bean.getName());
	}

	@Test
	public void testLoadProperties() {
		Properties props = (Properties) this.beanFactory.getBean("myProperties");
		assertEquals("Incorrect property value", "bar", props.get("foo"));
		assertEquals("Incorrect property value", null, props.get("foo2"));
		Properties props2 = (Properties) this.beanFactory.getBean("myProperties");
		assertThat(props == props2).isTrue();
	}

	@Test
	public void testScopedProperties() {
		Properties props = (Properties) this.beanFactory.getBean("myScopedProperties");
		assertEquals("Incorrect property value", "bar", props.get("foo"));
		assertEquals("Incorrect property value", null, props.get("foo2"));
		Properties props2 = (Properties) this.beanFactory.getBean("myScopedProperties");
		assertEquals("Incorrect property value", "bar", props.get("foo"));
		assertEquals("Incorrect property value", null, props.get("foo2"));
		assertThat(props != props2).isTrue();
	}

	@Test
	public void testLocalProperties() {
		Properties props = (Properties) this.beanFactory.getBean("myLocalProperties");
		assertEquals("Incorrect property value", null, props.get("foo"));
		assertEquals("Incorrect property value", "bar2", props.get("foo2"));
	}

	@Test
	public void testMergedProperties() {
		Properties props = (Properties) this.beanFactory.getBean("myMergedProperties");
		assertEquals("Incorrect property value", "bar", props.get("foo"));
		assertEquals("Incorrect property value", "bar2", props.get("foo2"));
	}

	@Test
	public void testLocalOverrideDefault() {
		Properties props = (Properties) this.beanFactory.getBean("defaultLocalOverrideProperties");
		assertEquals("Incorrect property value", "bar", props.get("foo"));
		assertEquals("Incorrect property value", "local2", props.get("foo2"));
	}

	@Test
	public void testLocalOverrideFalse() {
		Properties props = (Properties) this.beanFactory.getBean("falseLocalOverrideProperties");
		assertEquals("Incorrect property value", "bar", props.get("foo"));
		assertEquals("Incorrect property value", "local2", props.get("foo2"));
	}

	@Test
	public void testLocalOverrideTrue() {
		Properties props = (Properties) this.beanFactory.getBean("trueLocalOverrideProperties");
		assertEquals("Incorrect property value", "local", props.get("foo"));
		assertEquals("Incorrect property value", "local2", props.get("foo2"));
	}

}
