/*
 * Copyright 2002-2016 the original author or authors.
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * {@link InvocationHandler} for an {@link Annotation} that Spring has
 * <em>synthesized</em> (i.e., wrapped in a dynamic proxy) with additional
 * functionality.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 5.2
 * @param <A> the annotation type
 * @see Annotation
 * @see AnnotationAttributeExtractor
 * @see AnnotationUtils#synthesizeAnnotation(Annotation, AnnotatedElement)
 */
class SynthesizedMergedAnnotationInvocationHandler<A extends Annotation>
		implements InvocationHandler {

	private final MergedAnnotation<?> annotation;

	private final Class<A> type;

	private volatile Integer hashCode;

	SynthesizedMergedAnnotationInvocationHandler(MergedAnnotation<A> annotation,
			Class<A> type) {
		Assert.notNull(annotation, "Annotation must not be null");
		Assert.notNull(type, "Type must not be null");
		Assert.isTrue(type.isAnnotation(), "Type must be an annotation");
		this.type = type;
		this.annotation = annotation;
		verifyAttributeMethods();
	}

	private void verifyAttributeMethods() {
		ReflectionUtils.doWithLocalMethods(this.type, this::getAttributeValue,
				this::isAttributeMethod);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) {
		if (ReflectionUtils.isEqualsMethod(method)) {
			return annotationEquals(args[0]);
		}
		if (ReflectionUtils.isHashCodeMethod(method)) {
			return annotationHashCode();
		}
		if (ReflectionUtils.isToStringMethod(method)) {
			return this.annotation.toString();
		}
		if (isAnnotationTypeMethod(method)) {
			return this.type;
		}
		if (isAttributeMethod(method)) {
			return getAttributeValue(method);
		}
		throw new AnnotationConfigurationException(String.format(
				"Method [%s] is unsupported for synthesized annotation type [%s]", method,
				this.type));
	}

	private boolean isAnnotationTypeMethod(Method method) {
		return Objects.equals(method.getName(), "annotationType")
				&& method.getParameterCount() == 0;
	}

	private Object getAttributeValue(Method method) {
		String name = method.getName();
		Class<?> type = ClassUtils.resolvePrimitiveIfNecessary(method.getReturnType());
		return this.annotation.getValue(name, type).orElseThrow(
				() -> new NoSuchElementException("No value found for attribute named '" + name
						+ "' in merged annotation " + this.annotation.getType()));
	}

	/**
	 * See {@link Annotation#equals(Object)} for a definition of the required
	 * algorithm.
	 * @param other the other object to compare against
	 */
	private boolean annotationEquals(Object other) {
		if (this == other) {
			return true;
		}
		if (!this.type.isInstance(other)) {
			return false;
		}
		EqualsCheck check = new EqualsCheck(other);
		ReflectionUtils.doWithLocalMethods(this.type, check, this::isAttributeMethod);
		return check.isEquals();
	}

	/**
	 * See {@link Annotation#hashCode()} for a definition of the required
	 * algorithm.
	 */
	private int annotationHashCode() {
		Integer hashCode = this.hashCode;
		if (hashCode == null) {
			HashCodeGenerator generator = new HashCodeGenerator();
			ReflectionUtils.doWithLocalMethods(this.type, generator,
					this::isAttributeMethod);
			hashCode = generator.getHashCode();
			this.hashCode = hashCode;
		}
		return hashCode;
	}

	private boolean isAttributeMethod(Method method) {
		return method.getParameterCount() == 0 && method.getReturnType() != void.class;
	}

	@SuppressWarnings("unchecked")
	static <A extends Annotation> A createProxy(MergedAnnotation<A> annotation, Class<A> type) {
		ClassLoader classLoader = type.getClassLoader();
		InvocationHandler handler = new SynthesizedMergedAnnotationInvocationHandler<>(
				annotation, type);
		Class<?>[] interfaces = isVisible(classLoader, SynthesizedAnnotation.class)
						? new Class<?>[] { type, SynthesizedAnnotation.class }
						: new Class<?>[] { type };
		return (A) Proxy.newProxyInstance(classLoader, interfaces, handler);
	}

	private static boolean isVisible(ClassLoader classLoader, Class<?> interfaceClass) {
		try {
			return Class.forName(interfaceClass.getName(), false,
					classLoader) == interfaceClass;
		}
		catch (ClassNotFoundException ex) {
			return false;
		}
	}

	/**
	 * {@link MethodCallback} used to implement {@code equals}.
	 */
	private class EqualsCheck implements MethodCallback {

		private final Object other;

		private boolean equals = true;

		public EqualsCheck(Object other) {
			this.other = other;
		}

		@Override
		public void doWith(Method method)
				throws IllegalArgumentException, IllegalAccessException {
			if (method.getParameterCount() == 0 && method.getReturnType() != void.class) {
				this.equals = this.equals && isEqual(method);
			}
		}

		private boolean isEqual(Method method) {
			ReflectionUtils.makeAccessible(method);
			Object thisValue = getAttributeValue(method);
			Object otherValue = ReflectionUtils.invokeMethod(method, this.other);
			return ObjectUtils.nullSafeEquals(thisValue, otherValue);
		}

		public boolean isEquals() {
			return this.equals;
		}

	}

	/**
	 * {@link MethodCallback} used to implement {@code hashCode}.
	 */
	private class HashCodeGenerator implements MethodCallback {

		private int hashCode;

		@Override
		public void doWith(Method method)
				throws IllegalArgumentException, IllegalAccessException {
			Object value = getAttributeValue(method);
			int valueHashCode = getValueHashCode(value);
			this.hashCode += (127 * method.getName().hashCode()) ^ valueHashCode;
		}

		private int getValueHashCode(Object value) {
			// Use Arrays.hashCode since ObjectUtils doesn't comply to to
			// Annotation#hashCode()
			if (value instanceof boolean[]) {
				return Arrays.hashCode((boolean[]) value);
			}
			if (value instanceof byte[]) {
				return Arrays.hashCode((byte[]) value);
			}
			if (value instanceof char[]) {
				return Arrays.hashCode((char[]) value);
			}
			if (value instanceof double[]) {
				return Arrays.hashCode((double[]) value);
			}
			if (value instanceof float[]) {
				return Arrays.hashCode((float[]) value);
			}
			if (value instanceof int[]) {
				return Arrays.hashCode((int[]) value);
			}
			if (value instanceof long[]) {
				return Arrays.hashCode((long[]) value);
			}
			if (value instanceof short[]) {
				return Arrays.hashCode((short[]) value);
			}
			if (value instanceof Object[]) {
				return Arrays.hashCode((Object[]) value);
			}
			return value.hashCode();
		}

		public int getHashCode() {
			return this.hashCode;
		}

	}

}
