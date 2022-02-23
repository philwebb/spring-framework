package org.springframework.javapoet.test;

import java.io.IOException;

/**
 * Base class for dynamically {@link GeneratedFile generated files}.
 *
 * @author Phillip Webb
 */
abstract class DynamicallyGeneratedFile implements GeneratedFile {

	private volatile String content;

	@Override
	public void withContentFrom(WritableContent content) throws IOException {
		StringBuilder out = new StringBuilder();
		content.writeTo(out);
		this.content = out.toString();
	}

	String getContent() {
		return this.content;
	}

}
