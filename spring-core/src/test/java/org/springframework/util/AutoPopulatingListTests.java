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

package org.springframework.util;

import java.util.LinkedList;

import org.junit.Test;

import org.springframework.tests.sample.objects.TestObject;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotNull;
import static temp.XAssert.assertNotSame;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class AutoPopulatingListTests {

	@Test
	public void withClass() throws Exception {
		doTestWithClass(new AutoPopulatingList<>(TestObject.class));
	}

	@Test
	public void withClassAndUserSuppliedBackingList() throws Exception {
		doTestWithClass(new AutoPopulatingList<Object>(new LinkedList<>(), TestObject.class));
	}

	@Test
	public void withElementFactory() throws Exception {
		doTestWithElementFactory(new AutoPopulatingList<>(new MockElementFactory()));
	}

	@Test
	public void withElementFactoryAndUserSuppliedBackingList() throws Exception {
		doTestWithElementFactory(new AutoPopulatingList<Object>(new LinkedList<>(), new MockElementFactory()));
	}

	private void doTestWithClass(AutoPopulatingList<Object> list) {
		Object lastElement = null;
		for (int x = 0; x < 10; x++) {
			Object element = list.get(x);
			assertNotNull("Element is null", list.get(x));
			boolean condition = element instanceof TestObject;
			assertThat(condition).as("Element is incorrect type").isTrue();
			assertNotSame(lastElement, element);
			lastElement = element;
		}

		String helloWorld = "Hello World!";
		list.add(10, null);
		list.add(11, helloWorld);
		assertEquals(helloWorld, list.get(11));

		boolean condition3 = list.get(10) instanceof TestObject;
		assertThat(condition3).isTrue();
		boolean condition2 = list.get(12) instanceof TestObject;
		assertThat(condition2).isTrue();
		boolean condition1 = list.get(13) instanceof TestObject;
		assertThat(condition1).isTrue();
		boolean condition = list.get(20) instanceof TestObject;
		assertThat(condition).isTrue();
	}

	private void doTestWithElementFactory(AutoPopulatingList<Object> list) {
		doTestWithClass(list);

		for (int x = 0; x < list.size(); x++) {
			Object element = list.get(x);
			if (element instanceof TestObject) {
				assertEquals(x, ((TestObject) element).getAge());
			}
		}
	}

	@Test
	public void serialization() throws Exception {
		AutoPopulatingList<?> list = new AutoPopulatingList<Object>(TestObject.class);
		assertEquals(list, SerializationTestUtils.serializeAndDeserialize(list));
	}


	private static class MockElementFactory implements AutoPopulatingList.ElementFactory<Object> {

		@Override
		public Object createElement(int index) {
			TestObject bean = new TestObject();
			bean.setAge(index);
			return bean;
		}
	}

}
