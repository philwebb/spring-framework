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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 *
 * @author Phillip Webb
 * @since 5.2
 */
public class TypeMappedAnnotation<A extends Annotation>
		extends AbstractMergedAnnotation<A> {

	@Nullable
	private final Object source;

	private final Annotation rootAnnotation;

	private final AnnotationTypeMapping mapping;

	private final int aggregateIndex;

	private final boolean nonMergedAttributes;

	@Nullable
	private final Predicate<String> attributeFilter;

	@Nullable
	private volatile A synthesizedAnnotation;

	TypeMappedAnnotation(@Nullable Object source, Annotation rootAnnotation,
			AnnotationTypeMapping mapping, int aggregateIndex) {
		this.source = source;
		this.rootAnnotation = rootAnnotation;
		this.mapping = mapping;
		this.aggregateIndex = aggregateIndex;
		this.nonMergedAttributes = false;
		this.attributeFilter = null;
	}

	TypeMappedAnnotation(@Nullable Object source, Annotation rootAnnotation,
			AnnotationTypeMapping mapping, int aggregateIndex,
			boolean nonMergedAttributes, Predicate<String> attributeFilter) {
		this.source = source;
		this.rootAnnotation = rootAnnotation;
		this.mapping = mapping;
		this.aggregateIndex = aggregateIndex;
		this.nonMergedAttributes = nonMergedAttributes;
		this.attributeFilter = attributeFilter;
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
		// FIXME returning a new item here might not be optimal if we add
		// caching at the aggregate level
		// perhaps calling the mapping to get the parent mapping is best
		return new TypeMappedAnnotation<>(this.source, this.rootAnnotation, parentMapping,
				aggregateIndex);
	}

	@Override
	public boolean hasDefaultValue(String attributeName) {
		getRequiredValue(attributeName, Object.class);
		// FIXME
		return false;
		//
		// Object value = getRequiredValue(attributeName, null);
		// AttributeType type = getAttributeType(attributeName, true);
		// return ObjectUtils.nullSafeEquals(value, type.getDefaultValue());
	}



	@Override
	public <T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName,
			Class<T> type) throws NoSuchElementException {
		Assert.notNull(attributeName, "AttributeName must not be null");
		Assert.notNull(type, "Type must not be null");
		// FIXME THIS is not going to work with ASM
		T nestedAnnotation = getRequiredValue(attributeName, type);
		AnnotationTypeMapping nestedMapping = AnnotationTypeMappings.forAnnotationType(
				type).get(0);
		return new TypeMappedAnnotation<>(this.source, nestedAnnotation, nestedMapping,
				this.aggregateIndex);
	}

	@Override
	public <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(
			String attributeName, Class<T> type) throws NoSuchElementException {
		Assert.notNull(attributeName, "AttributeName must not be null");
		Assert.notNull(type, "Type must not be null");
		return null;// FIXME getNestedArray(attributeName, type);
	}

	@Override
	public Optional<Object> getValue(String attributeName) {
		return getValue(attributeName, Object.class);
	}

	@Override
	public <T> Optional<T> getValue(String attributeName, Class<T> type) {
		return Optional.ofNullable(getValue(attributeName, type, false));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Optional<T> getDefaultValue(String attributeName, Class<T> type) {
		Assert.hasText(attributeName, "AttributeName must not be null");
		// AttributeType attributeType = getAttributeType(attributeName, false);
		// if (attributeType == null) {
		// return Optional.empty();
		// }
		// T value = (T) adapt(attributeType.getDefaultValue(), attributeType,
		// type);
		// return Optional.ofNullable(value);
		return null;
	}

	@Override
	public MergedAnnotation<A> filterAttributes(Predicate<String> predicate) {
		Assert.notNull(predicate, "Predicate must not be null");
		if (this.attributeFilter != null) {
			predicate = this.attributeFilter.and(predicate);
		}
		return new TypeMappedAnnotation<>(this.source, this.rootAnnotation, this.mapping,
				this.aggregateIndex, this.nonMergedAttributes, predicate);
	}

	@Override
	public MergedAnnotation<A> withNonMergedAttributes() {
		return new TypeMappedAnnotation<>(this.source, this.rootAnnotation, this.mapping,
				this.aggregateIndex, true, this.attributeFilter);
	}


	@Override
	@SuppressWarnings("unchecked")
	public <T extends Map<String, Object>> T asMap(
			Function<MergedAnnotation<?>, T> factory, MapValues... options) {
		Assert.state(isPresent(), "Unable to get map for missing annotation");
		// T map = (factory != null) ? factory.apply(this)
		// : (T) new LinkedHashMap<String, Object>();
		// if (map == null) {
		// return null;
		// }
		// for (AttributeType attributeType :
		// getAnnotationType().getAttributeTypes()) {
		// Class<?> type = resolveTypeClass(attributeType.getClassName());
		// type = ClassUtils.resolvePrimitiveIfNecessary(type);
		// type = getTypeForMapValueOption(options, type);
		// String name = attributeType.getAttributeName();
		// Object value = getValue(name, type, false);
		// if (value != null) {
		// map.put(name, getValueForMapValueOption(value, factory, options));
		// }
		// }
		// return (factory != null) ? map : (T)
		// Collections.unmodifiableMap(map);
		return null; // FIXME
	}

	@Override
	public A synthesize() {
		if (!isPresent()) {
			throw new NoSuchElementException("Unable to synthesize missing annotation");
		}
		A synthesized = this.synthesizedAnnotation;
		if (synthesized == null) {
			synthesized = createSynthesized();
			this.synthesizedAnnotation = synthesized;
		}
		return synthesized;
	}

	private A createSynthesized() {
		// FIXME Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	protected <T> T getRequiredValue(String attributeName, Class<T> type) {
		return getValue(attributeName, type, true);
	}

	@SuppressWarnings("unchecked")
	private <T> T getValue(String attributeName, @Nullable Class<T> type,
			boolean required) {
		// If it's an annotation and type is an object we should wrap it


		// FIXME
		return null;
	}

}
