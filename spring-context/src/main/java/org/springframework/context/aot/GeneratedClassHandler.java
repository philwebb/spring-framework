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

package org.springframework.context.aot;

import java.util.function.BiConsumer;

import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.JavaReflectionHint.Category;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.Assert;

/**
 * Handle generated classes by adding them to a {@link GenerationContext},
 * and register the necessary hints so that they can be instantiated.
 *
 * @author Stephane Nicoll
 * @see ReflectUtils#setGeneratedClassHandler(BiConsumer)
 */
class GeneratedClassHandler implements BiConsumer<String, byte[]> {

	private final RuntimeHints runtimeHints;

	private final GeneratedFiles generatedFiles;

	GeneratedClassHandler(GenerationContext generationContext) {
		this.runtimeHints = generationContext.getRuntimeHints();
		this.generatedFiles = generationContext.getGeneratedFiles();
	}

	@Override
	public void accept(String className, byte[] content) {
		String targetTypeClassName = getTargetTypeClassName(className);
		this.runtimeHints.reflection().register(
				Category.INVOKE_DECLARED_CONSTRUCTORS,
				Category.INVOKE_DECLARED_METHODS,
				Category.DECLARED_FIELDS ).forType(className);
		this.runtimeHints.reflection().register(
				Category.INTROSPECT_DECLARED_CONSTRUCTORS,
				Category.INVOKE_DECLARED_METHODS).forType(targetTypeClassName);
		String path = className.replace(".", "/") + ".class";
		this.generatedFiles.addFile(Kind.CLASS, path, new ByteArrayResource(content));
	}

	private String getTargetTypeClassName(String proxyClassName) {
		int index = proxyClassName.indexOf("$$SpringCGLIB$$");
		Assert.isTrue(index != -1, () -> "Failed to extract target type from " + proxyClassName);
		return proxyClassName.substring(0, index);
	}

}
