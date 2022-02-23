package org.springframework.aot.test.compile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/**
 * In-memory {@link JavaFileObject} used to hold class bytecode.
 *
 * @author Phillip Webb
 */
class DynamicClassFileObject extends SimpleJavaFileObject {

	private volatile byte[] bytes;

	DynamicClassFileObject(String className) {
		super(URI.create("class:///" + className.replace('.', '/') + ".class"), Kind.CLASS);
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		return new JavaClassOutputStream();
	}

	byte[] getBytes() {
		return this.bytes;
	}

	class JavaClassOutputStream extends ByteArrayOutputStream {

		@Override
		public void close() throws IOException {
			DynamicClassFileObject.this.bytes = toByteArray();
		}

	}

}
