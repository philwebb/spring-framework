package org.springframework.javapoet.test;

import java.io.IOException;

/**
 * Interface that can be used to update a generated file.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @see GenerationContext#addResourceFile(String)
 */
public interface GeneratedFile {

	/**
	 * Update the file with the given {@link CharSequence} content.
	 * @param content the file content
	 * @throws IOException on IO error
	 */
	default void withContent(CharSequence content) throws IOException {
		withContentFrom((appendable) -> appendable.append(content));
	}

	/**
	 * Update the file with the given {@link WritableContent}. This method is often passed
	 * a reference to a method that writes the content. For example,
	 * {@code generatedFile.withContent(javaFile::writeTo)}.
	 * @param content the generated content
	 * @throws IOException on IO error
	 */
	void withContentFrom(WritableContent content) throws IOException;

}
