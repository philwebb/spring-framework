package org.springframework.aot.test.compile;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

import org.springframework.aot.test.file.ResourceFiles;
import org.springframework.aot.test.file.SourceFile;
import org.springframework.aot.test.file.SourceFiles;

public class Compiled {

	private final ClassLoader classLoader;

	private final SourceFiles sourceFiles;

	private final ResourceFiles resourceFiles;

	public Compiled(ClassLoader classLoader, SourceFiles sourceFiles, ResourceFiles resourceFiles) {
		this.classLoader = classLoader;
		this.sourceFiles = sourceFiles;
		this.resourceFiles = resourceFiles;
	}

	public SourceFile getSourceFile() {
		return null;
	}

	public SourceFiles getSourceFiles() {
		return null;
	}

	public <T> T getInstance(Class<T> type) {
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> T getInstance(Class<T> type, String className) {
		try {
			Class<?> loaded = this.classLoader.loadClass(className);
			Constructor<?> constructor = loaded.getDeclaredConstructor();
			return (T) constructor.newInstance();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
