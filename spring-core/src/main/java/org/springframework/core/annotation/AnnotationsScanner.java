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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.type.DeclaredAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Scanner search for {@link DeclaredAnnotations} on the hierarchy of an
 * {@link AnnotatedElement}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class AnnotationsScanner implements Iterable<DeclaredAnnotations> {

	private static Map<AnnotatedElement, Results<?>> cache = new ConcurrentReferenceHashMap<>(
			256);

	@Nullable
	private static transient Log logger;

	private final Results<?> results;

	private final SearchStrategy searchStrategy;

	AnnotationsScanner(AnnotatedElement source, SearchStrategy searchStrategy) {
		this.results = cache.computeIfAbsent(source, Results::create);
		this.searchStrategy = searchStrategy;
	}

	@Override
	public Iterator<DeclaredAnnotations> iterator() {
		return this.results.get(this.searchStrategy).iterator();
	}

	public int size() {
		return this.results.get(this.searchStrategy).size();
	}

	/**
	 * Cacheable results for a single {@link AnnotatedElement} source.
	 * @param <E> the annotated element type
	 * @see ClassResults
	 * @see MethodResults
	 * @see ElementResults
	 */
	private static abstract class Results<E extends AnnotatedElement> {

		private final E source;

		private final Map<SearchStrategy, Collection<DeclaredAnnotations>> results = new ConcurrentHashMap<>();

		Results(E source) {
			this.source = source;
		}

		public final Collection<DeclaredAnnotations> get(SearchStrategy searchStrategy) {
			Collection<DeclaredAnnotations> result = this.results.get(searchStrategy);
			if (result == null) {
				result = compute(searchStrategy);
				this.results.put(searchStrategy, result);
			}
			return result;
		}

		protected abstract Collection<DeclaredAnnotations> compute(
				SearchStrategy searchStrategy);

		@SafeVarargs
		protected final <T> Set<T> asSet(T... elements) {
			return new LinkedHashSet<>(Arrays.asList(elements));
		}

		protected final E getSource() {
			return this.source;
		}

		static Results<?> create(AnnotatedElement element) {
			if (element instanceof Class<?>) {
				return new ClassResults((Class<?>) element);
			}
			if (element instanceof Method) {
				return new MethodResults((Method) element);
			}
			return new ElementResults(element);
		}

	}

	/**
	 * Cacheable results for a single class element.
	 */
	private static class ClassResults extends Results<Class<?>> {

		ClassResults(Class<?> source) {
			super(source);
		}

		@Override
		protected Collection<DeclaredAnnotations> compute(SearchStrategy searchStrategy) {
			switch (searchStrategy) {
				case DIRECT:
					return computeDirect();
				case INHERITED_ANNOTATIONS:
					return computeInheritedAnnotations();
				case SUPER_CLASS:
					return computeWithHierarchy(TypeHierarchy::superclasses);
				case EXHAUSTIVE:
					return computeWithHierarchy(TypeHierarchy::superclassesAndInterfaces);
			}
			throw new IllegalStateException(
					"Unsupported search strategy " + searchStrategy);
		}

		private Collection<DeclaredAnnotations> computeDirect() {
			return Collections.singleton(getDeclaredAnnotations(getSource()));
		}

		private Collection<DeclaredAnnotations> computeInheritedAnnotations() {
			Set<Annotation> present = asSet(getSource().getAnnotations());
			return TypeHierarchy.superclasses(getSource()).stream().map(type -> {
				Set<Annotation> annotations = asSet(type.getDeclaredAnnotations());
				annotations.retainAll(present);
				return DeclaredAnnotations.from(type, annotations);
			}).collect(Collectors.toList());
		}

		private Collection<DeclaredAnnotations> computeWithHierarchy(
				Function<Class<?>, TypeHierarchy> hierarchyFactory) {
			return hierarchyFactory.apply(getSource()).stream().map(
					this::getDeclaredAnnotations).collect(Collectors.toList());
		}

		private DeclaredAnnotations getDeclaredAnnotations(Class<?> type) {
			return DeclaredAnnotations.from(type, type.getDeclaredAnnotations());
		}

	}

	/**
	 * Cacheable results for a single method element.
	 */
	private static class MethodResults extends Results<Method> {

		MethodResults(Method source) {
			super(source);
		}

		@Override
		protected Collection<DeclaredAnnotations> compute(SearchStrategy searchStrategy) {
			switch (searchStrategy) {
				case DIRECT:
					return computeDirect();
				case INHERITED_ANNOTATIONS:
					return get(SearchStrategy.DIRECT);
				case SUPER_CLASS:
					return computeWithHierarchy(TypeHierarchy::superclasses);
				case EXHAUSTIVE:
					return computeWithHierarchy(TypeHierarchy::superclassesAndInterfaces);
			}
			throw new IllegalStateException(
					"Unsupported search strategy " + searchStrategy);
		}

		private Collection<DeclaredAnnotations> computeDirect() {
			return Collections.singleton(getAnnotations(getSource()));
		}

		private Collection<DeclaredAnnotations> computeWithHierarchy(
				Function<Class<?>, TypeHierarchy> hierarchyFactory) {
			Class<?> declaringClass = getSource().getDeclaringClass();
			TypeHierarchy hierarchy = hierarchyFactory.apply(declaringClass).excluding(
					declaringClass);
			List<DeclaredAnnotations> result = new ArrayList<>();
			result.add(getAnnotations(getSource()));
			hierarchy.stream().flatMap(this::getOverrideCandidates).filter(
					this::isOverride).map(this::getAnnotations).forEach(result::add);
			return result;
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

		private boolean isOverride(Method candidate) {
			if (!candidate.getName().equals(getSource().getName())) {
				return false;
			}
			if (candidate.getParameterCount() != getSource().getParameterCount()) {
				return false;
			}
			Class<?>[] types = getSource().getParameterTypes();
			if (Arrays.equals(candidate.getParameterTypes(), types)) {
				return true;
			}
			Class<?> implementationClass = getSource().getDeclaringClass();
			for (int i = 0; i < types.length; i++) {
				if (types[i] != ResolvableType.forMethodParameter(candidate, i,
						implementationClass).resolve()) {
					return false;
				}
			}
			return true;
		}

		private DeclaredAnnotations getAnnotations(Method method) {
			Set<Annotation> annotations = new LinkedHashSet<>(
					Arrays.asList(method.getDeclaredAnnotations()));
			Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
			annotations.addAll(Arrays.asList(bridgedMethod.getDeclaredAnnotations()));
			return DeclaredAnnotations.from(method, annotations);
		}
	}

	private static class ElementResults extends Results<AnnotatedElement> {

		public ElementResults(AnnotatedElement source) {
			super(source);
		}

		@Override
		protected Collection<DeclaredAnnotations> compute(SearchStrategy searchStrategy) {
			if (searchStrategy != SearchStrategy.DIRECT) {
				return get(SearchStrategy.DIRECT);
			}
			return Collections.singleton(DeclaredAnnotations.from(getSource(),
					getSource().getDeclaredAnnotations()));
		}

	}

	private static final class TypeHierarchy {

		private final Set<Class<?>> hierarchy = new LinkedHashSet<>();

		private TypeHierarchy(Class<?> type, boolean includeInterfaces) {
			collect(type, includeInterfaces);
		}

		private void collect(Class<?> type, boolean includeInterfaces) {
			if (type == null || Object.class.equals(type)) {
				return;
			}
			this.hierarchy.add(type);
			collect(type.getSuperclass(), includeInterfaces);
			if (includeInterfaces) {
				for (Class<?> interfaceType : type.getInterfaces()) {
					collect(interfaceType, includeInterfaces);
				}
			}
		}

		public TypeHierarchy excluding(Class<?> type) {
			this.hierarchy.remove(type);
			return this;
		}

		public Stream<Class<?>> stream() {
			return this.hierarchy.stream();
		}

		public static TypeHierarchy superclasses(Class<?> type) {
			return new TypeHierarchy(type, false);
		}

		public static TypeHierarchy superclassesAndInterfaces(Class<?> type) {
			return new TypeHierarchy(type, true);
		}

	}

}
