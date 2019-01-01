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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;

import org.springframework.lang.Nullable;

/**
 * Filter that can be used to restrict annotations.
 *
 * @author Phillip Webb
 * @since 5.2
 */
@FunctionalInterface
public interface AnnotationFilter {

	/**
	 * {@link AnnotationFilter} that matches annotations is in the
	 * {@code java.lang.annotation} or in the {@code org.springframework.lang}
	 * package.
	 */
	static final AnnotationFilter PLAIN = packages("java.lang.annotation",
			"org.springframework.lang");

	/**
	 * {@link AnnotationFilter} that matches annotations is in the
	 * {@code java.lang.annotation} package.
	 */
	static final AnnotationFilter JAVA = packages("java.lang.annotation");

	/**
	 * {@link AnnotationFilter} that never matches and can be used when no
	 * filtering is needed.
	 */
	static final AnnotationFilter NONE = annotationType ->  false;

	/**
	 * Test if the given annotation matches the filter.
	 * @param annotation the annotation to test
	 * @return {@code true} if the annotation matches
	 */
	default boolean matches(@Nullable Annotation annotation) {
		return matches(annotation != null ? annotation.annotationType() : null);
	}

	/**
	 * Test if the given annotation type matches the filter.
	 * @param annotationType the annotation type to test
	 * @return {@code true} if the annotation matches
	 */
	default boolean matches(@Nullable Class<? extends Annotation> annotationType) {
		return matches(annotationType != null ? annotationType.getName() : null);
	}

	/**
	 * Test if the given annotation type matches the filter.
	 * @param annotationType the annotation type to test
	 * @return {@code true} if the annotation matches
	 */
	boolean matches(@Nullable String annotationType);

	/**
	 * Return a new {@link AnnotationFilter} that matches annotations in the
	 * specified packages.
	 * @param packages the annotation packages that should match
	 * @return a new {@link AnnotationFilter} instance
	 */
	static AnnotationFilter packages(String... packages) {
		return new PackagesAnnotationFilter(packages);
	}

}
