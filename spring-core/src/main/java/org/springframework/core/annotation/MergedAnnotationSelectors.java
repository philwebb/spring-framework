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
import java.util.function.Predicate;

/**
 * {@link MergedAnnotationSelector} implementations that provide various options for
 * {@link MergedAnnotation MergedAnnotations}.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see MergedAnnotations#get(Class, Predicate, MergedAnnotationSelector)
 * @see MergedAnnotations#get(String, Predicate, MergedAnnotationSelector)
 */
public final class MergedAnnotationSelectors {

	private MergedAnnotationSelectors() {
	}

	/**
	 * Select the nearest annotation, i.e. the one with the lowest depth.
	 * @return a selector that picks the annotation with the lowest depth
	 */
	public static <A extends Annotation> MergedAnnotationSelector<A> nearest() {
		return (existing, candidate) -> {
			if (candidate.getDepth() < existing.getDepth()) {
				return candidate;
			}
			return existing;
		};
	}

	/**
	 * Select the first directly declared annotation when possible. If not direct
	 * annotations are declared then the earliest annotation is selected.
	 * @return a selector that picks the first directly declared annotation whenever possible
	 */
	public static <A extends Annotation> MergedAnnotationSelector<A> firstDirectlyDeclared() {
		return (existing, candidate) -> {
			if (existing.getDepth() > 0 && candidate.getDepth() == 0) {
				return candidate;
			}
			return existing;
		};
	}

}
