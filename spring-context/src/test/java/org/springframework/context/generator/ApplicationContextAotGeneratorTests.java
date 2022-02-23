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

package org.springframework.context.generator;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.aot.test.compile.TestCompiler;
import org.springframework.aot.test.file.SourceFile;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ApplicationContextAotGenerator}.
 *
 * @author Stephane Nicoll
 */
class ApplicationContextAotGeneratorTests {

	private TestCompiler compiler = TestCompiler.forSystem();

	@Test
	void generateApplicationContextWitNoContributors2() {
		GeneratedTypeContext generationContext = createGenerationContext();
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator(List.of());
		generator.generateApplicationContext(new GenericApplicationContext(), generationContext);
		SourceFile generated = SourceFile.of(generationContext.getMainGeneratedType()::writeTo);
		assertThat(generated).implementsInterface("ApplicationContextInitializer");
		assertThat(generated).hasMethod("initialize", GenericApplicationContext.class).withBody(
						"DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();");
	}

	@Test
	void generateApplicationContextWitNoContributors3() {
		GeneratedTypeContext generationContext = createGenerationContext();
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator(List.of());
		generator.generateApplicationContext(new GenericApplicationContext(), generationContext);
		this.compiler.compile(generationContext.getMainGeneratedType()::writeTo, (compiled) -> {
			SourceFile source = compiled.getSourceFile();
			assertThat(source).implementsInterface("ApplicationContextInitializer");
			assertThat(source).hasMethod("initialize", GenericApplicationContext.class).withBody(
					"DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();");
		});
	}


	@Test
	void generateApplicationContextWitNoContributors() {
		GeneratedTypeContext generationContext = createGenerationContext();
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator(List.of());
		generator.generateApplicationContext(new GenericApplicationContext(), generationContext);
		assertThat(write(generationContext.getMainGeneratedType())).contains("""
				public class Test implements ApplicationContextInitializer {
					@Override
					public void initialize(GenericApplicationContext context) {
						DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();
					}
				}
				""");
	}

	@Test
	void generateApplicationContextApplyContributionAsIsWithNewLineAtThend() {
		GeneratedTypeContext generationContext = createGenerationContext();
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator(List.of(genContext -> CodeBlock.of("// Hello")));
		generator.generateApplicationContext(new GenericApplicationContext(), generationContext);
		assertThat(write(generationContext.getMainGeneratedType())).contains("""
				public class Test implements ApplicationContextInitializer {
					@Override
					public void initialize(GenericApplicationContext context) {
						DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();
						// Hello
					}
				}
				""");
	}

	@Test
	void generateApplicationContextApplyMultipleContributionAsIsWithNewLineAtThend() {
		GeneratedTypeContext generationContext = createGenerationContext();
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator(List.of(
				genContext -> CodeBlock.of("// Hello"),
				genContext -> CodeBlock.of("// World")));
		generator.generateApplicationContext(new GenericApplicationContext(), generationContext);
		assertThat(write(generationContext.getMainGeneratedType())).contains("""
				public class Test implements ApplicationContextInitializer {
					@Override
					public void initialize(GenericApplicationContext context) {
						DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();
						// Hello
						// World
					}
				}
				""");
	}


	private GeneratedTypeContext createGenerationContext() {
		return new DefaultGeneratedTypeContext("com.example", packageName ->
				GeneratedType.of(ClassName.get(packageName, "Test")));
	}

	private String write(GeneratedType generatedType) {
		try {
			StringWriter out = new StringWriter();
			generatedType.toJavaFile().writeTo(out);
			return out.toString();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to write " + generatedType, ex);
		}
	}

}
