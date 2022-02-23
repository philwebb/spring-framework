package org.springframework.aot.test.file;

import java.io.IOException;

import org.assertj.core.util.Strings;

/**
 * Abstract base class for dynamically generated files.
 *
 * @author Phillip Webb
 * @see SourceFile
 * @see ResourceFile
 */
public abstract sealed class DynamicFile permits SourceFile, ResourceFile {

	private final String path;

	private final String content;

	protected DynamicFile(String path, String content) {
		if (Strings.isNullOrEmpty(content)) {
			throw new IllegalArgumentException("'path' must not to be empty");
		}
		if (Strings.isNullOrEmpty(content)) {
			throw new IllegalArgumentException("'content' must not to be empty");
		}
		this.path = path;
		this.content = content;
	}

	protected static String toString(WritableContent writableContent) {
		if (writableContent == null) {
			throw new IllegalArgumentException("'writableContent' must not to be empty");
		}
		try {
			StringBuilder stringBuilder = new StringBuilder();
			writableContent.writeTo(stringBuilder);
			return stringBuilder.toString();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to read content", ex);
		}
	}

	/**
	 * Return the contents of the file.
	 * @return the file contents
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Return the relative path of the file.
	 * @return the file path
	 */
	public String getPath() {
		return path;
	}

	@Override
	public String toString() {
		return this.path;
	}

}
