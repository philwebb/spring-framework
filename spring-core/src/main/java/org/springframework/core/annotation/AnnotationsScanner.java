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
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EventListener;
import java.util.Map;
import java.util.function.BiPredicate;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;

/**
 * Scanner to search for relevant annotations on the hierarchy of an
 * {@link AnnotatedElement}.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see AnnotationsProcessor
 */
abstract class AnnotationsScanner {

	private static final Annotation[] NO_ANNOTATIONS = {};

	private static final Method[] NO_METHODS = {};

	private static final Map<AnnotatedElement, Annotation[]> declaredAnnotationCache = new ConcurrentReferenceHashMap<>(
			256);

	private static final Map<Class<?>, Method[]> baseTypeMethodsCache = new ConcurrentReferenceHashMap<>(
			256);

	private AnnotationsScanner() {
	}

	/**
	 * Scan the hierarchy of the specified element for relevant annotations and
	 * call the processor as required.
	 * @param context an optional context object that will be passed back to the
	 * processor
	 * @param source the source element to scan
	 * @param searchStrategy the search strategy to use
	 * @param processor the processor that receives the annotations
	 * @return the result of {@link AnnotationsProcessor#finish(Object)}
	 */
	public static <C, R> R scan(@Nullable C context, AnnotatedElement source,
			SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor) {
		return scan(context, source, searchStrategy, processor, null);
	}

	/**
	 * Scan the hierarchy of the specified element for relevant annotations and
	 * call the processor as required.
	 * @param context an optional context object that will be passed back to the
	 * processor
	 * @param source the source element to scan
	 * @param searchStrategy the search strategy to use
	 * @param processor the processor that receives the annotations
	 * @param classFilter an optional filter that can be used to entirely filter
	 * out a specific class from the hierarchy
	 * @return the result of {@link AnnotationsProcessor#finish(Object)}
	 */
	public static <C, R> R scan(C context, AnnotatedElement source,
			SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor,
			@Nullable BiPredicate<C, Class<?>> classFilter) {
		return processor.finish(
				process(context, source, searchStrategy, processor, classFilter));
	}

	private static <C, R> R process(C context, AnnotatedElement source,
			SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor,
			@Nullable BiPredicate<C, Class<?>> classFilter) {
		if (source instanceof Class) {
			return processClass(context, (Class<?>) source, searchStrategy, processor,
					classFilter);
		}
		if (source instanceof Method) {
			return processMethod(context, (Method) source, searchStrategy, processor,
					classFilter);
		}
		return processElement(context, source, processor, classFilter);
	}

	private static <C, R> R processClass(C context, Class<?> source,
			SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor,
			@Nullable BiPredicate<C, Class<?>> classFilter) {
		switch (searchStrategy) {
			case DIRECT:
				return processElement(context, source, processor, classFilter);
			case INHERITED_ANNOTATIONS:
				return processClassInheritedAnnotations(context, source, processor,
						classFilter);
			case SUPER_CLASS:
				return processClassHierarchy(context, new int[] { 0 }, source, processor,
						classFilter, false);
			case EXHAUSTIVE:
				return processClassHierarchy(context, new int[] { 0 }, source, processor,
						classFilter, true);
		}
		throw new IllegalStateException("Unsupported search strategy " + searchStrategy);
	}

