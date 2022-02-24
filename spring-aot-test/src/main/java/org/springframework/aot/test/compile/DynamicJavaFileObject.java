package org.springframework.aot.test.compile;

import java.io.IOException;
import java.net.URI;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import org.springframework.aot.test.file.SourceFile;

/**
 * Adapts a {@link SourceFile} instance to a {@link JavaFileObject}.
 *
 * @author Phillip Webb
 */
class DynamicJavaFileObject extends SimpleJavaFileObject {

	private final SourceFile sourceFile;

	DynamicJavaFileObject(SourceFile sourceFile) {
		super(URI.create(sourceFile.getPath()), Kind.SOURCE);
		this.sourceFile = sourceFile;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		return sourceFile.getContent();
	}

}
