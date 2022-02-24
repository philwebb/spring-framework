package org.springframework.aot.test.compile;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.springframework.aot.test.file.ResourceFile;
import org.springframework.aot.test.file.ResourceFiles;
import org.springframework.aot.test.file.SourceFile;
import org.springframework.aot.test.file.SourceFiles;

public class Compiled {

	private final ClassLoader classLoader;

	private final SourceFiles sourceFiles;

	private final ResourceFiles resourceFiles;

	private List<Class<?>> compiledClasses;

	public Compiled(ClassLoader classLoader, SourceFiles sourceFiles, ResourceFiles resourceFiles) {
		this.classLoader = classLoader;
		this.sourceFiles = sourceFiles;
		this.resourceFiles = resourceFiles;
	}

	public SourceFile getSourceFile() {
		return this.sourceFiles.getSingle();
	}

	public SourceFiles getSourceFiles() {
		return this.sourceFiles;
	}

	public ResourceFile getResourceFile() {
		return this.resourceFiles.getSingle();
	}

	public ResourceFiles getResourceFiles() {
		return this.resourceFiles;
	}

	public <T> T getInstance(Class<T> type) {
		List<Class<?>> matching = getAllCompiledClasses().stream()
				.filter((candidate) -> type.isAssignableFrom(candidate)).toList();
		if (matching.isEmpty()) {
			throw new IllegalStateException("No instance found of type " + type.getName());
		}
		if (matching.size() > 1) {
			throw new IllegalStateException("Multiple instances found of type " + type.getName());
		}
		return newInstance(matching.get(0));
	}

	public List<Class<?>> getAllCompiledClasses() {
		List<Class<?>> compiledClasses = this.compiledClasses;
		if (compiledClasses == null) {
			compiledClasses = new ArrayList<>();
			this.sourceFiles.stream().map(this::loadClass).forEach(compiledClasses::add);
			this.compiledClasses = compiledClasses;
		}
		return compiledClasses;
	}

	public <T> T getInstance(Class<T> type, String className) {
		Class<?> loaded = loadClass(className);
		return newInstance(loaded);
	}

	@SuppressWarnings("unchecked")
	private <T> T newInstance(Class<?> loaded) {
		try {
			Constructor<?> constructor = loaded.getDeclaredConstructor();
			return (T) constructor.newInstance();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Class<?> loadClass(SourceFile sourceFile) {
		return loadClass(sourceFile.getClassName());
	}

	private Class<?> loadClass(String className) {
		try {
			return this.classLoader.loadClass(className);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
