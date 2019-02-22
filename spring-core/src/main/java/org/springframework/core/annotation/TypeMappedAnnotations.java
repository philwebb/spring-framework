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
import java.util.stream.StreamSupport;

import org.springframework.lang.Nullable;

/**
 *
 * @author Phillip Webb
 * @since 5.1
 */
final class TypeMappedAnnotations implements MergedAnnotations {

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

	@Nullable
	private volatile List<Aggregate> aggregates;

	private TypeMappedAnnotations(AnnotatedElement element, SearchStrategy searchStrategy,
			RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		this.source = element;
		this.element = element;
		this.searchStrategy = searchStrategy;
		this.annotations = null;
		this.repeatableContainers = repeatableContainers;
		this.annotationFilter = annotationFilter;
	}

	private TypeMappedAnnotations(@Nullable Object source, Annotation[] annotations,
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
	public <A extends Annotation> boolean isPresent(@Nullable Class<A> annotationType) {
		if (annotationType == null || this.annotationFilter.matches(annotationType)) {
			return false;
		}
		return scan(annotationType, this::isPresent);
	}

	@Override
	public <A extends Annotation> boolean isPresent(@Nullable String annotationType) {
		if (annotationType == null || this.annotationFilter.matches(annotationType)) {
			return false;
		}
		return scan(annotationType, this::isPresent);
	}

	private boolean isPresent(Object requiredType, int aggregateIndex, Object source,
			Annotation[] annotations) {
		for (Annotation annotation : annotations) {
			if (isPresent(annotation, requiredType)) {
				return true;
			}
		}
		return false;
	}

	private boolean isPresent(Annotation annotation, Object requiredType) {
		Class<? extends Annotation> actualType = annotation.annotationType();
		if (this.annotationFilter.matches(actualType)) {
			return false;
		}
		if (actualType == requiredType || actualType.getName().equals(requiredType)) {
			return true;
		}
		Annotation[] repeatedAnnotations = this.repeatableContainers.findRepeatedAnnotations(
				annotation);
		if (repeatedAnnotations != null) {
			return repeatedAnnotations.length > 0
					? isPresent(repeatedAnnotations[0], requiredType)
					: false;
		}
		return isPresent(AnnotationTypeMappings.forAnnotationType(actualType),
				requiredType);
	}

	private boolean isPresent(AnnotationTypeMappings mappings, Object requiredType) {
		for (int i = 0; i < mappings.size(); i++) {
			AnnotationTypeMapping mapping = mappings.get(i);
			isMappingForType(mapping, this.annotationFilter, requiredType);
		}
		return false;
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
		return scan(annotationType,
				new MergedAnnotationFinder<>(annotationType, predicate, selector));
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
		return scan(annotationType,
				new MergedAnnotationFinder<>(annotationType, predicate, selector));
	}

	@Override
	public <A extends Annotation> Stream<MergedAnnotation<A>> stream(
			Class<A> annotationType) {
		return StreamSupport.stream(spliterator(annotationType), false);
	}

	@Override
	public <A extends Annotation> Stream<MergedAnnotation<A>> stream(
			String annotationType) {
		return StreamSupport.stream(spliterator(annotationType), false);
	}

	@Override
	public Stream<MergedAnnotation<Annotation>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	@Override
	public Iterator<MergedAnnotation<Annotation>> iterator() {
		return Spliterators.iterator(spliterator());
	}

	@Override
	public Spliterator<MergedAnnotation<Annotation>> spliterator() {
		return spliterator(null);
	}

	private <A extends Annotation> Spliterator<MergedAnnotation<A>> spliterator(
			@Nullable Object annotationType) {
		return new AggregatesSpliterator<>(annotationType, getAggregates());
	}

	private List<Aggregate> getAggregates() {
		List<Aggregate> aggregates = this.aggregates;
		if (aggregates == null) {
			aggregates = scan(null, new AggregatesCollector());
			this.aggregates = aggregates;
		}
		return aggregates;
	}

	private <C, R> R scan(C criteria, AnnotationProcessor<C, R> processor) {
		// FIXME if we already have aggregates and the processor could support
		// it we could use those instead
		if (this.annotations != null) {
			R result = processor.process(criteria, 0, this.source, this.annotations);
			return processor.getFinalResult(result);
		}
		return AnnotationsScanner.scan(criteria, this.element, this.searchStrategy,
				processor);
	}

	public static MergedAnnotations from(AnnotatedElement element,
			SearchStrategy searchStrategy, RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		return new TypeMappedAnnotations(element, searchStrategy, repeatableContainers,
				annotationFilter);
	}

	public static MergedAnnotations from(@Nullable Object source,
			Annotation[] annotations, RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		return new TypeMappedAnnotations(source, annotations, repeatableContainers,
				annotationFilter);
	}

	private static boolean isMappingForType(AnnotationTypeMapping mapping,
			AnnotationFilter annotationFilter, Object requiredType) {
		Class<? extends Annotation> actualType = mapping.getAnnotationType();
		return !annotationFilter.matches(actualType) && (actualType == requiredType
				|| actualType.getName().equals(requiredType));
	}

	/**
	 * {@link AnnotationProcessor} that finds a single {@link MergedAnnotation}.
	 */
	private class MergedAnnotationFinder<A extends Annotation>
			implements AnnotationProcessor<Object, MergedAnnotation<A>> {

		private final Object requiredType;

		@Nullable
		private Predicate<? super MergedAnnotation<A>> predicate;

		private MergedAnnotationSelector<A> selector;

		@Nullable
		private MergedAnnotation<A> result;

		MergedAnnotationFinder(Object requiredType,
				@Nullable Predicate<? super MergedAnnotation<A>> predicate,
				@Nullable MergedAnnotationSelector<A> selector) {
			this.requiredType = requiredType;
			this.predicate = predicate;
			this.selector = selector != null ? selector
					: MergedAnnotationSelectors.nearest();
		}

		@Override
		public MergedAnnotation<A> process(Object type, int aggregateIndex,
				@Nullable Object source, Annotation[] annotations) {
			for (Annotation annotation : annotations) {
				if (annotation == null || annotationFilter.matches(annotation)) {
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
				@Nullable Object source, Annotation annotation) {
			Annotation[] repeatedAnnotations = repeatableContainers.findRepeatedAnnotations(
					annotation);
			if (repeatedAnnotations != null) {
				return process(type, aggregateIndex, source, repeatedAnnotations);
			}
			AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
					annotation.annotationType(), annotationFilter);
			for (int i = 0; i < mappings.size(); i++) {
				AnnotationTypeMapping mapping = mappings.get(i);
				if (isMappingForType(mapping, annotationFilter, requiredType)) {
					MergedAnnotation<A> candidate = new TypeMappedAnnotation<>(source,
							annotation, mapping, aggregateIndex);
					if (this.predicate == null || this.predicate.test(candidate)) {
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
					: candidate;
		}

		@Override
		public MergedAnnotation<A> getFinalResult(MergedAnnotation<A> result) {
			result = result != null ? result : this.result;
			return result != null ? result : MergedAnnotation.missing();
		}

	}

	/**
	 * {@link AnnotationProcessor} that collects {@link Aggregate} instances.
	 */
	private class AggregatesCollector
			implements AnnotationProcessor<Object, List<Aggregate>> {

		private final List<Aggregate> aggregates = new ArrayList<>();

		@Override
		public List<Aggregate> process(Object criteria, int aggregateIndex,
				@Nullable Object source, Annotation[] annotations) {
			this.aggregates.add(createAggregate(aggregateIndex, source, annotations));
			return null;
		}

		private Aggregate createAggregate(int aggregateIndex, @Nullable Object source,
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
					if (repeatedAnnotations != null) {
						addAggregateAnnotations(aggregateAnnotations,
								repeatedAnnotations);
					}
					else {
						aggregateAnnotations.add(annotation);
					}
				}
			}
		}

		@Override
		public List<Aggregate> getFinalResult(List<Aggregate> processResult) {
			return this.aggregates;
		}

	}

	private static class Aggregate {

		private final int aggregateIndex;

		private final Object source;

		private final List<Annotation> annotations;

		private final AnnotationTypeMappings[] mappings;

		public Aggregate(int aggregateIndex, Object source,
				List<Annotation> annotations) {
			this.aggregateIndex = aggregateIndex;
			this.source = source;
			this.annotations = annotations;
			this.mappings = new AnnotationTypeMappings[annotations.size()];
			for (int i = 0; i < annotations.size(); i++) {
				this.mappings[i] = AnnotationTypeMappings.forAnnotationType(
						annotations.get(i).annotationType());
			}
		}

		public int size() {
			return this.annotations.size();
		}

		public AnnotationTypeMapping getMapping(int annotationIndex, int mappingIndex) {
			AnnotationTypeMappings mappings = this.mappings[annotationIndex];
			return mappingIndex < mappings.size() ? mappings.get(mappingIndex) : null;
		}

		public <A extends Annotation> MergedAnnotation<A> getMergedAnnotation(
				int annotationIndex, int mappingIndex) {
			return new TypeMappedAnnotation<>(this.source,
					this.annotations.get(annotationIndex),
					this.mappings[annotationIndex].get(mappingIndex),
					this.aggregateIndex);
		}

	}

	/**
	 * {@link Spliterator} used to consume merged annotations from the
	 * aggregates in depth fist order.
	 */
	private class AggregatesSpliterator<A extends Annotation>
			implements Spliterator<MergedAnnotation<A>> {

		private Object requiredType;

		private final List<Aggregate> aggregates;

		private final int[][] currentMappingIndex;

		public AggregatesSpliterator(@Nullable Object requiredType,
				List<Aggregate> aggregates) {
			this.requiredType = requiredType;
			this.aggregates = aggregates;
			this.currentMappingIndex = new int[aggregates.size()][];
			for (int i = 0; i < aggregates.size(); i++) {
				this.currentMappingIndex[i] = new int[aggregates.get(i).size()];
			}
		}

		public boolean tryAdvance(Consumer<? super MergedAnnotation<A>> action) {
			int aggregateResult = -1;
			int annotationResult = -1;
			int lowestDepth = Integer.MAX_VALUE;
			for (int aggregateIndex = 0; aggregateIndex < this.aggregates.size(); aggregateIndex++) {
				Aggregate aggregate = this.aggregates.get(aggregateIndex);
				for (int annotationIndex = 0; annotationIndex < aggregate.size(); annotationIndex++) {
					AnnotationTypeMapping mapping = getNextMapping(aggregate,
							aggregateIndex, annotationIndex);
					if (mapping != null && mapping.getDepth() < lowestDepth) {
						aggregateResult = aggregateIndex;
						annotationResult = annotationIndex;
						lowestDepth = mapping.getDepth();
					}
					if (lowestDepth == 0) {
						break;
					}
				}
			}
			if (aggregateResult == -1) {
				return false;
			}
			Aggregate aggregate = this.aggregates.get(aggregateResult);
			int mappingIndex = this.currentMappingIndex[aggregateResult][annotationResult];
			this.currentMappingIndex[aggregateResult][annotationResult]++;
			action.accept(aggregate.getMergedAnnotation(annotationResult, mappingIndex));
			return true;
		}

		private AnnotationTypeMapping getNextMapping(Aggregate aggregate,
				int aggregateIndex, int annotationIndex) {
			AnnotationTypeMapping mapping = aggregate.getMapping(annotationIndex,
					this.currentMappingIndex[aggregateIndex][annotationIndex]);
			while (mapping != null
					&& !isMappingForType(mapping, annotationFilter, this.requiredType)) {
				this.currentMappingIndex[aggregateIndex][annotationIndex]++;
				mapping = aggregate.getMapping(annotationIndex,
						this.currentMappingIndex[aggregateIndex][annotationIndex]);
			}
			return mapping;
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

}
