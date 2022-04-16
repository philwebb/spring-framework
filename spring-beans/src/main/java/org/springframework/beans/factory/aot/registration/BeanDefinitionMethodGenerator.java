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

package org.springframework.beans.factory.aot.registration;

import java.util.List;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GeneratedClassName;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.MethodNameGenerator;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.log.LogMessage;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;
import org.springframework.lang.Nullable;

/**
 * Generates a method that returns a {@link BeanDefinition} to be registered.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanDefinitionMethodGeneratorFactory
 */
class BeanDefinitionMethodGenerator {

	private static final Log logger = LogFactory.getLog(BeanDefinitionMethodGenerator.class);

	private final BeanDefinitionMethodGeneratorFactory methodGeneratorFactory;

	private final RegisteredBean registeredBean;

	@Nullable
	private final String innerBeanPropertyName;

	private final List<BeanRegistrationAotContribution> aotContributions;

	private final List<? extends BeanRegistrationCodeGeneratorFactory> codeGeneratorFactories;

	/**
	 * Create a new {@link BeanDefinitionMethodGenerator} instance.
	 * @param registeredBean the registered bean
	 * @param aotContributions the AOT contributions that should be applied before
	 * generating the registration method
	 */
	BeanDefinitionMethodGenerator(BeanDefinitionMethodGeneratorFactory methodGeneratorFactory,
			RegisteredBean registeredBean, @Nullable String innerBeanPropertyName,
			List<BeanRegistrationAotContribution> aotContributions,
			List<? extends BeanRegistrationCodeGeneratorFactory> codeGeneratorFactories) {
		this.methodGeneratorFactory = methodGeneratorFactory;
		this.registeredBean = registeredBean;
		this.innerBeanPropertyName = innerBeanPropertyName;
		this.aotContributions = aotContributions;
		this.codeGeneratorFactories = codeGeneratorFactories;
	}

	/**
	 * Generate the method that returns the {@link BeanDefinition} to be registered.
	 * @param generationContext the generation context
	 * @param beanRegistrationsCode the bean registrations code
	 * @return a reference to the generated method.
	 */
	MethodReference generateBeanDefinitionMethod(GenerationContext generationContext,
			BeanRegistrationsCode beanRegistrationsCode) {
		BeanRegistrationCodeGeneratorFactory codeGeneratorFactory = getBeanRegistrationCodeGeneratorFactory();
		Class<?> packagePrivateTarget = codeGeneratorFactory.getPackagePrivateTarget(this.registeredBean);
		if (packagePrivateTarget != null) {
			GeneratedClassName registrarClassName = generationContext.getClassNameGenerator()
					.generateClassName(packagePrivateTarget, "Registrar");
			GeneratedMethods generatedMethods = new GeneratedMethods();
			GeneratedMethod generatedMethod = generateBeanDefinitionMethod(generationContext, beanRegistrationsCode,
					codeGeneratorFactory, generatedMethods, Modifier.PUBLIC);
			JavaFile registrar = generateRegistrar(registrarClassName, generatedMethods);
			generationContext.getGeneratedFiles().addSourceFile(registrar, packagePrivateTarget);
			return MethodReference.of(registrarClassName, generatedMethod.getName());
		}
		MethodGenerator methodGenerator = beanRegistrationsCode.getMethodGenerator().withName(getName());
		GeneratedMethod generatedMethod = generateBeanDefinitionMethod(generationContext, beanRegistrationsCode,
				codeGeneratorFactory, methodGenerator, Modifier.PRIVATE);
		return MethodReference.of(generatedMethod.getName());
	}

	private JavaFile generateRegistrar(GeneratedClassName registrarClassName,
			GeneratedMethods generatedMethods) {
		TypeSpec.Builder classBuilder = registrarClassName.classBuilder();
		classBuilder.addJavadoc("BeanDefinition registrar for package-private bean '$L'", getName());
		classBuilder.addModifiers(Modifier.PUBLIC);
		generatedMethods.doWithMethodSpecs(classBuilder::addMethod);
		return registrarClassName.toJavaFile(classBuilder);
	}

