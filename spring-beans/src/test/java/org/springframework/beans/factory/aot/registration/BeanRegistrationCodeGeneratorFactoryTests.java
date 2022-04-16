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

import org.springframework.aot.generate.MethodGenerator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.TestBeanWithPackagePrivateConstructor;
import org.springframework.beans.testfixture.beans.TestBeanWithPrivateConstructor;
import org.springframework.beans.testfixture.beans.factory.generator.SimpleConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanRegistrationCodeGeneratorFactory}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class BeanRegistrationCodeGeneratorFactoryTests {

	private final BeanRegistrationCodeGeneratorFactory factory = new TestBeanRegistrationCodeGeneratorFactory();
	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Test
	void getPackagePrivateTargetWhenHasPackagePrivateStaticFactoryMethod() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(String.class)
				.setFactoryMethodOnBean("packageStaticStringBean", "config").getBeanDefinition();
		this.beanFactory.registerBeanDefinition("config",
				BeanDefinitionBuilder.genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
		RegisteredBean registeredBean = registerBean((RootBeanDefinition) beanDefinition);
		assertThat(this.factory.getPackagePrivateTarget(registeredBean)).isEqualTo(SimpleConfiguration.class);
	}

	@Test
	void getPackagePrivateTargetWhenHasPackagePrivateConstructor() {
		BeanDefinition beanDefinition = new RootBeanDefinition(TestBeanWithPackagePrivateConstructor.class);
		RegisteredBean registeredBean = registerBean((RootBeanDefinition) beanDefinition);
		assertThat(this.factory.getPackagePrivateTarget(registeredBean))
				.isEqualTo(TestBeanWithPackagePrivateConstructor.class);
	}

	@Test
	void getPackagePrivateTargetWhenPublicClassWithPublicConstructorReturnsNull() {
		BeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		RegisteredBean registeredBean = registerBean((RootBeanDefinition) beanDefinition);
		assertThat(this.factory.getPackagePrivateTarget(registeredBean)).isNull();
	}

	@Test
	void getPackagePrivateTargetWhenHasPrivateConstructorReturnsNull() {
		BeanDefinition beanDefinition = new RootBeanDefinition(TestBeanWithPrivateConstructor.class);
		RegisteredBean registeredBean = registerBean((RootBeanDefinition) beanDefinition);
		assertThat(this.factory.getPackagePrivateTarget(registeredBean)).isNull();
	}

	private RegisteredBean registerBean(RootBeanDefinition beanDefinition) {
		String beanName = "testBean";
		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
		return RegisteredBean.of(this.beanFactory, beanName);
	}

	private static class TestBeanRegistrationCodeGeneratorFactory implements BeanRegistrationCodeGeneratorFactory {

		@Override
		public boolean isSupported(RegisteredBean registeredBean) {
			return true;
		}

		@Override
		public BeanRegistrationCodeGenerator getBeanRegistrationCodeGenerator(MethodGenerator methodGenerator,
				InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator, RegisteredBean registeredBean) {
			throw new IllegalStateException();
		}

	}

}
