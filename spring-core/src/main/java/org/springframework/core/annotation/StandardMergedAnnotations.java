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
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.core.annotation.AnnotationScanner.Processor;
import org.springframework.lang.Nullable;

/**
 *
 * @author Phillip Webb
 * @since 5.1
 */
final class StandardMergedAnnotations implements MergedAnnotations {

	@Nullable
	private final Object source;

	@Nullable
	private final AnnotatedElement element;

	@Nullable
	private final SearchStrategy searchStrategy;

	@Nullable
	private final Annotation[] annotations;

	private final RepeatableContainers repeatableContainers;

	private final AnnotationFilter annotationFilter;

	private StandardMergedAnnotations(AnnotatedElement element,
			SearchStrategy searchStrategy, RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		this.source = element;
		this.element = element;
		this.searchStrategy = searchStrategy;
		this.annotations = null;
		this.repeatableContainers = repeatableContainers;
		this.annotationFilter = annotationFilter;
	}

	private StandardMergedAnnotations(@Nullable Object source, Annotation[] annotations,
			RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		this.source = source;
		this.element = null;
		this.searchStrategy = null;
		this.annotations = annotations;
		this.repeatableContainers = repeatableContainers;
		this.annotationFilter = annotationFilter;
	}

	@Override
	public Iterator<MergedAnnotation<Annotation>> iterator() {
		return stream().iterator();
	}

	@Override
	public <A extends Annotation> boolean isPresent(Class<A> annotationType) {
		if (annotationType == null || this.annotationFilter.matches(annotationType)) {
			return false;
		}
		return scan(annotationType, this::isPresent);
	}

	@Override
	public <A extends Annotation> boolean isPresent(String annotationType) {
		if (annotationType == null || this.annotationFilter.matches(annotationType)) {
			return false;
		}
		return scan(annotationType, this::isPresent);
	}

	private boolean isPresent(Object annotationType, int aggregateIndex, Object source,
			Annotation[] annotations) {
		for (Annotation annotation : annotations) {
			if (isPresent(annotationType, annotation)) {
				return true;
			}
		}
		return false;
	}