	private String getName() {
		String name = (this.innerBeanPropertyName != null) ? this.innerBeanPropertyName : getName(registeredBean);
		return name;
	}

	private String getName(RegisteredBean registeredBean) {
		if (!registeredBean.isGeneratedBeanName()) {
			return registeredBean.getBeanName();
		}
		while (registeredBean != null && registeredBean.isGeneratedBeanName()) {
			registeredBean = registeredBean.getParent();
		}
		if (registeredBean != null) {
			return MethodNameGenerator.join(registeredBean.getBeanName(), "innerBean");
		}
		return "innerBean";
	}

	private BeanRegistrationCodeGeneratorFactory getBeanRegistrationCodeGeneratorFactory() {
		for (BeanRegistrationCodeGeneratorFactory candidate : this.codeGeneratorFactories) {
			if (candidate.isSupported(this.registeredBean)) {
				return candidate;
			}
		}
		return DefaultBeanRegistrationCodeGeneratorFactory.INSTANCE;
	}

	private GeneratedMethod generateBeanDefinitionMethod(GenerationContext generationContext,
			BeanRegistrationsCode beanRegistrationsCode, BeanRegistrationCodeGeneratorFactory codeGeneratorFactory,
			MethodGenerator methodGenerator, Modifier modifier) {
		BeanRegistrationCodeGenerator codeGenerator = codeGeneratorFactory.getBeanRegistrationCodeGenerator(
				methodGenerator, getInnerBeanDefinitionMethodGenerator(beanRegistrationsCode), this.registeredBean);
		if (codeGeneratorFactory != DefaultBeanRegistrationCodeGeneratorFactory.INSTANCE) {
			logger.trace(LogMessage.format("Using custom bean registration code generator %S for '%S'",
					codeGenerator.getClass().getName(), this.registeredBean.getBeanName()));
		}
		GeneratedMethod method = methodGenerator.generateMethod("get", getName(), "BeanDefinition");
		applyAotContributions(generationContext, codeGenerator);
		return method.using(builder -> {
			builder.addJavadoc("Get the $L definition for '$L'",
					(!this.registeredBean.isInnerBean()) ? "bean" : "inner-bean", getName());
			builder.addModifiers(modifier);
			builder.returns(BeanDefinition.class);
			CodeBlock methodCode = codeGenerator.generateCode(generationContext);
			builder.addCode(methodCode);
		});
	}

	/**
	 * @param beanRegistrationsCode
	 * @return
	 */
	private InnerBeanDefinitionMethodGenerator getInnerBeanDefinitionMethodGenerator(
			BeanRegistrationsCode beanRegistrationsCode) {
		InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator = this.methodGeneratorFactory
				.getInnerBeanDefinitionMethodGenerator(beanRegistrationsCode);
		return innerBeanDefinitionMethodGenerator;
	}

	private void applyAotContributions(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
		this.aotContributions
				.forEach(aotContribution -> aotContribution.applyTo(generationContext, beanRegistrationCode));
	}

	private static class DefaultBeanRegistrationCodeGeneratorFactory implements BeanRegistrationCodeGeneratorFactory {

		static final DefaultBeanRegistrationCodeGeneratorFactory INSTANCE = new DefaultBeanRegistrationCodeGeneratorFactory();

		@Override
		public boolean isSupported(RegisteredBean registeredBean) {
			return true;
		}

		@Override
		public BeanRegistrationCodeGenerator getBeanRegistrationCodeGenerator(MethodGenerator methodGenerator,
				InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator, RegisteredBean registeredBean) {
			return new DefaultBeanRegistrationCodeGenerator(methodGenerator, innerBeanDefinitionMethodGenerator,
					registeredBean);
		}

	}

}
