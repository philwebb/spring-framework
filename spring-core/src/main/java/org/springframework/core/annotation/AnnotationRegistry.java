/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;

import org.springframework.core.Ordered;

/**
 * An annotation registry that can be {@link AnnotationRegistries#add added} to
 * allow potentially expensive reflection based introspections to be skipped. An
 * {@link AnnotationRegistry} provides a quick way to tell if a class does not
 * have a direct annotation, meta-annotation or repeated annotation on
 * <strong>any</strong> of its member elements.
 * <p>
 * If {@code canSkipIntrospection} returns {@code true} then it is guaranteed
 * that the given annotation is not contained on any of the following local
 * {@link java.lang.reflect.Member} types:
 * <ul>
 * <li>{@link java.lang.reflect.Field}</li>
 * <li>{@link java.lang.reflect.Method}</li>
 * <li>{@link java.lang.reflect.Constructor}</li>
 * </ul>
 * <p>
 * If {@code canSkipIntrospection} return {@code false} then the registry cannot
 * tell if introspection should be skipped or not.
 * <p>
 * The registry should <strong>not</strong> check superclass types or
 * implemented interfaces, however it must check:
 * <ul>
 * <li>If the annotation is directly present</li>
 * <li>If the annotation is meta-present</li>
 * <li>If the annotation is present via a {@link Repeatable @Repeatable}
 * container type</li>
 * </ul>
 * <p>
 * An AnnotationRegistry may use the {@link Order @Order} annotation or
 * implement the {@link Ordered} interface if it wishes to be given a specific
 * priority. Registries will never be used to check {@link AnnotationFilter#PLAIN
 * AnnotationFilter.PLAIN} annotations.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see AnnotationRegistries#add(AnnotationRegistry)
 */
@FunctionalInterface
public interface AnnotationRegistry {

	/**
	 * Determine whether the given class requires introspection for the the
	 * specified annotation.
	 * @param clazz the class to potentially introspect
	 * @param annotationType the searchable annotation type
	 * @return {@code true} if the class requires further introspection
	 */
	default boolean canSkipIntrospection(Class<?> clazz,
			Class<? extends Annotation> annotationType) {

		return canSkipIntrospection(clazz, annotationType.getName());
	}

	/**
	 * Determine whether the given class requires introspection for the the
	 * specified annotation.
	 * @param clazz the class to potentially introspect
	 * @param annotationName the fully-qualified name of the searchable
	 * annotation type
	 * @return {@code true} if the class requires further introspection
	 */
	boolean canSkipIntrospection(Class<?> clazz, String annotationName);

}
