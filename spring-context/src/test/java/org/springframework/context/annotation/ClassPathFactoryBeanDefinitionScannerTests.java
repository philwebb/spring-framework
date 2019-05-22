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

package org.springframework.context.annotation;

import org.junit.Test;

import org.springframework.aop.scope.ScopedObject;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation4.DependencyBean;
import org.springframework.context.annotation4.FactoryMethodComponent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.tests.context.SimpleMapScope;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static temp.XAssert.assertEquals;
import static temp.XAssert.assertNotSame;
import static temp.XAssert.assertSame;

/**
 * @author Mark Pollack
 * @author Juergen Hoeller
 */
public class ClassPathFactoryBeanDefinitionScannerTests {

	private static final String BASE_PACKAGE = FactoryMethodComponent.class.getPackage().getName();


	@Test
	public void testSingletonScopedFactoryMethod() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);

		context.getBeanFactory().registerScope("request", new SimpleMapScope());

		scanner.scan(BASE_PACKAGE);
		context.registerBeanDefinition("clientBean", new RootBeanDefinition(QualifiedClientBean.class));
		context.refresh();

		FactoryMethodComponent fmc = context.getBean("factoryMethodComponent", FactoryMethodComponent.class);
		assertThat(fmc.getClass().getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)).isFalse();

		TestBean tb = (TestBean) context.getBean("publicInstance"); //2
		assertThat((Object) tb.getName()).isEqualTo("publicInstance");
		TestBean tb2 = (TestBean) context.getBean("publicInstance"); //2
		assertThat((Object) tb2.getName()).isEqualTo("publicInstance");
		assertSame(tb2, tb);

		tb = (TestBean) context.getBean("protectedInstance"); //3
		assertThat((Object) tb.getName()).isEqualTo("protectedInstance");
		assertSame(tb, context.getBean("protectedInstance"));
		assertThat((Object) tb.getCountry()).isEqualTo("0");
		tb2 = context.getBean("protectedInstance", TestBean.class); //3
		assertThat((Object) tb2.getName()).isEqualTo("protectedInstance");
		assertSame(tb2, tb);

		tb = context.getBean("privateInstance", TestBean.class); //4
		assertThat((Object) tb.getName()).isEqualTo("privateInstance");
		assertEquals(1, tb.getAge());
		tb2 = context.getBean("privateInstance", TestBean.class); //4
		assertEquals(2, tb2.getAge());
		assertNotSame(tb2, tb);

		Object bean = context.getBean("requestScopedInstance"); //5
		assertThat(AopUtils.isCglibProxy(bean)).isTrue();
		boolean condition = bean instanceof ScopedObject;
		assertThat(condition).isTrue();

		QualifiedClientBean clientBean = context.getBean("clientBean", QualifiedClientBean.class);
		assertSame(context.getBean("publicInstance"), clientBean.testBean);
		assertSame(context.getBean("dependencyBean"), clientBean.dependencyBean);
		assertSame(context, clientBean.applicationContext);
	}


	public static class QualifiedClientBean {

		@Autowired @Qualifier("public")
		public TestBean testBean;

		@Autowired
		public DependencyBean dependencyBean;

		@Autowired
		AbstractApplicationContext applicationContext;
	}

}
