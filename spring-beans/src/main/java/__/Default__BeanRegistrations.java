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

package __;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactoryInitializer;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * {@link DefaultListableBeanFactoryInitializer} for the {@literal "default"}
 * {@link BeanFactory}.
 */
public class Default__BeanRegistrations implements DefaultListableBeanFactoryInitializer {

	@Override
	public void initialize(DefaultListableBeanFactory beanFactory) {
		registerMyBeanBeanDefinition(beanFactory);
		registerMyOtherBeanBeanDefinition(beanFactory);
		registerMyOuterBeanBeanDefinition(beanFactory);
	}

	private void registerMyBeanBeanDefinition(DefaultListableBeanFactory beanFactory) {
		RootBeanDefinition beanDefinition = RootBeanDefinition.supply(MyBean.class).usingConstructor()
				.resolvedBy(MyBean::new);
		beanFactory.registerBeanDefinition("myBean", beanDefinition);
	}

	private void registerMyOtherBeanBeanDefinition(DefaultListableBeanFactory beanFactory) {
		RootBeanDefinition beanDefinition = RootBeanDefinition.supply(MyOtherBean.class).usingConstructor(MyBean.class)
				.resolvedBy(beanFactory, this::createMyOtherBeanInstance);
		beanDefinition.setAttribute("foo", "bar");
		beanFactory.registerBeanDefinition("myOtherBean", beanDefinition);
	}

	private void registerMyOuterBeanBeanDefinition(DefaultListableBeanFactory beanFactory) {
		RootBeanDefinition beanDefinition = RootBeanDefinition.supply(MyOuterBean.class).usingConstructor()
				.resolvedBy(MyOuterBean::new);
		beanDefinition.getPropertyValues().add("myBean", createMyOuterBeanMyBeanBeanDefinition());
	}

	private RootBeanDefinition createMyOuterBeanMyBeanBeanDefinition() {
		return RootBeanDefinition.supply(MyBean.class).usingConstructor().resolvedBy(MyBean::new);
	}

	private MyOtherBean createMyOtherBeanInstance(Object[] args) {
		return new MyOtherBean((MyBean) args[0]);
	}

	static class MyBean {

	}

	static class MyOtherBean {

		MyOtherBean(MyBean myBean) {
		}

	}

	static class MyOuterBean {

	}

}
