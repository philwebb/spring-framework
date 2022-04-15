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

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.aot.AotFactoriesLoader;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.AnnotatedBean;
import org.springframework.beans.testfixture.beans.GenericBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.ResolvableType;
import org.springframework.core.mock.MockSpringFactoriesLoader;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultBeanRegistrationCodeGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see BeanDefinitionPropertiesCodeGeneratorTests
 * @see InstanceSupplierCodeGeneratorTests
 */
class DefaultBeanRegistrationCodeGeneratorTests {

	private InMemoryGeneratedFiles generatedFiles;

	private GenerationContext generationContext;

	private DefaultListableBeanFactory beanFactory;

	private MockSpringFactoriesLoader springFactoriesLoader;

	private MockBeanRegistrationsCode beanRegistrationsCode;

	@BeforeEach
	void setup() {
		this.generatedFiles = new InMemoryGeneratedFiles();
		this.generationContext = new DefaultGenerationContext(this.generatedFiles);
		this.beanFactory = new DefaultListableBeanFactory();
		this.springFactoriesLoader = new MockSpringFactoriesLoader();
		BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
				new AotFactoriesLoader(this.beanFactory, this.springFactoriesLoader));
		this.beanRegistrationsCode = new MockBeanRegistrationsCode(methodGeneratorFactory);
	}

	@Test
	void generateCodeWhenBeanDoesNotHaveGenerics() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		RegisteredBean registeredBean = registerBean(beanDefinition);
		DefaultBeanRegistrationCodeGenerator generator = new DefaultBeanRegistrationCodeGenerator(registeredBean, null,
				this.beanRegistrationsCode);
		testCompiledResult(generator, registeredBean, (actual, compiled) -> {
			assertThat(actual.getBeanClass()).isEqualTo(TestBean.class);
			assertThat(compiled.getSourceFile()).contains("beanType = TestBean.class");
		});
	}

	@Test
	void generateCodeWhenBeanHasGenerics() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(
				ResolvableType.forClassWithGenerics(GenericBean.class, Integer.class));
		RegisteredBean registeredBean = registerBean(beanDefinition);
		DefaultBeanRegistrationCodeGenerator generator = new DefaultBeanRegistrationCodeGenerator(registeredBean, null,
				this.beanRegistrationsCode);
		testCompiledResult(generator, registeredBean, (actual, compiled) -> {
			assertThat(actual.getResolvableType().resolve()).isEqualTo(GenericBean.class);
			assertThat(compiled.getSourceFile())
					.contains("beanType = ResolvableType.forClassWithGenerics(GenericBean.class, Integer.class)");
		});
	}

	@Test
	void generateCodeWithAttributeIncludeFilter() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		beanDefinition.setAttribute("a", "A");
		beanDefinition.setAttribute("b", "B");
		RegisteredBean registeredBean = registerBean(beanDefinition);
		DefaultBeanRegistrationCodeGenerator generator = new DefaultBeanRegistrationCodeGenerator(registeredBean, null,
				this.beanRegistrationsCode) {

			@Override
			protected boolean isAttributeIncluded(String attributeName) {
				return "a".equals(attributeName);
			}

		};
		testCompiledResult(generator, registeredBean, (actual, compiled) -> {
			assertThat(actual.getAttribute("a")).isEqualTo("A");
			assertThat(actual.getAttribute("b")).isNull();
		});
	}

	@Test
	void generateCodeWithInnerBean() {
		RootBeanDefinition innerBeanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
				.rootBeanDefinition(AnnotatedBean.class).setRole(BeanDefinition.ROLE_INFRASTRUCTURE).setPrimary(true)
				.getBeanDefinition();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
		beanDefinition.getPropertyValues().add("name", innerBeanDefinition);
		RegisteredBean registeredBean = registerBean(beanDefinition);
		DefaultBeanRegistrationCodeGenerator generator = new DefaultBeanRegistrationCodeGenerator(registeredBean, null,
				this.beanRegistrationsCode);
		testCompiledResult(generator, registeredBean, (actual, compiled) -> {
			RootBeanDefinition actualInnerBeanDefinition = (RootBeanDefinition) actual.getPropertyValues().get("name");
			assertThat(actualInnerBeanDefinition.isPrimary()).isTrue();
			assertThat(actualInnerBeanDefinition.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
			InstanceSupplier<?> innerInstanceSupplier = (InstanceSupplier<?>) actualInnerBeanDefinition
					.getInstanceSupplier();
			try {
				assertThat(innerInstanceSupplier.get(RegisteredBean.of(this.beanFactory, "temp")))
						.isInstanceOf(AnnotatedBean.class);
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		});
	}

	@Test
	void generateCodeForPackagePrivateType() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(PackagePrivateTestBean.class);
		RegisteredBean registeredBean = registerBean(beanDefinition);
		DefaultBeanRegistrationCodeGenerator generator = new DefaultBeanRegistrationCodeGenerator(registeredBean, null,
				this.beanRegistrationsCode);
		testCompiledResult(generator, registeredBean, (actual, compiled) -> {
			assertThat(actual.getBeanClass()).isEqualTo(PackagePrivateTestBean.class);
		});
	}

	private RegisteredBean registerBean(RootBeanDefinition beanDefinition) {
		String beanName = "testBean";
		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
		return RegisteredBean.of(this.beanFactory, beanName);
	}

	private void testCompiledResult(DefaultBeanRegistrationCodeGenerator generator, RegisteredBean registeredBean,
			BiConsumer<RootBeanDefinition, Compiled> result) {
		CodeBlock codeBlock = generator.generateCode(this.generationContext);
		JavaFile javaFile = createJavaFile(codeBlock);
		TestCompiler.forSystem().compile(javaFile::writeTo, compiled -> {
			RootBeanDefinition beanDefinition = (RootBeanDefinition) compiled.getInstance(Supplier.class).get();
			result.accept(beanDefinition, compiled);
		});
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

}
