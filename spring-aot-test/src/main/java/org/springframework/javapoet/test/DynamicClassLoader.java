package org.springframework.javapoet.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ClassUtils;

/**
 * {@link ClassLoader} used to expose dynamically generated content.
 *
 * @author Phillip Webb
 */
public class DynamicClassLoader extends ClassLoader {

	private final Log logger = LogFactory.getLog(DynamicClassLoader.class);

	private final Map<String, DynamicallyGeneratedJavaFile> javaFiles;

	private final Map<String, DynamicallyGeneratedResourceFile> resourceFiles;

	private final Map<String, ClassFileObject> classFiles;

	public DynamicClassLoader(ClassLoader parent, Map<String, DynamicallyGeneratedJavaFile> javaFiles,
			Map<String, DynamicallyGeneratedResourceFile> resourceFiles, Map<String, ClassFileObject> classFiles) {
		super((parent != null) ? parent : ClassUtils.getDefaultClassLoader());
		this.javaFiles = javaFiles;
		this.resourceFiles = resourceFiles;
		this.classFiles = classFiles;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		ClassFileObject classFile = this.classFiles.get(name);
		if (classFile != null) {
			return defineClass(name, classFile);
		}
		return super.findClass(name);
	}

	private Class<?> defineClass(String name, ClassFileObject classFile) {
		byte[] bytes = classFile.getBytes();
		DynamicallyGeneratedJavaFile javaFile = this.javaFiles.get(name);
		if (javaFile != null && javaFile.getClassName().isOwned()) {
			try {
				Class<?> target = javaFile.getClassName().getTarget();
				Lookup lookup = MethodHandles.privateLookupIn(target, MethodHandles.lookup());
				return lookup.defineClass(bytes);
			}
			catch (IllegalAccessException ex) {
				this.logger.warn("Unable to define class using MethodHandles Lookup, "
						+ "only public methods and classes will be accessible");
			}
		}
		return defineClass(name, bytes, 0, bytes.length, null);
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		DynamicallyGeneratedResourceFile file = this.resourceFiles.get(name);
		if (file != null) {
			URL url = new URL(null, "resource:///" + file.getRelativePath(), new ResourceFileHandler(file));
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

		private final DynamicallyGeneratedResourceFile file;

		ResourceFileHandler(DynamicallyGeneratedResourceFile file) {
			this.file = file;
		}

		@Override
		protected URLConnection openConnection(URL url) throws IOException {
			return new ResourceFileConnection(url, this.file);
		}

	}

	private static class ResourceFileConnection extends URLConnection {

		private final DynamicallyGeneratedResourceFile file;

		protected ResourceFileConnection(URL url, DynamicallyGeneratedResourceFile file) {
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
