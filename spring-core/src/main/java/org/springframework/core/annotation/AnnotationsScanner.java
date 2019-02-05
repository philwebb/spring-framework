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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.BiPredicate;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Scanner to search for relevant annotations on the hierarchy of an
 * {@link AnnotatedElement}.
 *
 * @author Phillip Webb
 * @since 5.1
 */
class AnnotationsScanner {

	private static final Annotation[] NO_ANNOTATIONS = {};

	private static final Method[] NO_METHODS = {};

	/**
	 * Scan the hierarchy of the specified element for relevant annotations and
	 * call the processor as required.
	 * @param element the source element to scan
	 * @param searchStrategy the search strategy to use
	 * @param criteria an optional criteria object that will be passed back to
	 * the processor
	 * @param processor the processor that receives the annotations
	 * @return the result of {@link Processor#getScanResult(Object)}
	 */
	public static <C, R> R scan(AnnotatedElement element, SearchStrategy searchStrategy,
			@Nullable C criteria, Processor<C, R> processor) {
		return scan(element, searchStrategy, criteria, null, processor);
	}

	/**
	 * Scan the hierarchy of the specified element for relevant annotations and
	 * call the processor as required.
	 * @param element the source element to scan
	 * @param searchStrategy the search strategy to use
	 * @param criteria an optional criteria object that will be passed back to
	 * the processor
	 * @param classFilter an optional filter that can be used to entirely filter
	 * out a specific class from the hierarchy
	 * @param processor the processor that receives the annotations
	 * @return the result of {@link Processor#getScanResult(Object)}
	 */
	public static <C, R> R scan(AnnotatedElement element, SearchStrategy searchStrategy,
			C criteria, @Nullable BiPredicate<C, Class<?>> classFilter,
			Processor<C, R> processor) {
		R processResult = process(element, searchStrategy, criteria, classFilter,
				processor);
		return processor.getScanResult(processResult);
	}

	private static <C, R> R process(AnnotatedElement element,
			SearchStrategy searchStrategy, C criteria,
			@Nullable BiPredicate<C, Class<?>> classFilter, Processor<C, R> processor) {
		if (element instanceof Class) {
			return processClass((Class<?>) element, searchStrategy, criteria, classFilter,
					processor);
		}
		if (element instanceof Method) {
			return processMethod((Method) element, searchStrategy, criteria, classFilter,
					processor);
		}
		return processElement(element, criteria, classFilter, processor);
	}

	private static <C, R> R processClass(Class<?> element, SearchStrategy searchStrategy,
			C criteria, @Nullable BiPredicate<C, Class<?>> classFilter,
			Processor<C, R> processor) {
		switch (searchStrategy) {
			case DIRECT:
				return processClassDirect(element, criteria, classFilter, processor);
			case INHERITED_ANNOTATIONS:
				return processClassInheritedAnnotations(element, criteria, classFilter,
						processor);
			case SUPER_CLASS:
				return processClassSuperClass(element, criteria, classFilter, processor);
			case EXHAUSTIVE:
				return processClassExhaustive(element, criteria, classFilter, processor);
		}
		throw new IllegalStateException("Unsupported search strategy " + searchStrategy);
	}

	private static <C, R> R processClassDirect(Class<?> element, C criteria,
			BiPredicate<C, Class<?>> classFilter, Processor<C, R> processor) {
		if (isFiltered(element, criteria, classFilter)) {
			return null;
		}
		return processElement(element, criteria, classFilter, processor);
	}

	private static <C, R> R processClassInheritedAnnotations(Class<?> element, C criteria,
			@Nullable BiPredicate<C, Class<?>> classFilter, Processor<C, R> processor) {
		Annotation[] relevant = null;
		int remaining = Integer.MAX_VALUE;
		int aggregateIndex = 0;
		Class<?> source = element;
		while (source != null && source != Object.class && remaining > 0) {
			if (!isFiltered(source, criteria, classFilter)) {
				Annotation[] declaredAnnotations = getDeclaredAnnotations(source,
						criteria, classFilter);
				if (relevant == null && declaredAnnotations.length > 0) {
					relevant = element.getAnnotations();
					remaining = relevant.length;
				}
				for (int i = 0; i < declaredAnnotations.length; i++) {
					boolean isRelevant = false;
					for (int relevantIndex = 0; relevantIndex < relevant.length; relevantIndex++) {
						if (declaredAnnotations[i].equals(relevant[relevantIndex])) {
							isRelevant = true;
							relevant[relevantIndex] = null;
							remaining--;
						}
					}
					if (!isRelevant) {
						declaredAnnotations[i] = null;
					}
				}
				R result = processor.process(criteria, aggregateIndex, source,
						declaredAnnotations);
				if (result != null) {
					return result;
				}
			}
			source = source.getSuperclass();
			aggregateIndex++;
		}
		return null;
	}

