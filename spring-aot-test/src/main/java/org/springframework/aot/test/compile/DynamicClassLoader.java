package org.springframework.aot.test.compile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;

import org.springframework.aot.test.file.ResourceFile;
import org.springframework.aot.test.file.ResourceFiles;
import org.springframework.aot.test.file.SourceFile;
import org.springframework.aot.test.file.SourceFiles;

/**
 * {@link ClassLoader} used to expose dynamically generated content.
 *
 * @author Phillip Webb
 */
public class DynamicClassLoader extends ClassLoader {

	private final Logger logger = System.getLogger(DynamicClassLoader.class.getName());

	private final SourceFiles sourceFiles;

	private final ResourceFiles resourceFiles;

	private final Map<String, DynamicClassFileObject> classFiles;

	public DynamicClassLoader(ClassLoader parent, SourceFiles sourceFiles, ResourceFiles resourceFiles,
			Map<String, DynamicClassFileObject> classFiles) {
		super(parent);
		this.sourceFiles = sourceFiles;
		this.resourceFiles = resourceFiles;
		this.classFiles = classFiles;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		DynamicClassFileObject classFile = this.classFiles.get(name);
		if (classFile != null) {
			return defineClass(name, classFile);
		}
		return super.findClass(name);
	}

	private Class<?> defineClass(String name, DynamicClassFileObject classFile) {
		byte[] bytes = classFile.getBytes();
		SourceFile sourceFile = this.sourceFiles.get(name);
		if (sourceFile != null && sourceFile.getTarget() != null) {
			try {
				Lookup lookup = MethodHandles.privateLookupIn(sourceFile.getTarget(), MethodHandles.lookup());
				return lookup.defineClass(bytes);
			}
			catch (IllegalAccessException ex) {
				this.logger.log(Level.WARNING, "Unable to define class using MethodHandles Lookup, "
						+ "only public methods and classes will be accessible");
			}
		}
		return defineClass(name, bytes, 0, bytes.length, null);
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		ResourceFile file = this.resourceFiles.get(name);
		if (file != null) {
			URL url = new URL(null, "resource:///" + file.getPath(), new ResourceFileHandler(file));
			return new SingletonEnumeration<>(url);
		}
		return super.findResources(name);
	}

	private static class SingletonEnumeration<E> implements Enumeration<E> {

		private E element;

		SingletonEnumeration(E element) {
			this.element = element;
		}

		@Override
		public boolean hasMoreElements() {
			return this.element != null;
		}

		@Override
		public E nextElement() {
			E next = this.element;
			this.element = null;
			return next;
		}

	}

	private static class ResourceFileHandler extends URLStreamHandler {

		private final ResourceFile file;

		ResourceFileHandler(ResourceFile file) {
			this.file = file;
		}

		@Override
		protected URLConnection openConnection(URL url) throws IOException {
			return new ResourceFileConnection(url, this.file);
		}

	}

	private static class ResourceFileConnection extends URLConnection {

		private final ResourceFile file;

		protected ResourceFileConnection(URL url, ResourceFile file) {
			super(url);
			this.file = file;
		}

		@Override
		public void connect() throws IOException {
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(this.file.getContent().getBytes(StandardCharsets.UTF_8));

		}

	}

}
