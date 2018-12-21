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
import java.lang.reflect.Member;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.core.annotation.type.DeclaredAttributes;

/**
 * {@link MergedAnnotations} implementation that uses
 * {@link AnnotationTypeMappings} to adapt annotations.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class TypeMappedAnnotations extends AbstractMergedAnnotations {

	private final List<MappableAnnotations> aggregates;

	private volatile List<MergedAnnotation<Annotation>> all;

	TypeMappedAnnotations(RepeatableContainers repeatableContainers,
			AnnotatedElement source, Annotation[] annotations) {
		this.aggregates = Collections.singletonList(
				new MappableAnnotations(source, annotations, repeatableContainers));
	}

	TypeMappedAnnotations(ClassLoader classLoader,
			RepeatableContainers repeatableContainers,
			Iterable<DeclaredAnnotations> aggregates) {
		this.aggregates = new ArrayList<>(getInitialSize(aggregates));
		int aggregateIndex = 0;
		for (DeclaredAnnotations declaredAnnotations : aggregates) {
			this.aggregates.add(new MappableAnnotations(classLoader, aggregateIndex,
					declaredAnnotations, repeatableContainers));
			aggregateIndex++;
		}
	}

	private int getInitialSize(Iterable<DeclaredAnnotations> aggregates) {
		if (aggregates instanceof AnnotationsScanner) {
			return ((AnnotationsScanner) aggregates).size();
		}
		return 10;
	}

	@Override
	public <A extends Annotation> boolean isPresent(String annotationType) {
		for (MappableAnnotations annotations : this.aggregates) {
			if (annotations.isPresent(annotationType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType,
			Predicate<? super MergedAnnotation<A>> predicate) {
		for (MappableAnnotations annotations : this.aggregates) {
			MergedAnnotation<A> result = annotations.get(annotationType, predicate);
			if (result != null) {
				return result;
			}
		}
		return MergedAnnotation.missing();
	}

	@Override
	public Stream<MergedAnnotation<Annotation>> stream() {
		return getAll().stream();
	}

	private List<MergedAnnotation<Annotation>> getAll() {
		List<MergedAnnotation<Annotation>> all = this.all;
		if (all == null) {
			all = computeAll();
			this.all = all;
		}
		return all;
	}

	private List<MergedAnnotation<Annotation>> computeAll() {
		List<MergedAnnotation<Annotation>> result = new ArrayList<>(totalSize());
		for (MappableAnnotations annotations : this.aggregates) {
			List<Deque<MergedAnnotation<Annotation>>> queues = new ArrayList<>(
					annotations.size());
			for (MappableAnnotation annotation : annotations) {
				queues.add(annotation.getQueue());
			}
			addAllInDepthOrder(result, queues);
		}
		return result;
	}

	private void addAllInDepthOrder(List<MergedAnnotation<Annotation>> result,
			List<Deque<MergedAnnotation<Annotation>>> queues) {
		int depth = 0;
		boolean hasMore = true;
		while (hasMore) {
			hasMore = false;
			for (Deque<MergedAnnotation<Annotation>> queue : queues) {
				hasMore = hasMore | addAllForDepth(result, queue, depth);
			}
			depth++;
		}
	}

	private boolean addAllForDepth(List<MergedAnnotation<Annotation>> result,
			Deque<MergedAnnotation<Annotation>> queue, int depth) {
		while (!queue.isEmpty() && queue.peek().getDepth() <= depth) {
			result.add(queue.pop());
		}
		return !queue.isEmpty();
	}

	private int totalSize() {
		int size = 0;
		for (MappableAnnotations annotations : this.aggregates) {
			size += annotations.totalSize();
		}
		return size;
	}

	/**
	 * A collection of {@link MappableAnnotation mappable annotations}.
	 */
	private static class MappableAnnotations implements Iterable<MappableAnnotation> {

		private final List<MappableAnnotation> mappableAnnotations;

		public MappableAnnotations(AnnotatedElement source, Annotation[] annotations,
				RepeatableContainers repeatableContainers) {
			this.mappableAnnotations = new ArrayList<>(annotations.length);
			ClassLoader sourceClassLoader = getClassLoader(source);
			for (Annotation annotation : annotations) {
				ClassLoader classLoader = sourceClassLoader != null ? sourceClassLoader
						: annotation.getClass().getClassLoader();
				add(classLoader, source, 0, DeclaredAnnotation.from(annotation),
						repeatableContainers);
			}
		}

		public MappableAnnotations(ClassLoader classLoader, int aggregateIndex,
				DeclaredAnnotations annotations,
				RepeatableContainers repeatableContainers) {
			this.mappableAnnotations = new ArrayList<>(annotations.size());
			if (classLoader == null) {
				classLoader = getClassLoader(annotations.getSource());
			}
			for (DeclaredAnnotation annotation : annotations) {
				add(classLoader, annotations.getSource(), aggregateIndex, annotation,
						repeatableContainers);
			}
		}

		private void add(ClassLoader classLoader, Object source, int aggregateIndex,
				DeclaredAnnotation annotation,
				RepeatableContainers repeatableContainers) {
			repeatableContainers.visit(annotation, classLoader, (type, attributes) -> {
				AnnotationTypeMappings mappings = AnnotationTypeMappings.forType(
						classLoader, repeatableContainers, type);
				if (mappings != null) {
					this.mappableAnnotations.add(new MappableAnnotation(mappings, source,
							aggregateIndex, attributes));
				}
			});

		}

		private ClassLoader getClassLoader(Object source) {
			if (source instanceof Member) {
				return getClassLoader(((Member) source).getDeclaringClass());
			}
			if (source instanceof Class) {
				return ((Class<?>) source).getClassLoader();
			}
			return null;
		}

		public boolean isPresent(String annotationType) {
			for (MappableAnnotation mappableAnnotation : this.mappableAnnotations) {
				if (mappableAnnotation.isPresent(annotationType)) {
					return true;
				}
			}
			return false;
		}

		public <A extends Annotation> MergedAnnotation<A> get(String annotationType,
				Predicate<? super MergedAnnotation<A>> predicate) {
			MergedAnnotation<A> result = null;
			for (MappableAnnotation mappableAnnotation : this.mappableAnnotations) {
				MergedAnnotation<A> candidate = mappableAnnotation.get(annotationType,
						predicate);
				if (isBetterGetCandidate(candidate, result)) {
					result = candidate;
				}
			}
			return result;
		}

		private boolean isBetterGetCandidate(MergedAnnotation<?> candidate,
				MergedAnnotation<?> previous) {
			return candidate != null
					&& (previous == null || candidate.getDepth() < previous.getDepth());
		}

		public int size() {
			return this.mappableAnnotations.size();
		}

		public int totalSize() {
			int size = 0;
			for (MappableAnnotation mappableAnnotation : this.mappableAnnotations) {
				size += mappableAnnotation.size();
			}
			return size;
		}

		@Override
		public Iterator<MappableAnnotation> iterator() {
			return this.mappableAnnotations.iterator();
		}

	}

	/**
	 * A single mappable annotation.
	 */
	private static class MappableAnnotation {

		private final AnnotationTypeMappings mappings;

		private final Object source;

		private final int aggregateIndex;

		private final DeclaredAttributes attributes;

		public MappableAnnotation(AnnotationTypeMappings mappings, Object source,
				int aggregateIndex, DeclaredAttributes attributes) {
			this.mappings = mappings;
			this.source = source;
			this.aggregateIndex = aggregateIndex;
			this.attributes = attributes;
		}

		public boolean isPresent(String annotationType) {
			return this.mappings.get(annotationType) != null;
		}

		public <A extends Annotation> MergedAnnotation<A> get(String annotationType,
				Predicate<? super MergedAnnotation<A>> predicate) {
			if (predicate == null) {
				AnnotationTypeMapping mapping = this.mappings.get(annotationType);
				return mapping != null ? map(mapping) : null;
			}
			for (AnnotationTypeMapping mapping : this.mappings.getAll()) {
				if (mapping.getAnnotationType().getClassName().equals(annotationType)) {
					MergedAnnotation<A> mapped = map(mapping);
					if (predicate.test(mapped)) {
						return mapped;
					}
				}
			}
			return null;
		}

		public Deque<MergedAnnotation<Annotation>> getQueue() {
			Deque<MergedAnnotation<Annotation>> queue = new ArrayDeque<>(size());
			for (AnnotationTypeMapping mapping : this.mappings.getAll()) {
				queue.add(map(mapping));
			}
			return queue;
		}

		private <A extends Annotation> TypeMappedAnnotation<A> map(
				AnnotationTypeMapping mapping) {
			return new TypeMappedAnnotation<A>(mapping, this.source, this.aggregateIndex,
					this.attributes);
		}

		public int size() {
			return this.mappings.getAll().size();
		}

	}

}
