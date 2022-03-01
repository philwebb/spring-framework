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

package org.springframework.aot.test.compile;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.test.file.SourceFile;

import static org.assertj.core.api.Assertions.assertThat;

public class TempTests {

	private static final String HELLO_WORLD = """
			package com.example;

			import java.util.function.Supplier;

			@Deprecated
			public class Hello implements Supplier<String> {

				public String get() {
					return "Hello World!";
					// FIXME
				}

			}
			""";

	@Test
	void testName() {
		TestCompiler.forSystem().compile(SourceFile.of(HELLO_WORLD), (compiled) -> {
			Object result = compiled.getInstance(Supplier.class).get();
			assertThat(result).isEqualTo("Hello World!");
			assertThat(compiled.getSourceFile()).hasMethodNamed("get").withBody("fail");
		});
	}

}
