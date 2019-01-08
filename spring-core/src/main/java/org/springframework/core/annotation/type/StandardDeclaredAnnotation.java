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

package org.springframework.core.annotation.type;

import java.lang.annotation.Annotation;

import org.springframework.util.Assert;

/**
 * {@link DeclaredAnnotation} backed by an {@link Annotation} and implemented
 * using standard Java reflection.
 *
 * @author Phillip webb
 * @since 5.2
 */
class StandardDeclaredAnnotation implements DeclaredAnnotation {

	private final Annotation annotation;

	private final StandardDeclaredAttributes attributes;

	StandardDeclaredAnnotation(Annotation annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		this.annotation = annotation;
		this.attributes = new StandardDeclaredAttributes(annotation);
	}

	@Override
	public String getType() {
		return this.annotation.annotationType().getName();
	}

	@Override
	public DeclaredAttributes getAttributes() {
		return this.attributes;
	}

	@Override
	public String toString() {
		return AnnotationString.get(this);
	}

}
