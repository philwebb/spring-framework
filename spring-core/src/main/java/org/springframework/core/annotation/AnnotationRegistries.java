/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * A collection of {@link AnnotationRegistries} used to bypass introspection.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see AnnotationRegistry
 */
public final class AnnotationRegistries {

	private static final AnnotationRegistry[] EMPTY = {};

	private static AnnotationRegistry[] registries = EMPTY;

	private AnnotationRegistries() {
	}

	static boolean requiresIntrospection(Class<?> clazz,
			Class<? extends Annotation> annotationType) {
		return requiresIntrospection(clazz, annotationType.getName(), annotationType);
	}

	static boolean requiresIntrospection(Class<?> clazz, String annotationName) {
		return requiresIntrospection(clazz, annotationName, null);
	}

	private static boolean requiresIntrospection(Class<?> clazz, String annotationName,
			@Nullable Class<? extends Annotation> annotationType) {

		while (clazz != Object.class && clazz != null) {
			if (!canSkipIntrospection(clazz, annotationName, annotationType) ||
					hasInterfaceRequiringIntrospection(clazz, annotationName, annotationType)) {
				return true;
			}
			clazz = clazz.getSuperclass();
		}
		return false;
	}

	private static boolean hasInterfaceRequiringIntrospection(Class<?> clazz, String annotationName,
			@Nullable Class<? extends Annotation> annotationType) {

		for (Class<?> interfaceClass : clazz.getInterfaces()) {
			if (!canSkipIntrospection(interfaceClass, annotationName, annotationType) ||
					hasInterfaceRequiringIntrospection(interfaceClass, annotationName, annotationType)) {
				return true;
			}
		}
		return false;
	}

	private static boolean canSkipIntrospection(Class<?> clazz, String annotationName,
			@Nullable Class<? extends Annotation> annotationType) {
		if (!annotationName.startsWith("java.") && AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz)) {
			return true;
		}
		if (!AnnotationFilter.PLAIN.matches(annotationName)) {
			for (AnnotationRegistry registry : registries) {
				if (canSkipIntrospection(registry, clazz, annotationName, annotationType)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean canSkipIntrospection(AnnotationRegistry registry, Class<?> clazz,
			String annotationName, @Nullable Class<? extends Annotation> annotationType) {

		return (annotationType != null)
				? registry.canSkipIntrospection(clazz, annotationType)
				: registry.canSkipIntrospection(clazz, annotationName);
	}

	/**
	 * Add the given registry to the existing collection.
	 * @param registry the registry to add
	 */
	public static synchronized void add(AnnotationRegistry registry) {
		AnnotationRegistry[] current = AnnotationRegistries.registries;
		if (!ObjectUtils.containsElement(current, registry)) {
			AnnotationRegistry[] updated = new AnnotationRegistry[current.length + 1];
			System.arraycopy(current, 0, updated, 0, current.length);
			updated[current.length] = registry;
			Arrays.sort(current, AnnotationAwareOrderComparator.INSTANCE);
			AnnotationRegistries.registries = updated;
		}
	}

	static synchronized void clear() {
		AnnotationRegistries.registries = EMPTY;
	}

}
