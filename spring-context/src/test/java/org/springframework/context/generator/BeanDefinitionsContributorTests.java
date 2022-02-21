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

package org.springframework.context.generator;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.support.CodeSnippet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanDefinitionsContributor}.
 *
 * @author Stephane Nicoll
 */
class BeanDefinitionsContributorTests {

	@Test
	void contributeWritesBeanDefinitionsInOrder() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("counter", BeanDefinitionBuilder
				.rootBeanDefinition(Integer.class, "valueOf").addConstructorArgValue(42).getBeanDefinition());
		beanFactory.registerBeanDefinition("name", BeanDefinitionBuilder
				.rootBeanDefinition(String.class).addConstructorArgValue("Hello").getBeanDefinition());
		CodeSnippet contribution = contribute(beanFactory, createGenerationContext());
		assertThat(contribution.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("counter", Integer.class).withFactoryMethod(Integer.class, "valueOf", int.class)
						.instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> Integer.valueOf(attributes.get(0)))).customize((bd) -> bd.getConstructorArgumentValues().addIndexedArgumentValue(0, 42)).register(beanFactory);
				BeanDefinitionRegistrar.of("name", String.class).withConstructor(String.class)
						.instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> new String(attributes.get(0, String.class)))).customize((bd) -> bd.getConstructorArgumentValues().addIndexedArgumentValue(0, "Hello")).register(beanFactory);
				""");
	}


	private CodeSnippet contribute(DefaultListableBeanFactory beanFactory, GeneratedTypeContext generationContext) {
		BeanDefinitionsContributor contributor = new BeanDefinitionsContributor(beanFactory);
		return CodeSnippet.of(contributor.contribute(generationContext));
	}

	private GeneratedTypeContext createGenerationContext() {
		return new DefaultGeneratedTypeContext("com.example", packageName ->
				GeneratedType.of(ClassName.get(packageName, "Test")));
	}

}
