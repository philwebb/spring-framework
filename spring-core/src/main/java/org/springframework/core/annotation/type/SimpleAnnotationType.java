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

import org.springframework.util.Assert;

/**
 * Simple in-memory {@link AnnotationType} implementation.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class SimpleAnnotationType implements AnnotationType {

	private final String className;

	private final DeclaredAnnotations declaredAnnotations;

	private final AttributeTypes attributeTypes;

	SimpleAnnotationType(String className, DeclaredAnnotations declaredAnnotations,
			AttributeTypes attributeTypes) {
		Assert.hasText(className, "ClassName must not be empty");
		Assert.notNull(declaredAnnotations, "DeclaredAnnotations must not be null");
		Assert.notNull(attributeTypes, "AttributeTypes must not be null");
		this.className = className;
		this.declaredAnnotations = declaredAnnotations;
		this.attributeTypes = attributeTypes;
	}

	@Override
	public String getClassName() {
		return this.className;
	}

	@Override
	public DeclaredAnnotations getDeclaredAnnotations() {
		return this.declaredAnnotations;
	}

	@Override
	public AttributeTypes getAttributeTypes() {
		return this.attributeTypes;
	}

	@Override
	public String toString() {
		return AnnotationString.get(this);
	}

}
