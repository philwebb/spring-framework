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
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.aot.generate.AccessVisibility;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.instance.InstanceCodeGenerationService;
import org.springframework.beans.factory.support.AutowiredInstantiationArgumentsResolver;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.MethodSpec;
import org.springframework.util.ClassUtils;

/**
 * Internal code generator to create an {@link InstanceSupplier}.
 * <p>
 * Generates code in one of the following forms:<pre class="code">{@code
 * InstanceSupplier.of(this::getMyBeanInstance);
 * }</pre> <pre class="code">{@code
 * InstanceSupplier.of(this::getMyBeanInstance);
 * }</pre>
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class InstanceSupplierCodeGenerator {

	private static final String REGISTERED_BEAN_PARAMETER_NAME = "registeredBean";

	private static final CodeBlock NO_ARGS = CodeBlock.of("");

	private final GenerationContext generationContext;

	private final MethodGenerator methodGenerator;

	InstanceSupplierCodeGenerator(GenerationContext generationContext, MethodGenerator methodGenerator) {
		this.generationContext = generationContext;
		this.methodGenerator = methodGenerator;
	}

	CodeBlock generateCode(RegisteredBean registeredBean) {
		return generateCode(registeredBean.getBeanName(), registeredBean);
	}

	CodeBlock generateCode(String name, RegisteredBean registeredBean) {
		Executable constructorOrFactoryMethod = ConstructorOrFactoryMethodResolver.resolve(registeredBean);
		if (constructorOrFactoryMethod instanceof Constructor<?> constructor) {
			return generateCodeForConstructor(name, registeredBean, constructor);
		}
		if (constructorOrFactoryMethod instanceof Method method) {
			return generateCodeForFactoryMethod(name, registeredBean, method);
		}
		throw new IllegalStateException("No suitable executor found for " + registeredBean.getBeanName());
	}

	private CodeBlock generateCodeForConstructor(String name, RegisteredBean registeredBean,
			Constructor<?> constructor) {
		Class<?> declaringClass = ClassUtils.getUserClass(constructor.getDeclaringClass());
		boolean isInnerClass = ClassUtils.isInnerClass(declaringClass);
		AccessVisibility accessVisibility = getAccessVisibility(registeredBean, constructor);
		if (accessVisibility == AccessVisibility.PUBLIC) {
			if (!isInnerClass && constructor.getParameterCount() == 0) {
				return CodeBlock.of("$T.suppliedBy($T::new)", InstanceSupplier.class, declaringClass);
			}
			GeneratedMethod getInstanceMethod = InstanceSupplierCodeGenerator.this.methodGenerator
					.generateMethod("get", name, "instance").using(builder -> buildGetInstanceMethodForConstructor(name,
							declaringClass, isInnerClass, constructor, builder));
			return CodeBlock.of("$T.of(this::$L)", InstanceSupplier.class, getInstanceMethod.getName());

		}
		throw new IllegalStateException("Only public classes supported"); // FIXME
	}

	private void buildGetInstanceMethodForConstructor(String name, Class<?> declaringClass, boolean isInnerClass,
			Constructor<?> constructor, MethodSpec.Builder builder) {
		// drop is innser class and inline
		builder.addJavadoc("Create the bean instance for '$L'.", name);
		builder.returns(declaringClass);
		builder.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME);
		if (constructor.getParameterCount() == 0) {
			CodeBlock instantiationCode = generateNewInstanceCodeForConstructor(isInnerClass, declaringClass, NO_ARGS);
			builder.addCode(CodeBlock.of("return $L", instantiationCode));
		}
		else {
			int parameterOffset = (!isInnerClass) ? 0 : 1;
			CodeBlock ParameterTypes = generateParameterTypesCode(constructor.getParameterTypes(), parameterOffset);
			CodeBlock extraction = generateArgsExtractionCode(constructor.getParameterTypes(), parameterOffset);
			CodeBlock newInstance = generateNewInstanceCodeForConstructor(isInnerClass, declaringClass, extraction);
			CodeBlock.Builder code = CodeBlock.builder();
			code.addStatement("return $T.forConstructor($L).resolve($L, (args) -> $L)",
					AutowiredInstantiationArgumentsResolver.class, ParameterTypes, REGISTERED_BEAN_PARAMETER_NAME,
					newInstance);
			builder.addCode(code.build());
		}
	}

	private CodeBlock generateNewInstanceCodeForConstructor(boolean isInnerClass, Class<?> declaringClass,
			CodeBlock args) {
		if (!isInnerClass) {
			return CodeBlock.of("new $T($L)", declaringClass, args);
		}
		return CodeBlock.of("$L.getBeanFactory().getBean($T.class).new $L($L)", REGISTERED_BEAN_PARAMETER_NAME,
				declaringClass.getEnclosingClass(), declaringClass.getSimpleName(), args);
	}

	private CodeBlock generateCodeForFactoryMethod(String name, RegisteredBean registeredBean, Method factoryMethod) {
		Class<?> factoryClass = ClassUtils.getUserClass(factoryMethod.getDeclaringClass());
		boolean isStaticFactoryMethod = Modifier.isStatic(factoryMethod.getModifiers());
		AccessVisibility accessVisibility = getAccessVisibility(registeredBean, factoryMethod);
		if (accessVisibility == AccessVisibility.PUBLIC) {
			if (isStaticFactoryMethod && factoryMethod.getParameterCount() == 0) {
				return CodeBlock.of("$T.suppliedBy($T::$L)", InstanceSupplier.class, factoryClass,
						factoryMethod.getName());
			}
			GeneratedMethod getInstanceMethod = InstanceSupplierCodeGenerator.this.methodGenerator
					.generateMethod("get", name, "instance")
					.using(builder -> buildGetInstanceMethodForFactoryMethod(name, factoryClass, isStaticFactoryMethod,
							factoryMethod, builder));
			return CodeBlock.of("$T.of(this::$L)", InstanceSupplier.class, getInstanceMethod.getName());

		}
		throw new IllegalStateException("Only public factory methods supported"); // FIXME
	}

	private void buildGetInstanceMethodForFactoryMethod(String name, Class<?> factoryClass,
			boolean isStaticFactoryMethod, Method factoryMethod, MethodSpec.Builder builder) {
		Class<?> declaringClass = factoryMethod.getDeclaringClass();
		Class<?> returnType = factoryMethod.getReturnType();
		builder.addJavadoc("Get the bean instance for '$L'.", name);
		builder.returns(returnType);
		builder.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME);
		if (factoryMethod.getParameterCount() == 0) {
			CodeBlock instantiationCode = generateNewInstanceCodeForMethod(isStaticFactoryMethod, declaringClass,
					factoryMethod, NO_ARGS);
			CodeBlock.Builder code = CodeBlock.builder();
			code.addStatement("return $L", instantiationCode);
			builder.addCode(code.build());
		}
		else {
			CodeBlock ParameterTypes = generateParameterTypesCode(factoryMethod.getParameterTypes(), 0);
			CodeBlock extraction = generateArgsExtractionCode(factoryMethod.getParameterTypes(), 0);
			CodeBlock newInstance = generateNewInstanceCodeForMethod(isStaticFactoryMethod, declaringClass,
					factoryMethod, extraction);
			CodeBlock.Builder code = CodeBlock.builder();
			code.addStatement("return $T.forFactoryMethod($L).resolve($L, (args) -> $L)",
					AutowiredInstantiationArgumentsResolver.class, ParameterTypes, REGISTERED_BEAN_PARAMETER_NAME,
					newInstance);
			builder.addCode(code.build());
		}
	}

	private CodeBlock generateNewInstanceCodeForMethod(boolean isStaticFactoryMethod, Class<?> declaringClass,
			Method factoryMethod, CodeBlock args) {
		// FIXME use fm directly and check unused params
		if (isStaticFactoryMethod) {
			return CodeBlock.of("$T.$L($L)", declaringClass, factoryMethod.getName(), args);
		}
		return CodeBlock.of("$L.getBeanFactory().getBean($T.class).$L($L)", REGISTERED_BEAN_PARAMETER_NAME,
				declaringClass, factoryMethod.getName(), args);
	}

	protected AccessVisibility getAccessVisibility(RegisteredBean registeredBean, Member member) {
		AccessVisibility beanTypeAccessVisibility = AccessVisibility.forResolvableType(registeredBean.getBeanType());
		AccessVisibility memberAccessVisibility = AccessVisibility.forMember(member);
		return AccessVisibility.lowest(beanTypeAccessVisibility, memberAccessVisibility);
	}

	private CodeBlock generateParameterTypesCode(Class<?>[] parameterTypes, int offset) {
		InstanceCodeGenerationService generationService = InstanceCodeGenerationService.getSharedInstance();
		CodeBlock.Builder builder = CodeBlock.builder();
		for (int i = offset; i < parameterTypes.length; i++) {
			builder.add(i != offset ? ", " : "");
			builder.add(generationService.generateCode(parameterTypes[i]));
		}
		return builder.build();
	}

	private CodeBlock generateArgsExtractionCode(Class<?>[] parameterTypes, int startIndex) {
		CodeBlock.Builder builder = CodeBlock.builder();
		for (int i = startIndex; i < parameterTypes.length; i++) {
			builder.add((i != startIndex) ? ", " : "");
			if (!parameterTypes[i].equals(Object.class)) {
				builder.add("($T) ", parameterTypes[i]);
			}
			builder.add("args[$L]", i - startIndex);
		}
		return builder.build();
	}

}
