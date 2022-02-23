package org.springframework.aot.test.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.thoughtworks.qdox.model.JavaSource;

/**
 * Tests for {@link SourceFile}.
 *
 * @author Phillip Webb
 */
class SourceFileTests {

	private static final String HELLO_WORLD = """
			package com.example.helloworld;

			public class HelloWorld {
				public static void main(String[] args) {
					System.out.println("Hello World!");
				}
			}
			""";

	@Test
	void ofWhenContentIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> SourceFile.of((WritableContent) null))
				.withMessage("'writableContent' must not to be empty");
	}

	@Test
	void ofWhenContentIsEmptyThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> SourceFile.of(""))
				.withMessage("WritableContent did not append any content");
	}

	@Test
	void ofWhenSourceDefinesNoClassThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> SourceFile.of("package com.example;"))
				.withMessageContaining("Unable to parse").havingCause()
				.withMessage("Source must define a single class");
	}

	@Test
	void ofWhenSourceDefinesMultipleClassesThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> SourceFile.of("public class One {}\npublic class Two{}"))
				.withMessageContaining("Unable to parse").havingCause()
				.withMessage("Source must define a single class");
	}

	@Test
	void ofWhenSourceCannotBeParsedThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> SourceFile.of("well this is broken {"))
				.withMessageContaining("Unable to parse source file content");
	}

	@Test
	void ofWithoutPathDeducesPath() {
		SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
		assertThat(sourceFile.getPath()).isEqualTo("com/example/helloworld/HelloWorld.java");
	}

	@Test
	void ofWithPathUsesPath() {
		SourceFile sourceFile = SourceFile.of("com/example/DifferentPath.java", HELLO_WORLD);
		assertThat(sourceFile.getPath()).isEqualTo("com/example/DifferentPath.java");
	}

	@Test
	void getContentReturnsContent() {
		SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
		assertThat(sourceFile.getContent()).isEqualTo(HELLO_WORLD);
	}

	@Test
	void getJavaSourceReturnsJavaSource() {
		SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
		assertThat(sourceFile.getJavaSource()).isInstanceOf(JavaSource.class);
	}

	@Test
	@SuppressWarnings("deprecation")
	void assertThatReturnsAssert() {
		SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
		assertThat(sourceFile.assertThat()).isInstanceOf(SourceFileAssert.class);
	}

	@Test
	void createFromJavaPoetStyleApi() {
		JavaFile javaFile = new JavaFile(HELLO_WORLD);
		SourceFile sourceFile = SourceFile.of(javaFile::writeTo);
		assertThat(sourceFile.getContent()).isEqualTo(HELLO_WORLD);
	}

	/**
	 * JavaPoet style API with a {@code writeTo} method.
	 */
	static class JavaFile {

		private final String content;

		JavaFile(String content) {
			this.content = content;
		}

		void writeTo(Appendable out) throws IOException {
			out.append(this.content);
		}

	}

}
