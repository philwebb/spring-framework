package org.springframework.javapoet.test;

import java.io.IOException;
import java.net.URI;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import org.springframework.util.ClassUtils;

/**
 * {@link JavaFileObject} for java source.
 *
 * @author Phillip Webb
 */
class JavaSourceFileObject extends SimpleJavaFileObject {

	private final String content;

	JavaSourceFileObject(String className, String content) {
		super(URI.create(ClassUtils.convertClassNameToResourcePath(className) + ".java"), Kind.SOURCE);
		this.content = content;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		return this.content;
	}

}
