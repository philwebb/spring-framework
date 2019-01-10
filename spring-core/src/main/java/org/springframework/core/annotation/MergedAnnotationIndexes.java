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

import org.springframework.core.annotation.MergedAnnotationIndex.QueryResult;
import org.springframework.core.annotation.MergedAnnotationIndex.Scope;
import org.springframework.lang.Nullable;

/**
 * API that allows all registered {@link MergedAnnotationIndex} instances to be queried to
 * determine if potentially expensive operations can be skipped.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see MergedAnnotationIndex
 */
public interface MergedAnnotationIndexes {

	/**
	 * Query the indexes to determine is a specific annotation or meta-annotation is
	 * present on an annotated element.
	 * @param classLoader the classloader that could be used to load the annotated element
	 * @param sourceClass the class name of the source element to check
	 * @param annotationType the class name of the annotation or meta-annotation to check
	 * @param scope the scope of the search
	 * @return an appropriate {@link QueryResult} value
	 */
	QueryResult query(String sourceClass, String annotationType, Scope scope);

	/**
	 * Get a {@link MergedAnnotationIndexes} instance for the specified class loader.
	 * @param classLoader the source class loader
	 * @return a {@link MergedAnnotationIndexes} instance that can be used to query all
	 * indexes
	 */
	static MergedAnnotationIndexes get(@Nullable ClassLoader classLoader) {
		return null;
	}

}
