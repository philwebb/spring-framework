/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * SPI that can be used to provide an annotation index so that potentially expensive
 * annotation search operations can be skipped. Implementations should be registered in
 * the {@code spring.factories} file under the
 * {@code org.springframework.core.annotation.MergedAnnotationIndex} key.
 * <p>
 * The index provides a mechanism to answer if an annotation or meta-annotation is
 * {@link QueryResult#KNOWN_PRESENT} or {@link QueryResult#KNOWN_MISSING}. If the index is
 * unable to determine a result then {@link QueryResult#UNKNOWN} should be returned.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see MergedAnnotationIndexes
 */
public interface MergedAnnotationIndex {

	/**
	 * Query the index to determine is a specific annotation or meta-annotation is present
	 * on an annotated element.
	 *
	 * @param resourceLoader the resource loader that can be used to load the annotated
	 * element
	 * @param sourceClass the class name of the source element to check
	 * @param annotationType the class name of the annotation or meta-annotation to check
	 * @param scope the scope of the search
	 * @return an appropriate {@link QueryResult} value
	 */
	QueryResult query(ResourceLoader resourceLoader, String sourceClass,
			String annotationType, Scope scope);

	/**
	 * The scope of a query search.
	 */
	enum Scope {

		/**
		 * Look for annotations on the class, superclass or implemented interfaces.
		 */
		CLASS_HIERARCHY,

		/**
		 * Look for annotations any methods in the class, superclass or implemented
		 * interfaces.
		 */
		METHOD_HIERARCHY,

	}

	/**
	 * The result of a query.
	 */
	enum QueryResult {

		/**
		 * The annotation is known to be present within the provided {@link Scope}. If
		 * this result is returned then searching for the annotation using
		 * {@link SearchStrategy#EXHAUSTIVE} will never return
		 * {@link MergedAnnotation#missing()}.
		 */
		KNOWN_PRESENT,

		/**
		 * The annotation is known to be missing within the provided {@link Scope}. If
		 * this result is returned then searching for the annotation using
		 * {@link SearchStrategy#EXHAUSTIVE} will always return
		 * {@link MergedAnnotation#missing()}.
		 */
		KNOWN_MISSING,

		/**
		 * It cannot be determined if the annotation is definitely present or definitely
		 * missing. A full {@link MergedAnnotations} search will be required.
		 */
		UNKNOWN;

		/**
		 * Returns {@code true} if the result is {@link #KNOWN_PRESENT} or
		 * {@link #KNOWN_MISSING}.
		 * @param result the result to check
		 * @return if the result is known
		 */
		public static boolean isKnown(@Nullable QueryResult result) {
			return result == KNOWN_PRESENT || result == KNOWN_MISSING;
		}

	}

}
