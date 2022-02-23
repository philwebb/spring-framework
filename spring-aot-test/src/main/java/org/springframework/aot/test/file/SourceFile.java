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

package org.springframework.aot.test.file;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;

import javax.annotation.Nullable;

import org.assertj.core.api.AssertProvider;
import org.assertj.core.util.Strings;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaPackage;
import com.thoughtworks.qdox.model.JavaSource;

/**
 * A single Java source file with {@link SourceFileAssert} support. Usually created from
 * an AOT generated type, for example:
 *
 * <pre class="code">
 * SourceFile.of(generatedFile::writeTo)
 * </pre>
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class SourceFile implements AssertProvider<SourceFileAssert> {

	private final String content;

	private final JavaSource javaSource;

	private final String path;

	private SourceFile(@Nullable String path, String content) {
		if (Strings.isNullOrEmpty(content)) {
			throw new AssertionError("Expecting source file content not to be empty");
		}
		this.content = content;
		this.javaSource = parse(content);
		this.path = (path != null) ? path : deducePath(this.javaSource);
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given {@link File}.
	 * @param file a file containing the source contents
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(File file) {
		return of(Content.of(file));
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given {@link Path}.
	 * @param path a path containing the source contents
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(Path path) {
		return of(Content.of(path));
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given
	 * {@link InputStream}.
	 * @param inputStream an input stream containing the source contents
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(InputStream inputStream) {
		return of(Content.of(inputStream));
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given {@link Reader}.
	 * @param reader a reader containing the source contents
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(Reader reader) {
		return of(Content.of(reader));
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given
	 * {@link CharSequence}.
	 * @param charSequence a file containing the source contents
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(CharSequence charSequence) {
		return of(Content.of(charSequence));
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given {@link Content}.
	 * @param content the source contents
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(Content content) {
		return of(null, content);
	}

	/**
	 * Factory method to create a new {@link SourceFile} from the given {@link Content}.
	 * @param path the relative path of the file or {@code null} to have the path deduced
	 * @param content the source contents
	 * @return a {@link SourceFile} instance
	 */
	public static SourceFile of(@Nullable String path, Content content) {
		return new SourceFile(path, Content.toString(content));
	}

	private static JavaSource parse(String content) {
		JavaProjectBuilder builder = new JavaProjectBuilder();
		try {
			JavaSource javaSource = builder.addSource(new StringReader(content));
			if (javaSource.getClasses().size() != 1) {
				throw new IllegalStateException("Source must define a single class");
			}
			return javaSource;
		}
		catch (Exception ex) {
			throw new AssertionError("Unable to parse source file content:\n\n" + content, ex);
		}
	}

	private static String deducePath(JavaSource javaSource) {
		JavaPackage javaPackage = javaSource.getPackage();
		JavaClass javaClass = javaSource.getClasses().get(0);
		String path = javaClass.getName() + ".java";
		if (javaPackage != null) {
			path = javaPackage.getName().replace('.', '/') + "/" + path;
		}
		return path;
	}

	/**
	 * Return the contents of the file.
	 * @return the file contents
	 */
	public String getContent() {
		return content;
	}

	JavaSource getJavaSource() {
		return javaSource;
	}

	/**
	 * Return the relative path of the file.
	 * @return the file path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @deprecated use {@code assertThat(sourceFile)} rather than calling this method
	 * directly.
	 */
	@Override
	@Deprecated
	public SourceFileAssert assertThat() {
		return new SourceFileAssert(this);
	}

	@Override
	public String toString() {
		return "SourceFile: " + this.path;
	}

	@Nullable
	public Class<?> getTarget() {
		return null; // FIXME not sure if we want this or not
	}

}