	private static <C, R> R processClassSuperClass(Class<?> element, C criteria,
			@Nullable BiPredicate<C, Class<?>> classFilter, Processor<C, R> processor) {
		return processClassHierarchy(element, new int[] { 0 }, false, criteria,
				classFilter, processor);
	}

	private static <C, R> R processClassExhaustive(Class<?> element, C criteria,
			@Nullable BiPredicate<C, Class<?>> classFilter, Processor<C, R> processor) {
		return processClassHierarchy(element, new int[] { 0 }, true, criteria,
				classFilter, processor);
	}

	private static <C, R> R processClassHierarchy(Class<?> source, int[] aggregateIndex,
			boolean includeInterfaces, C criteria,
			@Nullable BiPredicate<C, Class<?>> classFilter, Processor<C, R> processor) {
		Annotation[] annotations = getDeclaredAnnotations(source, criteria, classFilter);
		R result = processor.process(criteria, aggregateIndex[0], source, annotations);
		if (result != null) {
			return result;
		}
		aggregateIndex[0]++;
		if (includeInterfaces) {
			for (Class<?> interfaceType : source.getInterfaces()) {
				R interfacesResult = processClassHierarchy(interfaceType, aggregateIndex,
						includeInterfaces, criteria, classFilter, processor);
				if (interfacesResult != null) {
					return interfacesResult;
				}
			}
		}
		Class<?> superclass = source.getSuperclass();
		if (superclass != Object.class && superclass != null) {
			R superclassResult = processClassHierarchy(superclass, aggregateIndex,
					includeInterfaces, criteria, classFilter, processor);
			if (superclassResult != null) {
				return superclassResult;
			}
		}
		return null;
	}

	private static <C, R> R processMethod(Method method, SearchStrategy searchStrategy,
			C criteria, @Nullable BiPredicate<C, Class<?>> classFilter,
			Processor<C, R> processor) {
		switch (searchStrategy) {
			case DIRECT:
			case INHERITED_ANNOTATIONS:
				return processMethodDirect(method, criteria, classFilter, processor);
			case SUPER_CLASS:
				return processMethodSuperClass(method, criteria, classFilter, processor);
			case EXHAUSTIVE:
				return processMethodExhaustive(method, criteria, classFilter, processor);
		}
		throw new IllegalStateException("Unsupported search strategy " + searchStrategy);
	}

	private static <C, R> R processMethodDirect(Method method, C criteria,
			BiPredicate<C, Class<?>> classFilter, Processor<C, R> processor) {
		return processMethodAnnotations(method, method.getDeclaringClass(), criteria, 0,
				classFilter, processor);
	}

	private static <C, R> R processMethodSuperClass(Method element, C criteria,
			BiPredicate<C, Class<?>> classFilter, Processor<C, R> processor) {
		return processMethodHierarchy(element, element.getDeclaringClass(),
				new int[] { 0 }, false, criteria, classFilter, processor);
	}

	private static <C, R> R processMethodExhaustive(Method element, C criteria,
			BiPredicate<C, Class<?>> classFilter, Processor<C, R> processor) {
		return processMethodHierarchy(element, element.getDeclaringClass(),
				new int[] { 0 }, true, criteria, classFilter, processor);
	}

	private static <C, R> R processMethodHierarchy(Method element, Class<?> source,
			int[] aggregateIndex, boolean includeInterfaces, C criteria,
			@Nullable BiPredicate<C, Class<?>> classFilter, Processor<C, R> processor) {
		if (element.getDeclaringClass() == source) {
			processMethodAnnotations(element, source, criteria, aggregateIndex[0],
					classFilter, processor);
		}
		else {
			for (Method method : getMethods(source)) {
				if (isOverride(element, method)) {
					processMethodAnnotations(method, source, criteria, aggregateIndex[0],
							classFilter, processor);
				}
			}
		}
		aggregateIndex[0]++;
		if (includeInterfaces) {
			for (Class<?> interfaceType : source.getInterfaces()) {
				R interfacesResult = processMethodHierarchy(element, interfaceType,
						aggregateIndex, includeInterfaces, criteria, classFilter,
						processor);
				if (interfacesResult != null) {
					return interfacesResult;
				}
			}
		}
		Class<?> superclass = source.getSuperclass();
		if (superclass != Object.class && superclass != null) {
			R superclassResult = processMethodHierarchy(element, superclass,
					aggregateIndex, includeInterfaces, criteria, classFilter, processor);
			if (superclassResult != null) {
				return superclassResult;
			}
		}
		return null;
	}

	private static Method[] getMethods(Class<?> type) {
		if (type == Object.class) {
			return NO_METHODS;
		}
		if (type.isInterface() && ClassUtils.isJavaLanguageInterface(type)) {
			return NO_METHODS;
		}
		return type.isInterface() ? type.getMethods() : type.getDeclaredMethods();
	}

