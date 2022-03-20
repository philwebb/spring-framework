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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedClassName;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.aot.UniqueBeanFactoryName;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactoryInitializer;
import org.springframework.beans.testfixture.beans.AnnotatedBean;
import org.springframework.javapoet.JavaFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanRegistrationsJavaFileGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class BeanRegistrationsJavaFileGeneratorTests {

	@Test
	void generateBeanRegistrationsCodeGeneratesCode() {
		Map<String, BeanRegistrationMethodCodeGenerator> beanDefinitionGenerators = new LinkedHashMap<>();
		DefaultListableBeanFactory generationBeanFactory = new DefaultListableBeanFactory();
		beanDefinitionGenerators.put("primaryBean", new DefaultBeanRegistrationMethodCodeGenerator(
				generationBeanFactory, "primaryBean",
				BeanDefinitionBuilder.rootBeanDefinition(AnnotatedBean.class).setPrimary(true).getBeanDefinition()));
		beanDefinitionGenerators.put("anotherBean",
				new DefaultBeanRegistrationMethodCodeGenerator(generationBeanFactory, "anotherBean",
						BeanDefinitionBuilder.rootBeanDefinition(AnnotatedBean.class).getBeanDefinition()));
		BeanRegistrationsJavaFileGenerator generator = new BeanRegistrationsJavaFileGenerator(beanDefinitionGenerators);
		testCompiledResult(generator, (initializer, compiled) -> {
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			initializer.initialize(beanFactory);
			assertThat(beanFactory.getBeanDefinition("primaryBean").isPrimary()).isTrue();
			assertThat(beanFactory.getBeanDefinition("anotherBean").isPrimary()).isFalse();
			assertThat(beanFactory.getBean("primaryBean")).isInstanceOf(AnnotatedBean.class);
			assertThat(beanFactory.getBean("anotherBean")).isInstanceOf(AnnotatedBean.class);
		});
	}

	private void testCompiledResult(BeanRegistrationsJavaFileGenerator generator,
			BiConsumer<DefaultListableBeanFactoryInitializer, Compiled> result) {
		GenerationContext generationContext = new DefaultGenerationContext(new InMemoryGeneratedFiles());
		GeneratedClassName className = generationContext.getClassNameGenerator().generateClassName(getClass(),
				"registrations");
		JavaFile javaFile = generator.generateCode(generationContext, new UniqueBeanFactoryName("test"), className);
		TestCompiler.forSystem().compile(javaFile::writeTo, (compiled) -> {
			DefaultListableBeanFactoryInitializer initializer = compiled
					.getInstance(DefaultListableBeanFactoryInitializer.class);
			result.accept(initializer, compiled);
		});
	}
}
