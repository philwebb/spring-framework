/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aot.generate;

import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.InputStreamSource;
import org.springframework.javapoet.JavaFile;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.ThrowableConsumer;

/**
 * Interface that can be used to add {@link Kind#SOURCE source}, {@link Kind#RESOURCE
 * resource} or {@link Kind#CLASS class} files generated during ahead-of-time processing.
 * Source and resource files are written using UTF-8 encoding.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see InMemoryGeneratedFiles
 * @see FileSystemGeneratedFiles
 */
public interface GeneratedFiles {

	/**
	 * Add a generated {@link Kind#SOURCE source file} with content from the given
	 * {@link JavaFile}.
	 * @param javaFile the java file to add
	 */
	default void addSourceFile(JavaFile javaFile) {
		addSourceFile(javaFile, null);
	}

	/**
	 * Add a generated {@link Kind#SOURCE source file} with content from the given
	 * {@link JavaFile}.
	 * @param javaFile the java file to add
	 * @param targetClass the target class that can be used with a {@link MethodHandles}
	 * lookup or {@code null}
	 */
	default void addSourceFile(JavaFile javaFile, @Nullable Class<?> targetClass) {
		String className = javaFile.packageName + "." + javaFile.typeSpec.name;
		addSourceFile(className, javaFile::writeTo, targetClass);
	}

	/**
	 * Add a generated {@link Kind#SOURCE source file} with content from the given
	 * {@link CharSequence}.
	 * @param className the class name that should be used to determine the path of the
	 * file
	 * @param content the contents of the file
	 */
	default void addSourceFile(String className, CharSequence content) {
		addSourceFile(className, content, null);
	}

	/**
	 * Add a generated {@link Kind#SOURCE source file} with content from the given
	 * {@link CharSequence}.
	 * @param className the class name that should be used to determine the path of the
	 * file
	 * @param content the contents of the file
	 * @param targetClass the target class that can be used with a {@link MethodHandles}
	 * lookup or {@code null}
	 */
	default void addSourceFile(String className, CharSequence content, @Nullable Class<?> targetClass) {
		addSourceFile(className, appendable -> appendable.append(content), targetClass);
	}

	/**
	 * Add a generated {@link Kind#SOURCE source file} with content written to an
	 * {@link Appendable} passed to the given {@link ThrowableConsumer}.
	 * @param className the class name that should be used to determine the path of the
	 * file
	 * @param content a {@link ThrowableConsumer} that accepts an {@link Appendable} which
	 * will receive the file contents
	 */
	default void addSourceFile(String className, ThrowableConsumer<Appendable> content) {
		addSourceFile(className, content, null);
	}

	/**
	 * Add a generated {@link Kind#SOURCE source file} with content written to an
	 * {@link Appendable} passed to the given {@link ThrowableConsumer}.
	 * @param className the class name that should be used to determine the path of the
	 * file
	 * @param content a {@link ThrowableConsumer} that accepts an {@link Appendable} which
	 * will receive the file contents
	 * @param targetClass the target class that can be used with a {@link MethodHandles}
	 * lookup or {@code null}
	 */
	default void addSourceFile(String className, ThrowableConsumer<Appendable> content,
			@Nullable Class<?> targetClass) {
		addFile(Kind.SOURCE, getClassNamePath(className), content, targetClass);
	}

	/**
	 * Add a generated {@link Kind#SOURCE source file} with content from the given
	 * {@link InputStreamSource}.
	 * @param className the class name that should be used to determine the path of the
	 * file
	 * @param content an {@link InputStreamSource} that will provide an input stream
	 * containing the file contents
	 */
	default void addSourceFile(String className, InputStreamSource content) {
		addSourceFile(className, content, null);
	}

	/**
	 * Add a generated {@link Kind#SOURCE source file} with content from the given
	 * {@link InputStreamSource}.
	 * @param className the class name that should be used to determine the path of the
	 * file
	 * @param content an {@link InputStreamSource} that will provide an input stream
	 * containing the file contents
	 * @param targetClass the target class that can be used with a {@link MethodHandles}
	 * lookup or {@code null}
	 */
	default void addSourceFile(String className, InputStreamSource content, @Nullable Class<?> targetClass) {
		addFile(Kind.SOURCE, getClassNamePath(className), content, targetClass);
	}

	/**
	 * Add a generated {@link Kind#RESOURCE resource file} with content from the given
	 * {@link CharSequence}.
	 * @param path the relative path of the file
	 * @param content the contents of the file
	 */
	default void addResourceFile(String path, CharSequence content) {
		addResourceFile(path, appendable -> appendable.append(content));
	}

