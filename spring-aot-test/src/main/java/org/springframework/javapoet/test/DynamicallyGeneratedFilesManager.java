package org.springframework.javapoet.test;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link GeneratedFilesManager} implementation that stores files in memory.
 *
 * @author Phillip Webb
 */
class DynamicallyGeneratedFilesManager implements GeneratedFilesManager {

	private Map<String, DynamicallyGeneratedJavaFile> javaFiles = Collections.synchronizedMap(new LinkedHashMap<>());

	private Map<String, DynamicallyGeneratedResourceFile> resourceFiles = Collections
			.synchronizedMap(new LinkedHashMap<>());

	@Override
	public GeneratedFile addJavaFile(GeneratedClassName className) throws IOException {
		return this.javaFiles.computeIfAbsent(className.getFullyQualifiedName(),
				(key) -> new DynamicallyGeneratedJavaFile(className));
	}

	@Override
	public GeneratedFile addResourceFile(String relativePath) throws IOException {
		return this.resourceFiles.computeIfAbsent(relativePath, DynamicallyGeneratedResourceFile::new);
	}

	Map<String, DynamicallyGeneratedJavaFile> getJavaFiles() {
		return Collections.unmodifiableMap(this.javaFiles);
	}

	Map<String, DynamicallyGeneratedResourceFile> getResourceFiles() {
		return Collections.unmodifiableMap(this.resourceFiles);
	}

}
