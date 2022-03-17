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

package org.springframework.beans.factory.dunno;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.generator.config.BeanDefinitionRegistrar;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactoryInitializer;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * {@link DefaultListableBeanFactoryInitializer} for the {@literal "default"}
 * {@link BeanFactory}.
 */
public class Default$$BeanRegistrations implements DefaultListableBeanFactoryInitializer {

	@Override
	public void initialize(BeanDefinitionRegistry registry) {
		registerFooBeanDefinition(registry);

	}

	private void registerFooBeanDefinition(BeanDefinitionRegistry registry) {
		// FIXME use registrar
		BeanDefinitionRegistrar.of("boo", String.class).customize(this::customizeFooBeanDefinition).registerWith(registry);
	}

	private void customizeFooBeanDefinition(RootBeanDefinition rootBeanDefinition) {
		rootBeanDefinition.getPropertyValues().add("innerBean", createFooInnerBeanDefinition());
	}

	private RootBeanDefinition createFooInnerBeanDefinition() {
		return BeanDefinitionRegistrar.inner(Integer.class).toBeanDefinition();
	}

}