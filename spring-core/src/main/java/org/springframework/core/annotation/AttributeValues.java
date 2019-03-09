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
import java.util.function.BiFunction;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Internal utilities for consistent handling of annotation attribute values.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see AttributeMethods
 */
abstract class AttributeValues {

	private AttributeValues() {
	}


	/**
	 * Checks that accessing values will not cause a
	 * {@link TypeNotPresentException} to be raised.
	 * @param annotation the annotation to check
	 * @return {@true} if all values are present
	 * @see #validate(Annotation)
	 */
	public static boolean areValid(Annotation annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				annotation.annotationType());
		for (int i = 0; i < attributes.size(); i++) {
			if (attributes.canThrowTypeNotPresentException(i)) {
				try {
					attributes.get(i).invoke(annotation);
				}
				catch (Throwable ex) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Check the declared attributes of the given annotation, in particular
	 * covering Google App Engine's late arrival of
	 * {@code TypeNotPresentExceptionProxy} for {@code Class} values (instead of
	 * early {@code Class.getAnnotations() failure}.
	 * @param annotation the annotation to validate
	 * @throws IllegalStateException if a declared {@code Class} attribute could
	 * not be read
	 * @see #isValid(Annotation)
	 */
	public static void validate(Annotation annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				annotation.annotationType());
		for (int i = 0; i < attributes.size(); i++) {
			if (attributes.canThrowTypeNotPresentException(i)) {
				try {
					attributes.get(i).invoke(annotation);
				}
				catch (Throwable ex) {
					throw new IllegalStateException(
							"Could not obtain annotation attribute value for "
									+ attributes.get(i).getName() + " declared on "
									+ annotation.annotationType(),
							ex);
				}
			}
		}
	}

	/**
	 * Return if the specified value is equivalent to the default value of the
	 * attribute.
	 * @param value the value to check
	 * @param attribute the annotation attribute
	 * @param valueExtractor the value extractor used to extract value from any
	 * nested annotations
	 * @return {@code true} if the value is equivalent to the default value
	 */
	public static boolean isDefaultValue(@Nullable Object value, Method attribute,
			BiFunction<Method, Object, Object> valueExtractor) {
		return areEquivalent(attribute.getDefaultValue(), value, valueExtractor);
	}

	private static boolean areEquivalent(@Nullable Object value,
			@Nullable Object extractedValue,
			BiFunction<Method, Object, Object> valueExtractor) {
		if (ObjectUtils.nullSafeEquals(value, extractedValue)) {
			return true;
		}
		if (value instanceof Class && extractedValue instanceof String) {
			return areEquivalent((Class<?>) value, (String) extractedValue);
		}
		if (value instanceof Class[] && extractedValue instanceof String[]) {
			return areEquivalent((Class[]) value, (String[]) extractedValue);
		}
		if (value instanceof Annotation) {
			return areEquivalent((Annotation) value, extractedValue, valueExtractor);
		}
		return false;
	}

	private static boolean areEquivalent(Class<?>[] value, String[] extractedValue) {
		if (value.length != extractedValue.length) {
			return false;
		}
		for (int i = 0; i < value.length; i++) {
			if (!areEquivalent(value[i], extractedValue[i])) {
				return false;
			}
		}
		return true;
	}

	private static boolean areEquivalent(Class<?> value, String extractedValue) {
		return value.getName().equals(extractedValue);
	}

	private static boolean areEquivalent(Annotation value, Object extractedValue,
			BiFunction<Method, Object, Object> valueExtractor) {
		AttributeMethods attributes = AttributeMethods.forAnnotationType(
				value.annotationType());
		for (int i = 0; i < attributes.size(); i++) {
			Method attribute = attributes.get(i);
			if (!areEquivalent(ReflectionUtils.invokeMethod(attribute, value),
					valueExtractor.apply(attribute, extractedValue), valueExtractor)) {
				return false;
			}
		}
		return true;
	}

}
