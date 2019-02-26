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
import java.util.Map;

import org.springframework.util.ReflectionUtils;

/**
 *
 * @author pwebb
 * @since 5.1
 */
interface AttributeValueExtractor {

    Object apply(Object annotation, Method attribute);

	static Object fromAnnotation(Object annotation, Method attribute) {
		return ReflectionUtils.invokeMethod(attribute, annotation);
	}

	@SuppressWarnings("unchecked")
	static Object fromMap(Object map, Method attribute) {
		return map != null ? ((Map<String, ?>) map).get(attribute.getName()) : null;
	}

	static AttributeValueExtractor forValue(Object value, AttributeValueExtractor fallback) {
		if (value instanceof Annotation) {
			return AttributeValueExtractor::fromAnnotation;
		}
		if (value instanceof Map) {
			return AttributeValueExtractor::fromMap;
		}
		return fallback;
	}


}
