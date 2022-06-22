package org.springframework.aot.graalvm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Phillip Webb
 */
class ThrowawayClassLoader extends ClassLoader {

	static {
		registerAsParallelCapable();
	}

	private final ClassLoader resourceLoader;

	ThrowawayClassLoader(ClassLoader parent) {
		super(parent.getParent());
		this.resourceLoader = parent;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> loaded = findLoadedClass(name);
			if (loaded != null) {
				return loaded;
			}
			try {
				return super.loadClass(name, true);
			} catch (ClassNotFoundException ex) {
				return loadClassFromResource(name);
			}
		}
	}

	private Class<?> loadClassFromResource(String name) throws ClassNotFoundException, ClassFormatError {
		String resourceName = name.replace('.', '/') + ".class";
		InputStream inputStream = resourceLoader.getResourceAsStream(resourceName);
		if (inputStream == null) {
			return null;
		}
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			inputStream.transferTo(outputStream);
			byte[] bytes = outputStream.toByteArray();
			return defineClass(name, bytes, 0, bytes.length);

		} catch (IOException ex) {
			throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
		}
	}

	@Override
	protected URL findResource(String name) {
		return resourceLoader.getResource(name);
	}

}
