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

package org.springframework.core.type.classreading;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.lang.Nullable;

/**
 * Internal class used to provide a {@link MergedAnnotation} instance as late
 * as possible.
 *
 * @author Phillip Webb
 * @since 5.2
 * @param <A> the annotation type
 */
final class MergedAnnotationSupplier<A extends Annotation> {

	@Nullable
	private final ClassLoader classLoader;

	private final Class<A> annotationType;

	private final Map<String, Object> attributes;

	private final boolean nestedAnnotations;

	MergedAnnotationSupplier(@Nullable ClassLoader classLoader,
			Class<A> annotationType, Map<String, Object> attributes, boolean nestedAnnotations) {

		this.classLoader = classLoader;
		this.annotationType = annotationType;
		this.attributes = attributes;
		this.nestedAnnotations = nestedAnnotations;
	}

	MergedAnnotation<A> get(@Nullable Object source) {
		return MergedAnnotation.of(this.classLoader, source, this.annotationType, getAttributes(source));
	}

	private Map<String, Object> getAttributes(@Nullable Object source) {
		if (!this.nestedAnnotations) {
			return this.attributes;
		}
		Map<String, Object> result = new LinkedHashMap<>(this.attributes.size());
		this.attributes.forEach((key, value) -> {
			if (value instanceof MergedAnnotationSupplier) {
				value = ((MergedAnnotationSupplier<?>) value).get(source);
			}
			if (value instanceof MergedAnnotationSupplier[]) {
				MergedAnnotationSupplier<?>[] suppliers = (MergedAnnotationSupplier<?>[]) value;
				MergedAnnotation<?>[] nested = new MergedAnnotation<?>[suppliers.length];
				for (int i = 0; i < suppliers.length; i++) {
					nested[i] = suppliers[i].get(source);
				}
				value = nested;
			}
			result.put(key, value);
		});
		return result;
	}

}