	private static <C, R> R processClassInheritedAnnotations(C context, Class<?> source,
			AnnotationsProcessor<C, R> processor,
			@Nullable BiPredicate<C, Class<?>> classFilter) {
		if (isWithoutHierarchy(source)) {
			return processElement(context, source, processor, classFilter);
		}
		Annotation[] relevant = null;
		int remaining = Integer.MAX_VALUE;
		int aggregateIndex = 0;
		Class<?> root = source;
		while (source != null && source != Object.class && remaining > 0) {
			R result = processor.doWithAggregate(context, aggregateIndex);
			if (result != null) {
				return result;
			}
			if (!isFiltered(source, context, classFilter)) {
				Annotation[] declaredAnnotations = getDeclaredAnnotations(context, source,
						classFilter, true);
				if (relevant == null && declaredAnnotations.length > 0) {
					relevant = root.getAnnotations();
					remaining = relevant.length;
				}
				for (int i = 0; i < declaredAnnotations.length; i++) {
					if (declaredAnnotations[i] != null) {
						boolean isRelevant = false;
						for (int relevantIndex = 0; relevantIndex < relevant.length; relevantIndex++) {
							if (relevant[relevantIndex] != null
									&& declaredAnnotations[i].annotationType() == relevant[relevantIndex].annotationType()) {
								isRelevant = true;
								relevant[relevantIndex] = null;
								remaining--;
								break;
							}
						}
						if (!isRelevant) {
							declaredAnnotations[i] = null;
						}
					}
				}
				result = processor.doWithAnnotations(context, aggregateIndex, source,
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

	private static <C, R> R processClassHierarchy(C context, int[] aggregateIndex,
			Class<?> source, AnnotationsProcessor<C, R> processor,
			@Nullable BiPredicate<C, Class<?>> classFilter, boolean includeInterfaces) {
		R result = processor.doWithAggregate(context, aggregateIndex[0]);
		if (result != null) {
			return result;
		}
		Annotation[] annotations = getDeclaredAnnotations(context, source, classFilter,
				false);
		result = processor.doWithAnnotations(context, aggregateIndex[0], source,
				annotations);
		if (result != null) {
			return result;
		}
		aggregateIndex[0]++;
		if (includeInterfaces) {
			for (Class<?> interfaceType : source.getInterfaces()) {
				R interfacesResult = processClassHierarchy(context, aggregateIndex,
						interfaceType, processor, classFilter, includeInterfaces);
				if (interfacesResult != null) {
					return interfacesResult;
				}
			}
		}
		Class<?> superclass = source.getSuperclass();
		if (superclass != Object.class && superclass != null) {
			R superclassResult = processClassHierarchy(context, aggregateIndex,
					superclass, processor, classFilter, includeInterfaces);
			if (superclassResult != null) {
				return superclassResult;
			}
		}
		return null;
	}

	private static <C, R> R processMethod(C context, Method source,
			SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor,
			@Nullable BiPredicate<C, Class<?>> classFilter) {
		switch (searchStrategy) {
			case DIRECT:
			case INHERITED_ANNOTATIONS:
				return processMethodInheritedAnnotations(context, source, processor,
						classFilter);
			case SUPER_CLASS:
				return processMethodHierarchy(context, new int[] { 0 },
						source.getDeclaringClass(), processor, classFilter, source,
						false);
			case EXHAUSTIVE:
				return processMethodHierarchy(context, new int[] { 0 },
						source.getDeclaringClass(), processor, classFilter, source, true);
		}
		throw new IllegalStateException("Unsupported search strategy " + searchStrategy);
	}

	private static <C, R> R processMethodInheritedAnnotations(C context, Method source,
			AnnotationsProcessor<C, R> processor, BiPredicate<C, Class<?>> classFilter) {
		R result = processor.doWithAggregate(context, 0);
		return result != null ? result
				: processMethodAnnotations(context, 0, source, processor, classFilter);
	}

	private static <C, R> R processMethodHierarchy(C context, int[] aggregateIndex,
			Class<?> sourceClass, AnnotationsProcessor<C, R> processor,
			@Nullable BiPredicate<C, Class<?>> classFilter, Method rootMethod,
			boolean includeInterfaces) {
		R result = processor.doWithAggregate(context, aggregateIndex[0]);
		if (result != null) {
			return result;
		}
		boolean calledProcessor = false;
		if (sourceClass == rootMethod.getDeclaringClass()) {
			result = processMethodAnnotations(context, aggregateIndex[0], rootMethod,
					processor, classFilter);
			calledProcessor = true;
			if (result != null) {
				return result;
			}
		}
		else {
			for (Method candidateMethod : getBaseTypeMethods(context, sourceClass,
					classFilter)) {
				if (candidateMethod != null && isOverride(rootMethod, candidateMethod)) {
					result = processMethodAnnotations(context, aggregateIndex[0],
							candidateMethod, processor, classFilter);
					calledProcessor = true;
					if (result != null) {
						return result;
					}
				}
			}
		}
		if (Modifier.isPrivate(rootMethod.getModifiers())) {
			return null;
		}
		if (calledProcessor) {
			aggregateIndex[0]++;
		}
		if (includeInterfaces) {
			for (Class<?> interfaceType : sourceClass.getInterfaces()) {
				R interfacesResult = processMethodHierarchy(context, aggregateIndex,
						interfaceType, processor, classFilter, rootMethod,
						includeInterfaces);
				if (interfacesResult != null) {
					return interfacesResult;
				}
			}
		}
		Class<?> superclass = sourceClass.getSuperclass();
		if (superclass != Object.class && superclass != null) {
			R superclassResult = processMethodHierarchy(context, aggregateIndex,
					superclass, processor, classFilter, rootMethod, includeInterfaces);
			if (superclassResult != null) {
				return superclassResult;
			}
		}
		return null;
	}

	private static <C> Method[] getBaseTypeMethods(C context, Class<?> baseType,
			BiPredicate<C, Class<?>> classFilter) {
		if (baseType == Object.class) {
			return NO_METHODS;
		}
		if (baseType.isInterface() && (ClassUtils.isJavaLanguageInterface(baseType)
				|| baseType == EventListener.class
				|| (baseType.getName().startsWith("org.springframework.")
						&& baseType.getName().endsWith("Aware")))) {
			return NO_METHODS;
		}
		if (isFiltered(baseType, context, classFilter)) {
			return NO_METHODS;
		}
		Method[] methods = baseTypeMethodsCache.get(baseType);
		if (methods == null) {
			boolean isInterface = baseType.isInterface();
			methods = isInterface ? baseType.getMethods() : baseType.getDeclaredMethods();
			int cleared = 0;
			for (int i = 0; i < methods.length; i++) {
				if ((!isInterface && Modifier.isPrivate(methods[i].getModifiers()))
						|| hasPlainJavaAnnotationsOnly(methods[i])
						|| getDeclaredAnnotations(methods[i], false).length == 0) {
					methods[i] = null;
					cleared++;
				}
			}
			if (cleared == methods.length || methods.length == 0) {
				methods = NO_METHODS;
			}
			baseTypeMethodsCache.put(baseType, methods);
		}
		return methods;
	}

	private static boolean isOverride(Method rootMethod, Method candidateMethod) {
		return !Modifier.isPrivate(candidateMethod.getModifiers())
				&& candidateMethod.getName().equals(rootMethod.getName())
				&& hasSameParameterTypes(rootMethod, candidateMethod);
	}

	private static boolean hasSameParameterTypes(Method rootMethod,
			Method candidateMethod) {
		if (candidateMethod.getParameterCount() != rootMethod.getParameterCount()) {
			return false;
		}
		Class<?>[] rootParameterTypes = rootMethod.getParameterTypes();
		Class<?>[] candidateParameterTypes = candidateMethod.getParameterTypes();
		if (Arrays.equals(candidateParameterTypes, rootParameterTypes)) {
			return true;
		}
		return hasSameGenericTypeParameters(rootMethod, candidateMethod,
				rootParameterTypes);
	}

	private static boolean hasSameGenericTypeParameters(Method rootMethod,
			Method candidateMethod, Class<?>[] rootParameterTypes) {
		Class<?> sourceDeclaringClass = rootMethod.getDeclaringClass();
		Class<?> candidateDeclaringClass = candidateMethod.getDeclaringClass();
		if (!candidateDeclaringClass.isAssignableFrom(sourceDeclaringClass)) {
			return false;
		}
		for (int i = 0; i < rootParameterTypes.length; i++) {
			Class<?> resolvedParameterType = ResolvableType.forMethodParameter(
					candidateMethod, i, sourceDeclaringClass).resolve();
			if (rootParameterTypes[i] != resolvedParameterType) {
				return false;
			}
		}
		return true;
	}

	private static <C, R> R processMethodAnnotations(C context, int aggregateIndex,
			Method source, AnnotationsProcessor<C, R> processor,
			BiPredicate<C, Class<?>> classFilter) {
		Annotation[] annotations = getDeclaredAnnotations(context, source, classFilter,
				false);
		R result = processor.doWithAnnotations(context, aggregateIndex, source,
				annotations);
		if (result != null) {
			return result;
		}
		Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(source);
		if (bridgedMethod != source) {
			Annotation[] bridgedAnnotations = getDeclaredAnnotations(context,
					bridgedMethod, classFilter, true);
			for (int i = 0; i < bridgedAnnotations.length; i++) {
				if (ObjectUtils.containsElement(annotations, bridgedAnnotations[i])) {
					bridgedAnnotations[i] = null;
				}
			}
			return processor.doWithAnnotations(context, aggregateIndex, source,
					bridgedAnnotations);
		}
		return null;
	}

	private static <C, R> R processElement(C context, AnnotatedElement source,
			AnnotationsProcessor<C, R> processor,
			@Nullable BiPredicate<C, Class<?>> classFilter) {
		R result = processor.doWithAggregate(context, 0);
		return result != null ? result
				: processor.doWithAnnotations(context, 0, source,
						getDeclaredAnnotations(context, source, classFilter, false));
	}

	private static <C, R> Annotation[] getDeclaredAnnotations(C context,
			AnnotatedElement source, @Nullable BiPredicate<C, Class<?>> classFilter,
			boolean copy) {
		if (source instanceof Class
				&& isFiltered((Class<?>) source, context, classFilter)) {
			return NO_ANNOTATIONS;
		}
		if (source instanceof Method && isFiltered(((Method) source).getDeclaringClass(),
				context, classFilter)) {
			return NO_ANNOTATIONS;
		}
		return getDeclaredAnnotations(source, copy);
	}

	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A getDeclaredAnnotation(AnnotatedElement source,
			Class<A> annotationType) {
		Annotation[] annotations = getDeclaredAnnotations(source, false);
		for (int i = 0; i < annotations.length; i++) {
			if (annotations[i] != null
					&& annotationType.equals(annotations[i].annotationType())) {
				return (A) annotations[i];
			}
		}
		return null;
	}

	static Annotation[] getDeclaredAnnotations(AnnotatedElement source, boolean copy) {
		boolean cached = true;
		Annotation[] annotations = declaredAnnotationCache.get(source);
		if (annotations == null) {
			annotations = source.getDeclaredAnnotations();
			boolean allIgnored = true;
			for (int i = 0; i < annotations.length; i++) {
				if (isIgnorable(annotations[i].annotationType())) {
					annotations[i] = null;
				}
				else {
					allIgnored = false;
				}
			}
			annotations = allIgnored ? NO_ANNOTATIONS : annotations;
			cached = (source instanceof Class || source instanceof Member);
			if (cached) {
				declaredAnnotationCache.put(source, annotations);
			}
		}
		if (!copy || annotations.length == 0 || !cached) {
			return annotations;
		}
		Annotation[] copied = new Annotation[annotations.length];
		System.arraycopy(annotations, 0, copied, 0, annotations.length);
		return copied;
	}

	private static boolean isIgnorable(Class<?> annotationType) {
		return (annotationType == Nullable.class
				|| annotationType == FunctionalInterface.class);
	}

	private static <C> boolean isFiltered(Class<?> sourceClass, C context,
			BiPredicate<C, Class<?>> classFilter) {
		return classFilter != null && classFilter.test(context, sourceClass);
	}

	public static boolean isKnownEmpty(AnnotatedElement source,
			SearchStrategy searchStrategy, AnnotationFilter annotationFilter) {
		if(annotationFilter == AnnotationFilter.PLAIN && hasPlainJavaAnnotationsOnly(source)) {
			return true;
		}
		if (searchStrategy == SearchStrategy.DIRECT || isWithoutHierarchy(source)) {
			AnnotatedElement bridged = source instanceof Method
					? BridgeMethodResolver.findBridgedMethod((Method) source)
					: source;
			return getDeclaredAnnotations(source, false).length == 0 && (bridged == source
					|| getDeclaredAnnotations(bridged, false).length == 0);
		}
		return false;
	}

	public static boolean hasPlainJavaAnnotationsOnly(@Nullable Object annotatedElement) {
		String name = null;
		if (annotatedElement instanceof Class) {
			name = ((Class<?>) annotatedElement).getName();
		}
		else if (annotatedElement instanceof Member) {
			name = ((Member) annotatedElement).getDeclaringClass().getName();
		}
		return name != null && (name.startsWith("java")
				|| name.startsWith("org.springframework.lang.")
				|| name.startsWith("com.sun"));
	}

	private static boolean isWithoutHierarchy(AnnotatedElement source) {
		if (source == Object.class) {
			return true;
		}
		if (source instanceof Class) {
			Class<?> sourceClass = (Class<?>) source;
			return sourceClass.getSuperclass() == Object.class
					&& sourceClass.getInterfaces().length == 0;
		}
		if (source instanceof Method) {
			Method sourceMethod = (Method) source;
			return Modifier.isPrivate(sourceMethod.getModifiers())
					|| isWithoutHierarchy(sourceMethod.getDeclaringClass());
		}
		return true;
	}

	static void clearCache() {
		declaredAnnotationCache.clear();
	}

}
