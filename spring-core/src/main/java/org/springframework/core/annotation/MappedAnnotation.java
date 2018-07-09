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
import java.util.function.Predicate;

import org.springframework.core.annotation.type.DeclaredAttributes;

/**
 * {@link MergedAnnotation} backed by a {@link AnnotationTypeMapping}.
 *
 * @param <A>
 * @author Phillip Webb
 * @since 5.1
 */
class MappedAnnotation<A extends Annotation> extends AbstractMergedAnnotation<A> {

	private final AnnotationTypeMapping mapping;

	private final DeclaredAttributes mappedAttributes;

	private final boolean inherted;

	MappedAnnotation(AnnotationTypeMapping mapping,
			DeclaredAttributes mappedAttributes, boolean inherted,
			Predicate<String> attributeFilter) {
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
	protected AbstractMergedAnnotation<A> cloneWithAttributeFilter(
			Predicate<String> attributeFilter) {
		return new MappedAnnotation<>(this.mapping, this.mappedAttributes,
				this.inherted, attributeFilter);
	}

	@Override
	protected Object getAttributeValue(String attributeName, boolean merged) {
		DeclaredAttributes attributes = merged ? this.mappedAttributes
				: this.mapping.getSource().getAttributes();
		return attributes.get(attributeName);
	}

}
