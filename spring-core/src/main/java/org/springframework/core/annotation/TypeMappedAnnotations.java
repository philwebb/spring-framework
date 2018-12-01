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
class TypeMappedAnnotations extends AbstractMergedAnnotations {

	private final Hierarchy hierarchy;

	TypeMappedAnnotations(Iterable<DeclaredAnnotations> annotations,
			AnnotationTypeResolver resolver, RepeatableContainers repeatableContainers) {
		DeclaredAnnotationsMapper mapper = new DeclaredAnnotationsMapper(resolver,
				repeatableContainers);
		this.hierarchy = () -> StreamSupport.stream(annotations.spliterator(), false).map(
				mapper::map);
	}

	TypeMappedAnnotations(Iterable<MappableAnnotation> annotations) {
		this.hierarchy = () -> Stream.of(Element.of(annotations));
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
		ElementGetter<A> getter = new ElementGetter<A>(annotationType);
		return this.hierarchy.getElements().map(getter::get).filter(
				MergedAnnotation::isPresent).findFirst().orElse(
						MergedAnnotation.missing());
	}

	@Override
	public Stream<MergedAnnotation<?>> stream() {
		ElementMapper mapper = new ElementMapper();
		return this.hierarchy.getElements().flatMap(mapper::map);
	}

	/**
	 * A hierarchy of elements where the first is immediate and subsequent are
	 * inherited.
	 */
	@FunctionalInterface
	private static interface Hierarchy {

		Stream<Element> getElements();

	}

	/**
	 * An element that contains {@link MappableAnnotation mappable annotations}.
	 */
	private static interface Element {

		Stream<MappableAnnotation> getAnnotations();

		static Element of(Iterable<MappableAnnotation> annotations) {
			return () -> StreamSupport.stream(annotations.spliterator(), false);
		}

	}

	/**
	 * Maps a {@link DeclaredAnnotations} to an {@link Element}.
	 */
	private class DeclaredAnnotationsMapper {

		private final AnnotationTypeResolver resolver;

		private final RepeatableContainers repeatableContainers;

		DeclaredAnnotationsMapper(AnnotationTypeResolver resolver,
				RepeatableContainers repeatableContainers) {
			super();
			this.resolver = resolver;
			this.repeatableContainers = repeatableContainers;
		}

		public Element map(DeclaredAnnotations annotations) {
			return () -> MappableAnnotation.from(this.resolver, this.repeatableContainers,
					annotations);
		}

	}

	/**
	 * Maps an {@link Element} to a stream of {@link MergedAnnotation}.
	 */
	private static class ElementMapper {

		private boolean inherited = false;

		public Stream<MergedAnnotation<?>> map(Element element) {
			boolean inherited = this.inherited;
			this.inherited = true;
			return element.getAnnotations().flatMap(
					annotation -> map(annotation, inherited));
		}

		private Stream<MergedAnnotation<?>> map(MappableAnnotation annotation,
				boolean inherited) {
			return AnnotationTypeMappings.get(annotation).getAllMappings().map(
					mapping -> mapping.map(annotation, inherited));
		}

	}

	/**
	 * Maps an Element to a single {@link MergedAnnotation} of a given type.
	 */
	private class ElementGetter<A extends Annotation> {

		private final String type;

		private boolean inherited = false;

		ElementGetter(String type) {
			this.type = type;
		}

		public MergedAnnotation<A> get(Element element) {
			boolean inherited = this.inherited;
			this.inherited = true;
			Stream<MergedAnnotation<A>> candidates = element.getAnnotations().map(
					annotation -> get(annotation, inherited));
			return candidates.filter(MergedAnnotation::isPresent).min(
					MergedAnnotation.comparingDepth()).orElse(MergedAnnotation.missing());
		}

		private MergedAnnotation<A> get(MappableAnnotation annotation, boolean inherted) {
			AnnotationTypeMappings mappings = AnnotationTypeMappings.get(annotation);
			AnnotationTypeMapping mapping = mappings.getMapping(this.type);
			if (mapping == null) {
				return MergedAnnotation.missing();
			}
			return mapping.map(annotation, inherted);
		}

	}

}
