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

package org.springframework.beans.factory.aot;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Function;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.instance.InstanceCodeGenerationService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ClassUtils;

/**
 * Code generator to create an {@link InstanceSupplier}.
 * <p>
 * Generates code in the following form:<pre class="code">{@code
 * InstanceSupplier.of(this::getMyBeanInstance);
 * }</pre>
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class InstanceSupplierCodeGenerator {

	static final String BEAN_FACTORY_VARIABLE = BeanRegistrationMethodCodeGenerator.BEAN_FACTORY_VARIABLE;

	private final GeneratedMethods generatedMethods;

	private final Function<BeanDefinition, Executable> constructorOrFactoryMethodResolver;

	/**
	 * Create a new {@link InstanceSupplierCodeGenerator} instance.
	 * @param generatedMethods the generated methods
	 * @param constructorOrFactoryMethodResolver resolver used to find the constructor or
	 * factory method for a bean definition
	 */
	InstanceSupplierCodeGenerator(GeneratedMethods generatedMethods,
			Function<BeanDefinition, Executable> constructorOrFactoryMethodResolver) {
		this.generatedMethods = generatedMethods;
		this.constructorOrFactoryMethodResolver = constructorOrFactoryMethodResolver;
	}

	/**
	 * Generate code to create the {@link RootBeanDefinition} configured with an instance
	 * supplier.
	 * @param beanDefinition the source bean definition
	 * @param name a name that can be used when generating new methods. The bean name
	 * should be used unless generating source for an inner-beans
	 * @return the generated code
	 */
	CodeBlock generateCode(BeanDefinition beanDefinition, String name) {
		return new CodeBuilder(beanDefinition, name).build();
	}

	/**
	 * Builder used to create the {@link CodeBlock}.
	 */
	private class CodeBuilder {

		private final BeanDefinition beanDefinition;

		private final String name;

		CodeBuilder(BeanDefinition beanDefinition, String name) {
			this.beanDefinition = beanDefinition;
			this.name = name;
		}

		CodeBlock build() {
			CodeBlock.Builder builder = CodeBlock.builder();
			addSupplyBeanDefinition(builder);
			addUsingAndGeneratedBy(builder);
			return builder.build();
		}

		private void addSupplyBeanDefinition(CodeBlock.Builder builder) {
			builder.add("$T.supply(", RootBeanDefinition.class);
			ResolvableType beanType = this.beanDefinition.getResolvableType();
			Object supplyType = beanType;
			if (!beanType.hasGenerics() || hasAnyUnresolvableGenerics(beanType)) {
				supplyType = ClassUtils.getUserClass(beanType.toClass());
			}
			builder.add(InstanceCodeGenerationService.getSharedInstance().generateCode(supplyType));
			builder.add(")");
		}

		private boolean hasAnyUnresolvableGenerics(ResolvableType type) {
			return type.hasUnresolvableGenerics()
					|| Arrays.stream(type.getGenerics()).anyMatch(this::hasAnyUnresolvableGenerics);
		}

		private void addUsingAndGeneratedBy(CodeBlock.Builder builder) {
			Executable constructorOrFactoryMethod = InstanceSupplierCodeGenerator.this.constructorOrFactoryMethodResolver
					.apply(this.beanDefinition);
			if (constructorOrFactoryMethod instanceof Constructor<?> constructor) {
				addUsingAndGeneratedByConstructor(builder, constructor);
				return;
			}
			if (constructorOrFactoryMethod instanceof Method method) {
				addUsingAndGeneratedByFactoryMethod(builder, method);
				return;
			}
			throw new IllegalStateException("No suitable executor found for " + this.beanDefinition);
		}

		private void addUsingAndGeneratedByConstructor(CodeBlock.Builder builder, Constructor<?> constructor) {
			Class<?> declaringClass = ClassUtils.getUserClass(constructor.getDeclaringClass());
			boolean innerClass = ClassUtils.isInnerClass(declaringClass);
			int parameterOffset = (!innerClass) ? 0 : 1;
			CodeBlock parametersCode = generateUsingParametersCode(constructor.getParameterTypes(), parameterOffset);
			builder.add(".usingConstructor($L)", parametersCode);
			if (constructor.getParameterCount() == 0 && !innerClass) {
				builder.add(".resolvedBy($T::new)", declaringClass);
				return;
			}
			GeneratedMethod getBeanInstanceMethod = generateGetBeanInstanceMethod(this.name,
					constructor, declaringClass);
			builder.add(".resolvedBy($L, this::$L)", BEAN_FACTORY_VARIABLE, getBeanInstanceMethod.getName());
		}

		private GeneratedMethod generateGetBeanInstanceMethod(String name, Constructor<?> constructor,
				Class<?> declaringClass) {
			boolean isInnerClass = ClassUtils.isInnerClass(declaringClass);
			return InstanceSupplierCodeGenerator.this.generatedMethods.add("get", name, "instance")
					.generateBy(builder -> {
						builder.addJavadoc("Create the bean instance for '$L'.", name);
						builder.returns(declaringClass);
						if (isInnerClass) {
							builder.addParameter(BeanFactory.class, BEAN_FACTORY_VARIABLE);
						}
						builder.addParameter(Object[].class, "args");
						CodeBlock.Builder code = CodeBlock.builder();
						code.add("return ");
						if (!isInnerClass) {
							code.add("new $T(", declaringClass);
						}
						else {
							code.add("beanFactory.getBean($T.class).new $L(", declaringClass.getEnclosingClass(),
									declaringClass.getSimpleName());
						}
						addExtractFromArgs(code, constructor.getParameterTypes(), (!isInnerClass) ? 0 : 1);
						code.add(");");
						builder.addCode(code.build());
					});
		}

		private void addUsingAndGeneratedByFactoryMethod(CodeBlock.Builder builder, Method factoryMethod) {
			Class<?> factoryClass = ClassUtils.getUserClass(factoryMethod.getDeclaringClass());
			String factoryMethodName = factoryMethod.getName();
			builder.add(".usingFactoryMethod($T.class, $S", factoryClass, factoryMethodName);
			CodeBlock parametersCode = generateUsingParametersCode(factoryMethod.getParameterTypes(), 0);
			if (!parametersCode.isEmpty()) {
				builder.add(", $L", parametersCode);
			}
			builder.add(")");
			GeneratedMethod getBeanInstanceMethod = generateGetBeanInstanceMethod(this.name,
					factoryMethod);
			builder.add(".resolvedBy($L, this::$L)", BEAN_FACTORY_VARIABLE, getBeanInstanceMethod.getName());
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

		private GeneratedMethod generateGetBeanInstanceMethod(String name, Method factoryMethod) {
			boolean staticFactoryMethod = Modifier.isStatic(factoryMethod.getModifiers());
			Class<?> declaringClass = factoryMethod.getDeclaringClass();
			Class<?> returnType = factoryMethod.getReturnType();
			return InstanceSupplierCodeGenerator.this.generatedMethods.add("get", name, "instance")
					.generateBy(builder -> {
						builder.addJavadoc("Get the bean instance for '$L'.", name);
						builder.returns(returnType);
						if (!staticFactoryMethod) {
							builder.addParameter(BeanFactory.class, "beanFactory");
						}
						builder.addParameter(Object[].class, "args");
						CodeBlock.Builder code = CodeBlock.builder();
						code.add("return ");
						if (staticFactoryMethod) {
							code.add("$T", declaringClass);
						}
						else {
							code.add("beanFactory.getBean($T.class)", declaringClass);
						}
						code.add(".$L(", factoryMethod.getName());
						addExtractFromArgs(code, factoryMethod.getParameterTypes(), 0);
						code.add(");");
						builder.addCode(code.build());
					});
		}

		private void addExtractFromArgs(CodeBlock.Builder builder, Class<?>[] parameterTypes, int startIndex) {
			for (int i = startIndex; i < parameterTypes.length; i++) {
				builder.add((i != startIndex) ? ", " : "");
				if (!parameterTypes[i].equals(Object.class)) {
					builder.add("($T) ", parameterTypes[i]);
				}
				builder.add("args[$L]", i - startIndex);
			}
		}

	}

}