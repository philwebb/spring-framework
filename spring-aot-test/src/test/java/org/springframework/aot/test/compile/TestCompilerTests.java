package org.springframework.aot.test.compile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.aot.test.file.SourceFile;

/**
 * Tests for {@link TestCompiler}.
 *
 * @author Phillip Webb
 */
class TestCompilerTests {

	private static final String HELLO_WORLD = """
			package com.example;

			import java.util.function.Supplier;

			@Deprecated
			public class Hello implements Supplier<String> {

				public String get() {
					return "Hello World!";
				}

			}
			""";

	private static final String HELLO_SPRING = """
			package com.example;

			import java.util.function.Supplier;

			public class Hello implements Supplier<String> {

				public String get() {
					return "Hello Spring!"; // !!
				}

			}
			""";

	private static final String HELLO_BAD = """
			package com.example;

			public class Hello implements Supplier<String> {

				public String get() {
					return "Missing Import!";
				}

			}
			""";

	@Test
	@SuppressWarnings("unchecked")
	void compileWhenHasDifferentClassesWithSameClassNameCompilesBoth() {
		TestCompiler.forSystem().withSources(SourceFile.of(HELLO_WORLD)).compile((compiled) -> {
			Supplier<String> supplier = compiled.getInstance(Supplier.class, "com.example.Hello");
			assertThat(supplier.get()).isEqualTo("Hello World!");
		});
		TestCompiler.forSystem().withSources(SourceFile.of(HELLO_SPRING)).compile((compiled) -> {
			Supplier<String> supplier = compiled.getInstance(Supplier.class, "com.example.Hello");
			assertThat(supplier.get()).isEqualTo("Hello Spring!");
		});
	}

	@Test
	void compileAndGetInstanceWithoutName() {
		TestCompiler.forSystem().withSources(SourceFile.of(HELLO_WORLD)).compile((compiled) -> {
			assertThat(compiled.getInstance(Supplier.class).get()).isEqualTo("Hello World!");
		});
	}

	@Test
	void compileAndGetSourceFile() {
		TestCompiler.forSystem().withSources(SourceFile.of(HELLO_SPRING)).compile((compiled) -> {
			assertThat(compiled.getSourceFile()).hasMethodNamed("get").withBodyContaining("// !!");
		});
	}

	@Test
	void compileWhenSourceHasCompileErrors() {
		assertThatExceptionOfType(CompilationException.class)
				.isThrownBy(() -> TestCompiler.forSystem().withSources(SourceFile.of(HELLO_BAD)).compile((compiled) -> {
				}));
	}

	@Test
	void compileWithSourceDirectly() {
		TestCompiler.forSystem().compile(SourceFile.of(HELLO_WORLD),
				(compiled) -> assertThat(compiled.getInstance(Supplier.class).getClass()).hasAnnotation(Deprecated.class));

	}

}
