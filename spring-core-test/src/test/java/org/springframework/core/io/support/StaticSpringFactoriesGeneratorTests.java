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

package org.springframework.core.io.support;

import java.net.URI;
import java.util.List;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.test.generator.compile.CompileWithTargetClassAccess;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.core.io.support.test.NonPublicFactory;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StaticSpringFactoriesGenerator} that compile the generated source code.
 *
 * @author Brian Clozel
 */
@CompileWithTargetClassAccess(classes = {SpringFactoriesLoader.class, NonPublicFactory.class})
public class StaticSpringFactoriesGeneratorTests {

	private InMemoryGeneratedFiles generatedFiles;

	private DefaultGenerationContext generationContext;

	@BeforeEach
	void setup() {
		SpringFactoriesLoader.cache.clear();
		this.generatedFiles = new InMemoryGeneratedFiles();
		this.generationContext = new DefaultGenerationContext(this.generatedFiles);
	}

	@Test
	void generatePublicFactoriesWithDefaultConstructor() {
		StaticSpringFactoriesGenerator generator = new StaticSpringFactoriesGenerator(null);
		MethodReference initSpringFactories = generator.generateStaticSpringFactories(this.generationContext);

		runCompiledResult(initSpringFactories);

		List<DummyFactory> factories = SpringFactoriesLoader.forDefaultResourceLocation().load(DummyFactory.class);
		assertThat(factories).hasSize(2);
		assertThat(factories.get(0)).isInstanceOf(MyDummyFactory.class);
		assertThat(factories.get(1)).isInstanceOf(OtherDummyFactory.class);
	}

	@Test
	void generatePackagePrivateFactoriesWithDefaultConstructor() {
		StaticSpringFactoriesGenerator generator = new StaticSpringFactoriesGenerator(null);
		MethodReference initSpringFactories = generator.generateStaticSpringFactories(this.generationContext);

		runCompiledResult(initSpringFactories);

		List<NonPublicFactory> factories = SpringFactoriesLoader.forDefaultResourceLocation().load(NonPublicFactory.class);
		assertThat(factories).hasSize(1);
		assertThat(factories.get(0).getClass().getName())
				.isEqualTo("org.springframework.core.io.support.test.PackagePrivateFactory");
	}

	@Test
	void generateFactoriesWithConstructorArgument() {
		StaticSpringFactoriesGenerator generator = new StaticSpringFactoriesGenerator(null);
		MethodReference initSpringFactories = generator.generateStaticSpringFactories(this.generationContext);

		runCompiledResult(initSpringFactories);

		List<ConstructorFactory> factories = SpringFactoriesLoader.forDefaultResourceLocation().load(ConstructorFactory.class,
				SpringFactoriesLoader.ArgumentResolver.of(String.class, "Spring").and(URI.class, URI.create("https://spring.io")));
		assertThat(factories).hasSize(1);
		assertThat(factories.get(0)).isInstanceOf(MyConstructorFactory.class);
	}

	@SuppressWarnings("unchecked")
	private void runCompiledResult(MethodReference method) {
		this.generationContext.writeGeneratedContent();
		JavaFile javaFile = generateJavaFile(method);
		TestCompiler.forSystem().withFiles(this.generatedFiles).printFiles(System.out)
				.compile(javaFile::writeTo, compiled -> compiled.getInstance(Runnable.class).run());
	}

	private JavaFile generateJavaFile(MethodReference method) {
		TypeSpec.Builder builder = TypeSpec.classBuilder("SpringFactoriesInitializer");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(Runnable.class);
		builder.addMethod(MethodSpec.methodBuilder("run")
				.addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC)
				.addStatement("$L", method.toInvokeCodeBlock(CodeBlock.of("null"))).build());
		return JavaFile.builder("__", builder.build()).build();
	}

	public interface DummyFactory {

		String getString();

	}

	public static class MyDummyFactory implements DummyFactory {

		@Override
		public String getString() {
			return "Test";
		}
	}

	public static class OtherDummyFactory implements DummyFactory {

		@Override
		public String getString() {
			return "Test";
		}
	}

	public interface ConstructorFactory {

		String getString();

	}

	public static class MyConstructorFactory implements ConstructorFactory {

		private final String value;

		private final URI uri;

		public MyConstructorFactory(String value, URI uri) {
			this.value = value;
			this.uri = uri;
		}

		@Override
		public String getString() {
			return this.value;
		}
	}
}
