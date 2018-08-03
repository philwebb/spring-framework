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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.type.DeclaredAnnotation;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Scanner search for {@link DeclaredAnnotations} on the hierarchy of an
 * {@link AnnotatedElement}.
 *
 * @author Phillip Webb
 * @since 5.1
 */
class StandardDeclaredAnnotationsScanner implements Iterable<DeclaredAnnotations> {

	private static Map<AnnotatedElement, Results<?>> cache = new ConcurrentReferenceHashMap<>(
			256);

	private final Results<?> results;

	private final SearchStrategy searchStrategy;

	StandardDeclaredAnnotationsScanner(AnnotatedElement source,
			SearchStrategy searchStrategy) {
		this.results = cache.computeIfAbsent(source, Results::create);
		this.searchStrategy = searchStrategy;
	}

	@Override
	public Iterator<DeclaredAnnotations> iterator() {
		return this.results.get(this.searchStrategy);
	}

	/**
	 * Cacheable results for a single element.
	 * @param <E> the annotated element type
	 * @see ClassResults
	 * @see MethodResults
	 */
	private static class Results<E extends AnnotatedElement> {

		private final E element;

		private Map<SearchStrategy, Iterable<DeclaredAnnotations>> results = new ConcurrentHashMap<>(
				SearchStrategy.values().length);

		Results(E element) {
			this.element = element;
		}

		protected final E getElement() {
			return this.element;
		}

		public Iterator<DeclaredAnnotations> get(SearchStrategy searchStrategy) {
			return this.results.computeIfAbsent(searchStrategy, this::scan).iterator();
		}

		private Iterable<DeclaredAnnotations> scan(SearchStrategy searchStrategy) {
			List<DeclaredAnnotations> result = new ArrayList<>();
			for (E element : getElements(searchStrategy)) {
				result.add(adapt(getDeclaredAnnotations(element)));
			}
			return Collections.unmodifiableList(result);
		}

		protected Set<Annotation> getDeclaredAnnotations(E source) {
			return new LinkedHashSet<>(Arrays.asList(source.getDeclaredAnnotations()));
		}

		protected Iterable<E> getElements(SearchStrategy searchStrategy) {
			return Collections.singleton(this.element);
		}

		private DeclaredAnnotations adapt(Collection<Annotation> source) {
			List<DeclaredAnnotation> annotations = new ArrayList<>(source.size());
			source.stream().map(DeclaredAnnotation::from).forEach(annotations::add);
			return DeclaredAnnotations.of(annotations);
		}

		protected final Collection<Class<?>> getFullHierarchy(Class<?> type,
				boolean onlyInherited) {
			Set<Class<?>> hierarchy = new LinkedHashSet<>();
			collectFullHierarchy(hierarchy, type, !onlyInherited);
			return hierarchy;
		}

		private void collectFullHierarchy(Set<Class<?>> hierarchy, Class<?> type,
				boolean addType) {
			if (type == null || Object.class.equals(type)) {
				return;
			}
			if (addType) {
				hierarchy.add(type);
			}
			collectFullHierarchy(hierarchy, type.getSuperclass(), true);
			for (Class<?> interfaceType : type.getInterfaces()) {
				collectFullHierarchy(hierarchy, interfaceType, true);
			}
		}

		static Results<?> create(AnnotatedElement element) {
			if (element instanceof Class<?>) {
				return new ClassResults((Class<?>) element);
			}
			if (element instanceof Method) {
				return new MethodResults((Method) element);
			}
			return new Results<>(element);
		}

	}

	/**
	 * Cacheable results for a single class element.
	 */
	private static class ClassResults extends Results<Class<?>> {

		ClassResults(Class<?> element) {
			super(element);
		}

		@Override
		protected Iterable<Class<?>> getElements(SearchStrategy searchStrategy) {
			switch (searchStrategy) {
				case DIRECT:
					return Collections.singleton(getElement());
				case EXHAUSTIVE:
					return getFullHierarchy(getElement(), false);
			}
			throw new IllegalStateException(
					"Unsupported search strategy " + searchStrategy);
		}

	}

	/**
	 * Cacheable results for a single method element.
	 */
	private static class MethodResults extends Results<Method> {

		MethodResults(Method element) {
			super(element);
		}

		@Override
		protected Set<Annotation> getDeclaredAnnotations(Method source) {
			Set<Annotation> annotations = super.getDeclaredAnnotations(source);
			Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(source);
			annotations.addAll(super.getDeclaredAnnotations(bridgedMethod));
			return annotations;
		}

		@Override
		protected Iterable<Method> getElements(SearchStrategy searchStrategy) {
			switch (searchStrategy) {
				case DIRECT:
					return Collections.singleton(getElement());
				case EXHAUSTIVE:
					Method method = getElement();
					Stream<Method> direct = Stream.of(method);
					Stream<Class<?>> hierarchy = getFullHierarchy(
							method.getDeclaringClass(), true).stream();
					Stream<Method> inherited = hierarchy.flatMap(
							this::getOverrideCandidates).filter(
									candidate -> isOverride(method, candidate));
					return Stream.concat(direct, inherited)::iterator;
			}
			throw new IllegalStateException(
					"Unsupported search strategy " + searchStrategy);
		}

		private Stream<Method> getOverrideCandidates(Class<?> type) {
			if (type.isInterface() && ClassUtils.isJavaLanguageInterface(type)) {
				return Stream.empty();
			}
			Method[] methods = (type.isInterface() ? type.getMethods()
					: type.getDeclaredMethods());
			return Arrays.stream(methods).filter(
					method -> isOverrideCandidate(type, method));
		}

		private boolean isOverrideCandidate(Class<?> type, Method method) {
			if (!type.isInterface() && Modifier.isPrivate(method.getModifiers())) {
				return false;
			}
			Annotation[] annotations = method.getAnnotations();
			for (Annotation annotation : annotations) {
				if (!isIgnorable(annotation.annotationType())) {
					return true;
				}
			}
			return false;
		}

		private boolean isIgnorable(Class<?> type) {
			return (type == Nullable.class || type == Deprecated.class);
		}

		private boolean isOverride(Method method, Method candidate) {
			if (!candidate.getName().equals(method.getName())) {
				return false;
			}
			if (candidate.getParameterCount() != method.getParameterCount()) {
				return false;
			}
			Class<?>[] types = method.getParameterTypes();
			if (Arrays.equals(candidate.getParameterTypes(), types)) {
				return true;
			}
			Class<?> implementationClass = method.getDeclaringClass();
			for (int i = 0; i < types.length; i++) {
				if (types[i] != ResolvableType.forMethodParameter(candidate, i,
						implementationClass).resolve()) {
					return false;
				}
			}
			return true;
		}

	}

	// FIXME handleIntrospectionFailure in general

}
