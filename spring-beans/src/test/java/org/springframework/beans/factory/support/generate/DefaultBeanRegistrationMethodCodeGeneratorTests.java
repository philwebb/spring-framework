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
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.aot.TestConstructorOrFactoryMethodResolver;
import org.springframework.beans.testfixture.beans.AnnotatedBean;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultBeanRegistrationMethodCodeGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class DefaultBeanRegistrationMethodCodeGeneratorTests {

	@Test
	void generateBeanRegistrationMethodCodeGeneratesCode() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(AnnotatedBean.class).setPrimary(true)
				.getBeanDefinition();
		testCompiledResult("test", beanDefinition, (beanFactory, result) -> {
			BeanDefinition actualDefinition = beanFactory.getBeanDefinition("test");
			assertThat(actualDefinition.isPrimary()).isTrue();
			assertThat(beanFactory.getBean("test")).isInstanceOf(AnnotatedBean.class);
		});
	}

	@SuppressWarnings("unchecked")
	private void testCompiledResult(String beanName, BeanDefinition beanDefinition,
			BiConsumer<DefaultListableBeanFactory, Compiled> result) {
		DefaultListableBeanFactory generationBeanFactory = new DefaultListableBeanFactory();
		DefaultBeanRegistrationMethodCodeGenerator generator = new DefaultBeanRegistrationMethodCodeGenerator("test",
				beanDefinition, new TestConstructorOrFactoryMethodResolver(generationBeanFactory));
		GenerationContext generationContext = new DefaultGenerationContext(new InMemoryGeneratedFiles());
		GeneratedMethods generatedMethods = new GeneratedMethods();
		CodeBlock generatedCode = generator.generateBeanRegistrationMethodCode(generationContext, generatedMethods);
		JavaFile javaFile = createJavaFile(generatedCode, generatedMethods);
		TestCompiler.forSystem().compile(javaFile::writeTo, (compiled) -> {
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			compiled.getInstance(Consumer.class).accept(beanFactory);
			result.accept(beanFactory, compiled);
		});
	}

	private JavaFile createJavaFile(CodeBlock generatedCode, GeneratedMethods generatedMethods) {
		TypeSpec.Builder builder = TypeSpec.classBuilder("RegistrationFunction");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(ParameterizedTypeName.get(Consumer.class, DefaultListableBeanFactory.class));
		builder.addMethod(MethodSpec.methodBuilder("accept").addModifiers(Modifier.PUBLIC)
				.addParameter(DefaultListableBeanFactory.class, "beanFactory").addCode(generatedCode).build());
		generatedMethods.doWithMethodSpecs(builder::addMethod);
		return JavaFile.builder("com.example", builder.build()).build();
	}

}
