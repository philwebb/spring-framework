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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

/**
 *
 * @author Phillip Webb
 * @since 5.1
 */
final class StandardMergedAnnotations implements MergedAnnotations {

	private final Object source;

	private final RepeatableContainers repeatableContainers;

	private final AnnotationFilter annotationFilter;

	private final Annotation[] directlyPresent;

	@Nullable
	private final Supplier<Annotation[][]> aggregatesSupplier;

	private volatile Annotation[][] aggregates;

	public StandardMergedAnnotations(Object source,
			RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter,
			Annotation[] directlyPresent, Supplier<Annotation[][]> aggregatesSupplier) {
		this.source = source;
		this.repeatableContainers = repeatableContainers;
		this.annotationFilter = annotationFilter;
		this.directlyPresent = directlyPresent;
		this.aggregatesSupplier = aggregatesSupplier;
	}

	@Override
	public Iterator<MergedAnnotation<Annotation>> iterator() {
		return stream().iterator();
	}

	public <A extends Annotation> boolean isPresent(@Nullable Class<A> annotationType) {
		if (annotationType == null || this.annotationFilter.matches(annotationType)) {
			return false;
		}
		return isPresent(annotationType::equals);
	}

	@Override
	public <A extends Annotation> boolean isPresent(@Nullable String annotationType) {
		if (annotationType == null || this.annotationFilter.matches(annotationType)) {
			return false;
		}
		return isPresent(withClassName(annotationType));
	}

	private boolean isPresent(Predicate<Class<? extends Annotation>> typePredicate) {
		return isPresent(this.directlyPresent, this::getAggregates, typePredicate,
				this.repeatableContainers, this.annotationFilter);
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
		if (annotationType == null) {
			return MergedAnnotation.missing();
		}
		return get(annotationType::equals, predicate, selector);
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
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType,
			Predicate<? super MergedAnnotation<A>> predicate,
			MergedAnnotationSelector<A> selector) {
		if (annotationType == null) {
			return MergedAnnotation.missing();
		}
		return get(withClassName(annotationType), predicate, selector);
	}

	private <A extends Annotation> MergedAnnotation<A> get(
			Predicate<Class<? extends Annotation>> typePredicate,
			Predicate<? super MergedAnnotation<A>> predicate,
			MergedAnnotationSelector<A> selector) {
		// FIXME
		return null;
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

	private Annotation[][] getAggregates() {
		Annotation[][] aggregates = this.aggregates;
		if (aggregates == null) {
			aggregates = this.aggregatesSupplier.get();
			this.aggregates = aggregates;
		}
		return aggregates;
	}

	static boolean isPresent(AnnotatedElement element,
			Class<? extends Annotation> annotationType, SearchStrategy searchStrategy,
			RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		if (annotationType == null || annotationFilter.matches(annotationType)) {
			return false;
		}
		return isPresent(element, annotationType::equals, searchStrategy,
				repeatableContainers, annotationFilter);
	}

	static boolean isPresent(AnnotatedElement element, String annotationType,
			SearchStrategy searchStrategy, RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		if (annotationType == null || annotationFilter.matches(annotationType)) {
			return false;
		}
		return isPresent(element, withClassName(annotationType), searchStrategy,
				repeatableContainers, annotationFilter);
	}

	private static boolean isPresent(AnnotatedElement element,
			Predicate<Class<? extends Annotation>> predicate,
			SearchStrategy searchStrategy, RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		return isPresent(AnnotationScanner.getDirectlyPresent(element, searchStrategy),
				AnnotationScanner.getAggregatesSupplier(element, searchStrategy),
				predicate, repeatableContainers, annotationFilter);
	}

	private static boolean isPresent(Annotation[] directlyPresent,
			Supplier<Annotation[][]> aggregatesSupplier,
			Predicate<Class<? extends Annotation>> typePredicate,
			RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		if (isPresent(directlyPresent, typePredicate, repeatableContainers,
				annotationFilter)) {
			return true;
		}
		Annotation[][] aggregates = aggregatesSupplier.get();
		for (Annotation[] aggregateAnnotations : aggregates) {
			if (isPresent(aggregateAnnotations, typePredicate, repeatableContainers,
					annotationFilter)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isPresent(Annotation[] candidates,
			Predicate<Class<? extends Annotation>> typePredicate,
			RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		for (Annotation candidate : candidates) {
			if (isPresent(candidate, typePredicate, repeatableContainers,
					annotationFilter)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isPresent(Annotation candidate,
			Predicate<Class<? extends Annotation>> typePredicate,
			RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		Class<? extends Annotation> annotationType = candidate.annotationType();
		if (annotationFilter.matches(annotationType)) {
			return false;
		}
		Annotation[] repeatedAnnotations = repeatableContainers.findRepeatedAnnotations(
				candidate);
		if (repeatedAnnotations != null) {
			for (Annotation repeatedAnnotation : repeatedAnnotations) {
				if (isPresent(repeatedAnnotation, typePredicate, repeatableContainers,
						annotationFilter)) {
					return true;
				}
			}
		}
		return AnnotationTypeMappings.get(annotationType).isPresent(typePredicate,
				annotationFilter);
	}

	static MergedAnnotations from(Object source, Annotation[] annotations,
			RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		return new StandardMergedAnnotations(source, repeatableContainers,
				annotationFilter, annotations, null);
	}

	static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy,
			RepeatableContainers repeatableContainers,
			AnnotationFilter annotationFilter) {
		Annotation[] directlyPresent = AnnotationScanner.getDirectlyPresent(element,
				searchStrategy);
		return new StandardMergedAnnotations(element, repeatableContainers,
				annotationFilter, directlyPresent,
				AnnotationScanner.getAggregatesSupplier(element, searchStrategy));
	}

	private static Predicate<Class<? extends Annotation>> withClassName(
			String annotationType) {
		return candidate -> candidate.getName().equals(annotationType);
	}

}
