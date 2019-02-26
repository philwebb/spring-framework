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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Provides a quick way to access the attribute methods of an {@link Annotation}
 * with consistent ordering.
 *
 * @author Phillip Webb
 * @since 5.2
 */
final class AttributeMethods implements Iterable<Method> {

	private static final Map<Class<? extends Annotation>, AttributeMethods> cache = new ConcurrentReferenceHashMap<>();

	private static final Comparator<Method> methodComparator = (m1, m2) -> {
		if (m1 != null && m2 != null) {
			return m1.getName().compareTo(m2.getName());
		}
		return m1 != null ? -1 : 1;
	};

	static final AttributeMethods NONE = new AttributeMethods(new Method[0]);

	private final Method[] attributeMethods;

	public AttributeMethods(Method[] attributeMethods) {
		this.attributeMethods = attributeMethods;
		for (Method method : attributeMethods) {
			method.setAccessible(true);
		}
	}

	public boolean isValueOnly() {
		return this.attributeMethods.length == 1
				&& MergedAnnotation.VALUE.equals(this.attributeMethods[0].getName());
	}

	@Nullable
	public Method get(String name) {
		int index = indexOf(name);
		return index != -1 ? this.attributeMethods[index] : null;
	}

	@Nullable
	public Method get(int index) {
		return this.attributeMethods[index];
	}

	public int indexOf(String name) {
		for (int i = 0; i < this.attributeMethods.length; i++) {
			if (this.attributeMethods[i].getName().equals(name)) {
				return i;
			}
		}
		return -1;
	}

	public int indexOf(Method attribute) {
		for (int i = 0; i < this.attributeMethods.length; i++) {
			if (this.attributeMethods[i] == attribute) {
				return i;
			}
		}
		return -1;
	}

	public int size() {
		return this.attributeMethods.length;
	}

	@Override
	public Iterator<Method> iterator() {
		return new MethodsIterator();
	}

	public static Method get(Class<? extends Annotation> annotationType, String name) {
		return forAnnotationType(annotationType).get(name);
	}

	public static AttributeMethods forAnnotationType(
			@Nullable Class<? extends Annotation> annotationType) {
		if (annotationType == null) {
			return NONE;
		}
		return cache.computeIfAbsent(annotationType, AttributeMethods::compute);
	}

	private static AttributeMethods compute(Class<? extends Annotation> annotationType) {
		Method[] methods = annotationType.getDeclaredMethods();
		int size = methods.length;
		for (int i = 0; i < methods.length; i++) {
			if (!isAttributeMethod(methods[0])) {
				methods[i] = null;
				size--;
			}
		}
		if (size == 0) {
			return NONE;
		}
		Arrays.sort(methods, methodComparator);
		Method[] attributeMethods = new Method[size];
		System.arraycopy(methods, 0, attributeMethods, 0, size);
		return new AttributeMethods(attributeMethods);
	}

	private static boolean isAttributeMethod(Method method) {
		return method.getParameterCount() == 0 && method.getReturnType() != void.class;
	}

	public static String describe(Method attributeMethod) {
		return describe(attributeMethod.getDeclaringClass(), attributeMethod.getName());
	}

	public static String describe(Class<?> annotationType, String attributeName) {
		return "attribute '" + attributeName + "' in annotation ["
				+ annotationType.getName() + "]";
	}

	/**
	 * Method {@link Iterator}.
	 */
	private class MethodsIterator implements Iterator<Method> {

		private int index = 0;

		@Override
		public boolean hasNext() {
			return this.index < attributeMethods.length;
		}

		@Override
		public Method next() {
			return attributeMethods[index++];
		}

	}

}
