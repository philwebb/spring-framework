package org.springframework.aot.test.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link Content}.
 *
 * @author Phillip Webb
 */
class ContentTests {

	private static final String CONTENT = "Spring Framework!\n";

	@TempDir
	Path temp;

	Path tempFile;

	@BeforeEach
	void copyToTempFile() throws IOException {
		this.tempFile = temp.resolve("content");
		Files.copy(new ByteArrayInputStream(CONTENT.getBytes(StandardCharsets.UTF_8)), this.tempFile);
	}

	@Test
	void ofFileCreatesContent() {
		Content content = Content.of(this.tempFile.toFile());
		assertThat(Content.toString(content)).isEqualTo(CONTENT);
	}

	@Test
	void ofPathCreatesContent() {
		Content content = Content.of(this.tempFile);
		assertThat(Content.toString(content)).isEqualTo(CONTENT);
	}

	@Test
	void ofInputStreamCreatesContent() throws FileNotFoundException {
		Content content = Content.of(new FileInputStream(this.tempFile.toFile()));
		assertThat(Content.toString(content)).isEqualTo(CONTENT);
	}

	@Test
	void ofReaderCreatesContent() {
		Content content = Content.of(new StringReader(CONTENT));
		assertThat(Content.toString(content)).isEqualTo(CONTENT);
	}

	@Test
	void ofCharSequenceCreatesContent() {
		Content content = Content.of(CONTENT);
		assertThat(Content.toString(content)).isEqualTo(CONTENT);
	}

	@Test
	void toStringWhenNullReturnsNull() {
		assertThat(Content.toString(null)).isNull();
	}

	@Test
	void toStringWhenStringContentReturnsString() {
		StringContent content = new StringContent(CONTENT) {

			@Override
			public void transferTo(Appendable appendable) throws IOException {
				throw new IOException();
			}

		};
		assertThat(Content.toString(content)).isEqualTo(CONTENT);
	}

	@Test
	void toStringTransfersToString() {
		Content content = appendable -> appendable.append(CONTENT);
		assertThat(Content.toString(content)).isEqualTo(CONTENT);
	}

	@Test
	void toStringWhenIOExceptionThrowsIllegalStateException() {
		Content content = appendable -> {
			throw new IOException();
		};
		assertThatIllegalStateException().isThrownBy(() -> Content.toString(content))
				.withMessage("Unable to append content").withCauseInstanceOf(IOException.class);
	}

}
