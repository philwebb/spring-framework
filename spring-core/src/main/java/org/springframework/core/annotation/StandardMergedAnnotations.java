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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
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

	private volatile List<Aggregate> aggregates;

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
		return Spliterators.iterator(spliterator());
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
		Annotation[] repeatedAnnotations = this.repeatableContainers.findRepeatedAnnotations(
				annotation);
		if (repeatedAnnotations != null) {
			return repeatedAnnotations.length > 0
					? isPresent(annotationType, repeatedAnnotations[0])
					: false;
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
		return scan(annotationType, new FindMergedAnnotationProcessor<>());
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
		return scan(annotationType, new FindMergedAnnotationProcessor<>());
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

	private List<Aggregate> getAggregates() {
		List<Aggregate> aggregates = this.aggregates;
		if (aggregates == null) {
			aggregates = scan(null, new CollectAggregates());
			this.aggregates = aggregates;
		}
		return aggregates;
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
	private class FindMergedAnnotationProcessor<A extends Annotation>
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
	 * {@link Processor} that finds a single {@link MergedAnnotation}.
	 */
	private class CollectAggregates implements Processor<Object, List<Aggregate>> {

		private final List<Aggregate> aggregates = new ArrayList<>();

		@Override
		public List<Aggregate> process(Object criteria, int aggregateIndex, Object source,
				Annotation[] annotations) {
			this.aggregates.add(createAggregate(aggregateIndex, source, annotations));
			return null;
		}

		private Aggregate createAggregate(int aggregateIndex, Object source,
				Annotation[] annotations) {
			List<Annotation> aggregateAnnotations = getAggregateAnnotations(annotations);
			return new Aggregate(aggregateIndex, source, aggregateAnnotations);
		}

		private List<Annotation> getAggregateAnnotations(Annotation[] annotations) {
			List<Annotation> result = new ArrayList<>(annotations.length);
			addAggregateAnnotations(result, annotations);
			return result;
		}

		private void addAggregateAnnotations(List<Annotation> aggregateAnnotations,
				Annotation[] annotations) {
			for (Annotation annotation : annotations) {
				if (!annotationFilter.matches(annotation)) {
					Annotation[] repeatedAnnotations = repeatableContainers.findRepeatedAnnotations(
							annotation);
					if (repeatedAnnotations == null) {
						addAggregateAnnotations(aggregateAnnotations,
								repeatedAnnotations);
					}
					else {
						aggregateAnnotations.add(annotation);
					}
				}
			}
		}

	}

	/**
	 * {@link Spliterator} used to consume merged annotations from the
	 * aggregates in depth fist order.
	 */
	private class AggregatesSpliterator<A extends Annotation>
			implements Spliterator<MergedAnnotation<A>> {

		private final List<Aggregate> aggregates;

		private final int[][] positions;

		public AggregatesSpliterator(List<Aggregate> aggregates) {
			this.aggregates = aggregates;
			this.positions = new int[aggregates.size()][];
			for (int i = 0; i < aggregates.size(); i++) {
				this.positions[i] = new int[aggregates.get(i).size()];
			}
		}

		public boolean tryAdvance(Consumer<? super MergedAnnotation<A>> action) {
			int aggregateIndex = -1;
			int mappingsIndex = -1;
			int lowestDepth = Integer.MAX_VALUE;
			for (int i = 0; i < this.aggregates.size(); i++) {
				Aggregate aggregate = this.aggregates.get(i);
				for (int j = 0; j < aggregate.size(); j++) {
					int postion = this.positions[i][j];
					AnnotationTypeMapping mapping = aggregate.getMapping(j, postion);
					if (mapping.getDepth() < lowestDepth) {
						aggregateIndex = i;
						mappingsIndex = j;
						lowestDepth = mapping.getDepth();
					}
				}
			}
			if (aggregateIndex == -1) {
				return false;
			}
			int postion = this.positions[aggregateIndex][mappingsIndex];
			Aggregate aggregate = this.aggregates.get(aggregateIndex);
			action.accept(aggregate.getMergedAnnotation(postion));
			this.positions[aggregateIndex][mappingsIndex] = postion + 1;
			return true;
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

	}

	private static class Aggregate {

		private final int aggregateIndex;

		private final List<Annotation> annotations;

		private final AnnotationTypeMappings[] mappings;

		public Aggregate(int aggregateIndex, Object source,
				List<Annotation> annotations) {
			this.aggregateIndex = aggregateIndex;
			this.annotations = annotations;
			this.mappings = new AnnotationTypeMappings[annotations.size()];
			for (int i = 0; i < annotations.size(); i++) {
				mappings[i] = AnnotationTypeMappings.lookup(
						annotations.get(i).annotationType());
			}
		}

		/**
		 * @param j
		 * @param postion
		 * @return
		 */
		public AnnotationTypeMapping getMapping(int j, int postion) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		/**
		 * @param mappingIndex
		 */
		public <A extends Annotation> MergedAnnotation<A> getMergedAnnotation(int mappingIndex) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		public AnnotationTypeMappings getMappings(int index) {
			return null;
		}

		public int size() {
			return -1;
		}

	}

}
