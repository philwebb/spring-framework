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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class SourceFiles {

	private final List<SourceFile> files;

	private SourceFiles(List<SourceFile> files) {
		this.files = files;
	}

	public static SourceFiles none() {
		return new SourceFiles(Collections.emptyList());
	}

	public static SourceFiles of(SourceFile... sourceFiles) {
		return new SourceFiles(List.of(sourceFiles));
	}

	public SourceFiles and(SourceFile... sourceFiles) {
		return and(SourceFiles.of(sourceFiles));
	}

	public SourceFiles and(SourceFiles sourceFiles) {
		List<SourceFile> files = new ArrayList<>(this.files);
		files.addAll(sourceFiles.files);
		return new SourceFiles(Collections.unmodifiableList(files));
	}

	public Stream<SourceFile> stream() {
		return this.files.stream();
	}

	public SourceFile get(String name) {
		// TODO Auto-generated method stub
		return null;
	}

}
