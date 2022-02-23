package org.springframework.aot.test.compile;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

/**
 * {@link JavaFileManager} to create in-memory {@link DynamicClassFileObject ClassFileObjects}
 * when compiling.
 *
 * @author Phillip Webb
 */
class DynamicJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

	private final ClassLoader classLoader;

	private final Map<String, DynamicClassFileObject> classFiles = Collections.synchronizedMap(new LinkedHashMap<>());

	DynamicJavaFileManager(JavaFileManager fileManager, ClassLoader classLoader) {
		super(fileManager);
		this.classLoader = classLoader;
	}

	@Override
	public ClassLoader getClassLoader(Location location) {
		return this.classLoader;
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
			FileObject sibling) throws IOException {
		if (kind == JavaFileObject.Kind.CLASS) {
			return this.classFiles.computeIfAbsent(className, DynamicClassFileObject::new);
		}
		return super.getJavaFileForOutput(location, className, kind, sibling);
	}

	Map<String, DynamicClassFileObject> getClassFiles() {
		return Collections.unmodifiableMap(this.classFiles);
	}

}
