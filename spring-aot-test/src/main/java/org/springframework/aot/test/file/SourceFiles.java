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

import java.util.stream.Stream;

import javax.annotation.Nullable;

/**
 * An immutable collection of {@link SourceFile} instances.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class SourceFiles {

	private static final SourceFiles NONE = new SourceFiles(DynamicFiles.none());

	private final DynamicFiles<SourceFile> files;

	private SourceFiles(DynamicFiles<SourceFile> files) {
		this.files = files;
	}

	/**
	 * Return a {@link SourceFiles} instance with no items.
	 * @return the empty instance
	 */
	public static SourceFiles none() {
		return NONE;
	}

	/**
	 * Factory method that can be used to create a {@link SourceFiles} instance containing
	 * the specified files.
	 * @param sourceFiles the files to include
	 * @return a {@link SourceFiles} instance
	 */
	public static SourceFiles of(SourceFile... sourceFiles) {
		return none().and(sourceFiles);
	}

	/**
	 * Return a new {@link SourceFiles} instance that merges files from another array of
	 * {@link SourceFile} instances.
	 * @param sourceFiles the instances to merge
	 * @return a new {@link SourceFiles} instance containing merged content
	 */
	public SourceFiles and(SourceFile... sourceFiles) {
		return new SourceFiles(this.files.and(sourceFiles));
	}

	/**
	 * Return a new {@link SourceFiles} instance that merges files from another
	 * {@link SourceFiles} instance.
	 * @param sourceFiles the instance to merge
	 * @return a new {@link SourceFiles} instance containing merged content
	 */
	public SourceFiles and(SourceFiles sourceFiles) {
		return new SourceFiles(this.files.and(sourceFiles.files));
	}

	/**
	 * Stream the {@link SourceFile} instances contained in this collection.
	 * @return a stream of file instances
	 */
	public Stream<SourceFile> stream() {
		return this.files.stream();
	}

	/**
	 * Get the {@link SourceFile} with the given {@code DynamicFile#getPath() path}.
	 * @param path the path to find
	 * @return a {@link SourceFile} instance or {@code null}
	 */
	@Nullable
	public SourceFile get(String path) {
		return this.files.get(path);
	}

}
