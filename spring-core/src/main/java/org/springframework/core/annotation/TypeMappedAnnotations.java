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
import java.util.stream.Stream;

import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.util.Assert;

/**
 * {@link MergedAnnotations} implementation that uses
 * {@link AnnotationTypeMappings} to adapt annotations.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class TypeMappedAnnotations extends AbstractMergedAnnotations {

	private List<Mappable> mappables = new ArrayList<>(10);

	TypeMappedAnnotations(Iterable<DeclaredAnnotations> annotations,
			ClassLoader classLoader, RepeatableContainers repeatableContainers) {
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

	TypeMappedAnnotations(AnnotatedElement source, Annotation[] annotations) {
		Assert.notNull(annotations, "Annotations must not be null");
		ClassLoader sourceClassLoader = getClassLoader(source);
		for (Annotation annotation : annotations) {
			ClassLoader classLoader = sourceClassLoader != null ? sourceClassLoader
					: annotation.getClass().getClassLoader();
			addMappable(classLoader, RepeatableContainers.none(),
					DeclaredAnnotation.from(annotation), false);
		}
	}

	private void addMappable(ClassLoader classLoader,
			RepeatableContainers repeatableContainers, DeclaredAnnotation annotation,
			boolean inherited) {
		repeatableContainers.visit(classLoader, annotation, (type, attributes) -> {
			AnnotationTypeMappings mappings = AnnotationTypeMappings.get(classLoader,
					repeatableContainers, type);
			if (mappings != null) {
				this.mappables.add(new Mappable(annotation, inherited, mappings));
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
		for (Mappable mappable : this.mappables) {
			MergedAnnotation<A> annotation = mappable.get(annotationType);
			if (annotation != null) {
				return annotation;
			}
		}
		return MergedAnnotation.missing();
	}

	@Override
	public Stream<MergedAnnotation<?>> stream() {
		return this.mappables.stream().flatMap(Mappable::stream);
	}

	private static class Mappable {

		private final DeclaredAnnotation annotation;

		private final boolean inherited;

		private final AnnotationTypeMappings mappings;

		public Mappable(DeclaredAnnotation annotation, boolean inherited,
				AnnotationTypeMappings mappings) {
			this.annotation = annotation;
			this.inherited = inherited;
			this.mappings = mappings;
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
			return new TypeMappedAnnotation(mapping, annotation.getAttributes(), this.inherited);
		}

	}

}
