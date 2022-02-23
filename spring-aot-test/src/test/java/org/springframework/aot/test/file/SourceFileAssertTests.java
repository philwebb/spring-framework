package org.springframework.aot.test.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SourceFileAssert}.
 *
 * @author Phillip Webb
 */
class SourceFileAssertTests {

	private static final String SAMPLE = """
			package com.example;

			import java.lang.Runnable;

			public class Sample implements Runnable {

				void run() {
					run("Hello World!");
				}

				void run(String message) {
					System.out.println(message);
				}

				public static void main(String[] args) {
					new Sample().run();
				}
			}
			""";

	private SourceFile sourceFile = SourceFile.of(SAMPLE);

	@Test
	void containsWhenContainsAll() {
		assertThat(sourceFile).contains("Sample", "main");
	}

	@Test
	void containsWhenMissingOneThrowsException() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(this.sourceFile).contains("Sample", "missing"))
				.withMessageContaining("to contain");
	}

	@Test
	void isEqualToWhenEqual() {
		assertThat(this.sourceFile).isEqualTo(SAMPLE);
	}

	@Test
	void isEqualToWhenNotEqualThrowsException() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(this.sourceFile).isEqualTo("no"))
				.withMessageContaining("expected", "but was");
	}

	@Test
	void implementsInterfaceWhenImplementsInterface() {
		assertThat(this.sourceFile).implementsInterface(Runnable.class);
	}

	@Test
	void implementsInterfaceWhenDoesNotImplementInterfaceThrowsException() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(this.sourceFile).implementsInterface(Callable.class))
				.withMessageContaining("to contain:");
	}

	@Test
	void hasMethodNamedWhenHasName() {
		MethodAssert methodAssert = assertThat(this.sourceFile).hasMethodNamed("main");
		assertThat(methodAssert).isNotNull();
	}

	@Test
	void hasMethodNameWhenDoesNotHaveMethodThrowsException() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(this.sourceFile).hasMethodNamed("missing"))
				.withMessageContaining("to contain method");
	}

	@Test
	void hasMethodNameWhenHasMultipleMethodsWithNameThrowsException() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(this.sourceFile).hasMethodNamed("run"))
				.withMessageContaining("to contain unique method");
	}

	@Test
	void hasMethodWhenHasMethod() {
		MethodAssert methodAssert = assertThat(this.sourceFile).hasMethod("run", String.class);
		assertThat(methodAssert).isNotNull();
	}

	@Test
	void hasMethodWhenDoesNotHaveMethod() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(this.sourceFile).hasMethod("run", Integer.class))
				.withMessageContaining("to contain").withMessageContaining("run(java.lang.Integer");
	}

}
