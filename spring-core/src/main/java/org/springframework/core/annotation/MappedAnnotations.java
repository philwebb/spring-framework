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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.core.annotation.type.AnnotationTypeResolver;
import org.springframework.core.annotation.type.DeclaredAnnotations;

/**
 * {@link MergedAnnotations} implementation that uses
 * {@link AnnotationTypeMappings} to adapt sources {@link DeclaredAnnotations
 * annotations}.
 *
 * @author Phillip Webb
 * @since 5.1
 */
class MappedAnnotations extends AbstractMergedAnnotations {

	private final Supplier<Stream<Source>> sources;

	MappedAnnotations(AnnotationTypeResolver resolver,
			Iterable<DeclaredAnnotations> annotations) {
		this.sources = () -> {
			DeclaredAnnotationsMapper mapper = new DeclaredAnnotationsMapper(resolver);
			return StreamSupport.stream(annotations.spliterator(), false).flatMap(mapper);
		};
	}

	MappedAnnotations(Iterable<MappableAnnotation> annotations) {
		this.sources = () -> StreamSupport.stream(annotations.spliterator(), false).map(
				Source::new);
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
		Function<Source, MergedAnnotation<A>> mapper = source -> getMergedAnnotation(
				source, annotationType);
		Stream<MergedAnnotation<A>> candidates = this.sources.get().map(mapper).filter(
				MergedAnnotation::isPresent);
		return candidates.min(MergedAnnotation.comparingDepth()).orElse(
				MergedAnnotation.missing());
	}

	private <A extends Annotation> MergedAnnotation<A> getMergedAnnotation(Source source,
			String annotationType) {
		AnnotationTypeMappings mappings = getTypeMappings(source);
		AnnotationTypeMapping mapping = mappings.getMapping(annotationType);
		return map(mapping, source);
	}

	@Override
	public Stream<MergedAnnotation<?>> stream() {
		return this.sources.get().flatMap(this::mapAll);
	}

	private Stream<MergedAnnotation<?>> mapAll(Source source) {
		return getTypeMappings(source).getAllMappings().map(
				mapping -> map(mapping, source));
	}

	private <A extends Annotation> MergedAnnotation<A> map(AnnotationTypeMapping mapping,
			Source source) {
		if (mapping == null) {
			return MergedAnnotation.missing();
		}
		return mapping.map(source.getAnnotation(), source.isInherited());
	}

	private AnnotationTypeMappings getTypeMappings(Source source) {
		MappableAnnotation sourceAnnotation = source.getAnnotation();
		return AnnotationTypeMappings.get(sourceAnnotation.getResolver(),
				sourceAnnotation.getAnnotationType());
	}

	/**
	 * Flat maps {@link DeclaredAnnotations} to a {@link MappableAnnotation}
	 * stream, setting the {@code inherited} flag for the first set.
	 */
	class DeclaredAnnotationsMapper
			implements Function<DeclaredAnnotations, Stream<Source>> {

		private final AnnotationTypeResolver resolver;

		private boolean inherited;

		public DeclaredAnnotationsMapper(AnnotationTypeResolver resolver) {
			this.resolver = resolver;
		}

		@Override
		public Stream<Source> apply(DeclaredAnnotations annotations) {
			boolean inherited = this.inherited;
			this.inherited = false;
			return MappableAnnotation.from(this.resolver, annotations).map(
					annotation -> new Source(annotation, inherited));
		}

	}

	/**
	 * Source to be mapped. Includes the {@link MappableAnnotation} and if it
	 * was from an inherited source or not.
	 */
	private static class Source {

		private final MappableAnnotation annotation;

		private final boolean inherited;

		Source(MappableAnnotation annotation) {
			this(annotation, false);
		}

		Source(MappableAnnotation annotation, boolean inherited) {
			this.annotation = annotation;
			this.inherited = inherited;
		}

		public MappableAnnotation getAnnotation() {
			return this.annotation;
		}

		public boolean isInherited() {
			return this.inherited;
		}

	}

}
