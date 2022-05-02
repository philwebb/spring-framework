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

package org.springframework.beans.factory.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.AccessVisibility;
import org.springframework.aot.generate.GeneratedClassName;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.generate.instance.InstanceCodeGenerationService;
import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.FieldHint;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.annotation.InjectionMetadata.InjectedElement;
import org.springframework.beans.factory.aot.registration.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.registration.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;

/**
 * {@link BeanRegistrationAotContribution} provided by the
 * {@link AutowiredAnnotationBeanPostProcessor} to autowire fields and methods.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class AutowiredAnnotationBeanRegistrationAotContribution implements BeanRegistrationAotContribution {

	private static final String APPLY_METHOD = "apply";

	private static final String REGISTERED_BEAN_PARAMETER = "registeredBean";

	private static final String INSTANCE_PARAMETER = "instance";

	private static final Consumer<ExecutableHint.Builder> INTROSPECT = builder -> builder
			.withMode(ExecutableMode.INTROSPECT);

	private static final Consumer<FieldHint.Builder> ALLOW_WRITE = builder -> builder.allowWrite(true);

	private final Class<?> target;

	private final Collection<InjectedElement> injectedElements;

	AutowiredAnnotationBeanRegistrationAotContribution(Class<?> target, Collection<InjectedElement> injectedElements) {
		this.target = target;
		this.injectedElements = injectedElements;
	}

	@Override
	public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
		GeneratedClassName className = generationContext.getClassNameGenerator().generateClassName(this.target,
				"Autowiring");
		TypeSpec.Builder classBuilder = className.classBuilder();
		classBuilder.addJavadoc("Autowiring for {@link $T}.", this.target);
		classBuilder.addModifiers(Modifier.PUBLIC);
		classBuilder.addMethod(generateMethod(generationContext.getRuntimeHints()));
		JavaFile javaFile = className.toJavaFile(classBuilder);
		generationContext.getGeneratedFiles().addSourceFile(javaFile);
		beanRegistrationCode.addInstancePostProcessor(MethodReference.ofStatic(className, APPLY_METHOD));
	}

	private MethodSpec generateMethod(RuntimeHints hints) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder(APPLY_METHOD);
		builder.addJavadoc("Apply the autowiring.");
		builder.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
		builder.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER);
		builder.addParameter(this.target, INSTANCE_PARAMETER);
		builder.returns(this.target);
		builder.addCode(generateMethodCode(hints));
		return builder.build();
	}

	private CodeBlock generateMethodCode(RuntimeHints hints) {
		CodeBlock.Builder builder = CodeBlock.builder();
		for (InjectedElement injectedElement : this.injectedElements) {
			builder.addStatement(generateMethodStatementForElement(injectedElement, hints));
		}
		builder.addStatement("return $L", INSTANCE_PARAMETER);
		return builder.build();
	}

	private CodeBlock generateMethodStatementForElement(InjectedElement injectedElement, RuntimeHints hints) {
		Member member = injectedElement.getMember();
		boolean required = injectedElement.required;
		if (member instanceof Field field) {
			return generateMethodStatementForField(field, required, hints);
		}
		if (member instanceof Method method) {
			return generateMethodStatementForMethod(method, required, hints);
		}
		throw new IllegalStateException("Unsupported member type " + member.getClass().getName());
	}

	private CodeBlock generateMethodStatementForField(Field field, boolean required, RuntimeHints hints) {
		CodeBlock resolver = CodeBlock.of("$T.$L($S)", AutowiredFieldValueResolver.class,
				(!required) ? "forField" : "forRequiredField", field.getName());
		AccessVisibility visibility = AccessVisibility.forMember(field);
		if (visibility == AccessVisibility.PRIVATE || visibility == AccessVisibility.PROTECTED) {
			hints.reflection().registerField(field);
			return CodeBlock.of("$L.resolveAndSet($L, $L)", resolver, REGISTERED_BEAN_PARAMETER, INSTANCE_PARAMETER);
		}
		hints.reflection().registerField(field, ALLOW_WRITE);
		return CodeBlock.of("$L.$L = $L.resolve($L)", INSTANCE_PARAMETER, field.getName(), resolver, REGISTERED_BEAN_PARAMETER);
	}

	private CodeBlock generateMethodStatementForMethod(Method method, boolean required, RuntimeHints hints) {
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.add("$T.$L", AutowiredMethodArgumentsResolver.class, (!required) ? "forMethod" : "forRequiredMethod");
		builder.add("($S", method.getName());
		if (method.getParameterCount() > 0) {
			builder.add(", $L", generateParameterTypesCode(method.getParameterTypes()));
		}
		builder.add(")");
		AccessVisibility visibility = AccessVisibility.forMember(method);
		if (visibility == AccessVisibility.PRIVATE || visibility == AccessVisibility.PROTECTED) {
			hints.reflection().registerMethod(method);
			builder.add(".resolveAndInvoke($L, $L)", REGISTERED_BEAN_PARAMETER, INSTANCE_PARAMETER);
		}
		else {
			hints.reflection().registerMethod(method, INTROSPECT);
			CodeBlock arguments = new AutowiredArgumentsCodeGenerator(this.target, method)
					.generateCode(method.getParameterTypes());
			CodeBlock injectionCode = CodeBlock.of("args -> $L.$L($L)", INSTANCE_PARAMETER, method.getName(),
					arguments);
			builder.add(".resolve($L, $L)", REGISTERED_BEAN_PARAMETER, injectionCode);
		}
		return builder.build();
	}

	private CodeBlock generateParameterTypesCode(Class<?>[] parameterTypes) {
		InstanceCodeGenerationService generationService = InstanceCodeGenerationService.getSharedInstance();
		CodeBlock.Builder builder = CodeBlock.builder();
		for (int i = 0; i < parameterTypes.length; i++) {
			builder.add(i != 0 ? ", " : "");
			builder.add(generationService.generateCode(parameterTypes[i]));
		}
		return builder.build();
	}

}
