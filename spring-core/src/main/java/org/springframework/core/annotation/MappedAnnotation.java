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
import java.util.function.Predicate;

import org.springframework.core.annotation.type.AnnotationType;
import org.springframework.core.annotation.type.AttributeType;
import org.springframework.core.annotation.type.DeclaredAttributes;

/**
 * {@link MergedAnnotation} backed by a {@link AnnotationTypeMapping}.
 *
 * @author Phillip Webb
 * @since 5.1
 * @param <A> the annotation type
 */
class MappedAnnotation<A extends Annotation> extends AbstractMergedAnnotation<A> {

	private final AnnotationTypeMapping mapping;

	private final DeclaredAttributes mappedAttributes;

	private final boolean inherted;

	MappedAnnotation(AnnotationTypeMapping mapping, DeclaredAttributes mappedAttributes,
			boolean inherted, Predicate<String> attributeFilter) {
		super(mapping.getResolver(), mapping.getAnnotationType(), attributeFilter);
		this.mapping = mapping;
		this.mappedAttributes = mappedAttributes;
		this.inherted = inherted;
	}

	@Override
	public boolean isDescendant(MergedAnnotation<?> annotation) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean isFromInherited() {
		return this.inherted;
	}

	@Override
	public int getDepth() {
		return this.mapping.getDepth();
	}

	@Override
	public String toString() {
		StringBuilder attributes = new StringBuilder();
		this.mapping.getAnnotationType().getAttributeTypes().forEach(attributeType -> {
			attributes.append(attributes.length() > 0 ? ", " : "");
			attributes.append(toString(attributeType));
		});
		return "@" + getType() + "(" + attributes + ")";
	}

	private String toString(AttributeType attributeType) {
		String name = attributeType.getAttributeName();
		Object value = getAttributeValue(name, true);
		if (value instanceof DeclaredAttributes) {
			value = getAnnotation(name);
		}
		else if (value instanceof DeclaredAttributes[]) {
			value = getAnnotationArray(name);
		}
		if(value.getClass().isArray()) {
			StringBuilder content = new StringBuilder();
			content.append("[");
			for (int i = 0; i < Array.getLength(value); i++) {
				content.append(i > 0 ? ", " : "");
				content.append(Array.get(value, i));
			}
			content.append("]");
			value = content.toString();
		}
		return (value != null) ? name + "=" + value : "";
	}

	@Override
	protected Object getAttributeValue(String attributeName, boolean merged) {
		DeclaredAttributes attributes = merged ? this.mappedAttributes
				: this.mapping.getSource().getAttributes();
		return attributes.get(attributeName);
	}

	@Override
	protected AbstractMergedAnnotation<A> cloneWithAttributeFilter(
			Predicate<String> attributeFilter) {
		return new MappedAnnotation<>(this.mapping, this.mappedAttributes, this.inherted,
				attributeFilter);
	}

	@Override
	protected <T extends Annotation> MergedAnnotation<T> createNested(AnnotationType type,
			DeclaredAttributes attributes) {
		return createNested(new MappableAnnotation(getResolver(), type, attributes));
	}

	private <T extends Annotation> MergedAnnotation<T> createNested(
			MappableAnnotation mappable) {
		AnnotationTypeMappings mappings = AnnotationTypeMappings.get(getResolver(),
				mappable.getAnnotationType());
		AnnotationTypeMapping mapping = mappings.getMapping(
				mappable.getAnnotationType().getClassName());
		return mapping.map(mappable, this.inherted);
	}

}
