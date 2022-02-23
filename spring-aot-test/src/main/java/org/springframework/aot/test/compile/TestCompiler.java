package org.springframework.aot.test.compile;

import java.util.function.Consumer;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.springframework.aot.test.file.Content;
import org.springframework.aot.test.file.ResourceFile;
import org.springframework.aot.test.file.ResourceFiles;
import org.springframework.aot.test.file.SourceFile;
import org.springframework.aot.test.file.SourceFiles;
import org.springframework.javapoet.test.CompilationException;

public class TestCompiler {

	private final JavaCompiler javaCompiler;

	private final SourceFiles sourceFiles;

	private final ResourceFiles resourceFiles;

	private TestCompiler(JavaCompiler javaCompiler, SourceFiles sourceFiles, ResourceFiles resourceFiles) {
		this.javaCompiler = javaCompiler;
		this.sourceFiles = sourceFiles;
		this.resourceFiles = resourceFiles;
	}

	public static TestCompiler forSystem() {
		return forCompiler(ToolProvider.getSystemJavaCompiler());
	}

	public static TestCompiler forCompiler(JavaCompiler javaCompiler) {
		return new TestCompiler(javaCompiler, SourceFiles.none(), ResourceFiles.none());
	}

	public TestCompiler withSources(SourceFile... sourceFiles) {
		return new TestCompiler(javaCompiler, this.sourceFiles.and(sourceFiles), resourceFiles);
	}

	public TestCompiler withSources(SourceFiles sourceFiles) {
		return new TestCompiler(javaCompiler, this.sourceFiles.and(sourceFiles), resourceFiles);
	}

	public TestCompiler withResources(ResourceFile... resourceFiles) {
		return new TestCompiler(javaCompiler, sourceFiles, this.resourceFiles.and(resourceFiles));
	}

	public TestCompiler withResources(ResourceFiles resourceFiles) {
		return new TestCompiler(javaCompiler, sourceFiles, this.resourceFiles.and(resourceFiles));
	}

	public void compile(Content content, Consumer<Compiled> compiled) {
		compile(SourceFile.of(content), compiled);
	}

	public void compile(SourceFile sourceFile, Consumer<Compiled> compiled) {
		withSources(sourceFile).compile(compiled);
	}

	public void compile(SourceFiles sourceFiles, Consumer<Compiled> compiled) {
		withSources(sourceFiles).compile(compiled);
	}

	public void compile(SourceFiles sourceFiles, ResourceFiles resourceFiles, Consumer<Compiled> compiled) {
		withSources(sourceFiles).withResources(resourceFiles).compile(compiled);
	}

	public void compile(Consumer<Compiled> compiled) throws CompilationException {
		// FIXME
	}

}
