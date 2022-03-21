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

import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.aot.TestConstructorOrFactoryMethodResolver;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.EnvironmentAwareComponent;
import org.springframework.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.NoDependencyComponent;
import org.springframework.beans.testfixture.beans.factory.generator.SimpleConfiguration;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolder;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolderFactoryBean;
import org.springframework.beans.testfixture.beans.factory.generator.factory.SampleFactory;
import org.springframework.beans.testfixture.beans.factory.generator.injection.InjectionComponent;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SuppliedInstanceBeanDefinitionCodeGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class SuppliedInstanceBeanDefinitionCodeGeneratorTests {

	@Test
	void generateWhenHasDefaultConstructor() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(TestBean.class).getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		testCompiledResult(beanFactory, beanDefinition, (supplierBeanDefinition, compiled) -> {
			TestBean bean = getBean(beanFactory, supplierBeanDefinition);
			assertThat(bean).isInstanceOf(TestBean.class);
			assertThat(compiled.getSourceFile()).contains("resolvedBy(TestBean::new)");
		});
	}

	@Test
	void generateWhenHasConstructorWithParameter() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(InjectionComponent.class)
				.getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("injected", "injected");
		testCompiledResult(beanFactory, beanDefinition, (supplierBeanDefinition, compiled) -> {
			InjectionComponent bean = getBean(beanFactory, supplierBeanDefinition);
			assertThat(bean).isInstanceOf(InjectionComponent.class).extracting("bean").isEqualTo("injected");
		});
	}

	@Test
	void generateWhenHasConstructorWithInnerClassAndDefaultConstructor() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(NoDependencyComponent.class)
				.getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("configuration", new InnerComponentConfiguration());
		testCompiledResult(beanFactory, beanDefinition, (supplierBeanDefinition, compiled) -> {
			NoDependencyComponent bean = getBean(beanFactory, supplierBeanDefinition);
			assertThat(bean).isInstanceOf(NoDependencyComponent.class);
			assertThat(compiled.getSourceFile())
					.contains("Factory.getBean(InnerComponentConfiguration.class).new NoDependencyComponent()");
		});
	}

	@Test
	void generateWhenHasConstructorWithInnerClassAndParameter() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(EnvironmentAwareComponent.class)
				.getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("configuration", new InnerComponentConfiguration());
		beanFactory.registerSingleton("environment", new StandardEnvironment());
		testCompiledResult(beanFactory, beanDefinition, (supplierBeanDefinition, compiled) -> {
			EnvironmentAwareComponent bean = getBean(beanFactory, supplierBeanDefinition);
			assertThat(bean).isInstanceOf(EnvironmentAwareComponent.class);
			assertThat(compiled.getSourceFile())
					.contains("Factory.getBean(InnerComponentConfiguration.class).new EnvironmentAwareComponent(");
		});
	}

	@Test
	void generateWhenHasConstructorWithGeneric() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(NumberHolderFactoryBean.class)
				.getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("number", 123);
		testCompiledResult(beanFactory, beanDefinition, (supplierBeanDefinition, compiled) -> {
			NumberHolder<?> bean = getBean(beanFactory, supplierBeanDefinition);
			assertThat(bean).isInstanceOf(NumberHolder.class);
			assertThat(bean).extracting("number").isNull(); // No property actually set
			assertThat(compiled.getSourceFile()).contains("NumberHolderFactoryBean::new");
		});
	}

	@Test
	void generateWhenHasFactoryMethodWithNoArg() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(String.class)
				.setFactoryMethodOnBean("stringBean", "config").getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config",
				BeanDefinitionBuilder.genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
		testCompiledResult(beanFactory, beanDefinition, (supplierBeanDefinition, compiled) -> {
			String bean = getBean(beanFactory, supplierBeanDefinition);
			assertThat(bean).isInstanceOf(String.class);
			assertThat(bean).isEqualTo("Hello");
			assertThat(compiled.getSourceFile())
					.contains("beanFactory.getBean(SimpleConfiguration.class).stringBean()");
		});
	}

	@Test
	void generateWhenHasStaticFactoryMethodWithNoArg() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(Integer.class)
				.setFactoryMethodOnBean("integerBean", "config").getBeanDefinition();
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config",
				BeanDefinitionBuilder.genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
		testCompiledResult(beanFactory, beanDefinition, (supplierBeanDefinition, compiled) -> {
			Integer bean = getBean(beanFactory, supplierBeanDefinition);
			assertThat(bean).isInstanceOf(Integer.class);
			assertThat(bean).isEqualTo(42);
			assertThat(compiled.getSourceFile()).contains("SimpleConfiguration.integerBean()");
		});
	}

	@Test
	void generateWhenHasStaticFactoryMethodWithArg() {
		RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder.rootBeanDefinition(String.class)
				.setFactoryMethodOnBean("create", "config").getBeanDefinition();
		beanDefinition.setResolvedFactoryMethod(
				ReflectionUtils.findMethod(SampleFactory.class, "create", Number.class, String.class));
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config",
				BeanDefinitionBuilder.genericBeanDefinition(SampleFactory.class).getBeanDefinition());
		beanFactory.registerSingleton("number", 42);
		beanFactory.registerSingleton("string", "test");
		testCompiledResult(beanFactory, beanDefinition, (supplierBeanDefinition, compiled) -> {
			String bean = getBean(beanFactory, supplierBeanDefinition);
			assertThat(bean).isInstanceOf(String.class);
			assertThat(bean).isEqualTo("42test");
			assertThat(compiled.getSourceFile()).contains("SampleFactory.create(");
		});
	}

	@SuppressWarnings("unchecked")
	private <T> T getBean(DefaultListableBeanFactory beanFactory, RootBeanDefinition beanDefinition) {
		assertThat(beanDefinition.getInstanceSupplier()).isNotNull();
		beanFactory.registerBeanDefinition("test", beanDefinition);
		return (T) beanFactory.getBean("test");
	}

	@SuppressWarnings("unchecked")
	private void testCompiledResult(DefaultListableBeanFactory beanFactory, BeanDefinition beanDefinition,
			BiConsumer<RootBeanDefinition, Compiled> result) {
		GeneratedMethods generatedMethods = new GeneratedMethods();
		SuppliedInstanceBeanDefinitionCodeGenerator generator = new SuppliedInstanceBeanDefinitionCodeGenerator(
				generatedMethods, new TestConstructorOrFactoryMethodResolver(beanFactory));
		RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition(beanFactory, beanDefinition);
		CodeBlock generatedCode = generator.generateCode(mergedBeanDefinition, "test");
		JavaFile javaFile = createJavaFile(generatedCode, generatedMethods);
		TestCompiler.forSystem().compile(javaFile::writeTo, compiled -> result
				.accept((RootBeanDefinition) compiled.getInstance(Function.class).apply(beanFactory), compiled));
	}

	private RootBeanDefinition getMergedBeanDefinition(DefaultListableBeanFactory parentBeanFactory,
			BeanDefinition beanDefinition) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(parentBeanFactory);
		beanFactory.registerBeanDefinition("test", beanDefinition);
		return (RootBeanDefinition) beanFactory.getMergedBeanDefinition("test");
	}

	private JavaFile createJavaFile(CodeBlock generatedCode, GeneratedMethods generatedMethods) {
		TypeSpec.Builder builder = TypeSpec.classBuilder("BeanDefinitionFunction");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(
				ParameterizedTypeName.get(Function.class, DefaultListableBeanFactory.class, RootBeanDefinition.class));
		builder.addMethod(MethodSpec.methodBuilder("apply").addModifiers(Modifier.PUBLIC)
				.addParameter(DefaultListableBeanFactory.class, "beanFactory").returns(RootBeanDefinition.class)
				.addStatement("return $L", generatedCode).build());
		generatedMethods.doWithMethodSpecs(builder::addMethod);
		return JavaFile.builder("com.example", builder.build()).build();
	}

}
