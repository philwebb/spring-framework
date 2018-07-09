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
import java.util.Arrays;
import java.util.Objects;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link InvocationHandler} for an {@link Annotation} that Spring has
 * <em>synthesized</em> (i.e., wrapped in a dynamic proxy) with additional
 * functionality.
 *
 * @author Sam Brannen
 * @author Phillip WEbb
 * @since 5.1
 * @see Annotation
 * @see AnnotationAttributeExtractor
 * @see AnnotationUtils#synthesizeAnnotation(Annotation, AnnotatedElement)
 */
class SynthesizedMergedAnnotationInvocationHandler<A extends Annotation>
		implements InvocationHandler {

	private final Class<A> annotationType;

	private final MergedAnnotation<?> annotation;

	SynthesizedMergedAnnotationInvocationHandler(Class<A> annotationType,
			MergedAnnotation<A> mergedAnnotation) {
		this.annotationType = annotationType;
		this.annotation = mergedAnnotation;
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
			return this.annotationType;
		}
		if (isAttributeMethod(method)) {
			return getAttributeValue(method);
		}
		throw new AnnotationConfigurationException(String.format(
				"Method [%s] is unsupported for synthesized annotation type [%s]", method,
				this.annotationType));
	}

	private boolean isAnnotationTypeMethod(Method method) {
		return Objects.equals(method.getName(), "annotationType")
				&& method.getParameterCount() == 0;
	}

	private boolean isAttributeMethod(@Nullable Method method) {
		return method.getParameterCount() == 0 && method.getReturnType() != void.class;
	}

	private Object getAttributeValue(Method method) {
		String name = method.getName();
		Class<?> type = method.getReturnType();
		return this.annotation.getAttribute(name, type).get();
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
//		if (!annotationType().isInstance(other)) {
//			return false;
//		}
//
//		for (Method attributeMethod : AnnotationUtils.getAttributeMethods(
//				annotationType())) {
//			Object thisValue = getAttributeValue(attributeMethod);
//			Object otherValue = ReflectionUtils.invokeMethod(attributeMethod, other);
//			if (!ObjectUtils.nullSafeEquals(thisValue, otherValue)) {
//				return false;
//			}
//		}
		// FIXME we need to also look at equals in mergedannotation
		return false;
	}

	/**
	 * See {@link Annotation#hashCode()} for a definition of the required
	 * algorithm.
	 */
	private int annotationHashCode() {
		int result = 0;
//
//		for (Method attributeMethod : AnnotationUtils.getAttributeMethods(
//				annotationType())) {
//			Object value = getAttributeValue(attributeMethod);
//			int hashCode;
//			if (value.getClass().isArray()) {
//				hashCode = hashCodeForArray(value);
//			}
//			else {
//				hashCode = value.hashCode();
//			}
//			result += (127 * attributeMethod.getName().hashCode()) ^ hashCode;
//		}
		// FIXME
		return result;
	}

	/**
	 * WARNING: we can NOT use any of the {@code nullSafeHashCode()} methods in
	 * Spring's {@link ObjectUtils} because those hash code generation
	 * algorithms do not comply with the requirements specified in
	 * {@link Annotation#hashCode()}.
	 * @param array the array to compute the hash code for
	 */
	private int hashCodeForArray(Object array) {
		if (array instanceof boolean[]) {
			return Arrays.hashCode((boolean[]) array);
		}
		if (array instanceof byte[]) {
			return Arrays.hashCode((byte[]) array);
		}
		if (array instanceof char[]) {
			return Arrays.hashCode((char[]) array);
		}
		if (array instanceof double[]) {
			return Arrays.hashCode((double[]) array);
		}
		if (array instanceof float[]) {
			return Arrays.hashCode((float[]) array);
		}
		if (array instanceof int[]) {
			return Arrays.hashCode((int[]) array);
		}
		if (array instanceof long[]) {
			return Arrays.hashCode((long[]) array);
		}
		if (array instanceof short[]) {
			return Arrays.hashCode((short[]) array);
		}

		// else
		return Arrays.hashCode((Object[]) array);
	}

}
