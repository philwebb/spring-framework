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

package org.springframework.jmx.support;

import java.beans.PropertyDescriptor;
import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.junit.Test;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.JmxTestBean;
import org.springframework.jmx.export.TestDynamicMBean;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class JmxUtilsTests {

	@Test
	public void testIsMBeanWithDynamicMBean() throws Exception {
		DynamicMBean mbean = new TestDynamicMBean();
		assertThat(JmxUtils.isMBean(mbean.getClass())).as("Dynamic MBean not detected correctly").isTrue();
	}

	@Test
	public void testIsMBeanWithStandardMBeanWrapper() throws Exception {
		StandardMBean mbean = new StandardMBean(new JmxTestBean(), IJmxTestBean.class);
		assertThat(JmxUtils.isMBean(mbean.getClass())).as("Standard MBean not detected correctly").isTrue();
	}

	@Test
	public void testIsMBeanWithStandardMBeanInherited() throws Exception {
		StandardMBean mbean = new StandardMBeanImpl();
		assertThat(JmxUtils.isMBean(mbean.getClass())).as("Standard MBean not detected correctly").isTrue();
	}

	@Test
	public void testNotAnMBean() throws Exception {
		assertThat(JmxUtils.isMBean(Object.class)).as("Object incorrectly identified as an MBean").isFalse();
	}

	@Test
	public void testSimpleMBean() throws Exception {
		Foo foo = new Foo();
		assertThat(JmxUtils.isMBean(foo.getClass())).as("Simple MBean not detected correctly").isTrue();
	}

	@Test
	public void testSimpleMXBean() throws Exception {
		FooX foo = new FooX();
		assertThat(JmxUtils.isMBean(foo.getClass())).as("Simple MXBean not detected correctly").isTrue();
	}

	@Test
	public void testSimpleMBeanThroughInheritance() throws Exception {
		Bar bar = new Bar();
		Abc abc = new Abc();
		assertThat(JmxUtils.isMBean(bar.getClass())).as("Simple MBean (through inheritance) not detected correctly").isTrue();
		assertThat(JmxUtils.isMBean(abc.getClass())).as("Simple MBean (through 2 levels of inheritance) not detected correctly").isTrue();
	}

	@Test
	public void testGetAttributeNameWithStrictCasing() {
		PropertyDescriptor pd = new BeanWrapperImpl(AttributeTestBean.class).getPropertyDescriptor("name");
		String attributeName = JmxUtils.getAttributeName(pd, true);
		assertThat(attributeName).as("Incorrect casing on attribute name").isEqualTo("Name");
	}

	@Test
	public void testGetAttributeNameWithoutStrictCasing() {
		PropertyDescriptor pd = new BeanWrapperImpl(AttributeTestBean.class).getPropertyDescriptor("name");
		String attributeName = JmxUtils.getAttributeName(pd, false);
		assertThat(attributeName).as("Incorrect casing on attribute name").isEqualTo("name");
	}

	@Test
	public void testAppendIdentityToObjectName() throws MalformedObjectNameException {
		ObjectName objectName = ObjectNameManager.getInstance("spring:type=Test");
		Object managedResource = new Object();
		ObjectName uniqueName = JmxUtils.appendIdentityToObjectName(objectName, managedResource);

		String typeProperty = "type";

		assertThat(uniqueName.getDomain()).as("Domain of transformed name is incorrect").isEqualTo(objectName.getDomain());
		assertThat(uniqueName.getKeyProperty("type")).as("Type key is incorrect").isEqualTo(objectName.getKeyProperty(typeProperty));
		assertThat(uniqueName.getKeyProperty(JmxUtils.IDENTITY_OBJECT_NAME_KEY)).as("Identity key is incorrect").isEqualTo(ObjectUtils.getIdentityHexString(managedResource));
	}

	@Test
	public void testLocatePlatformMBeanServer() {
		MBeanServer server = null;
		try {
			server = JmxUtils.locateMBeanServer();
		}
		finally {
			if (server != null) {
				MBeanServerFactory.releaseMBeanServer(server);
			}
		}
	}

	@Test
	public void testIsMBean() {
		// Correctly returns true for a class
		assertThat(JmxUtils.isMBean(JmxClass.class)).isTrue();

		// Correctly returns false since JmxUtils won't navigate to the extended interface
		assertThat(JmxUtils.isMBean(SpecializedJmxInterface.class)).isFalse();

		// Incorrectly returns true since it doesn't detect that this is an interface
		assertThat(JmxUtils.isMBean(JmxInterface.class)).isFalse();
	}


	public static class AttributeTestBean {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	public static class StandardMBeanImpl extends StandardMBean implements IJmxTestBean {

		public StandardMBeanImpl() throws NotCompliantMBeanException {
			super(IJmxTestBean.class);
		}

		@Override
		public int add(int x, int y) {
			return 0;
		}

		@Override
		public long myOperation() {
			return 0;
		}

		@Override
		public int getAge() {
			return 0;
		}

		@Override
		public void setAge(int age) {
		}

		@Override
		public void setName(String name) {
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public void dontExposeMe() {
		}
	}


	public interface FooMBean {

		String getName();
	}


	public static class Foo implements FooMBean {

		@Override
		public String getName() {
			return "Rob Harrop";
		}
	}


	public interface FooMXBean {

		String getName();
	}


	public static class FooX implements FooMXBean {

		@Override
		public String getName() {
			return "Rob Harrop";
		}
	}


	public static class Bar extends Foo {
	}


	public static class Abc extends Bar {
	}


	private interface JmxInterfaceMBean {
	}


	private interface JmxInterface extends JmxInterfaceMBean {
	}


	private interface SpecializedJmxInterface extends JmxInterface {
	}


	private interface JmxClassMBean {
	}


	private static class JmxClass implements JmxClassMBean {
	}

}
