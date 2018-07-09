/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.type.classreading;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link AnnotatedTypeMetadata} implementation for types returned from
 * {@link SimpleMetadataReader}.
 *
 * @author Phillip Webb
 * @since 5.1
 */
abstract class SimpleAnnotatedTypeMetadata implements AnnotatedTypeMetadata {

	@Nullable
	private final ClassLoader classLoader;

	private final LinkedMultiValueMap<String, AnnotationAttributes> annotationAttributes;

	private final Map<String, Set<String>> metaAnnotations;


	SimpleAnnotatedTypeMetadata(ClassLoader classLoader,
			LinkedMultiValueMap<String, AnnotationAttributes> annotationAttributes,
			Map<String, Set<String>> metaAnnotations) {
		this.classLoader = classLoader;
		this.annotationAttributes = annotationAttributes;
		this.metaAnnotations = metaAnnotations;
	}


	@Override
	public boolean isAnnotated(String annotationName) {
		System.err.println(getDescription()+" isAnnotated");
		return (!AnnotationUtils.isInJavaLangAnnotationPackage(annotationName)
				&& this.annotationAttributes.containsKey(annotationName));
	}

	@Override
	public Map<String, Object> getAnnotationAttributes(String annotationName,
			boolean classValuesAsString) {
		System.err.println(getDescription()+" getAnnotationAttributes");
		new RuntimeException().printStackTrace();
		AnnotationAttributes mergedAttributes = AnnotationReadingVisitorUtils.getMergedAnnotationAttributes(
				this.annotationAttributes, this.metaAnnotations, annotationName);
		return (mergedAttributes != null
				? convertClassValues(mergedAttributes, classValuesAsString)
				: null);
	}

	@Override
	@Nullable
	public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName,
			boolean classValuesAsString) {
		System.err.println(getDescription()+" getAllAnnotationAttributes");
		List<AnnotationAttributes> attributes = this.annotationAttributes.get(annotationName);
		return (attributes != null
				? getAllAnnotationAttributes(attributes, classValuesAsString)
				: null);
	}

	protected final LinkedMultiValueMap<String, AnnotationAttributes> getDirectAnnotationAttributes() {
		return this.annotationAttributes;
	}

	protected final Map<String, Set<String>> getMetaAnnotations() {
		System.err.println(getDescription()+" getMetaAnnotations");
		return this.metaAnnotations;
	}

	protected Set<String> getMetaAnnotationTypes(String annotationName) {
		System.err.println(getDescription()+" getMetaAnnotationTypes");
		return this.metaAnnotations.get(annotationName);
	}

	protected boolean hasMetaAnnotation(String metaAnnotationType) {
		System.err.println(getDescription()+" hasMetaAnnotation");
		for (Set<String> metaTypes : this.metaAnnotations.values()) {
			if (metaTypes.contains(metaAnnotationType)) {
				return true;
			}
		}
		return false;
	}

	private MultiValueMap<String, Object> getAllAnnotationAttributes(
			List<AnnotationAttributes> attributesList, boolean classValuesAsString) {
		MultiValueMap<String, Object> allAttributes = new LinkedMultiValueMap<>();
		for (AnnotationAttributes attributes : attributesList) {
			convertClassValues(attributes, classValuesAsString).forEach(allAttributes::add);
		}
		return allAttributes;
	}

	private AnnotationAttributes convertClassValues(AnnotationAttributes attributes,
			boolean classValuesAsString) {
		return AnnotationReadingVisitorUtils.convertClassValues(getDescription(),
				this.classLoader, attributes, classValuesAsString);
	}

	protected abstract String getDescription();

}
