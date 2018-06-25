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

package org.springframework.beans.factory.config;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.Resource;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;
import static org.springframework.tests.TestResourceUtils.*;

/**
 * Simple test to illustrate and verify scope usage.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class SimpleScopeTests {

	private static final Resource CONTEXT = qualifiedResource(SimpleScopeTests.class, "context.xml");

	private DefaultListableBeanFactory beanFactory;

	@Before
	public void setUp() {
		this.beanFactory = new DefaultListableBeanFactory();
		Scope scope = new NoOpScope() {
			private int index;
			private List<TestBean> objects = new LinkedList<>(); {
				this.objects.add(new TestBean());
				this.objects.add(new TestBean());
			}
			@Override
			public Object get(String name, ObjectFactory<?> objectFactory) {
				if (this.index >= this.objects.size()) {
					this.index = 0;
				}
				return this.objects.get(this.index++);
			}
		};

		this.beanFactory.registerScope("myScope", scope);

		String[] scopeNames = this.beanFactory.getRegisteredScopeNames();
		assertEquals(1, scopeNames.length);
		assertEquals("myScope", scopeNames[0]);
		assertSame(scope, this.beanFactory.getRegisteredScope("myScope"));

		XmlBeanDefinitionReader xbdr = new XmlBeanDefinitionReader(this.beanFactory);
		xbdr.loadBeanDefinitions(CONTEXT);
	}

	@Test
	public void testCanGetScopedObject() {
		TestBean tb1 = (TestBean) this.beanFactory.getBean("usesScope");
		TestBean tb2 = (TestBean) this.beanFactory.getBean("usesScope");
		assertNotSame(tb1, tb2);
		TestBean tb3 = (TestBean) this.beanFactory.getBean("usesScope");
		assertSame(tb3, tb1);
	}

}
