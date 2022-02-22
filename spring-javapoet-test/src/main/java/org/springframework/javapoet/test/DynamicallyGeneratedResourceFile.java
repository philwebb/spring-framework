package org.springframework.javapoet.test;

/**
 * A dynamically generated resource file.
 *
 * @author Phillip Webb
 */
class DynamicallyGeneratedResourceFile extends DynamicallyGeneratedFile {

	private final String relativePath;

	DynamicallyGeneratedResourceFile(String relativePath) {
		this.relativePath = relativePath;
	}

	String getRelativePath() {
		return this.relativePath;
	}

}
