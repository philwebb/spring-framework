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

	private final List<MappableAnnotations> hierarchy;

	private volatile List<MergedAnnotation<?>> all;

	TypeMappedAnnotations(RepeatableContainers repeatableContainers,
			AnnotatedElement source, Annotation[] annotations) {
		this.hierarchy = Collections.singletonList(new MappableAnnotations(source,
				annotations, repeatableContainers, false));
	}

	TypeMappedAnnotations(ClassLoader classLoader,
			RepeatableContainers repeatableContainers,
			Iterable<DeclaredAnnotations> annotations) {
		this.hierarchy = new ArrayList<>(getInitialSize(annotations));
		boolean inherited = false;
		for (DeclaredAnnotations declaredAnnotations : annotations) {
			this.hierarchy.add(new MappableAnnotations(classLoader, declaredAnnotations,
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
		for (MappableAnnotations annotations : this.hierarchy) {
			if (annotations.isPresent(annotationType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
		for (MappableAnnotations annotations : this.hierarchy) {
			MergedAnnotation<A> result = annotations.get(annotationType);
			if (result != null) {
				return result;
			}
		}
		return MergedAnnotation.missing();
	}

	@Override
	public Stream<MergedAnnotation<?>> stream() {
		return getAll().stream();
	}

	private List<MergedAnnotation<?>> getAll() {
		List<MergedAnnotation<?>> all = this.all;
		if (all == null) {
			all = computeAll();
			this.all = all;
		}
		return all;
	}

	private List<MergedAnnotation<?>> computeAll() {
		List<MergedAnnotation<?>> result = new ArrayList<>(totalSize());
		for (MappableAnnotations annotations : this.hierarchy) {
			List<Deque<MergedAnnotation<?>>> queues = new ArrayList<>(annotations.size());
			for (MappableAnnotation annotation : annotations) {
				queues.add(annotation.getQueue());
			}
			addAllInDepthOrder(result, queues);
		}
		return result;
	}

	private void addAllInDepthOrder(List<MergedAnnotation<?>> result,
			List<Deque<MergedAnnotation<?>>> queues) {
		int depth = 0;
		boolean hasMore = true;
		while (hasMore) {
			hasMore = false;
			for (Deque<MergedAnnotation<?>> queue : queues) {
				hasMore = hasMore | addAllForDepth(result, queue, depth);
			}
			depth++;
		}
	}

	private boolean addAllForDepth(List<MergedAnnotation<?>> result,
			Deque<MergedAnnotation<?>> queue, int depth) {
		while (!queue.isEmpty() && queue.peek().getDepth() <= depth) {
			result.add(queue.pop());
		}
		return !queue.isEmpty();
	}

	private int totalSize() {
		int size = 0;
		for (MappableAnnotations annotations : this.hierarchy) {
			size += annotations.totalSize();
		}
		return size;
	}

	/**
	 * A collection of {@link MappableAnnotation mappable annotations}.
	 */
	private static class MappableAnnotations implements Iterable<MappableAnnotation> {

		private List<MappableAnnotation> mappableAnnotations;

		public MappableAnnotations(AnnotatedElement source, Annotation[] annotations,
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

		public MappableAnnotations(ClassLoader classLoader,
				DeclaredAnnotations annotations,
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
				AnnotationTypeMappings mappings = AnnotationTypeMappings.forType(
						classLoader, repeatableContainers, type);
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
	 * A single mappable annotation.
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
			return this.mappings.get(annotationType) != null;
		}

		public <A extends Annotation> MergedAnnotation<A> get(String annotationType) {
			AnnotationTypeMapping mapping = this.mappings.get(annotationType);
			return mapping != null ? map(mapping) : null;
		}

		public Deque<MergedAnnotation<?>> getQueue() {
			Deque<MergedAnnotation<?>> queue = new ArrayDeque<>(size());
			for (AnnotationTypeMapping mapping : this.mappings.getAll()) {
				queue.add(map(mapping));
			}
			return queue;
		}

		private <A extends Annotation> MergedAnnotation<A> map(
				AnnotationTypeMapping mapping) {
			return new TypeMappedAnnotation<A>(mapping, this.inherited, this.attributes);
		}

		public int size() {
			return this.mappings.getAll().size();
		}

	}

}
