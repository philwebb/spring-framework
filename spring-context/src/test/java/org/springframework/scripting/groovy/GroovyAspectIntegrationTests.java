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

package org.springframework.scripting.groovy;

import org.junit.After;
import org.junit.Test;

import org.springframework.context.support.GenericXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 */
public class GroovyAspectIntegrationTests {

	private GenericXmlApplicationContext context;

	@Test
	public void testJavaBean() {
		context = new GenericXmlApplicationContext(getClass(), getClass().getSimpleName()+"-java-context.xml");
		TestService bean = context.getBean("javaBean", TestService.class);
		LogUserAdvice logAdvice = context.getBean(LogUserAdvice.class);

		assertThat((long) logAdvice.getCountThrows()).isEqualTo((long) 0);
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(
				bean::sayHello)
			.withMessage("TestServiceImpl");
		assertThat((long) logAdvice.getCountThrows()).isEqualTo((long) 1);
	}

	@Test
	public void testGroovyBeanInterface() {
		context = new GenericXmlApplicationContext(getClass(), getClass().getSimpleName()+"-groovy-interface-context.xml");
		TestService bean = context.getBean("groovyBean", TestService.class);
		LogUserAdvice logAdvice = context.getBean(LogUserAdvice.class);

		assertThat((long) logAdvice.getCountThrows()).isEqualTo((long) 0);
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(
				bean::sayHello)
			.withMessage("GroovyServiceImpl");
		assertThat((long) logAdvice.getCountThrows()).isEqualTo((long) 1);
	}


	@Test
	public void testGroovyBeanDynamic() {
		context = new GenericXmlApplicationContext(getClass(), getClass().getSimpleName()+"-groovy-dynamic-context.xml");
		TestService bean = context.getBean("groovyBean", TestService.class);
		LogUserAdvice logAdvice = context.getBean(LogUserAdvice.class);

		assertThat((long) logAdvice.getCountThrows()).isEqualTo((long) 0);
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(
				bean::sayHello)
			.withMessage("GroovyServiceImpl");
		// No proxy here because the pointcut only applies to the concrete class, not the interface
		assertThat((long) logAdvice.getCountThrows()).isEqualTo((long) 0);
		assertThat((long) logAdvice.getCountBefore()).isEqualTo((long) 0);
	}

	@Test
	public void testGroovyBeanProxyTargetClass() {
		context = new GenericXmlApplicationContext(getClass(), getClass().getSimpleName()+"-groovy-proxy-target-class-context.xml");
		TestService bean = context.getBean("groovyBean", TestService.class);
		LogUserAdvice logAdvice = context.getBean(LogUserAdvice.class);

		assertThat((long) logAdvice.getCountThrows()).isEqualTo((long) 0);
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(
				bean::sayHello)
			.withMessage("GroovyServiceImpl");
		assertThat((long) logAdvice.getCountBefore()).isEqualTo((long) 1);
		assertThat((long) logAdvice.getCountThrows()).isEqualTo((long) 1);
	}

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
	}

}
