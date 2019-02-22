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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Provides {@link AnnotationTypeMapping} information for a single source
 * annotation type. Performs a recursive breadth first crawl of all
 * meta-annotations to ultimately provide a quick way to map the attributes of
 * root {@link Annotation}.
 * <p>
 * Supports convention based merging of meta-annotations as well as implicit and
 * explicit {@link AliasFor @AliasFor} aliases.
 * <p>
 * This class is designed to be cached so that meta-annotations only need to be
 * searched once, regardless of how many times they are actually used.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see AnnotationTypeMapping
 */
class AnnotationTypeMappings implements Iterable<AnnotationTypeMapping> {

	private static final Map<AnnotationFilter, Cache> cache = new ConcurrentReferenceHashMap<>();

	private final AnnotationFilter filter;

	private final List<AnnotationTypeMapping> mappings;

	public AnnotationTypeMappings(AnnotationFilter filter,
			Class<? extends Annotation> annotationType) {
		this.filter = filter;
		this.mappings = new ArrayList<>();
		addAllMappings(annotationType);
		this.mappings.forEach(AnnotationTypeMapping::afterAllMappingsSet);
	}

	private void addAllMappings(Class<? extends Annotation> annotationType) {
		Deque<AnnotationTypeMapping> queue = new ArrayDeque<>();
		queue.add(new AnnotationTypeMapping(annotationType));
		while (!queue.isEmpty()) {
			AnnotationTypeMapping mapping = queue.removeFirst();
			this.mappings.add(mapping);
			addMetaAnnotationsToQueue(queue, mapping);
		}
	}

	private void addMetaAnnotationsToQueue(Deque<AnnotationTypeMapping> queue,
			AnnotationTypeMapping parent) {
		Annotation[] metaAnnotations = parent.getAnnotationType().getDeclaredAnnotations();
		for (Annotation metaAnnotation : metaAnnotations) {
			if (!isMappable(parent, metaAnnotation)) {
				continue;
			}
			Annotation[] repeatedAnnotations = RepeatableContainers.standardRepeatables().findRepeatedAnnotations(
					metaAnnotation);
			if (repeatedAnnotations != null) {
				for (Annotation repeatedAnnotation : repeatedAnnotations) {
					if (!isMappable(parent, metaAnnotation)) {
						continue;
					}
					queue.addLast(new AnnotationTypeMapping(parent, repeatedAnnotation));

				}
			}
			else {
				queue.addLast(new AnnotationTypeMapping(parent, metaAnnotation));
			}
		}
	}

	private boolean isMappable(AnnotationTypeMapping parent, Annotation metaAnnotation) {
		return !filter.matches(metaAnnotation)
				&& !isAlreadyMapped(parent, metaAnnotation);
	}

	private boolean isAlreadyMapped(AnnotationTypeMapping parent,
			Annotation metaAnnotation) {
		Class<? extends Annotation> annotationType = metaAnnotation.annotationType();
		AnnotationTypeMapping mapping = parent;
		while (mapping != null) {
			if (mapping.getAnnotationType().equals(annotationType)) {
				return true;
			}
			mapping = mapping.getParent();
		}
		return false;
	}

	public int size() {
		return this.mappings.size();
	}

	public AnnotationTypeMapping get(int index) {
		return this.mappings.get(index);
	}

	public Iterator<AnnotationTypeMapping> iterator() {
		return this.mappings.iterator();
	}

	public static AnnotationTypeMappings forAnnotationType(
			Class<? extends Annotation> annotationType) {
		return forAnnotationType(annotationType, AnnotationFilter.PLAIN);
	}

	public static AnnotationTypeMappings forAnnotationType(
			Class<? extends Annotation> annotationType,
			AnnotationFilter annotationFilter) {
		Assert.notNull(annotationType, "AnnotationType must not be null");
		Assert.notNull(annotationFilter, "AnnotationFilter must not be null");
		return cache.computeIfAbsent(annotationFilter, Cache::new).get(annotationType);
	}

	private static class Cache {

		private final AnnotationFilter filter;

		private final Map<Class<? extends Annotation>, AnnotationTypeMappings> mappings;

		public Cache(AnnotationFilter filter) {
			this.filter = filter;
			this.mappings = new ConcurrentReferenceHashMap<>();
		}

		public AnnotationTypeMappings get(Class<? extends Annotation> annotationType) {
			return this.mappings.computeIfAbsent(annotationType, this::createMappings);
		}

		private AnnotationTypeMappings createMappings(
				Class<? extends Annotation> annotationType) {
			return new AnnotationTypeMappings(this.filter, annotationType);
		}

	}

}
