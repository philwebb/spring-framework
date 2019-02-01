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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.function.Supplier;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * @author pwebb
 * @since 5.1
 */
class AnnotationScanner {

	/**
	 * @param element
	 * @param searchStrategy
	 */
	public static Annotation[] getDirectlyPresent(AnnotatedElement element,
			SearchStrategy searchStrategy) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param element
	 * @param searchStrategy
	 * @return
	 */
	public static Supplier<Annotation[][]> getAggregatesSupplier(AnnotatedElement element,
			SearchStrategy searchStrategy) {
		return () -> getAggregates(element, searchStrategy);
	}

	private static Annotation[][] getAggregates(AnnotatedElement element,
			SearchStrategy searchStrategy) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
