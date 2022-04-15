/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.aot.registration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author pwebb
 * @since 6.0
 */
class BeanRegistrationCodeGeneratorFactoryTests {

	@Test
	void test() {
		fail("Not yet implemented");
	}


	// FIXME
	// @Test
	// void getPackagePrivateTargetWhenHasPackagePrivateStaticFactoryMethod() {
	// BeanDefinition beanDefinition =
	// BeanDefinitionBuilder.rootBeanDefinition(String.class)
	// .setFactoryMethodOnBean("packageStaticStringBean", "config").getBeanDefinition();
	// beanFactory.registerBeanDefinition("config",
	// BeanDefinitionBuilder.genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
	// RegisteredBean registeredBean = registerBean((RootBeanDefinition) beanDefinition);
	// DefaultBeanRegistrationCodeGenerator generator = new
	// DefaultBeanRegistrationCodeGenerator(registeredBean, null,
	// this.beanRegistrationsCode);
	// assertThat(generator.getPackagePrivateTarget()).isEqualTo(SimpleConfiguration.class);
	// }
	//
	// @Test
	// void getPackagePrivateTargetWhenHasPackagePrivateConstructor() {
	// BeanDefinition beanDefinition = new
	// RootBeanDefinition(TestBeanWithPackagePrivateConstructor.class);
	// RegisteredBean registeredBean = registerBean((RootBeanDefinition) beanDefinition);
	// DefaultBeanRegistrationCodeGenerator generator = new
	// DefaultBeanRegistrationCodeGenerator(registeredBean, null,
	// this.beanRegistrationsCode);
	// assertThat(generator.getPackagePrivateTarget()).isEqualTo(TestBeanWithPackagePrivateConstructor.class);
	// }
	//
	// @Test
	// void getPackagePrivateTargetWhenPublicClassWithPublicConstructorReturnsNull() {
	// BeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
	// RegisteredBean registeredBean = registerBean((RootBeanDefinition) beanDefinition);
	// DefaultBeanRegistrationCodeGenerator generator = new
	// DefaultBeanRegistrationCodeGenerator(registeredBean, null,
	// this.beanRegistrationsCode);
	// assertThat(generator.getPackagePrivateTarget()).isNull();
	// }
	//
	// @Test
	// void getPackagePrivateTargetWhenHasPrivateConstructorReturnsNull() {
	// BeanDefinition beanDefinition = new
	// RootBeanDefinition(TestBeanWithPrivateConstructor.class);
	// RegisteredBean registeredBean = registerBean((RootBeanDefinition) beanDefinition);
	// DefaultBeanRegistrationCodeGenerator generator = new
	// DefaultBeanRegistrationCodeGenerator(registeredBean, null,
	// this.beanRegistrationsCode);
	// assertThat(generator.getPackagePrivateTarget()).isNull();
	// }

}
