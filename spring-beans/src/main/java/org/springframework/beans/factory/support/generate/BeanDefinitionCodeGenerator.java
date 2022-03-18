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
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.instance.InstanceCodeGenerationService;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ClassUtils;

/**
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class BeanDefinitionCodeGenerator {

	// DefaultBeanRegistrationContributionProvider and DefaultBeanInstantiationGenerator

	private final DefaultListableBeanFactory beanFactory;

	private final GeneratedMethods generatedMethods;

	private ConstructorOrFactoryMethodResolver constructorOrFactoryMethodResolver;

	BeanDefinitionCodeGenerator(DefaultListableBeanFactory beanFactory, GeneratedMethods generatedMethods) {
		this.beanFactory = beanFactory;
		this.generatedMethods = generatedMethods;
		constructorOrFactoryMethodResolver = new ConstructorOrFactoryMethodResolver(this.beanFactory);
	}

	CodeBlock generateCode(RootBeanDefinition beanDefinition) {
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.add(generateSuppliedRootBeanDefinitionBuilderCode(beanDefinition));
		builder.add(generateUsingAndGeneratedByCode(beanDefinition));
		return builder.build();
	}

	private CodeBlock generateSuppliedRootBeanDefinitionBuilderCode(RootBeanDefinition beanDefinition) {
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.add("$T.supply(", RootBeanDefinition.class);
		ResolvableType beanType = beanDefinition.getResolvableType();
		Object supplyType = beanType;
		if (!beanType.hasGenerics() || hasAnyUnresolvableGenerics(beanType)) {
			supplyType = ClassUtils.getUserClass(beanType.toClass());
		}
		builder.add(InstanceCodeGenerationService.getSharedInstance().generateCode(supplyType));
		builder.add(")");
		return builder.build();
	}

	private boolean hasAnyUnresolvableGenerics(ResolvableType type) {
		return type.hasUnresolvableGenerics()
				|| Arrays.stream(type.getGenerics()).anyMatch(this::hasAnyUnresolvableGenerics);
	}

	private CodeBlock generateUsingAndGeneratedByCode(RootBeanDefinition beanDefinition) {
		Executable executable = this.constructorOrFactoryMethodResolver.resolve(beanDefinition);
		if (executable instanceof Constructor<?> constructor) {
			return generateUsingAndGeneratedByConstructorCode(beanDefinition, constructor);
		}
		if (executable instanceof Method method) {
			return generateUsingAndGeneratedByMethodCode(beanDefinition, method);
		}
		throw new IllegalStateException("No suitable executor found for " + beanDefinition);
	}

	private CodeBlock generateUsingAndGeneratedByConstructorCode(RootBeanDefinition beanDefinition,
			Constructor<?> constructor) {
		Class<?> declaringClass = ClassUtils.getUserClass(constructor.getDeclaringClass());
		boolean innerClass = isInnerClass(declaringClass);
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.add(".usingConstructor($L)", generateUsingParametersCode(constructor.getParameterTypes()));
		if (constructor.getParameterCount() == 0 && !innerClass) {
			builder.add("resolvedBy($T::new)", declaringClass);
			return builder.build();
		}
		// resolvedBy(beanFactory, this::someMethod);

		// someMethod(beanFactory, Object[] args) {
		//   beanFactory.getBean(enclosingClass).new $L(args[0]);


		// }

		// someMethod(Object[] args)) {
		//	new SomeThing((String) args[0]);
		//}



		return builder.build();
	}

	private CodeBlock generateUsingAndGeneratedByMethodCode(RootBeanDefinition beanDefinition, Method method) {
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.add(".usingFactoryMethod($T.class, %S, $L)", ClassUtils.getUserClass(method.getDeclaringClass()),
				method.getName(), generateUsingParametersCode(method.getParameterTypes()));
		return builder.build();
	}

	private CodeBlock generateUsingParametersCode(Class<?>[] parameterTypes) {
		InstanceCodeGenerationService generationService = InstanceCodeGenerationService.getSharedInstance();
		CodeBlock.Builder builder = CodeBlock.builder();
		for (int i = 0; i < parameterTypes.length; i++) {
			builder.add(i != 0 ? ", " : "");
			builder.add(generationService.generateCode(parameterTypes[i]));
		}
		return builder.build();
	}

	private  boolean isInnerClass(Class<?> type) {
		return type.isMemberClass() && !Modifier.isStatic(type.getModifiers());
	}

	void testName() {
		Dunno d = null;
		d.new Inner();


	}

	static class Dunno {

		class Inner {

		}

	}

}
