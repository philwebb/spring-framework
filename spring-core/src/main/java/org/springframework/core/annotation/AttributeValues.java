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

import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Internal utilities for consistent handling of annotation attribute values.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see AttributeMethods
 */
class AttributeValues {

	public static boolean isDefaultValue(Method attribute, Object value,
			BiFunction<Method, Object, Object> valueExtractor) {
		return areEquivalent(attribute.getDefaultValue(), value, valueExtractor);
	}

	private static boolean areEquivalent(Object value, Object extractedValue,
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
