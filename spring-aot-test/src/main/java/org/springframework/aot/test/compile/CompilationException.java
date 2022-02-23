package org.springframework.aot.test.compile;

/**
 * Exception thrown when code cannot compile.
 *
 * @author Phillip Webb
 * @since 6.0.0
 */
public class CompilationException extends RuntimeException {

	CompilationException(String message) {
		super(message);
	}

}
