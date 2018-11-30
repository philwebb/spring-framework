/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation.type;

import java.lang.reflect.AnnotatedElement;

/**
 * Resolver that can load {@link AnnotationType AnnotationTypes} based on a type
 * name.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see SimpleAnnotationTypeResolver
 */
public interface AnnotationTypeResolver {

	/**
	 * Return the {@link AnnotationType} for the given type.
	 * @param className the class name of the type to resolve
	 * @return a resolved type or {@code null}
	 */
	AnnotationType resolve(String className);
	// FIXME search for usages and check null;

	/**
	 * Expose the ClassLoader used by this resolver.
	 * @return the ClassLoader (only {@code null} if even the system ClassLoader
	 * isn't accessible)
	 */
	ClassLoader getClassLoader();

	/**
	 * Get an ASM based {@link AnnotationTypeResolver} for the specified
	 * class loader.
	 * @param classLoader the source class loader
	 * @return an {@link AnnotationTypeResolver}
	 */
	static AnnotationTypeResolver get(ClassLoader classLoader) {
		return SimpleAnnotationTypeResolver.get(classLoader);
	}

	/**
	 * Get an ASM based {@link AnnotationTypeResolver} based on a class loader
	 * deduced from the specified element.
	 * @param element the source element
	 * @return an {@link AnnotationTypeResolver}
	 */
	static AnnotationTypeResolver get(AnnotatedElement element) {
		return SimpleAnnotationTypeResolver.get(element);
	}

}
