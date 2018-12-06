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
import java.util.Collections;
import java.util.List;
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

	private final List<Element> hierarchy;

	TypeMappedAnnotations(RepeatableContainers repeatableContainers,
			AnnotatedElement source, Annotation[] annotations) {
		this.hierarchy = Collections.singletonList(
				new Element(source, annotations, repeatableContainers, false));
	}

	TypeMappedAnnotations(ClassLoader classLoader,
			RepeatableContainers repeatableContainers,
			Iterable<DeclaredAnnotations> annotations) {
		this.hierarchy = new ArrayList<>(getInitialSize(annotations));
		boolean inherited = false;
		for (DeclaredAnnotations declaredAnnotations : annotations) {
			this.hierarchy.add(new Element(classLoader, declaredAnnotations,
					repeatableContainers, inherited));
			inherited = true;
		}
	}

	private int getInitialSize(Iterable<DeclaredAnnotations> annotations) {
		if (annotations instanceof AnnotationsScanner) {
			return ((AnnotationsScanner) annotations).size();
		}
		return 10;
	}

	@Override
	public <A extends Annotation> boolean isPresent(String annotationType) {
		for (Element element : this.hierarchy) {
			if (element.isPresent(annotationType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
		// FIXME check what AnnotationUtils does. Should smallest meta-depth win or should nearest class win
		for (Element element : this.hierarchy) {
			MergedAnnotation<A> result = element.get(annotationType);
			if (result != null) {
				return result;
			}
		}
		return MergedAnnotation.missing();
	}

	@Override
	public Stream<MergedAnnotation<?>> stream() {
		// FIXME Is the ordering correct here? Should we sort by depth?
		// Should we create a list to save a ton of stack on flapmap
		return this.hierarchy.stream().flatMap(Element::stream);
	}

	/**
	 * A single element in an inheritance hierarchy.
	 */
	private static class Element {

		private List<MappableAnnotation> mappableAnnotations;

		public Element(AnnotatedElement source, Annotation[] annotations,
				RepeatableContainers repeatableContainers, boolean inherited) {
			this.mappableAnnotations = new ArrayList<>(annotations.length);
			ClassLoader sourceClassLoader = getClassLoader(source);
			for (Annotation annotation : annotations) {
				ClassLoader classLoader = sourceClassLoader != null ? sourceClassLoader
						: annotation.getClass().getClassLoader();
				add(classLoader, DeclaredAnnotation.from(annotation),
						repeatableContainers, inherited);
			}
		}

		public Element(ClassLoader classLoader, DeclaredAnnotations annotations,
				RepeatableContainers repeatableContainers, boolean inherited) {
			this.mappableAnnotations = new ArrayList<>(annotations.size());
			if (classLoader == null) {
				classLoader = getClassLoader(annotations.getSource());
			}
			for (DeclaredAnnotation annotation : annotations) {
				add(classLoader, annotation, repeatableContainers, inherited);
			}
		}

		private void add(ClassLoader classLoader, DeclaredAnnotation annotation,
				RepeatableContainers repeatableContainers, boolean inherited) {
			repeatableContainers.visit(classLoader, annotation, (type, attributes) -> {
				AnnotationTypeMappings mappings = AnnotationTypeMappings.get(classLoader,
						repeatableContainers, type);
				if (mappings != null) {
					this.mappableAnnotations.add(
							new MappableAnnotation(mappings, attributes, inherited));
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

		public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
			MergedAnnotation<A> result = null;
			for (MappableAnnotation mappableAnnotation : this.mappableAnnotations) {
				MergedAnnotation<A> candidate = mappableAnnotation.get(annotationType);
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

		public Stream<MergedAnnotation<?>> stream() {
			return this.mappableAnnotations.stream().flatMap(MappableAnnotation::stream);
		}

	}

	/**
	 * A source annotation that is capable of being mapped.
	 */
	private static class MappableAnnotation {

		private final AnnotationTypeMappings mappings;

		private final DeclaredAttributes attributes;

		private final boolean inherited;

		public MappableAnnotation(AnnotationTypeMappings mappings,
				DeclaredAttributes attributes, boolean inherited) {
			this.mappings = mappings;
			this.attributes = attributes;
			this.inherited = inherited;
		}

		public boolean isPresent(String annotationType) {
			return this.mappings.getMapping(annotationType) != null;
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
