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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotSame;
import static temp.XAssert.assertSame;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class FactoryMethodTests {

	@Test
	public void testFactoryMethodsSingletonOnTargetClass() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		TestBean tb = (TestBean) xbf.getBean("defaultTestBean");
		assertThat((Object) tb.getName()).isEqualTo("defaultInstance");
		assertEquals(1, tb.getAge());

		FactoryMethods fm = (FactoryMethods) xbf.getBean("default");
		assertEquals(0, fm.getNum());
		assertThat((Object) fm.getName()).isEqualTo("default");
		assertThat((Object) fm.getTestBean().getName()).isEqualTo("defaultInstance");
		assertThat((Object) fm.getStringValue()).isEqualTo("setterString");

		fm = (FactoryMethods) xbf.getBean("testBeanOnly");
		assertEquals(0, fm.getNum());
		assertThat((Object) fm.getName()).isEqualTo("default");
		// This comes from the test bean
		assertThat((Object) fm.getTestBean().getName()).isEqualTo("Juergen");

		fm = (FactoryMethods) xbf.getBean("full");
		assertEquals(27, fm.getNum());
		assertThat((Object) fm.getName()).isEqualTo("gotcha");
		assertThat((Object) fm.getTestBean().getName()).isEqualTo("Juergen");

		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("full");
		assertSame(fm, fm2);

		xbf.destroySingletons();
		assertThat(tb.wasDestroyed()).isTrue();
	}

	@Test
	public void testFactoryMethodsWithInvalidDestroyMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("defaultTestBeanWithInvalidDestroyMethod"));
	}

	@Test
	public void testFactoryMethodsWithNullInstance() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		assertThat((Object) xbf.getBean("null").toString()).isEqualTo("null");
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("nullWithProperty"));
	}

	@Test
	public void testFactoryMethodsWithNullValue() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		FactoryMethods fm = (FactoryMethods) xbf.getBean("fullWithNull");
		assertEquals(27, fm.getNum());
		assertThat((Object) fm.getName()).isEqualTo(null);
		assertThat((Object) fm.getTestBean().getName()).isEqualTo("Juergen");

		fm = (FactoryMethods) xbf.getBean("fullWithGenericNull");
		assertEquals(27, fm.getNum());
		assertThat((Object) fm.getName()).isEqualTo(null);
		assertThat((Object) fm.getTestBean().getName()).isEqualTo("Juergen");

		fm = (FactoryMethods) xbf.getBean("fullWithNamedNull");
		assertEquals(27, fm.getNum());
		assertThat((Object) fm.getName()).isEqualTo(null);
		assertThat((Object) fm.getTestBean().getName()).isEqualTo("Juergen");
	}

	@Test
	public void testFactoryMethodsWithAutowire() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		FactoryMethods fm = (FactoryMethods) xbf.getBean("fullWithAutowire");
		assertEquals(27, fm.getNum());
		assertThat((Object) fm.getName()).isEqualTo("gotchaAutowired");
		assertThat((Object) fm.getTestBean().getName()).isEqualTo("Juergen");
	}

	@Test
	public void testProtectedFactoryMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		TestBean tb = (TestBean) xbf.getBean("defaultTestBean.protected");
		assertEquals(1, tb.getAge());
	}

	@Test
	public void testPrivateFactoryMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		TestBean tb = (TestBean) xbf.getBean("defaultTestBean.private");
		assertEquals(1, tb.getAge());
	}

	@Test
	public void testFactoryMethodsPrototypeOnTargetClass() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		FactoryMethods fm = (FactoryMethods) xbf.getBean("defaultPrototype");
		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("defaultPrototype");
		assertEquals(0, fm.getNum());
		assertThat((Object) fm.getName()).isEqualTo("default");
		assertThat((Object) fm.getTestBean().getName()).isEqualTo("defaultInstance");
		assertThat((Object) fm.getStringValue()).isEqualTo("setterString");
		assertEquals(fm.getNum(), fm2.getNum());
		assertThat((Object) fm2.getStringValue()).isEqualTo(fm.getStringValue());
		// The TestBean is created separately for each bean
		assertNotSame(fm.getTestBean(), fm2.getTestBean());
		assertNotSame(fm, fm2);

		fm = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype");
		fm2 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype");
		assertEquals(0, fm.getNum());
		assertThat((Object) fm.getName()).isEqualTo("default");
		// This comes from the test bean
		assertThat((Object) fm.getTestBean().getName()).isEqualTo("Juergen");
		assertEquals(fm.getNum(), fm2.getNum());
		assertThat((Object) fm2.getStringValue()).isEqualTo(fm.getStringValue());
		// The TestBean reference is resolved to a prototype in the factory
		assertSame(fm.getTestBean(), fm2.getTestBean());
		assertNotSame(fm, fm2);

		fm = (FactoryMethods) xbf.getBean("fullPrototype");
		fm2 = (FactoryMethods) xbf.getBean("fullPrototype");
		assertEquals(27, fm.getNum());
		assertThat((Object) fm.getName()).isEqualTo("gotcha");
		assertThat((Object) fm.getTestBean().getName()).isEqualTo("Juergen");
		assertEquals(fm.getNum(), fm2.getNum());
		assertThat((Object) fm2.getStringValue()).isEqualTo(fm.getStringValue());
		// The TestBean reference is resolved to a prototype in the factory
		assertSame(fm.getTestBean(), fm2.getTestBean());
		assertNotSame(fm, fm2);
	}

	/**
	 * Tests where the static factory method is on a different class.
	 */
	@Test
	public void testFactoryMethodsOnExternalClass() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		assertThat((Object) xbf.getType("externalFactoryMethodWithoutArgs")).isEqualTo(TestBean.class);
		assertThat((Object) xbf.getType("externalFactoryMethodWithArgs")).isEqualTo(TestBean.class);
		String[] names = xbf.getBeanNamesForType(TestBean.class);
		assertThat(Arrays.asList(names).contains("externalFactoryMethodWithoutArgs")).isTrue();
		assertThat(Arrays.asList(names).contains("externalFactoryMethodWithArgs")).isTrue();

		TestBean tb = (TestBean) xbf.getBean("externalFactoryMethodWithoutArgs");
		assertEquals(2, tb.getAge());
		assertThat((Object) tb.getName()).isEqualTo("Tristan");
		tb = (TestBean) xbf.getBean("externalFactoryMethodWithArgs");
		assertEquals(33, tb.getAge());
		assertThat((Object) tb.getName()).isEqualTo("Rod");

		assertThat((Object) xbf.getType("externalFactoryMethodWithoutArgs")).isEqualTo(TestBean.class);
		assertThat((Object) xbf.getType("externalFactoryMethodWithArgs")).isEqualTo(TestBean.class);
		names = xbf.getBeanNamesForType(TestBean.class);
		assertThat(Arrays.asList(names).contains("externalFactoryMethodWithoutArgs")).isTrue();
		assertThat(Arrays.asList(names).contains("externalFactoryMethodWithArgs")).isTrue();
	}

	@Test
	public void testInstanceFactoryMethodWithoutArgs() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		InstanceFactory.count = 0;
		xbf.preInstantiateSingletons();
		assertEquals(1, InstanceFactory.count);
		FactoryMethods fm = (FactoryMethods) xbf.getBean("instanceFactoryMethodWithoutArgs");
		assertThat((Object) fm.getTestBean().getName()).isEqualTo("instanceFactory");
		assertEquals(1, InstanceFactory.count);
		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("instanceFactoryMethodWithoutArgs");
		assertThat((Object) fm2.getTestBean().getName()).isEqualTo("instanceFactory");
		assertSame(fm2, fm);
		assertEquals(1, InstanceFactory.count);
	}

	@Test
	public void testFactoryMethodNoMatchingStaticMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		assertThatExceptionOfType(BeanCreationException.class).as("No static method matched").isThrownBy(() ->
				xbf.getBean("noMatchPrototype"));
	}

	@Test
	public void testNonExistingFactoryMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("invalidPrototype"))
			.withMessageContaining("nonExisting(TestBean)");
	}

	@Test
	public void testFactoryMethodArgumentsForNonExistingMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("invalidPrototype", new TestBean()))
			.withMessageContaining("nonExisting(TestBean)");
	}

	@Test
	public void testCanSpecifyFactoryMethodArgumentsOnFactoryMethodPrototype() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		TestBean tbArg = new TestBean();
		tbArg.setName("arg1");
		TestBean tbArg2 = new TestBean();
		tbArg2.setName("arg2");

		FactoryMethods fm1 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg);
		assertEquals(0, fm1.getNum());
		assertThat((Object) fm1.getName()).isEqualTo("default");
		// This comes from the test bean
		assertThat((Object) fm1.getTestBean().getName()).isEqualTo("arg1");

		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg2);
		assertThat((Object) fm2.getTestBean().getName()).isEqualTo("arg2");
		assertEquals(fm1.getNum(), fm2.getNum());
		assertThat((Object) "testBeanOnlyPrototypeDISetterString").isEqualTo(fm2.getStringValue());
		assertThat((Object) fm2.getStringValue()).isEqualTo(fm2.getStringValue());
		// The TestBean reference is resolved to a prototype in the factory
		assertSame(fm2.getTestBean(), fm2.getTestBean());
		assertNotSame(fm1, fm2);

		FactoryMethods fm3 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg2, new Integer(1), "myName");
		assertEquals(1, fm3.getNum());
		assertThat((Object) fm3.getName()).isEqualTo("myName");
		assertThat((Object) fm3.getTestBean().getName()).isEqualTo("arg2");

		FactoryMethods fm4 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg);
		assertEquals(0, fm4.getNum());
		assertThat((Object) fm4.getName()).isEqualTo("default");
		assertThat((Object) fm4.getTestBean().getName()).isEqualTo("arg1");
	}

	@Test
	public void testCanSpecifyFactoryMethodArgumentsOnSingleton() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		// First getBean call triggers actual creation of the singleton bean
		TestBean tb = new TestBean();
		FactoryMethods fm1 = (FactoryMethods) xbf.getBean("testBeanOnly", tb);
		assertSame(tb, fm1.getTestBean());
		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("testBeanOnly", new TestBean());
		assertSame(fm1, fm2);
		assertSame(tb, fm2.getTestBean());
	}

	@Test
	public void testCannotSpecifyFactoryMethodArgumentsOnSingletonAfterCreation() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		// First getBean call triggers actual creation of the singleton bean
		FactoryMethods fm1 = (FactoryMethods) xbf.getBean("testBeanOnly");
		TestBean tb = fm1.getTestBean();
		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("testBeanOnly", new TestBean());
		assertSame(fm1, fm2);
		assertSame(tb, fm2.getTestBean());
	}

	@Test
	public void testFactoryMethodWithDifferentReturnType() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		// Check that listInstance is not considered a bean of type FactoryMethods.
		assertThat(List.class.isAssignableFrom(xbf.getType("listInstance"))).isTrue();
		String[] names = xbf.getBeanNamesForType(FactoryMethods.class);
		boolean condition1 = !Arrays.asList(names).contains("listInstance");
		assertThat(condition1).isTrue();
		names = xbf.getBeanNamesForType(List.class);
		assertThat(Arrays.asList(names).contains("listInstance")).isTrue();

		xbf.preInstantiateSingletons();
		assertThat(List.class.isAssignableFrom(xbf.getType("listInstance"))).isTrue();
		names = xbf.getBeanNamesForType(FactoryMethods.class);
		boolean condition = !Arrays.asList(names).contains("listInstance");
		assertThat(condition).isTrue();
		names = xbf.getBeanNamesForType(List.class);
		assertThat(Arrays.asList(names).contains("listInstance")).isTrue();
		List<?> list = (List<?>) xbf.getBean("listInstance");
		assertThat((Object) list).isEqualTo(Collections.EMPTY_LIST);
	}

	@Test
	public void testFactoryMethodForJavaMailSession() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		MailSession session = (MailSession) xbf.getBean("javaMailSession");
		assertThat(session.getProperty("mail.smtp.user")).isEqualTo("someuser");
		assertThat(session.getProperty("mail.smtp.password")).isEqualTo("somepw");
	}
}


class MailSession {

	private Properties props;

	private MailSession() {
	}

	public void setProperties(Properties props) {
		this.props = props;
	}

	public static MailSession getDefaultInstance(Properties props) {
		MailSession session = new MailSession();
		session.setProperties(props);
		return session;
	}

	public Object getProperty(String key) {
		return this.props.get(key);
	}
}
