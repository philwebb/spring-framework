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

package org.springframework.beans.factory;

import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link DefaultListableBeanFactory} generic wiring capabilities.
 *
 * @author Oliver Gierke
 */
public class DefaultListableBeanFactoryGenericTypeWiringTests {

	DefaultListableBeanFactory factory;

	@Before
	public void setUp() {
		this.factory = new DefaultListableBeanFactory();
		this.factory.registerSingleton("stringType", new StringType());
		this.factory.registerSingleton("integerType", new IntegerType());

		AutowiredAnnotationBeanPostProcessor processor = new AutowiredAnnotationBeanPostProcessor();
		processor.setBeanFactory(this.factory);
		this.factory.addBeanPostProcessor(processor);
	}

	@Test
	public void injectsGenericProperties() {
		this.factory.registerBeanDefinition("client", new RootBeanDefinition(
				PropertyWiredClient.class));
		PropertyWiredClient client = this.factory.getBean(PropertyWiredClient.class);
		assertThat(client.integerType, instanceOf(IntegerType.class));
		assertThat(client.stringType, instanceOf(StringType.class));
		assertThat(client.stringTypes.size(), equalTo(1));
	}

	@Test
	public void injectsGenericConstructorArguments() {
		this.factory.registerBeanDefinition("constructorClient", new RootBeanDefinition(
				ConstructorWiredClient.class));
		ConstructorWiredClient client = this.factory.getBean(ConstructorWiredClient.class);
		assertThat(client.integerType, instanceOf(IntegerType.class));
		assertThat(client.stringType, instanceOf(StringType.class));
		assertThat(client.stringTypes.size(), equalTo(1));
	}

	@Test
	public void infersGenericPropertyFromSuperClass() {
		this.factory.registerBeanDefinition("client", new RootBeanDefinition(
				GenericStringClient.class));
		GenericStringClient client = this.factory.getBean(GenericStringClient.class);
		assertThat(client.property, instanceOf(StringType.class));
		assertThat(client.subClassProperty, instanceOf(StringType.class));
	}

	@Test
	public void injectsSubtypeButNotConreteReference() {
		this.factory.registerBeanDefinition("client", new RootBeanDefinition(
				GenericNumberClient.class));
		GenericNumberClient client = this.factory.getBean(GenericNumberClient.class);
		assertThat(client.subClassProperty, instanceOf(IntegerType.class));
		assertThat(client.property, nullValue());
	}

	@Test
	public void doesNotInjectSubTypeIntoGenericTypeWithParentTypeParameter() {
		this.factory.registerBeanDefinition("client", new RootBeanDefinition(
				GenericIntegerClient.class));
		GenericIntegerClient client = this.factory.getBean(GenericIntegerClient.class);
		assertThat(client.subClassProperty, instanceOf(IntegerType.class));
		assertThat(client.property, instanceOf(IntegerType.class));
	}

	interface GenericType<T> {

	}

	static class StringType implements GenericType<String> {

	}

	static class IntegerType implements GenericType<Integer> {

	}

	static abstract class GenericClient<T> {

		@Autowired
		GenericType<? extends T> subClassProperty;

		@Autowired(required = false)
		GenericType<T> property;
	}

	static class GenericStringClient extends GenericClient<String> {

	}

	static class GenericNumberClient extends GenericClient<Number> {

	}

	static class GenericIntegerClient extends GenericClient<Integer> {

	}

	static class PropertyWiredClient {

		@Autowired
		GenericType<String> stringType;

		@Autowired
		GenericType<Integer> integerType;

		@Autowired
		List<GenericType<String>> stringTypes;

		GenericType<GenericType<String>> wrappedStringType;
	}

	static class ConstructorWiredClient {

		GenericType<String> stringType;

		GenericType<Integer> integerType;

		List<GenericType<String>> stringTypes;

		@Autowired
		public ConstructorWiredClient(GenericType<String> stringType,
				GenericType<Integer> integerType, List<GenericType<String>> stringTypes) {

			this.integerType = integerType;
			this.stringType = stringType;
			this.stringTypes = stringTypes;
		}
	}

	// FIXME test for bounded types
}
