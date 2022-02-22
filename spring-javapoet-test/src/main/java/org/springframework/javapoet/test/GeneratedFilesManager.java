package org.springframework.javapoet.test;

import java.io.IOException;

/**
 * Interface used to manage {@link GeneratedFile} instances.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @see GenerationContext
 */
public interface GeneratedFilesManager {

	/**
	 * Add a Java {@link GeneratedFile} for the previously generated class name.
	 * @param className the previously generated class name
	 * @return a {@link GeneratedFile} instance that can be used to write content
	 * @throws IOException on IO error
	 */
	GeneratedFile addJavaFile(GeneratedClassName className) throws IOException;

	/**
	 * Add a {@link GeneratedFile} for the given path.
	 * @param relativePath the relative path of the file
	 * @return a {@link GeneratedFile} instance that can be used to write content
	 * @throws IOException on IO error
	 */
	GeneratedFile addResourceFile(String relativePath) throws IOException;

}
