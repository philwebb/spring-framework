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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

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

	private final int[] resolvedAnnotationMirrors;

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
		this.resolvedAnnotationMirrors = mapping.getRoot().getMirrorSets().resolve(source,
				annotation, valueExtractor);
		this.useNonMergedValues = false;
		this.attributeFilter = null;
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
		// FIXME resolved mirrors wont change
		return new TypeMappedAnnotation<>(this.source, this.annotation,
				this.valueExtractor, parentMapping, this.aggregateIndex);
	}

	@Override
	public boolean hasDefaultValue(String attributeName) {
		// FIXME
		return false;
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
		int mapped = this.useNonMergedValues ? -1
				: this.mapping.getMappedAttribute(attributeIndex);
		if (mapped != -1) {
			mapped = this.resolvedAnnotationMirrors[mapped];
			Method attribute = this.mapping.getRoot().getAttributes().get(mapped);
			return this.valueExtractor.apply(attribute, this.annotation);
		}
		if (getDepth() > 0) {
			attributeIndex = this.mapping.getResolvedAnnotationMirrors()[attributeIndex];
			Method attribute = this.mapping.getAttributes().get(attributeIndex);
			return ReflectionUtils.invokeMethod(attribute, this.mapping.getAnnotation());
		}
		attributeIndex = this.resolvedAnnotationMirrors[attributeIndex];
		Method attribute = this.mapping.getAttributes().get(attributeIndex);
		return this.valueExtractor.apply(attribute, this.annotation);
	}

	@SuppressWarnings("unchecked")
	private <T> T adapt(@Nullable Object value, Class<?> attributeType, Class<T> type) {
		if (value == null || type.isInstance(value)) {
			return (T) value;
		}
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	static <A extends Annotation> MergedAnnotation<A> from(Object source, A annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		AnnotationTypeMappings mappings = AnnotationTypeMappings.forAnnotationType(
				annotation.annotationType());
		return new TypeMappedAnnotation<>(source, annotation, mappings.get(0), 0);
	}

}
