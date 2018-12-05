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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.util.Assert;

/**
 * {@link MergedAnnotations} implementation that uses
 * {@link AnnotationTypeMappings} to adapt annotations.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class TypeMappedAnnotations2 extends AbstractMergedAnnotations {

	private List<Mappable> mappables = new ArrayList<>(10);

	TypeMappedAnnotations2(AnnotatedElement source, Annotation[] annotations) {
		ClassLoader sourceClassLoader = getClassLoader(source);
		for (Annotation annotation : annotations) {
			ClassLoader classLoader = sourceClassLoader != null ? sourceClassLoader
					: annotation.getClass().getClassLoader();
			addMappable(classLoader, RepeatableContainers.none(),
					DeclaredAnnotation.from(annotation), false);
		}
	}


	TypeMappedAnnotations2(ClassLoader classLoader,
			Iterable<DeclaredAnnotations> annotations, RepeatableContainers repeatableContainers) {
		Assert.notNull(annotations, "Annotations must not be null");
		boolean inherited = true;
		for (DeclaredAnnotations declaredAnnotations : annotations) {
			for (DeclaredAnnotation declaredAnnotation : declaredAnnotations) {
				addMappable(classLoader, repeatableContainers, declaredAnnotation,
						inherited);
			}
			inherited = false;
		}
	}


	private void addMappable(ClassLoader classLoader,
			RepeatableContainers repeatableContainers, DeclaredAnnotation annotation,
			boolean inherited) {
		repeatableContainers.visit(classLoader, annotation, (type, attributes) -> {
			AnnotationTypeMappings mappings = AnnotationTypeMappings.get(classLoader,
					repeatableContainers, type);
			if (mappings != null) {
				this.mappables.add(new Mappable(mappings, attributes, inherited));
			}
		});
	}

	private ClassLoader getClassLoader(AnnotatedElement source) {
		if (source instanceof Member) {
			return getClassLoader(((Member) source).getDeclaringClass());
		}
		if (source instanceof Class) {
			return ((Class<?>) source).getClassLoader();
		}
		return null;
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
		MergedAnnotation<Annotation> result = get(annotationType,
				mappable -> !mappable.isInherited());

	}

	private <A extends Annotation> MergedAnnotation<A> get(String annotationType,
			Predicate<Mappable> predicate) {
		MergedAnnotation<A> result = null;
		for (Mappable mappable : this.mappables) {
			if (predicate.test(mappable)) {
				MergedAnnotation<A> candidate = mappable.get(annotationType);
				if (isBetterGetCandidate(candidate, result)) {
					result = candidate;
				}
			}
		}
		return result;
	}

	private boolean isBetterGetCandidate(MergedAnnotation<?> candidate,
			MergedAnnotation<?> previous) {
		return candidate != null
				&& (previous == null || candidate.getDepth() < previous.getDepth());
	}

	@Override
	public Stream<MergedAnnotation<?>> stream() {
		return this.mappables.stream().flatMap(Mappable::stream);
	}

	private static class Mappable {

		private final AnnotationTypeMappings mappings;

		private final DeclaredAttributes attributes;

		private final boolean inherited;

		public Mappable(AnnotationTypeMappings mappings, DeclaredAttributes attributes,
				boolean inherited) {
			this.inherited = inherited;
			this.attributes = attributes;
			this.mappings = mappings;
		}

		public boolean isInherited() {
			return inherited;
		}

		public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
			AnnotationTypeMapping mapping = this.mappings.getMapping(annotationType);
			return mapping != null ? map(mapping) : null;
		}

		public Stream<MergedAnnotation<?>> stream() {
			return this.mappings.getAllMappings().map(this::map);
		}

		private <A extends Annotation> MergedAnnotation<A> map(
				AnnotationTypeMapping mapping) {
			return new TypeMappedAnnotation<A>(mapping, this.inherited, this.attributes);
		}

	}

}
