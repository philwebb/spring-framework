package org.springframework.aot.test.compile;

import java.util.function.Consumer;

import org.springframework.aot.test.file.Content;
import org.springframework.aot.test.file.ResourceFiles;
import org.springframework.aot.test.file.SourceFile;
import org.springframework.aot.test.file.SourceFiles;
import org.springframework.javapoet.test.CompilationException;


public class TestCompiler {

	public static TestCompiler forSystem() {
		return null;
	}

	public TestCompiler withSources(SourceFile... sourceFiles) {
		return this;
	}

	public TestCompiler withSources(SourceFiles sourceFiles) {
		return this;
	}

	public TestCompiler withResources(ResourceFiles resourceFiles) {
		return this;
	}

	public void compile(Content content, Consumer<Compiled> compiled) {
		withSources(SourceFile.of(content)).compile(compiled);
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

	}

}
