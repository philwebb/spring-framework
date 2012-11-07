/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Internal utility class used to collect all annotation values including those decalared
 * on meta-annotations.
 * @author Phillip Webb
 * @since 3.2
 */
class AnnotationAttributeCollector {

	private final String annotationType;

	private final boolean classValuesAsString;

	private final boolean nestedAnnotationsAsMap;


	public AnnotationAttributeCollector(String annotationType,
			boolean classValuesAsString, boolean nestedAnnotationsAsMap) {
		this.annotationType = annotationType;
		this.classValuesAsString = classValuesAsString;
		this.nestedAnnotationsAsMap = nestedAnnotationsAsMap;
	}


	public MultiValueMap<String, Object> collect(AnnotatedElement element) {
		MultiValueMap<String, Object> attributes = new LinkedMultiValueMap<String, Object>();
		recursivelyCollect(attributes, new HashSet<AnnotatedElement>(), element);
		return attributes;
	}

	private void recursivelyCollect(MultiValueMap<String, Object> attributes,
			Set<AnnotatedElement> visited, AnnotatedElement element) {
		if (visited.add(element)) {
			for (Annotation annotation : element.getAnnotations()) {
				if (annotation.annotationType().getName().equals(this.annotationType)) {
					for (Map.Entry<String, Object> entry : AnnotationUtils.getAnnotationAttributes(
							annotation, this.classValuesAsString,
							this.nestedAnnotationsAsMap).entrySet()) {
						attributes.add(entry.getKey(), entry.getValue());
					}
				}
				recursivelyCollect(attributes, visited, annotation.annotationType());
			}
		}
	}
}
