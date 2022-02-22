package org.springframework.javapoet.test;

import java.io.IOException;

/**
 * Callback interface used to write content to a {@link GeneratedFile}.
 *
 * @author Phillip Webb
 * @since 6.0.0
 */
@FunctionalInterface
public interface WritableContent {

	/**
	 * Callback method that should write the content to the given {@link Appendable}.
	 * @param out the {@link Appendable} used to receive the content
	 * @throws IOException on IO error
	 */
	void writeTo(Appendable out) throws IOException;

}
