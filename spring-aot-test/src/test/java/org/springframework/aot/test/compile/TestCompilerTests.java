package org.springframework.aot.test.compile;

import static org.assertj.core.api.Assertions.assertThat;

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
					return "Hello Spring!";
				}

			}
			""";

	@Test
	@SuppressWarnings("unchecked")
	void differentClassesWithSameClassNameDoNotClash() {
		TestCompiler.forSystem().withSources(SourceFile.of(HELLO_WORLD)).compile((compiled) -> {
			Supplier<String> supplier = compiled.getInstance(Supplier.class, "com.example.Hello");
			assertThat(supplier.get()).isEqualTo("Hello World!");
		});
		TestCompiler.forSystem().withSources(SourceFile.of(HELLO_SPRING)).compile((compiled) -> {
			Supplier<String> supplier = compiled.getInstance(Supplier.class, "com.example.Hello");
			assertThat(supplier.get()).isEqualTo("Hello Spring!");
		});
	}


}
