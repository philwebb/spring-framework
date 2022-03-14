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

package org.springframework.beans.factory.support.generate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.DefinedBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.Assert;

/**
 * Default {@link BeanRegistrationMethodCodeGenerator} providing registration code suitable for most
 * beans.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class DefaultBeanRegistrationMethodCodeGenerator implements BeanRegistrationMethodCodeGenerator {

	private final DefinedBean definedBean;

	DefaultBeanRegistrationMethodCodeGenerator(DefinedBean definedBean) {
		this.definedBean = definedBean;
	}

	@Override
	public CodeBlock generateRegistrationMethod(GenerationContext generationContext, GeneratedMethods registrationMethods) {
		BeanDefinition mergedBeanDefinition = this.definedBean.getMergedBeanDefinition();
		Executable executable = new ConstructorOrFactoryMethodResolver(this.definedBean.getBeanFactory())
				.resolve(mergedBeanDefinition);
		Assert.state(executable != null, () -> "No suitable executor found for " + mergedBeanDefinition);
		if (executable instanceof Constructor<?> constructor) {

		}
		if (executable instanceof Method method) {

		}
		return null;
	}

// @formatter:off

	/*

initializeBeanDefinitionRegistrar(instanceStatements, code);

	BeanDefinitionRegistrar.of("beanName",
		MyBean.class
	or
		ResolvableType.forClass(MyBean.class)
		Type.class
		ResolvableType.forClassWithGenerics
	)


	shouldDeclareCreator
		(yes if method or contructor declaring class is has params)
		 some withs


	.instanceSupplier

	handleBeanDefinitionMetadata
		(a bunch of BD stuff)

		.register(beanFactory)



	BeanDefinitionRegistrar.of("beanName", MyBeanClass.class).customize(beanDefinition -> {
		beanDefinition.setPrimary(true);
	});


	 */

// @formatter:on

}