	private boolean isPresent(Object annotationType, Annotation annotation) {
		Class<? extends Annotation> type = annotation.annotationType();
		if (this.annotationFilter.matches(type)) {
			return false;
		}
		if (type == annotationType || type.getName().equals(annotationType)) {
			return true;
		}
		Annotation[] repeating = this.repeatableContainers.findRepeatedAnnotations(
				annotation);
		if (repeating != null) {
			return repeating.length > 0 ? isPresent(annotationType, repeating[0]) : false;
		}
		return AnnotationTypeMappings.get(type).isPresent(annotationType,
				this.annotationFilter);
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(
			@Nullable Class<A> annotationType) {
		return get(annotationType, null, null);
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(
			@Nullable Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate) {
		return get(annotationType, predicate, null);
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(
			@Nullable Class<A> annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			@Nullable MergedAnnotationSelector<A> selector) {
		if (annotationType == null || this.annotationFilter.matches(annotationType)) {
			return MergedAnnotation.missing();
		}
		return scan(annotationType, new GetSingleAnnotation<>());
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(
			@Nullable String annotationType) {
		return get(annotationType, null, null);
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(@Nullable String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate) {
		return get(annotationType, predicate, null);
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(@Nullable String annotationType,
			@Nullable Predicate<? super MergedAnnotation<A>> predicate,
			@Nullable MergedAnnotationSelector<A> selector) {
		if (annotationType == null || this.annotationFilter.matches(annotationType)) {
			return MergedAnnotation.missing();
		}
		return scan(annotationType, new GetSingleAnnotation<>());
	}

	@Override
	public <A extends Annotation> Stream<MergedAnnotation<A>> stream(
			Class<A> annotationType) {

		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <A extends Annotation> Stream<MergedAnnotation<A>> stream(
			String annotationType) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Stream<MergedAnnotation<Annotation>> stream() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	public <C, R> R scan(C criteria, Processor<C, R> operation) {
		if (this.annotations != null) {
			R runResult = operation.process(criteria, 0, this.source, this.annotations);
			return operation.postProcess(runResult);
		}
		return AnnotationScanner.search(this.searchStrategy, this.element, criteria,
				operation);
	}

	public static MergedAnnotations from(AnnotatedElement element,
			SearchStrategy searchStrategy, RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		return null;
	}

	public static MergedAnnotations from(@Nullable Object source,
			Annotation[] annotations, RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		return null;
	}

	/**
	 * {@link Processor} that finds a single {@link MergedAnnotation}.
	 */
	private class GetSingleAnnotation<A extends Annotation>
			implements Processor<Object, MergedAnnotation<A>> {

		private MergedAnnotationSelector<A> selector;

		@Nullable
		private Predicate<? super MergedAnnotation<A>> predicate;

		@Nullable
		private MergedAnnotation<A> result;

		@Override
		public MergedAnnotation<A> process(Object type, int aggregateIndex, Object source,
				Annotation[] annotations) {
			for (Annotation annotation : annotations) {
				if (annotationFilter.matches(annotation)) {
					continue;
				}
				for (Annotation candidate : annotations) {
					MergedAnnotation<A> result = process(type, aggregateIndex, source,
							candidate);
					if (result != null) {
						return result;
					}
				}
			}
			return null;
		}

		private MergedAnnotation<A> process(Object type, int aggregateIndex,
				Object source, Annotation annotation) {
			Annotation[] repeatedAnnotations = repeatableContainers.findRepeatedAnnotations(
					annotation);
			if (repeatedAnnotations != null) {
				return process(type, aggregateIndex, source, repeatedAnnotations);
			}
			AnnotationTypeMappings mappings = AnnotationTypeMappings.get(
					annotation.annotationType());
			for (int i = 0; i < mappings.size(); i++) {
				AnnotationTypeMapping mapping = mappings.getMapping(i);
				if (mapping.isForType(type, annotationFilter)) {
					MergedAnnotation<A> candidate = StandardMergedAnnotation.from(
							annotation, mapping, aggregateIndex);
					if (candidate != null) {
						if (this.selector.isBestCandidate(candidate)) {
							return candidate;
						}
						updateLastResult(candidate);
					}
				}
			}
			return null;
		}

		private void updateLastResult(MergedAnnotation<A> candidate) {
			this.result = this.result != null
					? this.selector.select(this.result, candidate)
					: this.result;
		}

		@Override
		public MergedAnnotation<A> postProcess(MergedAnnotation<A> result) {
			result = result != null ? result : this.result;
			return result != null ? result : MergedAnnotation.missing();
		}

	}

	/**
	 * {@link Spliterator} used to consume merged annotations from multiple
	 * aggregates in depth fist order.
	 */
	private class MergedAnnotationsSpliterator<A extends Annotation>
			implements Spliterator<MergedAnnotation<A>> {

		private List<Aggregate> aggregates;

		public boolean tryAdvance(Consumer<? super MergedAnnotation<A>> action) {
			Aggregate aggregate = findLowestDepthAggregate();
			if (aggregate == null) {
				return false;
			}
			aggregate.advance(action);
			return true;
		}

		private Aggregate findLowestDepthAggregate() {
			Aggregate result = null;
			for (Aggregate candidate : this.aggregates) {
				if (result == null
						|| candidate.getCurrentDepth() < result.getCurrentDepth()) {
					result = candidate;
				}
			}
			return result;
		}

		@Override
		public Spliterator<MergedAnnotation<A>> trySplit() {
			return null;
		}

		@Override
		public long estimateSize() {
			// FIXME
			return Integer.MAX_VALUE;
		}

		@Override
		public int characteristics() {
			return NONNULL | IMMUTABLE;
		}

		private class Aggregate {

			private int aggregateIndex;

			private Annotation[] annotations;

			private AnnotationTypeMappings[] mappings;

			private int[] mappingPositions;

			private int current;

			private int getCurrentDepth() {
				AnnotationTypeMapping mapping = getCurrentMapping();
				return mapping != null ? mapping.getDepth() : Integer.MAX_VALUE;
			}

			private AnnotationTypeMapping getCurrentMapping() {
				if (this.current == -1) {
					return null;
				}
				int position = this.mappingPositions[this.current];
				return this.mappings[this.current].getMapping(position);
			}

			public void advance(Consumer<? super MergedAnnotation<A>> action) {
				int position = this.mappingPositions[this.current];
				Annotation annotation = this.annotations[this.current];
				AnnotationTypeMappings mappings = this.mappings[this.current];
				AnnotationTypeMapping mapping = mappings.getMapping(position);
				this.mappingPositions[this.current] = position++;
				this.current = getNextIndex();
				action.accept(StandardMergedAnnotation.from(annotation, mapping,
						this.aggregateIndex));
			}

			private int getNextIndex() {
				int result = -1;
				int lowestDepth = Integer.MAX_VALUE;
				for (int i = 0; i < this.mappings.length; i++) {
					AnnotationTypeMapping mapping = this.mappings[i].getMapping(
							this.mappingPositions[i]);
					if (mapping != null && mapping.getDepth() < lowestDepth) {
						result = i;
					}
				}
				return result;
			}

		}

	}

}
