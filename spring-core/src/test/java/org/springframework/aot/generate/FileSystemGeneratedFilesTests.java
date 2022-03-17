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

package org.springframework.aot.generate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FileSystemGeneratedFiles}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class FileSystemGeneratedFilesTests {

	@TempDir
	Path root;

	@Test
	void addFilesCopiesToFileSystem() {
		FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
		generatedFiles.addSourceFile("com.example.Test", "{}");
		generatedFiles.addResourceFile("META-INF/test", "test");
		generatedFiles.addClassFile("com/example/TestProxy.class",
				new ByteArrayResource("!".getBytes(StandardCharsets.UTF_8)));
		assertThat(root.resolve("sources/com/example/Test.java")).content().isEqualTo(
				"{}");
		assertThat(root.resolve("resources/META-INF/test")).content().isEqualTo("test");
		assertThat(
				root.resolve("classes/com/example/TestProxy.class")).content().isEqualTo(
						"!");
	}

	@Test
	void addFilesWithCustomRootsCopiesToFileSystem() {
		FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(
				kind -> this.root.resolve("the-" + kind));
		generatedFiles.addSourceFile("com.example.Test", "{}");
		generatedFiles.addResourceFile("META-INF/test", "test");
		generatedFiles.addClassFile("com/example/TestProxy.class",
				new ByteArrayResource("!".getBytes(StandardCharsets.UTF_8)));
		assertThat(root.resolve("the-SOURCE/com/example/Test.java")).content().isEqualTo(
				"{}");
		assertThat(root.resolve("the-RESOURCE/META-INF/test")).content().isEqualTo(
				"test");
		assertThat(root.resolve(
				"the-CLASS/com/example/TestProxy.class")).content().isEqualTo("!");
	}

	@Test
	void createWhenRootIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new FileSystemGeneratedFiles((Path) null)).withMessage(
						"'root' must not be null");
	}

	@Test
	void createWhenRootsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new FileSystemGeneratedFiles(
						(Function<Kind, Path>) null)).withMessage(
								"'roots' must not be null");
	}

	@Test
	void createWhenRootsResultsInNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new FileSystemGeneratedFiles(
						kind -> (kind != Kind.CLASS) ? root.resolve(kind.toString())
								: null)).withMessage(
										"'roots' must return a value for all file kinds");
	}

	@Test
	void addFileWhenPathIsOutsideOfRootThrowsException() {
		FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
		assertPathMustBeRelative(generatedFiles, "/test");
		assertPathMustBeRelative(generatedFiles, "../test");
		assertPathMustBeRelative(generatedFiles, "test/../../test");
	}

	private void assertPathMustBeRelative(FileSystemGeneratedFiles generatedFiles,
			String path) {
		assertThatIllegalArgumentException().isThrownBy(
				() -> generatedFiles.addResourceFile(path, "test")).withMessage(
						"'path' must be relative");
	}

}