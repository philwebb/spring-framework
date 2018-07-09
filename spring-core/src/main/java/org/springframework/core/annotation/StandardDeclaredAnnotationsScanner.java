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
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAnnotations;

/**
 *
 * @author Phillip Webb
 * @since 5.1
 */
class StandardDeclaredAnnotationsScanner implements Iterable<DeclaredAnnotations> {

	private final DeclaredAnnotations declaredAnnotations;

	public StandardDeclaredAnnotationsScanner(AnnotatedElement source,
			SearchStrategy searchStrategy) {
		this.declaredAnnotations = getDeclaredAnnotations(source);
	}

	private DeclaredAnnotations getDeclaredAnnotations(AnnotatedElement source) {
		Annotation[] annotations = source.getDeclaredAnnotations();
		List<DeclaredAnnotation> result = new ArrayList<>(annotations.length);
		for (Annotation annotation : annotations) {
			result.add(DeclaredAnnotation.from(annotation));
		}
		return DeclaredAnnotations.of(result);
	}

	@Override
	public Iterator<DeclaredAnnotations> iterator() {
		return Collections.singleton(this.declaredAnnotations).iterator();
	}

	// FIXME support searching

}
