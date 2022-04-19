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

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.test.generator.compile.CompileWithTargetClassAccess;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.aot.test.generator.file.SourceFile;
import org.springframework.beans.factory.aot.AotFactoriesLoader;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.AnnotatedBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.mock.MockSpringFactoriesLoader;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanDefinitionMethodGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class BeanDefinitionMethodGeneratorTests {

	private InMemoryGeneratedFiles generatedFiles;

	private DefaultGenerationContext generationContext;

	private DefaultListableBeanFactory beanFactory;

	private MockSpringFactoriesLoader springFactoriesLoader;

	private MockBeanRegistrationsCode beanRegistrationsCode;

	private BeanDefinitionMethodGeneratorFactory methodGeneratorFactory;

	@BeforeEach
	void setup() {
		this.generatedFiles = new InMemoryGeneratedFiles();
		this.generationContext = new DefaultGenerationContext(this.generatedFiles);
		this.beanFactory = new DefaultListableBeanFactory();
		this.springFactoriesLoader = new MockSpringFactoriesLoader();
		this.methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
				new AotFactoriesLoader(this.beanFactory, this.springFactoriesLoader));
		this.beanRegistrationsCode = new MockBeanRegistrationsCode(ClassName.get("__", "Registration"));
	}

	@Test
	void generateBeanDefinitionMethodGeneratesMethod() {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
				registeredBean, null, Collections.emptyList(), Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(this.generationContext,
				this.beanRegistrationsCode);
		testCompiledResult(method, (actual, compiled) -> {
			assertThat(compiled.getSourceFile(".*BeanDefinitions")).contains("Get the bean definition for 'testBean'");
			assertThat(actual).isInstanceOf(RootBeanDefinition.class);
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenInnerBeanGeneratesMethod() {
		RegisteredBean parent = registerBean(new RootBeanDefinition(TestBean.class));
		RegisteredBean innerBean = RegisteredBean.ofInnerBean(parent, new RootBeanDefinition(AnnotatedBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
				innerBean, "testInnerBean", Collections.emptyList(), Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(this.generationContext,
				this.beanRegistrationsCode);
		testCompiledResult(method, (actual, compiled) -> {
			assertThat(compiled.getSourceFile(".*BeanDefinitions"))
					.contains("Get the inner-bean definition for 'testInnerBean'");
			assertThat(actual).isInstanceOf(RootBeanDefinition.class);
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenHasAotContributionsAppliesContributions() {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
		List<BeanRegistrationAotContribution> aotContributions = new ArrayList<>();
		aotContributions.add((generationContext, beanRegistrationCode) -> beanRegistrationCode.getMethodGenerator()
				.generateMethod("aotContributedMethod").using(builder -> builder.addComment("Example Contribution")));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
				registeredBean, null, aotContributions, Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(this.generationContext,
				this.beanRegistrationsCode);
		testCompiledResult(method, (actual, compiled) -> {
			SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
			assertThat(sourceFile).contains("AotContributedMethod()");
			assertThat(sourceFile).contains("Example Contribution");
		});
	}

	@Test
	void generateBeanDefinitionMethodWhenBeanRegistrationCodeGeneratorFactoryReturnsCodeGeneratesMethod() {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
		List<BeanRegistrationCodeGeneratorFactory> codeGeneratorFactories = new ArrayList<>();
		codeGeneratorFactories.add(TestBeanRegistrationCodeGenerator::new);
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
				registeredBean, null, Collections.emptyList(), codeGeneratorFactories);
		MethodReference method = generator.generateBeanDefinitionMethod(this.generationContext,
				this.beanRegistrationsCode);
		testCompiledResult(method, (actual, compiled) -> assertThat(compiled.getSourceFile(".*BeanDefinitions"))
				.contains("// Custom Code"));
	}

	@Test
	@CompileWithTargetClassAccess
	void generateBeanDefinitionMethodWhenPackagePrivateBean() {
		RegisteredBean registeredBean = registerBean(new RootBeanDefinition(PackagePrivateTestBean.class));
		BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
				registeredBean, null, Collections.emptyList(), Collections.emptyList());
		MethodReference method = generator.generateBeanDefinitionMethod(this.generationContext,
				this.beanRegistrationsCode);
		testCompiledResult(method, false, (actual, compiled) -> {
			DefaultListableBeanFactory freshBeanFactory = new DefaultListableBeanFactory();
			freshBeanFactory.registerBeanDefinition("test", actual);
			Object bean = freshBeanFactory.getBean("test");
			assertThat(bean).isInstanceOf(PackagePrivateTestBean.class);
			assertThat(compiled.getSourceFileFromPackage(PackagePrivateTestBean.class.getPackageName())).isNotNull();
		});
	}

	private RegisteredBean registerBean(RootBeanDefinition beanDefinition) {
		String beanName = "testBean";
		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
		RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, beanName);
		return registeredBean;
	}

	@SuppressWarnings("unchecked")
	private void testCompiledResult(MethodReference method, BiConsumer<BeanDefinition, Compiled> result) {
		testCompiledResult(method, false, result);
	}

	@SuppressWarnings("unchecked")
	private void testCompiledResult(MethodReference method, boolean targetClassAccess,
			BiConsumer<BeanDefinition, Compiled> result) {
		this.generationContext.close();
		JavaFile javaFile = generateJavaFile(method);
		System.err.println(javaFile);
		TestCompiler.forSystem().withTargetClassAccess(targetClassAccess).withFiles(this.generatedFiles)
				.printFiles(System.out).compile(javaFile::writeTo, compiled -> result
						.accept((BeanDefinition) compiled.getInstance(Supplier.class).get(), compiled));
	}

	private JavaFile generateJavaFile(MethodReference method) {
		TypeSpec.Builder builder = TypeSpec.classBuilder("Registration");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(ParameterizedTypeName.get(Supplier.class, BeanDefinition.class));
		builder.addMethod(MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC).returns(BeanDefinition.class)
				.addCode("return $L;", method.toInvokeCodeBlock()).build());
		this.beanRegistrationsCode.getGeneratedMethods().doWithMethodSpecs(builder::addMethod);
		return JavaFile.builder("__", builder.build()).build();
	}

	static class TestBeanRegistrationCodeGenerator extends DefaultBeanRegistrationCodeGenerator {

		TestBeanRegistrationCodeGenerator(RegisteredBean registeredBean, Executable constructorOrFactoryMethod,
				ClassName className, MethodGenerator methodGenerator,
				InnerBeanDefinitionMethodGenerator innerBeanDefinitionMethodGenerator) {
			super(registeredBean, constructorOrFactoryMethod, className, methodGenerator,
					innerBeanDefinitionMethodGenerator);
		}

		@Override
		protected CodeBlock.Builder createBuilder() {
			CodeBlock.Builder builder = super.createBuilder();
			builder.add("// Custom Code\n");
			return builder;
		}

	}

}
