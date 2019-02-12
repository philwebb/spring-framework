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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.*;

/**
 *
 * @author Phillip Webb
 * @since 5.2
 */
public class TypeMappedAnnotation<A extends Annotation>
		extends AbstractMergedAnnotation<A> {

	@Nullable
	private final Object source;

	private final Object annotation;

	private final BiFunction<Method, Object, Object> valueExtractor;

	private final AnnotationTypeMapping mapping;

	private final int aggregateIndex;

	private final Method[] attributeToMappedAttribute;

	private final boolean useNonMergedValues;

	@Nullable
	private final Predicate<String> attributeFilter;

	@Nullable
	private volatile A synthesizedAnnotation;

	TypeMappedAnnotation(@Nullable Object source, Annotation annotation,
			AnnotationTypeMapping mapping, int aggregateIndex) {
		this(source, annotation, ReflectionUtils::invokeMethod, mapping, aggregateIndex);
	}

	TypeMappedAnnotation(@Nullable Object source, Object annotation,
			BiFunction<Method, Object, Object> valueExtractor,
			AnnotationTypeMapping mapping, int aggregateIndex) {
		this.source = source;
		this.annotation = annotation;
		this.valueExtractor = valueExtractor;
		this.mapping = mapping;
		this.aggregateIndex = aggregateIndex;
		AnnotationAttributeMethods attributes = mapping.getAttributes();
		this.attributeToMappedAttribute = new Method[attributes.size()];
		for (int i = 0; i < attributes.size(); i++) {
			List<Method> mappedAttributes = mapping.getMappedAttributes(i);
			if (mappedAttributes != null) {
				this.attributeToMappedAttribute[i] = getMappedAttribute(mappedAttributes);
			}
		}
		this.useNonMergedValues = false;
		this.attributeFilter = null;
	}

	private Method getMappedAttribute(List<Method> mappedAttributes) {
		if (mappedAttributes.isEmpty()) {
			return null;
		}
		if (mappedAttributes.size() == 1) {
			return mappedAttributes.get(0);
		}
		Method result = null;
		for (int i = 0; i < mappedAttributes.size(); i++) {
			Method candidate = mappedAttributes.get(i);
			Object defaultValue = candidate.getDefaultValue();
			Object value = this.valueExtractor.apply(candidate, this.annotation);
			if (!equivalent(defaultValue, value)) {
				result = candidate;
			}
		}
		return result;
	}

	@Override
	public String getType() {
		return this.mapping.getAnnotationType().getName();
	}

	@Override
	public boolean isPresent() {
		return true;
	}

	@Override
	public int getDepth() {
		return this.mapping.getDepth();
	}

	@Override
	public int getAggregateIndex() {
		return this.aggregateIndex;
	}

	@Override
	public Object getSource() {
		return this.source;
	}

	@Override
	@Nullable
	public MergedAnnotation<?> getParent() {
		AnnotationTypeMapping parentMapping = this.mapping.getParent();
		if (parentMapping == null) {
			return null;
		}
		return new TypeMappedAnnotation<>(this.source, this.annotation,
				this.valueExtractor, parentMapping, this.aggregateIndex);
	}

	@Override
	public boolean hasDefaultValue(String attributeName) {
		int attributeIndex = getAttributeIndex(attributeName, true);
		Object defaultValue = this.mapping.getAttributes().get(
				attributeIndex).getDefaultValue();
		return equivalent(defaultValue, getValue(attributeIndex));
	}

	@Override
	public <T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName,
			Class<T> type) throws NoSuchElementException {
		Assert.notNull(attributeName, "AttributeName must not be null");
		Assert.notNull(type, "Type must not be null");
		int attributeIndex = getAttributeIndex(attributeName, true);
		Class<?> actualType = this.mapping.getAttributes().get(
				attributeIndex).getReturnType();
		Assert.isAssignable(type, actualType, "Invalid annotation type:");
		Object nested = getValue(attributeIndex);
		AnnotationTypeMapping nestedMapping = AnnotationTypeMappings.forAnnotationType(
				type).get(0);
		return new TypeMappedAnnotation<>(this.source, nested, this.valueExtractor,
				nestedMapping, this.aggregateIndex);
	}

	@Override
	public <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(
			String attributeName, Class<T> type) throws NoSuchElementException {
		Assert.notNull(attributeName, "AttributeName must not be null");
		Assert.notNull(type, "Type must not be null");
		return null;// FIXME getNestedArray(attributeName, type);
	}

	@Override
	public <T> Optional<T> getDefaultValue(String attributeName, Class<T> type) {
		int attributeIndex = getAttributeIndex(attributeName, false);
		if (attributeIndex == -1) {
			return Optional.empty();
		}
		Method attribute = this.mapping.getAttributes().get(attributeIndex);
		Object defaultValue = attribute.getDefaultValue();
		Class<?> attributeType = attribute.getReturnType();
		return Optional.ofNullable(adapt(defaultValue, attributeType, type));
	}

	@Override
	public MergedAnnotation<A> filterAttributes(Predicate<String> predicate) {
		Assert.notNull(predicate, "Predicate must not be null");
		if (this.attributeFilter != null) {
			predicate = this.attributeFilter.and(predicate);
		}
		// return new TypeMappedAnnotation<>(this.source, this.attributes,
		// this.aggregateIndex, this.useNonMergedAttributes, predicate);
		return null;
	}

	@Override
	public MergedAnnotation<A> withNonMergedAttributes() {
		// return new TypeMappedAnnotation<>(this.source, this.attributes,
		// this.aggregateIndex, true, this.attributeFilter);
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.core.annotation.MergedAnnotation#asMap(java.util.
	 * function.Function,
	 * org.springframework.core.annotation.MergedAnnotation.MapValues[])
	 */
	@Override
	public <T extends Map<String, Object>> T asMap(
			Function<MergedAnnotation<?>, T> factory, MapValues... options) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.core.annotation.AbstractMergedAnnotation#
	 * createSynthesized()
	 */
	@Override
	protected A createSynthesized() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	protected <T> T getValue(String attributeName, Class<T> type, boolean required) {
		return getValue(getAttributeIndex(attributeName, required), type);
	}

	private int getAttributeIndex(String attributeName, boolean required) {
		Assert.hasText(attributeName, "AttributeName must not be null");
		int attributeIndex = this.mapping.getAttributes().indexOf(attributeName);
		if (attributeIndex == -1 && required) {
			throw new NoSuchElementException("No attribute named '" + attributeName
					+ "' present in merged annotation " + getType());
		}
		return attributeIndex;
	}

	private <T> T getValue(int attributeIndex, Class<T> type) {
		Method attribute = this.mapping.getAttributes().get(attributeIndex);
		Class<?> attributeType = attribute.getReturnType();
		Object value = getValue(attributeIndex);
		return adapt(value, attributeType, type);
	}

	private Object getValue(int attributeIndex) {
		Method mapped = this.attributeToMappedAttribute[attributeIndex];
		if (mapped != null && !this.useNonMergedValues) {
			return this.valueExtractor.apply(mapped, this.annotation);
		}
		Method attribute = this.mapping.getAttributes().get(attributeIndex);
		if (getDepth() == 0) {
			return this.valueExtractor.apply(attribute, this.annotation);
		}
		return ReflectionUtils.invokeMethod(attribute, mapping.getAnnotation());
	}

	private <T> T adapt(@Nullable Object value, Class<?> attributeType, Class<T> type) {
		if (value == null) {
			return null;
		}
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	private boolean equivalent(Object value, Object extractedValue) {
		if (ObjectUtils.nullSafeEquals(value, extractedValue)) {
			return true;
		}
		if (value instanceof Class && extractedValue instanceof String) {
			// FIXME
		}
		if (value instanceof Class[] && extractedValue instanceof String[]) {
			// FIXME
		}
		if (value instanceof Annotation) {
			if (equivalent((Annotation) value, extractedValue)) {
				return true;
			}
		}
		return false;
	}

	private boolean equivalent(Annotation value, Object extractedValue) {
		for (Method attribute : AnnotationAttributeMethods.forAnnotationType(
				value.annotationType())) {
			if (equivalent(ReflectionUtils.invokeMethod(attribute, value),
					this.valueExtractor.apply(attribute, extractedValue))) {
				return true;
			}
		}
		return false;
	}

}
