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

import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author pwebb
 * @since 6.0
 */
class BeanDefinitionMethodGeneratorTests {

	@Test
	void test() {
		fail("Not yet implemented");
	}

//	@Test
//	void getBeanDefinitionMethodGeneratorWhenCodeGeneratorFactoryReturnsNullUsesDefaultCodeGenerator() {
//		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
//		BeanRegistrationCodeGeneratorFactory codeGeneratorFactory = (registeredBean, innerBeanPropertyName,
//				innerBeanRegistrationMethodGenerator) -> null;
//		beanFactory.registerSingleton("codeGeneratorFactory", codeGeneratorFactory);
//		RegisteredBean registeredBean = registerTestBean(beanFactory);
//		BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
//				new AotFactoriesLoader(beanFactory, new MockSpringFactoriesLoader()));
//		BeanDefinitionMethodGenerator methodGenerator = methodGeneratorFactory
//				.getBeanDefinitionMethodGenerator(registeredBean, null);
//		assertThat(methodGenerator).extracting("codeGenerator")
//				.isInstanceOf(DefaultBeanRegistrationCodeGenerator.class);
//	}
//
//	@Test
//	void getBeanDefinitionMethodGeneratorWhenCodeGeneratorFactoryReturnsCustomGeneratorUsesCustomGenerator() {
//		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
//		BeanRegistrationCodeGenerator codeGenerator = mock(BeanRegistrationCodeGenerator.class);
//		BeanRegistrationCodeGeneratorFactory codeGeneratorFactory = (registeredBean, innerBeanPropertyName,
//				innerBeanRegistrationMethodGenerator) -> codeGenerator;
//		beanFactory.registerSingleton("codeGeneratorFactory", codeGeneratorFactory);
//		RegisteredBean registeredBean = registerTestBean(beanFactory);
//		BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
//				new AotFactoriesLoader(beanFactory, new MockSpringFactoriesLoader()));
//		BeanDefinitionMethodGenerator methodGenerator = methodGeneratorFactory
//				.getBeanDefinitionMethodGenerator(registeredBean, null);
//		assertThat(methodGenerator).extracting("codeGenerator").isSameAs(codeGenerator);
//	}

}
