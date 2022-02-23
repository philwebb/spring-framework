package org.springframework.aot.test.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MethodAssert}.
 *
 * @author Phillip Webb
 */
class MethodAssertTests {

	private static final String SAMPLE = """
			package com.example;

			public class Sample {

				public void run() {
					System.out.println("Hello World!");
				}

			}
			""";

	private SourceFile sourceFile = SourceFile.of(SAMPLE);

	@Test
	void withBodyWhenMatches() {
		assertThat(this.sourceFile).hasMethodNamed("run").withBody("""
				System.out.println("Hello World!");""");
	}

	@Test
	void withBodyWhenDoesNotMatchThrowsException() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(this.sourceFile).hasMethodNamed("run").withBody("""
						System.out.println("Hello Spring!");""")).withMessageContaining("to be equal to");
	}

	@Test
	void withBodyContainingWhenContainsAll() {
		assertThat(this.sourceFile).hasMethodNamed("run").withBodyContaining("Hello", "World!");
	}

	@Test
	void withBodyWhenDoesNotContainOneThrowsException() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(
						() -> assertThat(this.sourceFile).hasMethodNamed("run").withBodyContaining("Hello", "Spring!"))
				.withMessageContaining("to contain");
	}

}
