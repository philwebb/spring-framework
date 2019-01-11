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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * {@link MergedAnnotations} implementation that uses {@link AnnotationTypeMappings} to
 * adapt annotations.
 *
 * @author Phillip Webb
 * @since 5.2
 */
final class TypeMappedAnnotations extends AbstractMergedAnnotations {

	private final List<MappableAnnotations> aggregates;

	private volatile Set<MergedAnnotation<Annotation>> all;

	private Map<String, Set<MergedAnnotation<?>>> allByType = new ConcurrentReferenceHashMap<>();

	private TypeMappedAnnotations(AnnotatedElement source, Annotation[] annotations,
			RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		this.aggregates = Collections.singletonList(new MappableAnnotations(source,
				annotations, repeatableContainers, annotationFilter));
	}

	private TypeMappedAnnotations(ClassLoader classLoader,
			Iterable<DeclaredAnnotations> aggregates,
			RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		this.aggregates = new ArrayList<>(getInitialSize(aggregates));
		int aggregateIndex = 0;
		for (DeclaredAnnotations declaredAnnotations : aggregates) {
			this.aggregates.add(new MappableAnnotations(classLoader, aggregateIndex,
					declaredAnnotations, repeatableContainers, annotationFilter));
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

	<A extends Annotation> MergedAnnotation<A> getFirst(Class<A> annotationType) {
		return getFirst(getClassName(annotationType));
	}

	<A extends Annotation> MergedAnnotation<A> getFirst(String annotationType) {
		return get(annotationType, null, this::selectFirst);
	}

	private <A extends Annotation> MergedAnnotation<A> selectFirst(
			MergedAnnotation<A> existing, MergedAnnotation<A> candidate) {
		// FIXME rename this and other first methods
		if (existing.getDepth() > 0 && candidate.getDepth() == 0) {
			return candidate;
		}
		return existing;
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate) {
		return get(annotationType, predicate, this::selectLowestDepth);
	}

	private <A extends Annotation> MergedAnnotation<A> selectLowestDepth(
			MergedAnnotation<A> existing, MergedAnnotation<A> candidate) {
		if (candidate.getDepth() < existing.getDepth()) {
			return candidate;
		}
		return existing;
	}

	private <A extends Annotation> MergedAnnotation<A> get(String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			MergedAnnotationSelector<A> selector) {
		for (MappableAnnotations annotations : this.aggregates) {
			MergedAnnotation<A> result = annotations.get(annotationType, predicate,
					selector);
			if (result != null) {
				return result;
			}
		}
		return MergedAnnotation.missing();
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <A extends Annotation> Set<MergedAnnotation<A>> getAll(String annotationType) {
		Set<MergedAnnotation<?>> result = this.allByType.get(annotationType);
		if (result == null) {
			result = (Set) super.getAll(annotationType);
			this.allByType.put(annotationType, result);
		}
		return (Set) result;
	}

	public Set<MergedAnnotation<Annotation>> getAll() {
		Set<MergedAnnotation<Annotation>> all = this.all;
		if (all == null) {
			all = computeAll();
			this.all = all;
		}
		return all;
	}

	private Set<MergedAnnotation<Annotation>> computeAll() {
		Set<MergedAnnotation<Annotation>> result = new LinkedHashSet<>(totalSize());
		for (MappableAnnotations annotations : this.aggregates) {
			List<Deque<MergedAnnotation<Annotation>>> queues = new ArrayList<>(
					annotations.size());
			for (MappableAnnotation annotation : annotations) {
				queues.add(annotation.getQueue());
			}
			addAllInDepthOrder(result, queues);
		}
		return Collections.unmodifiableSet(result);
	}

	private void addAllInDepthOrder(Set<MergedAnnotation<Annotation>> result,
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

	private boolean addAllForDepth(Set<MergedAnnotation<Annotation>> result,
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

	static TypeMappedAnnotations from(AnnotatedElement element,
			SearchStrategy searchStrategy, RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		Assert.notNull(repeatableContainers, "RepeatableContainers must not be null");
		Assert.notNull(annotationFilter, "AnnotationFilter must not be null");
		Assert.notNull(searchStrategy, "SearchStrategy must not be null");
		Assert.notNull(element, "Element must not be null");
		AnnotationsScanner annotations = new AnnotationsScanner(element, searchStrategy);
		return of(null, annotations, repeatableContainers, annotationFilter);
	}

	static TypeMappedAnnotations from(@Nullable AnnotatedElement source,
			Annotation[] annotations, RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		Assert.notNull(annotations, "Annotations must not be null");
		return new TypeMappedAnnotations(source, annotations, repeatableContainers,
				annotationFilter);
	}

	static TypeMappedAnnotations of(ClassLoader classLoader,
			Iterable<DeclaredAnnotations> aggregates,
			RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		return new TypeMappedAnnotations(classLoader, aggregates, repeatableContainers,
				annotationFilter);
	}

	/**
	 * A collection of {@link MappableAnnotation mappable annotations}.
	 */
	private static class MappableAnnotations implements Iterable<MappableAnnotation> {

		private final List<MappableAnnotation> mappableAnnotations;

		public MappableAnnotations(AnnotatedElement source, Annotation[] annotations,
				RepeatableContainers repeatableContainers,
				AnnotationFilter annotationFilter) {
			this.mappableAnnotations = new ArrayList<>(annotations.length);
			ClassLoader sourceClassLoader = getClassLoader(source);
			for (Annotation annotation : annotations) {
				ClassLoader classLoader = sourceClassLoader != null ? sourceClassLoader
						: annotation.getClass().getClassLoader();
				add(classLoader, source, 0, DeclaredAnnotation.from(annotation),
						repeatableContainers, annotationFilter);
			}
		}

		public MappableAnnotations(ClassLoader classLoader, int aggregateIndex,
				DeclaredAnnotations annotations,
				RepeatableContainers repeatableContainers,
				AnnotationFilter annotationFilter) {
			this.mappableAnnotations = new ArrayList<>(annotations.size());
			if (classLoader == null) {
				classLoader = getClassLoader(annotations.getSource());
			}
			for (DeclaredAnnotation annotation : annotations) {
				add(classLoader, annotations.getSource(), aggregateIndex, annotation,
						repeatableContainers, annotationFilter);
			}
		}

		private void add(ClassLoader classLoader, Object source, int aggregateIndex,
				DeclaredAnnotation annotation, RepeatableContainers repeatableContainers,
				AnnotationFilter annotationFilter) {
			repeatableContainers.visit(annotation, classLoader, (type, attributes) -> {
				AnnotationTypeMappings mappings = AnnotationTypeMappings.forType(
						classLoader, repeatableContainers, annotationFilter, type);
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
				@Nullable Predicate<? super MergedAnnotation<A>> predicate,
				MergedAnnotationSelector<A> selector) {
			MergedAnnotation<A> result = null;
			for (MappableAnnotation mappableAnnotation : this.mappableAnnotations) {
				MergedAnnotation<A> candidate = mappableAnnotation.get(annotationType,
						predicate);
				if (candidate != null && result == null) {
					result = candidate;
				}
				else if (candidate != null) {
					result = selector.select(result, candidate);
				}
			}
			return result;
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
	 * Strategy interface used to select between two annotations.
	 */
	private interface MergedAnnotationSelector<A extends Annotation> {

		/**
		 * Select the annotation that should be used.
		 * @param existing an existing annotation returned from an earlier result
		 * @param candidate a candidate annotation that may be better suited
		 * @return the most appropriate annotation from the {@code existing} or
		 * {@code candidate}
		 */
		MergedAnnotation<A> select(MergedAnnotation<A> existing,
				MergedAnnotation<A> candidate);

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
				@Nullable Predicate<? super MergedAnnotation<A>> predicate) {
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
