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
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.instance.InstanceCodeGenerationService;
import org.springframework.beans.factory.support.AutowiredInstantiationArgumentsResolver;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.CodeBlock;
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

	private final MethodGenerator methodGenerator;

	InstanceSupplierCodeGenerator(MethodGenerator methodGenerator) {
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
		boolean dependsOnBean = ClassUtils.isInnerClass(declaringClass);
		AccessVisibility accessVisibility = getAccessVisibility(registeredBean, constructor);
		if (accessVisibility == AccessVisibility.PUBLIC) {
			if (!dependsOnBean && constructor.getParameterCount() == 0) {
				return CodeBlock.of("$T.suppliedBy($T::new)", InstanceSupplier.class, declaringClass);
			}
			GeneratedMethod getInstanceMethod = generateGetInstanceMethod(name)
					.using(builder -> buildGetInstanceMethodForConstructor(builder, name, constructor, declaringClass,
							dependsOnBean));
			return CodeBlock.of("$T.of(this::$L)", InstanceSupplier.class, getInstanceMethod.getName());

		}
		GeneratedMethod getInstanceMethod = generateGetInstanceMethod(name).using(builder -> {
			builder.addJavadoc("Instantiate the bean instance for '$L'.", name);
			builder.returns(declaringClass);
			builder.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME);
			int parameterOffset = (!dependsOnBean) ? 0 : 1;
			CodeBlock parameterTypes = generateParameterTypesCode(constructor.getParameterTypes(), parameterOffset);
			builder.addStatement("return $T.forConstructor($L).resolveAndInstantiate($L)",
					AutowiredInstantiationArgumentsResolver.class, parameterTypes, REGISTERED_BEAN_PARAMETER_NAME);
		});
		return CodeBlock.of("$T.of(this::$L)", InstanceSupplier.class, getInstanceMethod.getName());
	}

	private CodeBlock generateCodeForFactoryMethod(String name, RegisteredBean registeredBean, Method factoryMethod) {
		Class<?> declaringClass = ClassUtils.getUserClass(factoryMethod.getDeclaringClass());
		boolean dependsOnBean = !Modifier.isStatic(factoryMethod.getModifiers());
		AccessVisibility accessVisibility = getAccessVisibility(registeredBean, factoryMethod);
		if (accessVisibility == AccessVisibility.PUBLIC) {
			if (!dependsOnBean && factoryMethod.getParameterCount() == 0) {
				return CodeBlock.of("$T.suppliedBy($T::$L)", InstanceSupplier.class, declaringClass,
						factoryMethod.getName());
			}
			GeneratedMethod getInstanceMethod = generateGetInstanceMethod(name)
					.using(builder -> buildGetInstanceMethodForFactoryMethod(builder, name, factoryMethod,
							declaringClass, dependsOnBean));
			return CodeBlock.of("$T.of(this::$L)", InstanceSupplier.class, getInstanceMethod.getName());

		}
		GeneratedMethod getInstanceMethod = generateGetInstanceMethod(name).using(builder -> {
			builder.addJavadoc("Instantiate the bean instance for '$L'.", name);
			builder.returns(declaringClass);
			builder.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME);
			CodeBlock parameterTypes = generateParameterTypesCode(factoryMethod.getParameterTypes(), 0);
			builder.addStatement("return $T.forFactoryMethod($T.class, $S, $L).resolveAndInstantiate($L)",
					AutowiredInstantiationArgumentsResolver.class, declaringClass, factoryMethod.getName(),
					parameterTypes, REGISTERED_BEAN_PARAMETER_NAME);
		});
		return CodeBlock.of("$T.of(this::$L)", InstanceSupplier.class, getInstanceMethod.getName());
	}

	private GeneratedMethod generateGetInstanceMethod(String name) {
		return InstanceSupplierCodeGenerator.this.methodGenerator.generateMethod("get", name, "instance");
	}

	private void buildGetInstanceMethodForConstructor(MethodSpec.Builder builder, String name,
			Constructor<?> constructor, Class<?> declaringClass, boolean dependsOnBean) {
		builder.addJavadoc("Create the bean instance for '$L'.", name);
		builder.returns(declaringClass);
		builder.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME);
		if (constructor.getParameterCount() == 0) {
			CodeBlock instantiationCode = generateNewInstanceCodeForConstructor(dependsOnBean, declaringClass, NO_ARGS);
			builder.addCode(generateReturnStatement(instantiationCode));
		}
		else {
			int parameterOffset = (!dependsOnBean) ? 0 : 1;
			CodeBlock parameterTypes = generateParameterTypesCode(constructor.getParameterTypes(), parameterOffset);
			CodeBlock extraction = generateArgsExtractionCode(constructor.getParameterTypes(), parameterOffset);
			CodeBlock newInstance = generateNewInstanceCodeForConstructor(dependsOnBean, declaringClass, extraction);
			CodeBlock.Builder code = CodeBlock.builder();
			code.addStatement("return $T.forConstructor($L).resolve($L, (args) -> $L)",
					AutowiredInstantiationArgumentsResolver.class, parameterTypes, REGISTERED_BEAN_PARAMETER_NAME,
					newInstance);
			builder.addCode(code.build());
		}
	}

	private void buildGetInstanceMethodForFactoryMethod(MethodSpec.Builder builder, String name, Method factoryMethod,
			Class<?> declaringClass, boolean dependsOnBean) {
		String factoryMethodName = factoryMethod.getName();
		builder.addJavadoc("Get the bean instance for '$L'.", name);
		builder.returns(factoryMethod.getReturnType());
		builder.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME);
		if (factoryMethod.getParameterCount() == 0) {
			CodeBlock instantiationCode = generateNewInstanceCodeForMethod(dependsOnBean, declaringClass,
					factoryMethodName, NO_ARGS);
			builder.addCode(generateReturnStatement(instantiationCode));
		}
		else {
			CodeBlock parameterTypes = generateParameterTypesCode(factoryMethod.getParameterTypes(), 0);
			CodeBlock extraction = generateArgsExtractionCode(factoryMethod.getParameterTypes(), 0);
			CodeBlock newInstance = generateNewInstanceCodeForMethod(dependsOnBean, declaringClass, factoryMethodName,
					extraction);
			CodeBlock.Builder code = CodeBlock.builder();
			code.addStatement("return $T.forFactoryMethod($T.class, $S, $L).resolve($L, (args) -> $L)",
					AutowiredInstantiationArgumentsResolver.class, declaringClass, factoryMethodName, parameterTypes,
					REGISTERED_BEAN_PARAMETER_NAME, newInstance);
			builder.addCode(code.build());
		}
	}

	private CodeBlock generateNewInstanceCodeForConstructor(boolean dependsOnBean, Class<?> declaringClass,
			CodeBlock args) {
		if (!dependsOnBean) {
			return CodeBlock.of("new $T($L)", declaringClass, args);
		}
		return CodeBlock.of("$L.getBeanFactory().getBean($T.class).new $L($L)", REGISTERED_BEAN_PARAMETER_NAME,
				declaringClass.getEnclosingClass(), declaringClass.getSimpleName(), args);
	}

	private CodeBlock generateNewInstanceCodeForMethod(boolean dependsOnBean, Class<?> declaringClass,
			String factoryMethodName, CodeBlock args) {
		if (!dependsOnBean) {
			return CodeBlock.of("$T.$L($L)", declaringClass, factoryMethodName, args);
		}
		return CodeBlock.of("$L.getBeanFactory().getBean($T.class).$L($L)", REGISTERED_BEAN_PARAMETER_NAME,
				declaringClass, factoryMethodName, args);
	}

	private CodeBlock generateReturnStatement(CodeBlock instantiationCode) {
		CodeBlock.Builder code = CodeBlock.builder();
		code.addStatement("return $L", instantiationCode);
		return code.build();
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
