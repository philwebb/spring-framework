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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.ClassReference;
import org.springframework.core.annotation.type.DeclaredAttributes;
import org.springframework.core.annotation.type.EnumValueReference;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link MergedAnnotation} backed by a {@link AnnotationTypeMapping}.
 *
 * @author Phillip Webb
 * @since 5.1
 * @param <A> the annotation type
 */
class TypeMappedAnnotation<A extends Annotation> extends AbstractMergedAnnotation<A> {

	private final AnnotationTypeMapping mapping;

	private final DeclaredAttributes mappedAttributes;

	private final boolean inherited;

	private final Predicate<String> attributeFilter;

	TypeMappedAnnotation(AnnotationTypeMapping mapping,
			DeclaredAttributes mappedAttributes, boolean inherited,
			Predicate<String> attributeFilter) {
		this.mapping = mapping;
		this.mappedAttributes = mappedAttributes;
		this.inherited = inherited;
		this.attributeFilter = attributeFilter;
	}

	@Override
	public boolean isPresent() {
		return true;
	}


	@Override
	public boolean isParentOf(MergedAnnotation<?> annotation) {
		if (annotation == null || annotation.getClass() != getClass()) {
			return false;
		}
		AnnotationTypeMapping candidate = ((TypeMappedAnnotation<?>) annotation).mapping.getParent();
		while (candidate != null) {
			if (candidate == this.mapping) {
				return true;
			}
			candidate = candidate.getParent();
		}
		return false;
	}

	@Override
	public MergedAnnotation<?> getParent() {
		// FIXME
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean isFromInherited() {
		return this.inherited;
	}

	@Override
	public int getDepth() {
		// FIXME
		return 0; //this.mapping.getDepth();
	}

	@Override
	public String getType() {
		return this.mapping.getAnnotationType().getClassName();
	}

	@Override
	public boolean hasNonDefaultValue(String attributeName) {
		return !hasDefaultValue(attributeName);
	}



	@Override
	public <T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName,
			Class<T> type) throws NoSuchElementException {
		return getAnnotation(attributeName);
	}

	protected <T extends Annotation> MergedAnnotation<T> getAnnotation(
			String attributeName) {
		DeclaredAttributes nestedAttributes = getRequiredAttribute(attributeName,
				DeclaredAttributes.class);
		AttributeType attributeType = getAttributeType(attributeName);
		AnnotationType nestedType = AnnotationType.resolve(this.mapping.getClassLoader(),
				attributeType.getClassName());
		return createNested(nestedType, nestedAttributes);
	}

	@Override
	public <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(
			String attributeName, Class<T> type) throws NoSuchElementException {
		return getAnnotationArray(attributeName);
	}

	@SuppressWarnings("unchecked")
	protected final <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(
			String attributeName) {
		DeclaredAttributes[] nestedAttributes = getRequiredAttribute(attributeName,
				DeclaredAttributes[].class);
		AttributeType attributeType = getAttributeType(attributeName);
		String arrayType = attributeType.getClassName();
		String componentType = arrayType.substring(0, arrayType.length() - 2);
		AnnotationType nestedType = AnnotationType.resolve(this.mapping.getClassLoader(),
				componentType);
		MergedAnnotation<T>[] result = new MergedAnnotation[nestedAttributes.length];
		for (int i = 0; i < nestedAttributes.length; i++) {
			result[i] = createNested(nestedType, nestedAttributes[i]);
		}
		return result;
	}

	@Override
	public <T> Optional<T> getAttribute(String attributeName, Class<T> type) {
	}

	@Override
	public <T> Optional<T> getNonMergedAttribute(String attributeName, Class<T> type) {
	}

	@Override
	public MergedAnnotation<A> filterDefaultValues() {
	}

	@Override
	public MergedAnnotation<A> filterAttributes(Predicate<String> attributeFilter) {
		attributeFilter = (this.attributeFilter != null)
				? this.attributeFilter.and(attributeFilter)
				: attributeFilter;
		return new TypeMappedAnnotation<>(this.mapping, this.mappedAttributes,
				this.inherited, attributeFilter);
	}

	@Override
	public Map<String, Object> asMap(MapValues... options) {
		return asMap(null, options);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Map<String, Object>> T asMap(
			Function<MergedAnnotation<?>, T> factory, MapValues... options) {
		Assert.state(isPresent(), "Unable to get map for missing annotation");
		T map = (factory != null) ? factory.apply(this)
				: (T) new LinkedHashMap<String, Object>();
		if (map == null) {
			return null;
		}
		boolean nonMerged = MapValues.NON_MERGED.isIn(options);
		for (AttributeType attributeType : this.mapping.getAnnotationType().getAttributeTypes()) {
			Class<?> type = resolveClassName(attributeType.getClassName());
			type = ClassUtils.resolvePrimitiveIfNecessary(type);
			type = getTypeForMapValueOption(options, type);
			String name = attributeType.getAttributeName();
			Object value = getAttributeValue(name, type, nonMerged);
			if (value != null) {
				map.put(name, getValueForMapValueOption(value, factory, options));
			}
		}
		return (factory != null) ? map : (T) Collections.unmodifiableMap(map);
	}







	private AttributeType getAttributeType(String attributeName) {
		if (this.mapping.getAnnotationType() == null) {
			return null;
		}
		if (this.attributeFilter != null && !this.attributeFilter.test(attributeName)) {
			return null;
		}
		return this.mapping.getAnnotationType().getAttributeTypes().get(attributeName);
	}

	private Class<?> resolveClassName(String className) {
		return ClassUtils.resolveClassName(className, this.mapping.getClassLoader());
	}


	private Object getAttributeValue(String attributeName, boolean nonMerged) {
		DeclaredAttributes attributes = nonMerged ? this.mapping.getAnnotationAttributes()
				: this.mappedAttributes;
		return attributes.get(attributeName);
	}

	private <T extends Annotation> TypeMappedAnnotation<T> createNested(
			AnnotationType type, DeclaredAttributes attributes) {
		AnnotationTypeMapping mapping = getNestedMapping(type);
		// FIXME
		// return mapping.map(attributes, inherited);
		return null;
	}

	private AnnotationTypeMapping getNestedMapping(AnnotationType type) {
		return AnnotationTypeMappings.get(this.mapping.getClassLoader(),
				this.mapping.getRepeatableContainers(), type).getMapping(
						type.getClassName());
	}

}
