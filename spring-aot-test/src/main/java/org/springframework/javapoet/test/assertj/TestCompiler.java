package org.springframework.javapoet.test.assertj;

import java.util.function.Consumer;

import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.test.CompilationException;

public class TestCompiler {

	public static TestCompiler forSystem() {
		return null;
	}

	public TestCompiler withSources(JavaFile... javaFiles) {
		return this;
	}

	public TestCompiler withSources(SourceFiles sourceFiles) {
		return this;
	}

	public TestCompiler withResources(ResourceFiles resourceFiles) {
		return this;
	}

	public void compile(JavaFile sourceFile, Consumer<Compiled> compiled) {
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
