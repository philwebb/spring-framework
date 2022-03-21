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

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.GeneratedSpringFactories.Declarations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultGeneratedSpringFactories}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class DefaultGeneratedSpringFactoriesTests {

	private static final String DEFAULT_CONTENT = """
			com.example.FactoryA=\\
			com.example.FactoryA1,\\
			com.example.FactoryA2

			com.example.FactoryB=\\
			com.example.FactoryB1
			""";

	private static final String NAMED_CONTENT = """
			com.example.FactoryC=\\
			com.example.FactoryC1
			""";

	private static final String CUSTOM_CONTENT = """
			com.example.FactoryD=\\
			com.example.FactoryD1
			""";

	@Test
	void writeToWritesContent() throws Exception {
		DefaultGeneratedSpringFactories springFactories = new DefaultGeneratedSpringFactories();
		Declarations defaultLocation = springFactories.forDefaultResourceLocation();
		defaultLocation.add("com.example.FactoryA", "com.example.FactoryA1");
		defaultLocation.add("com.example.FactoryA", "com.example.FactoryA2");
		defaultLocation.add("com.example.FactoryB", "com.example.FactoryB1");
		Declarations namedLocation = springFactories.forNamedItem("com.example.Classification", "test");
		namedLocation.add("com.example.FactoryC", "com.example.FactoryC1");
		Declarations resourceLocation = springFactories.forResourceLocation("META-INF/custom.factories");
		resourceLocation.add("com.example.FactoryD", "com.example.FactoryD1");
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		springFactories.writeTo(generatedFiles);
		assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE, "META-INF/spring.factories"))
				.isEqualTo(DEFAULT_CONTENT);
		assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE,
				"META-INF/spring/com.example.Classification/test.factories")).isEqualTo(NAMED_CONTENT);
		assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE, "META-INF/custom.factories"))
				.isEqualTo(CUSTOM_CONTENT);
	}

	@Test
	void writeToWhenHasExistingContentMerges() throws Exception {
		Function<String, InputStreamSource> existing = name -> new ByteArrayResource(
				"com.example.FactoryA = com.example.FactoryA1".getBytes(StandardCharsets.UTF_8));
		DefaultGeneratedSpringFactories springFactories = new DefaultGeneratedSpringFactories(existing);
		Declarations defaultLocation = springFactories.forDefaultResourceLocation();
		defaultLocation.add("com.example.FactoryA", "com.example.FactoryA2");
		defaultLocation.add("com.example.FactoryB", "com.example.FactoryB1");
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		springFactories.writeTo(generatedFiles);
		assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE, "META-INF/spring.factories"))
				.isEqualTo(DEFAULT_CONTENT);
	}

}
