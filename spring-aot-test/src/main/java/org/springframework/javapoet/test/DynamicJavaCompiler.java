package org.springframework.javapoet.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/**
 * Compiler that generates class files directly in memory.
 *
 * @author Phillip Webb
 * @since 6.0.0
 */
public class DynamicJavaCompiler {

	private final ClassLoader classLoader;

	private final JavaCompiler compiler;

	private DynamicallyGeneratedFilesManager generatedFiles;

	public DynamicJavaCompiler(ClassLoader classLoader) {
		this(classLoader, ToolProvider.getSystemJavaCompiler());
	}

	DynamicJavaCompiler(ClassLoader classLoader, JavaCompiler compiler) {
		this.classLoader = classLoader;
		this.compiler = compiler;
		this.generatedFiles = new DynamicallyGeneratedFilesManager();
	}

	public GeneratedFilesManager getGeneratedFilesManager() {
		return this.generatedFiles;
	}

	public ClassLoader compile() {
		Map<String, DynamicallyGeneratedJavaFile> javaFiles = this.generatedFiles.getJavaFiles();
		if (javaFiles.isEmpty()) {
			return this.classLoader;
		}
		List<JavaSourceFileObject> compilationUnits = new ArrayList<>(javaFiles.size());
		javaFiles.forEach((name, file) -> compilationUnits.add(new JavaSourceFileObject(name, file.getContent())));
		DynamicJavaFileManager fileManager = new DynamicJavaFileManager(this.classLoader,
				this.compiler.getStandardFileManager(null, null, null));
		Errors errors = new Errors();
		CompilationTask task = this.compiler.getTask(null, fileManager, errors, null, null, compilationUnits);
		boolean result = task.call();
		if (!result || errors.hasReportedErrors()) {
			throw new CompilationException("Unable to compile source" + errors);
		}
		return new DynamicClassLoader(this.classLoader, javaFiles, this.generatedFiles.getResourceFiles(),
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