	/**
	 * Add a generated {@link Kind#RESOURCE resource file} with content written to an
	 * {@link Appendable} passed to the given {@link ThrowableConsumer}.
	 * @param path the relative path of the file
	 * @param content a {@link ThrowableConsumer} that accepts an {@link Appendable} which
	 * will receive the file contents
	 */
	default void addResourceFile(String path, ThrowableConsumer<Appendable> content) {
		addFile(Kind.RESOURCE, path, content);
	}

	/**
	 * Add a generated {@link Kind#RESOURCE resource file} with content from the given
	 * {@link InputStreamSource}.
	 * @param path the relative path of the file
	 * @param content an {@link InputStreamSource} that will provide an input stream
	 * containing the file contents
	 */
	default void addResourceFile(String path, InputStreamSource content) {
		addFile(Kind.RESOURCE, path, content);
	}

	/**
	 * Add a generated {@link Kind#CLASS class file} with content from the given
	 * {@link InputStreamSource}.
	 * @param path the relative path of the file
	 * @param content an {@link InputStreamSource} that will provide an input stream
	 * containing the file contents
	 */
	default void addClassFile(String path, InputStreamSource content) {
		addFile(Kind.CLASS, path, content);
	}

	/**
	 * Add a generated file of the specified {@link Kind} with content from the given
	 * {@link CharSequence}.
	 * @param kind the kind of file being written
	 * @param path the relative path of the file
	 * @param content the contents of the file
	 */
	default void addFile(Kind kind, String path, CharSequence content) {
		addFile(kind, path, appendable -> appendable.append(content));
	}

	/**
	 * Add a generated file of the specified {@link Kind} with content content written to
	 * an {@link Appendable} passed to the given {@link ThrowableConsumer}.
	 * @param kind the kind of file being written
	 * @param path the relative path of the file
	 * @param content a {@link ThrowableConsumer} that accepts an {@link Appendable} which
	 * will receive the file contents
	 */
	default void addFile(Kind kind, String path, ThrowableConsumer<Appendable> content) {
		addFile(kind, path, content, null);
	}

	/**
	 * Add a generated file of the specified {@link Kind} with content content written to
	 * an {@link Appendable} passed to the given {@link ThrowableConsumer}.
	 * @param kind the kind of file being written
	 * @param path the relative path of the file
	 * @param content a {@link ThrowableConsumer} that accepts an {@link Appendable} which
	 * will receive the file contents
	 */
	default void addFile(Kind kind, String path, ThrowableConsumer<Appendable> content,
			@Nullable Class<?> targetClass) {
		Assert.notNull(content, "'content' must not be null");
		InputStreamSource inputStreamSource = () -> {
			StringBuilder buffer = new StringBuilder();
			content.accept(buffer);
			return new ByteArrayInputStream(buffer.toString().getBytes(StandardCharsets.UTF_8));
		};
		addFile(kind, path, inputStreamSource, targetClass);
	}

	/**
	 * Add a generated file of the specified {@link Kind} with content from the given
	 * {@link InputStreamSource}.
	 * @param kind the kind of file being written
	 * @param path the relative path of the file
	 * @param content an {@link InputStreamSource} that will provide an input stream
	 * containing the file contents
	 */
	default void addFile(Kind kind, String path, InputStreamSource content) {
		addFile(kind, path, content, null);
	}

	/**
	 * Add a generated file of the specified {@link Kind} with content from the given
	 * {@link InputStreamSource}.
	 * @param kind the kind of file being written
	 * @param path the relative path of the file
	 * @param content an {@link InputStreamSource} that will provide an input stream
	 * containing the file contents
	 */
	void addFile(Kind kind, String path, InputStreamSource content, @Nullable Class<?> targetClass);

	private static String getClassNamePath(String className) {
		Assert.hasLength(className, "'className' must not be empty");
		Assert.isTrue(isJavaIdentifier(className), "'className' must be a valid identifier");
		return ClassUtils.convertClassNameToResourcePath(className) + ".java";
	}

	private static boolean isJavaIdentifier(String className) {
		char[] chars = className.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (i == 0 && !Character.isJavaIdentifierStart(chars[i])) {
				return false;
			}
			if (i > 0 && chars[i] != '.' && !Character.isJavaIdentifierPart(chars[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The various kinds of generated files that are supported.
	 */
	enum Kind {

		/**
		 * A source file containing Java code that should be compiled.
		 */
		SOURCE,

		/**
		 * A resource file that should be directly added to final application. For
		 * example, a {@code .properties} file.
		 */
		RESOURCE,

		/**
		 * A class file containing bytecode. For example, the result of a proxy generated
		 * using cglib.
		 */
		CLASS

	}

}