	private static boolean isOverride(Method source, Method candidate) {
		return !Modifier.isPrivate(candidate.getModifiers())
				&& candidate.getName().equals(source.getName())
				&& hasSameParameterTypes(source, candidate);
	}

	private static boolean hasSameParameterTypes(Method source, Method candidate) {
		if (candidate.getParameterCount() != source.getParameterCount()) {
			return false;
		}
		Class<?>[] sourceTypes = source.getParameterTypes();
		Class<?>[] candidateTypes = candidate.getParameterTypes();
		if (Arrays.equals(candidateTypes, sourceTypes)) {
			return true;
		}
		return hasSameGenericTypeParameters(source, candidate, sourceTypes);
	}

	private static boolean hasSameGenericTypeParameters(Method source, Method candidate,
			Class<?>[] types) {
		Class<?> sourceDeclaringClass = source.getDeclaringClass();
		Class<?> candidateDeclaringClass = candidate.getDeclaringClass();
		if (!candidateDeclaringClass.isAssignableFrom(sourceDeclaringClass)) {
			return false;
		}
		for (int i = 0; i < types.length; i++) {
			Class<?> resolved = ResolvableType.forMethodParameter(candidate, i,
					sourceDeclaringClass).resolve();
			if (types[i] != resolved) {
				return false;
			}
		}
		return true;
	}

	private static <C, R> R processMethodAnnotations(Method element, Class<?> source,
			C criteria, int aggregateIndex, BiPredicate<C, Class<?>> classFilter,
			Processor<C, R> processor) {
		Annotation[] annotations = getDeclaredAnnotations(element, criteria, classFilter);
		R result = processor.process(criteria, aggregateIndex, source, annotations);
		if (result != null) {
			return result;
		}
		Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(element);
		if (bridgedMethod != element) {
			Annotation[] bridgedAnnotations = getDeclaredAnnotations(bridgedMethod,
					criteria, classFilter);
			return processor.process(criteria, aggregateIndex, source,
					bridgedAnnotations);
		}
		return null;
	}

	private static <C, R> R processElement(AnnotatedElement element, C criteria,
			@Nullable BiPredicate<C, Class<?>> classFilter, Processor<C, R> processor) {
		Annotation[] declaredAnnotations = getDeclaredAnnotations(element, criteria,
				classFilter);
		return processor.process(criteria, 0, element, declaredAnnotations);
	}

	private static <C, R> Annotation[] getDeclaredAnnotations(AnnotatedElement element,
			C criteria, @Nullable BiPredicate<C, Class<?>> classFilter) {
		if (element instanceof Class
				&& isFiltered((Class<?>) element, criteria, classFilter)) {
			return NO_ANNOTATIONS;
		}
		if (element instanceof Method && isFiltered(
				((Method) element).getDeclaringClass(), criteria, classFilter)) {
			return NO_ANNOTATIONS;
		}
		Annotation[] declaredAnnotations = element.getDeclaredAnnotations();
		boolean allIgnored = true;
		for (int i = 0; i < declaredAnnotations.length; i++) {
			if (isIgnorable(declaredAnnotations[i].annotationType())) {
				declaredAnnotations[i] = null;
			}
			else {
				allIgnored = false;
			}
		}
		return allIgnored ? NO_ANNOTATIONS : declaredAnnotations;
	}

	private static boolean isIgnorable(Class<?> annotationType) {
		return (annotationType == Nullable.class || annotationType == Deprecated.class
				|| annotationType == FunctionalInterface.class);
	}

	private static <C> boolean isFiltered(Class<?> element, C criteria,
			BiPredicate<C, Class<?>> classFilter) {
		return classFilter != null && classFilter.test(criteria, element);
	}

	/**
	 * Callback interface used to receive annotation details.
	 * @param <C> the criteria type
	 * @param <R> the result type
	 */
	@FunctionalInterface
	static interface Processor<C, R> {

		/**
		 * Called when an array of annotations can be processed. This method may
		 * return a {@code non-null} result to short-circuit any further search.
		 * @param criteria the criteria passed to the {@code scan} method
		 * @param aggregateIndex the aggregate index of the provided annotations
		 * @param source the source of the annotations
		 * @param annotations the annotations to process (may contain
		 * {@code null} elements)
		 * @return a {@code non-null} result if no further processing is
		 * required
		 */
		@Nullable
		R process(@Nullable C criteria, int aggregateIndex, Object source,
				Annotation[] annotations);

		/**
		 * Return the final result to be returned from the {@code scan} method.
		 * @param processResult result returned from {@link #process}, or
		 * {@code null} if processing was not exited early. By default this
		 * method returns the last process result.
		 * @return the final result returned from the scan method.
		 */
		@Nullable
		default R getScanResult(@Nullable R processResult) {
			return processResult;
		}

	}

}
