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

import java.lang.reflect.Method;
import java.util.function.BiFunction;

import org.springframework.util.ObjectUtils;

/**
 *
 * @author pwebb
 * @since 5.1
 */
public class AttributeValues {

	public static boolean isDefault(Method attribute, Object value,
			BiFunction<Method, Object, Object> valueExtractor) {
		return areEquivalent(attribute.getDefaultValue(),
				value, valueExtractor);
	}

	private static boolean areEquivalent(Object value, Object extractedValue,
			BiFunction<Method, Object, Object> valueExtractor) {
		return ObjectUtils.nullSafeEquals(value, extractedValue);
	}

}
