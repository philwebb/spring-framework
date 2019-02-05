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
		return processor.getScanResult(
				process(element, searchStrategy, criteria, classFilter, processor));
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
				return processElement(element, criteria, classFilter, processor);
			case INHERITED_ANNOTATIONS:
				return processClassInheritedAnnotations(element, criteria, classFilter,
						processor);
			case SUPER_CLASS:
				return processClassHierarchy(element, criteria, classFilter, processor,
						new int[] { 0 }, false);
			case EXHAUSTIVE:
				return processClassHierarchy(element, criteria, classFilter, processor,
						new int[] { 0 }, true);
		}
		throw new IllegalStateException("Unsupported search strategy " + searchStrategy);
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

	private static <C, R> R processClassHierarchy(Class<?> sourceClass, C criteria,
			@Nullable BiPredicate<C, Class<?>> classFilter, Processor<C, R> processor,
			int[] aggregateIndex, boolean includeInterfaces) {
		Annotation[] annotations = getDeclaredAnnotations(sourceClass, criteria,
				classFilter);
		R result = processor.process(criteria, aggregateIndex[0], sourceClass,
				annotations);
		if (result != null) {
			return result;
		}
		aggregateIndex[0]++;
		if (includeInterfaces) {
			for (Class<?> interfaceType : sourceClass.getInterfaces()) {
				R interfacesResult = processClassHierarchy(interfaceType, criteria,
						classFilter, processor, aggregateIndex, includeInterfaces);
				if (interfacesResult != null) {
					return interfacesResult;
				}
			}
		}
		Class<?> superclass = sourceClass.getSuperclass();
		if (superclass != Object.class && superclass != null) {
			R superclassResult = processClassHierarchy(superclass, criteria, classFilter,
					processor, aggregateIndex, includeInterfaces);
			if (superclassResult != null) {
				return superclassResult;
			}
		}
		return null;
	}

	private static <C, R> R processMethod(Method element, SearchStrategy searchStrategy,
			C criteria, @Nullable BiPredicate<C, Class<?>> classFilter,
			Processor<C, R> processor) {
		switch (searchStrategy) {
			case DIRECT:
			case INHERITED_ANNOTATIONS:
				return processMethodAnnotations(element, element.getDeclaringClass(),
						criteria, 0, classFilter, processor);
			case SUPER_CLASS:
				return processMethodHierarchy(element.getDeclaringClass(), element,
						criteria, classFilter, processor, new int[] { 0 }, false);
			case EXHAUSTIVE:
				return processMethodHierarchy(element.getDeclaringClass(), element,
						criteria, classFilter, processor, new int[] { 0 }, true);
		}
		throw new IllegalStateException("Unsupported search strategy " + searchStrategy);
	}

	private static <C, R> R processMethodHierarchy(Class<?> sourceClass, Method element,
			C criteria, @Nullable BiPredicate<C, Class<?>> classFilter,
			Processor<C, R> processor, int[] aggregateIndex, boolean includeInterfaces) {
		if (element.getDeclaringClass() == sourceClass) {
			R result = processMethodAnnotations(element, sourceClass, criteria,
					aggregateIndex[0], classFilter, processor);
			if (result != null) {
				return result;
			}
		}
		else {
			for (Method candidate : getMethods(sourceClass, criteria, classFilter)) {
				if (isOverride(element, candidate)) {
					R result = processMethodAnnotations(candidate, sourceClass, criteria,
							aggregateIndex[0], classFilter, processor);
					if (result != null) {
						return result;
					}
				}
			}
		}
		aggregateIndex[0]++;
		if (includeInterfaces) {
			for (Class<?> interfaceType : sourceClass.getInterfaces()) {
				R interfacesResult = processMethodHierarchy(interfaceType, element,
						criteria, classFilter, processor, aggregateIndex,
						includeInterfaces);
				if (interfacesResult != null) {
					return interfacesResult;
				}
			}
		}
		Class<?> superclass = sourceClass.getSuperclass();
		if (superclass != Object.class && superclass != null) {
			R superclassResult = processMethodHierarchy(superclass, element, criteria,
					classFilter, processor, aggregateIndex, includeInterfaces);
			if (superclassResult != null) {
				return superclassResult;
			}
		}
		return null;
	}

	private static <C> Method[] getMethods(Class<?> type, C criteria,
			BiPredicate<C, Class<?>> classFilter) {
		if (type == Object.class) {
			return NO_METHODS;
		}
		if (type.isInterface() && ClassUtils.isJavaLanguageInterface(type)) {
			return NO_METHODS;
		}
		if (isFiltered(type, criteria, classFilter)) {
			return NO_METHODS;
		}
		return type.isInterface() ? type.getMethods() : type.getDeclaredMethods();
	}

	private static boolean isOverride(Method element, Method candidate) {
		return !Modifier.isPrivate(candidate.getModifiers())
				&& candidate.getName().equals(element.getName())
				&& hasSameParameterTypes(element, candidate);
	}

	private static boolean hasSameParameterTypes(Method element, Method candidate) {
		if (candidate.getParameterCount() != element.getParameterCount()) {
			return false;
		}
		Class<?>[] sourceTypes = element.getParameterTypes();
		Class<?>[] candidateTypes = candidate.getParameterTypes();
		if (Arrays.equals(candidateTypes, sourceTypes)) {
			return true;
		}
		return hasSameGenericTypeParameters(element, candidate, sourceTypes);
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
		return processor.process(criteria, 0, element,
				getDeclaredAnnotations(element, criteria, classFilter));
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
