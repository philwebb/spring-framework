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

package org.springframework.aop.scope;

import java.lang.reflect.Constructor;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.aot.registration.BeanRegistrationCodeGenerator;
import org.springframework.beans.factory.aot.registration.InnerBeanDefinitionMethodGenerator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolder;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ScopedProxyBeanRegistrationCodeGeneratorFactory}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
class ScopedProxyBeanRegistrationCodeGeneratorFactoryTests {

	private final ScopedProxyBeanRegistrationCodeGeneratorFactory codeGeneratorFactory = new ScopedProxyBeanRegistrationCodeGeneratorFactory();

	private DefaultListableBeanFactory beanFactory;

	private InMemoryGeneratedFiles generatedFiles;

	private DefaultGenerationContext generationContext;

	private ClassName className;

	private MockBeanRegistrationsCode beanRegistrationsCode;

	private InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator;

	private MethodGenerator methodGenerator;

	@BeforeEach
	void setup() {
		this.beanFactory = new DefaultListableBeanFactory();
		this.generatedFiles = new InMemoryGeneratedFiles();
		this.generationContext = new DefaultGenerationContext(this.generatedFiles);
		this.className = ClassName.get("__", "BeanDefinitionSupplier");
		this.beanRegistrationsCode = new MockBeanRegistrationsCode(this.className);
		this.innerBeanDefinitionMethodGenerator = mock(InnerBeanDefinitionMethodGenerator.class);
		this.methodGenerator = this.beanRegistrationsCode.getMethodGenerator();
	}

	@Test
	void getBeanRegistrationCodeGeneratorWhenNotScopedProxy() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(PropertiesFactoryBean.class)
				.getBeanDefinition();
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		BeanRegistrationCodeGenerator codeGenerator = getCodeGenerator(RegisteredBean.of(this.beanFactory, "test"));
		assertThat(codeGenerator).isNull();
	}

	@Test
	void getBeanRegistrationCodeGeneratorWhenScopedProxyWithoutTargetBeanName() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(ScopedProxyFactoryBean.class)
				.getBeanDefinition();
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		BeanRegistrationCodeGenerator codeGenerator = getCodeGenerator(RegisteredBean.of(this.beanFactory, "test"));
		assertThat(codeGenerator).isNull();
	}

	@Test
	void getBeanRegistrationCodeGeneratorWhenScopedProxyWithInvalidTargetBeanName() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(ScopedProxyFactoryBean.class)
				.addPropertyValue("targetBeanName", "testDoesNotExist").getBeanDefinition();
		this.beanFactory.registerBeanDefinition("test", beanDefinition);
		BeanRegistrationCodeGenerator codeGenerator = getCodeGenerator(RegisteredBean.of(this.beanFactory, "test"));
		assertThat(codeGenerator).isNull();
	}

	@Test
	void getBeanRegistrationCodeGeneratorWhenScopedProxyWithTargetBeanName() {
		RootBeanDefinition targetBean = new RootBeanDefinition();
		targetBean.setTargetType(ResolvableType.forClassWithGenerics(NumberHolder.class, Integer.class));
		targetBean.setScope("custom");
		this.beanFactory.registerBeanDefinition("numberHolder", targetBean);
		BeanDefinition scopedBean = BeanDefinitionBuilder.rootBeanDefinition(ScopedProxyFactoryBean.class)
				.addPropertyValue("targetBeanName", "numberHolder").getBeanDefinition();
		RegisteredBean registeredBean = registerBean(scopedBean);
		testCompiledResult(registeredBean, (beanDefinition, compiled) -> {
			DefaultListableBeanFactory freshBeanFactory = new DefaultListableBeanFactory();
			freshBeanFactory.registerBeanDefinition("numberHolder", targetBean);
			freshBeanFactory.registerBeanDefinition("testScopeBean", beanDefinition);
			NumberHolder<?> bean = freshBeanFactory.getBean("testScopeBean", NumberHolder.class);
			assertThat(bean).isNotNull().isInstanceOf(AopInfrastructureBean.class);
		});
	}

	private void testCompiledResult(RegisteredBean registeredBean, BiConsumer<RootBeanDefinition, Compiled> result) {
		BeanRegistrationCodeGenerator codeGenerator = getCodeGenerator(registeredBean);
		CodeBlock codeBlock = codeGenerator.generateCode(this.generationContext);
		this.generationContext.close();
		JavaFile javaFile = createJavaFile(codeBlock);
		System.out.println(javaFile);
		TestCompiler.forSystem().withFiles(this.generatedFiles).compile(javaFile::writeTo, compiled -> {
			RootBeanDefinition beanDefinition = (RootBeanDefinition) compiled.getInstance(Supplier.class).get();
			result.accept(beanDefinition, compiled);
		});
	}

	private BeanRegistrationCodeGenerator getCodeGenerator(RegisteredBean registeredBean) {
		Constructor<?> constructor = registeredBean.getBeanType().toClass().getDeclaredConstructors()[0];
		return this.codeGeneratorFactory.getBeanRegistrationCodeGenerator(registeredBean, constructor, this.className,
				this.methodGenerator, this.innerBeanDefinitionMethodGenerator);
	}

	private JavaFile createJavaFile(CodeBlock codeBlock) {
		TypeSpec.Builder builder = TypeSpec.classBuilder("BeanDefinitionSupplier");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(ParameterizedTypeName.get(Supplier.class, RootBeanDefinition.class));
		builder.addMethod(MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC)
				.returns(RootBeanDefinition.class).addCode(codeBlock).build());
		this.beanRegistrationsCode.getGeneratedMethods().doWithMethodSpecs(builder::addMethod);
		return JavaFile.builder("__", builder.build()).build();
	}

	private RegisteredBean registerBean(BeanDefinition beanDefinition) {
		String beanName = "testScopedBean";
		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
		return RegisteredBean.of(this.beanFactory, beanName);
	}

}
