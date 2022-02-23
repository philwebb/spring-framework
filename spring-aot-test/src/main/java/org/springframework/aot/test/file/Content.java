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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nullable;

/**
 * File contents that can be transfered to an {@link Appendable}. Designed to align with
 * JavaPoet's {@code JavaFile.writeTo} method.
 *
 * @author Phillip Webb
 * @since 6.0
 */
@FunctionalInterface
public interface Content {

	/**
	 * Factory method to create new {@link Content} from the given {@link File}.
	 * @param file a file containing the contents
	 * @return a {@link SourceFile} instance
	 */
	static Content of(File file) {
		return of(file.toPath());
	}

	/**
	 * Factory method to create new {@link Content} from the given {@link Path}.
	 * @param path a path containing the contents
	 * @return a {@link SourceFile} instance
	 */
	static Content of(Path path) {
		try {
			return of(Files.readString(path));
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to read content", ex);
		}
	}

	/**
	 * Factory method to create new {@link Content} from the given {@link InputStream}.
	 * @param inputStream an input stream containing the contents
	 * @return a {@link SourceFile} instance
	 */
	static Content of(InputStream inputStream) {
		return of(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
	}

	/**
	 * Factory method to create new {@link Content} from the given {@link Reader}.
	 * @param reader a reader containing the contents
	 * @return a {@link SourceFile} instance
	 */
	static Content of(Reader reader) {
		try {
			StringWriter writer = new StringWriter();
			reader.transferTo(writer);
			reader.close();
			return of(writer.toString());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to read content", ex);
		}
	}

	/**
	 * Factory method to create a new {@link Content} from the given {@link CharSequence}.
	 * @param charSequence a file containing the contents
	 * @return a {@link SourceFile} instance
	 */
	static Content of(CharSequence charSequence) {
		return new StringContent(charSequence.toString());
	}

	/**
	 * Transfer the content to given {@link Appendable}.
	 * @param appendable the destination appendable
	 * @throws IOException on IO error
	 */
	void transferTo(Appendable appendable) throws IOException;

	/**
	 * Utility used to build a {@link String} from {@link Content}.
	 * @param content the content to use
	 * @return the content string
	 */
	static @Nullable String toString(@Nullable Content content) {
		if (content == null) {
			return null;
		}
		if (content instanceof StringContent) {
			return content.toString();
		}
		try {
			StringBuilder stringBuilder = new StringBuilder();
			content.transferTo(stringBuilder);
			return stringBuilder.toString();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to append content", ex);
		}
	}

}
