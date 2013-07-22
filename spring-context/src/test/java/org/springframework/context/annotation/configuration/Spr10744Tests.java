/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation.configuration;

import org.junit.Test;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * @author Phillip Webb
 */
public class Spr10744Tests {

	@Test
	public void testSpr10744() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		MyTestScope scope = new MyTestScope();
		context.getBeanFactory().registerScope("myTestScope", scope);
		context.register(MyTestConfiguration.class);
		context.refresh();
		context.getBean(Foo.class);
		context.close();
	}

	private static class MyTestScope implements org.springframework.beans.factory.config.Scope {

		@Override
		public Object get(String name, ObjectFactory<?> objectFactory) {
			System.out.println(name);
			return objectFactory.getObject();
		}

		@Override
		public Object remove(String name) {
			return null;
		}

		@Override
		public void registerDestructionCallback(String name, Runnable callback) {
		}

		@Override
		public Object resolveContextualObject(String key) {
			return null;
		}

		@Override
		public String getConversationId() {
			return null;
		}

	}

	static class Foo {
	}

	@Configuration
	static class MyConfiguration {

		@Bean
		public Foo foo() {
			return new Foo();
		}

	}

	@Configuration
	static class MyTestConfiguration extends MyConfiguration {

		@Override
		@Scope(value = "myTestScope",  proxyMode = ScopedProxyMode.TARGET_CLASS)
		public Foo foo() {
			return new Foo();
		}

		@Bean
		public Foo foo2() {
			return new Foo();
		}

	}

}
