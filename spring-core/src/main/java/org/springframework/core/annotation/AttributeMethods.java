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
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Provides a quick way to access the attribute methods of an {@link Annotation}
 * with consistent ordering.
 *
 * @author Phillip Webb
 * @since 5.2
 */
final class AttributeMethods {

	private static final Map<Class<? extends Annotation>, AttributeMethods> cache = new ConcurrentReferenceHashMap<>();

	private static final Comparator<Method> methodComparator = (m1, m2) -> {
		if (m1 != null && m2 != null) {
			return m1.getName().compareTo(m2.getName());
		}
		return m1 != null ? -1 : 1;
	};

	static final AttributeMethods NONE = new AttributeMethods(new Method[0]);

	private final Method[] attributeMethods;

	private AttributeMethods(Method[] attributeMethods) {
		this.attributeMethods = attributeMethods;
		for (Method method : attributeMethods) {
			method.setAccessible(true);
		}
	}

	/**
	 * Return if this instance only contains only a single attribute named
	 * {@code value}.
	 * @return {@code true} if this is only a value attribute
	 */
	public boolean isOnlyValueAttribute() {
		return this.attributeMethods.length == 1
				&& MergedAnnotation.VALUE.equals(this.attributeMethods[0].getName());
	}

	/**
	 * Return the attribute with the specified name or {@code null} if no
	 * matching attribute exists.
	 * @param name the attribute name to find
	 * @return the attribute method or {@code null}
	 */
	@Nullable
	public Method get(String name) {
		int index = indexOf(name);
		return index != -1 ? this.attributeMethods[index] : null;
	}

	/**
	 * Return the attribute at the specified index.
	 * @param index the index of the attribute to return
	 * @return the attribute method
	 * @throws IndexOutOfBoundsException if the index is out of range
	 * (<tt>index &lt; 0 || index &gt;= size()</tt>)
	 */
	@Nullable
	public Method get(int index) {
		return this.attributeMethods[index];
	}

	/**
	 * Return the index of the attribute with the specified name, or {@code -1}
	 * if there is no attribute with the name.
	 * @param name the name to find
	 * @return the index of the attribute, or {@code -1}
	 */
	public int indexOf(String name) {
		for (int i = 0; i < this.attributeMethods.length; i++) {
			if (this.attributeMethods[i].getName().equals(name)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Return the index of the specified attribute , or {@code -1} if the
	 * attribute is not not in this collection.
	 * @param name the attribute to find
	 * @return the index of the attribute, or {@code -1}
	 */
	public int indexOf(Method attribute) {
		for (int i = 0; i < this.attributeMethods.length; i++) {
			if (this.attributeMethods[i] == attribute) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Return the number of attributes in this collection.
	 * @return the number of attributes
	 */
	public int size() {
		return this.attributeMethods.length;
	}

	/**
	 * Return the attribute methods for the given annotation type.
	 * @param annotationType the annotation type
	 * @return the attribute methods for the annotation
	 */
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

	/**
	 * Check the declared attributes of the given annotation, in particular
	 * covering Google App Engine's late arrival of
	 * {@code TypeNotPresentExceptionProxy} for {@code Class} values (instead of
	 * early {@code Class.getAnnotations() failure}.
	 * @param annotation the annotation to validate
	 * @throws IllegalStateException if a declared {@code Class} attribute could
	 * not be read
	 */
	public static void validate(Annotation annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				annotation.annotationType());
		for (int i = 0; i < attributes.size(); i++) {
			Method attribute = attributes.get(i);
			validate(annotation, attribute);
		}
	}

	private static void validate(Annotation annotation, Method attribute) {
		Class<?> type = attribute.getReturnType();
		if (type == Class.class || type == Class[].class) {
			try {
				attribute.invoke(annotation);
			}
			catch (Throwable ex) {
				throw new IllegalStateException(
						"Could not obtain annotation attribute value for "
								+ attribute.getName() + " declared on "
								+ annotation.annotationType(),
						ex);
			}
		}
	}

	/**
	 * Return a description for the given attribute method suitable to use in
	 * exception messages and logs.
	 * @param attribute the attribute to describe
	 * @return a description of the attribute
	 */
	public static String describe(@Nullable Method attribute) {
		if (attribute == null) {
			return "(none)";
		}
		return describe(attribute.getDeclaringClass(), attribute.getName());
	}

	/**
	 * Return a description for the given attribute method suitable to use in
	 * exception messages and logs.
	 * @param attribute the attribute to describe
	 * @return a description of the attribute
	 */
	public static String describe(@Nullable Class<?> annotationType,
			@Nullable String attributeName) {
		if (attributeName == null) {
			return "(none)";
		}
		String in = annotationType != null
				? " in annotation [" + annotationType.getName() + "]"
				: "";
		return "attribute '" + attributeName + "'" + in;
	}

}
