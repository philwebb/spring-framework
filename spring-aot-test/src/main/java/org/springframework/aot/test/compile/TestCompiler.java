package org.springframework.aot.test.compile;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.springframework.aot.test.file.WritableContent;
import org.springframework.aot.test.file.ResourceFile;
import org.springframework.aot.test.file.ResourceFiles;
import org.springframework.aot.test.file.SourceFile;
import org.springframework.aot.test.file.SourceFiles;

public class TestCompiler {

	private final ClassLoader classLoader;

	private final JavaCompiler compiler;

	private final SourceFiles sourceFiles;

	private final ResourceFiles resourceFiles;

	private TestCompiler(ClassLoader classLoader, JavaCompiler compiler, SourceFiles sourceFiles,
			ResourceFiles resourceFiles) {
		this.classLoader = classLoader;
		this.compiler = compiler;
		this.sourceFiles = sourceFiles;
		this.resourceFiles = resourceFiles;
	}

	public static TestCompiler forSystem() {
		return forCompiler(ToolProvider.getSystemJavaCompiler());
	}

	public static TestCompiler forCompiler(JavaCompiler javaCompiler) {
		return new TestCompiler(null, javaCompiler, SourceFiles.none(), ResourceFiles.none());
	}

	public TestCompiler withSources(SourceFile... sourceFiles) {
		return new TestCompiler(this.classLoader, this.compiler, this.sourceFiles.and(sourceFiles), this.resourceFiles);
	}

	public TestCompiler withSources(SourceFiles sourceFiles) {
		return new TestCompiler(this.classLoader, this.compiler, this.sourceFiles.and(sourceFiles), this.resourceFiles);
	}

	public TestCompiler withResources(ResourceFile... resourceFiles) {
		return new TestCompiler(this.classLoader, this.compiler, this.sourceFiles,
				this.resourceFiles.and(resourceFiles));
	}

	public TestCompiler withResources(ResourceFiles resourceFiles) {
		return new TestCompiler(this.classLoader, this.compiler, this.sourceFiles,
				this.resourceFiles.and(resourceFiles));
	}

	public void compile(WritableContent content, Consumer<Compiled> compiled) {
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
		DynamicClassLoader dynamicClassLoader = compile();
		ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(dynamicClassLoader);
			compiled.accept(new Compiled(dynamicClassLoader, this.sourceFiles, this.resourceFiles));
		}
		finally {
			Thread.currentThread().setContextClassLoader(previousClassLoader);
		}
	}

	private DynamicClassLoader compile() {
		ClassLoader classLoader = (this.classLoader != null) ? this.classLoader
				: Thread.currentThread().getContextClassLoader();
		List<DynamicJavaFileObject> compilationUnits = this.sourceFiles.stream().map(DynamicJavaFileObject::new)
				.toList();
		StandardJavaFileManager standardFileManager = this.compiler.getStandardFileManager(null, null, null);
		DynamicJavaFileManager fileManager = new DynamicJavaFileManager(standardFileManager, classLoader);
		Errors errors = new Errors();
		CompilationTask task = this.compiler.getTask(null, fileManager, errors, null, null, compilationUnits);
		boolean result = task.call();
		if (!result || errors.hasReportedErrors()) {
			throw new CompilationException("Unable to compile source" + errors);
		}
		return new DynamicClassLoader(this.classLoader, this.sourceFiles, this.resourceFiles,
				fileManager.getClassFiles());
	}

	/**
	 * {@link DiagnosticListener} used to collect errors.
	 */
	static class Errors implements DiagnosticListener<JavaFileObject> {

		private final StringBuilder message = new StringBuilder();

		@Override
		public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
			if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
				this.message.append("\n");
				this.message.append(diagnostic.getMessage(Locale.getDefault()));
				this.message.append(" ");
				this.message.append(diagnostic.getSource().getName());
				this.message.append(" ");
				this.message.append(diagnostic.getLineNumber() + ":" + diagnostic.getColumnNumber());
			}
		}

		boolean hasReportedErrors() {
			return this.message.length() > 0;
		}

		@Override
		public String toString() {
			return this.message.toString();
		}

	}

}
