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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link GeneratedSpringFactories} that can ultimately be
 * written to a {@link GeneratedFiles} instance.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class DefaultGeneratedSpringFactories implements GeneratedSpringFactories {

	private final Function<String, InputStreamSource> existingContent;

	private final Map<String, FactoryFile> factoryFiles = new LinkedHashMap<>();

	/**
	 * Create a new {@link DefaultGeneratedSpringFactories} instance.
	 */
	public DefaultGeneratedSpringFactories() {
		this(resourceLocation -> null);
	}

	/**
	 * Create a new {@link DefaultGeneratedSpringFactories} instance with access to
	 * existing file content.
	 * @param existingContent factory that will return any existing content given a
	 * resource location
	 */
	public DefaultGeneratedSpringFactories(Function<String, InputStreamSource> existingContent) {
		Assert.notNull(existingContent, "'existingContent' must not be null");
		this.existingContent = existingContent;
	}

	@Override
	public Declarations forResourceLocation(String resourceLocation) {
		Assert.hasLength(resourceLocation, "'resourceLocation' must not be empty");
		return this.factoryFiles.computeIfAbsent(resourceLocation,
				key -> new FactoryFile(resourceLocation, this.existingContent.apply(resourceLocation)));
	}

	/**
	 * Write generated Spring {@code .factories} files to the given {@link GeneratedFiles}
	 * instance.
	 * @param generatedFiles where to write the generated files
	 * @throws IOException on IO error
	 */
	public void writeTo(GeneratedFiles generatedFiles) throws IOException {
		Assert.notNull(generatedFiles, "'generatedFiles' must not be null");
		this.factoryFiles.values().forEach((factoryFile) -> factoryFile.writeTo(generatedFiles));
	}

	/**
	 * Factories in a specific resource.
	 */
	private static class FactoryFile implements Declarations {

		private final String resourceLocation;

		private final Map<String, Set<String>> factoryNames;

		FactoryFile(String resourceLocation, InputStreamSource existingContent) {
			this.resourceLocation = resourceLocation;
			this.factoryNames = new LinkedHashMap<>();
			if (existingContent != null) {
				try {
					addExisting(existingContent.getInputStream());
				}
				catch (IOException ex) {
					throw new IllegalStateException(ex);
				}
			}
		}

		private void addExisting(InputStream inputStream) throws IOException {
			Properties properties = PropertiesLoaderUtils.loadProperties(new InputStreamResource(inputStream));
			properties.forEach((factoryTypeName, implementationNames) -> {
				addAll((String) factoryTypeName,
						StringUtils.commaDelimitedListToStringArray((String) implementationNames));
			});
		}

		private void addAll(String factoryTypeName, String[] implementationNames) {
			for (String implementationName : implementationNames) {
				add(factoryTypeName, implementationName);
			}
		}

		@Override
		public void add(String factoryTypeName, String implementationName) {
			implementationName = implementationName.trim();
			if (StringUtils.hasLength(implementationName)) {
				this.factoryNames.computeIfAbsent(factoryTypeName.trim(), key -> new LinkedHashSet<>())
						.add(implementationName);
			}
		}

		private void writeTo(GeneratedFiles generatedFiles) {
			generatedFiles.addResourceFile(this.resourceLocation, this::writeTo);
		}

		private void writeTo(Appendable out) throws IOException {
			boolean first = true;
			for (Map.Entry<String, Set<String>> entry : this.factoryNames.entrySet()) {
				if (entry.getValue().isEmpty()) {
					continue;
				}
				out.append(first ? "" : "\n");
				out.append(entry.getKey());
				out.append("=\\\n");
				Iterator<String> iterator = entry.getValue().iterator();
				while (iterator.hasNext()) {
					out.append(iterator.next());
					out.append(iterator.hasNext() ? ",\\" : "");
					out.append("\n");
				}
				first = false;
			}
		}

	}

}
