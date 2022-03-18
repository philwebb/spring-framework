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

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.instance.InstanceCodeGenerationService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ClassUtils;

/**
 * Code generator to create a {@link RootBeanDefinition} via
 * {@link RootBeanDefinition#supply(ResolvableType)}.
 * <p>
 * For example:
 * <p>
 * <pre class="code">
 * RootBeanDefinition.supply(MyBean.class)
 * 		.usingConstructor()
 * 		.resolvedBy(MyBean::new);
 * </pre> This generator creates a {@link RootBeanDefinition} with only the
 * {@link RootBeanDefinition#setInstanceSupplier(java.util.function.Supplier) instance
 * supplier} set. Additional bean definition properties (such as
 * {@link AbstractBeanDefinition#setScope(String)} can be set using a
 * {@link BeanDefinitionPropertiesCodeGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class SuppliedInstanceBeanDefinitionCodeGenerator {

	// DefaultBeanRegistrationContributionProvider and DefaultBeanInstantiationGenerator

	private final DefaultListableBeanFactory beanFactory;

	private final String beanFactoryVariable = "beanFactory";

	private final GeneratedMethods generatedMethods;

	private ConstructorOrFactoryMethodResolver constructorOrFactoryMethodResolver;

	private final String beanName;

	SuppliedInstanceBeanDefinitionCodeGenerator(DefaultListableBeanFactory beanFactory,
			GeneratedMethods generatedMethods, String beanName) {
		this.beanFactory = beanFactory;
		this.generatedMethods = generatedMethods;
		this.constructorOrFactoryMethodResolver = new ConstructorOrFactoryMethodResolver(this.beanFactory);
		this.beanName = beanName;
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
		boolean innerClass = ClassUtils.isInnerClass(declaringClass);
		int parameterOffset = (!innerClass) ? 0 : 1;
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.add(".usingConstructor($L)",
				generateUsingParametersCode(constructor.getParameterTypes(), parameterOffset));
		if (constructor.getParameterCount() == 0 && !innerClass) {
			builder.add(".resolvedBy($T::new)", declaringClass);
			return builder.build();
		}
		GeneratedMethod getBeanInstanceMethod = generateGetBeanInstanceMethod(beanDefinition, constructor,
				declaringClass);
		builder.add(".resolvedBy($L, this::$L)", this.beanFactoryVariable, getBeanInstanceMethod.getName());
		return builder.build();
	}

	private GeneratedMethod generateGetBeanInstanceMethod(RootBeanDefinition beanDefinition, Constructor<?> constructor,
			Class<?> declaringClass) {
		return this.generatedMethods.add("get", beanName, "instance").generateBy((builder) -> {
			boolean isInnerClass = ClassUtils.isInnerClass(declaringClass);
			builder.returns(declaringClass);
			if (isInnerClass) {
				builder.addParameter(BeanFactory.class, "beanFactory");
			}
			builder.addParameter(Object[].class, "args");
			CodeBlock.Builder code = CodeBlock.builder();
			code.add("return ");
			if (isInnerClass) {
				code.add("beanFactory.getBean($T.class).new $L(", declaringClass.getEnclosingClass(),
						declaringClass.getSimpleName());
			}
			else {
				code.add("new $T(", declaringClass);
			}
			Class<?>[] parameterTypes = constructor.getParameterTypes();
			int startIndex = (!isInnerClass) ? 0 : 1;
			for (int i = startIndex; i < parameterTypes.length; i++) {
				code.add((i != startIndex) ? ", " : "");
				if (!parameterTypes[i].equals(Object.class)) {
					code.add("($T) ", parameterTypes[i]);
				}
				code.add("args[$L]", i - startIndex);
			}
			code.add(");");
			builder.addCode(code.build());
		});
	}

	private CodeBlock generateUsingAndGeneratedByMethodCode(RootBeanDefinition beanDefinition, Method method) {
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.add(".usingFactoryMethod($T.class, %S, $L)", ClassUtils.getUserClass(method.getDeclaringClass()),
				method.getName(), generateUsingParametersCode(method.getParameterTypes(), 0));
		return builder.build();
	}

	private CodeBlock generateUsingParametersCode(Class<?>[] parameterTypes, int offset) {
		InstanceCodeGenerationService generationService = InstanceCodeGenerationService.getSharedInstance();
		CodeBlock.Builder builder = CodeBlock.builder();
		for (int i = offset; i < parameterTypes.length; i++) {
			builder.add(i != offset ? ", " : "");
			builder.add(generationService.generateCode(parameterTypes[i]));
		}
		return builder.build();
	}

}
