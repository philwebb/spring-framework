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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.log.LogMessage;
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

	private final List<BeanRegistrationCodeGeneratorFactory> codeGeneratorFactories;

	/**
	 * Create a new {@link BeanDefinitionMethodGenerator} instance.
	 * @param registeredBean the registered bean
	 * @param aotContributions the AOT contributions that should be applied before
	 * generating the registration method
	 */
	BeanDefinitionMethodGenerator(BeanDefinitionMethodGeneratorFactory methodGeneratorFactory,
			RegisteredBean registeredBean, @Nullable String innerBeanPropertyName,
			List<BeanRegistrationAotContribution> aotContributions,
			List<BeanRegistrationCodeGeneratorFactory> codeGeneratorFactories) {
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
		InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator = this.methodGeneratorFactory
				.getInnerBeanDefinitionMethodGenerator(beanRegistrationsCode);
		Class<?> packagePrivateTarget = InstanceSupplierCodeGenerator.getPackagePrivateTarget(this.registeredBean);
		if (packagePrivateTarget != null) {
			GeneratedClassName generatedClassName = generationContext.getClassNameGenerator()
					.generateClassName(packagePrivateTarget, "BeanDefinitions");
			GeneratedMethods generatedMethods = new GeneratedMethods();
			GeneratedMethod generatedMethod = generateBeanDefinitionMethod(generationContext, generatedMethods,
					innerBeanDefinitionMethodGenerator, Modifier.PUBLIC);
			JavaFile javaFile = generateBeanDefinitionsJavaFile(generatedClassName, generatedMethods);
			generationContext.getGeneratedFiles().addSourceFile(javaFile, packagePrivateTarget);
			return MethodReference.of(generatedClassName, generatedMethod.getName());
		}
		MethodGenerator methodGenerator = beanRegistrationsCode.getMethodGenerator().withName(getName());
		GeneratedMethod generatedMethod = generateBeanDefinitionMethod(generationContext, methodGenerator,
				innerBeanDefinitionMethodGenerator, Modifier.PRIVATE);
		return MethodReference.of(generatedMethod.getName());
	}

	private JavaFile generateBeanDefinitionsJavaFile(GeneratedClassName registrarClassName,
			GeneratedMethods generatedMethods) {
		TypeSpec.Builder classBuilder = registrarClassName.classBuilder();
		classBuilder.addJavadoc("Bean definitions for package-private bean '$L'", getName());
		classBuilder.addModifiers(Modifier.PUBLIC);
		generatedMethods.doWithMethodSpecs(classBuilder::addMethod);
		return registrarClassName.toJavaFile(classBuilder);
	}

	private GeneratedMethod generateBeanDefinitionMethod(GenerationContext generationContext,
			MethodGenerator methodGenerator, InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator,
			Modifier modifier) {
		BeanRegistrationCodeGenerator codeGenerator = getBeanRegistrationCodeGenerator(methodGenerator,
				innerBeanDefinitionMethodGenerator);
		GeneratedMethod method = methodGenerator.generateMethod("get", "bean", "definition");
		return method.using(builder -> {
			builder.addJavadoc("Get the $L definition for '$L'",
					(!this.registeredBean.isInnerBean()) ? "bean" : "inner-bean", getName());
			builder.addModifiers(modifier);
			builder.returns(BeanDefinition.class);
			this.aotContributions.forEach(aotContribution -> aotContribution.applyTo(generationContext, codeGenerator));
			builder.addCode(codeGenerator.generateCode(generationContext));
		});
	}

	private BeanRegistrationCodeGenerator getBeanRegistrationCodeGenerator(MethodGenerator methodGenerator,
			InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator) {
		for (BeanRegistrationCodeGeneratorFactory codeGeneratorFactory : this.codeGeneratorFactories) {
			BeanRegistrationCodeGenerator codeGenerator = codeGeneratorFactory.getBeanRegistrationCodeGenerator(
					this.registeredBean, methodGenerator, innerBeanDefinitionMethodGenerator);
			if (codeGenerator != null) {
				logger.trace(LogMessage.format("Using bean registration code generator %S for '%S'",
						codeGenerator.getClass().getName(), this.registeredBean.getBeanName()));
				return codeGenerator;
			}
		}
		return new DefaultBeanRegistrationCodeGenerator(this.registeredBean, methodGenerator,
				innerBeanDefinitionMethodGenerator);
	}

	private String getName() {
		if (this.innerBeanPropertyName != null) {
			return this.innerBeanPropertyName;
		}
		if (!this.registeredBean.isGeneratedBeanName()) {
			return this.registeredBean.getBeanName();
		}
		RegisteredBean nonGeneratedParent = this.registeredBean;
		while (nonGeneratedParent != null && nonGeneratedParent.isGeneratedBeanName()) {
			nonGeneratedParent = nonGeneratedParent.getParent();
		}
		return (nonGeneratedParent != null) ? MethodNameGenerator.join(nonGeneratedParent.getBeanName(), "innerBean")
				: "innerBean";
	}

}
