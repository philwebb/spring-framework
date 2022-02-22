package org.springframework.javapoet.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import org.springframework.util.ClassUtils;

/**
 * In-memory {@link JavaFileObject} used to hold class bytecode.
 *
 * @author Phillip Webb
 */
class ClassFileObject extends SimpleJavaFileObject {

	private volatile byte[] bytes;

	ClassFileObject(String className) {
		super(URI.create("class:///" + ClassUtils.convertClassNameToResourcePath(className) + ".class"), Kind.CLASS);
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
			ClassFileObject.this.bytes = toByteArray();
		}

	}

}
