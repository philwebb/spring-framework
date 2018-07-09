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
 * Simple in-memory {@link AttributeType} implementation.
 *
 * @author Phillip Webb
 * @since 5.1
 */
class SimpleAttributeType implements AttributeType {

	private final String attributeName;

	private final String className;

	private final DeclaredAnnotations declaredAnnotations;

	private final Object defaultValue;

	SimpleAttributeType(String attributeName, String className,
			DeclaredAnnotations declaredAnnotations, Object defaultValue) {
		Assert.notNull(attributeName, "AttributeName must not be null");
		Assert.notNull(className, "ClassName must not be null");
		Assert.notNull(declaredAnnotations, "DeclaredAnnotations must not be null");
		this.attributeName = attributeName;
		this.className = className;
		this.declaredAnnotations = declaredAnnotations;
		this.defaultValue = defaultValue;
	}

	@Override
	public String getAttributeName() {
		return this.attributeName;
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
	public Object getDefaultValue() {
		return this.defaultValue;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		this.declaredAnnotations.forEach(annotation -> result.append(annotation + "\n"));
		result.append(this.className + " " + this.attributeName + "()");
		if (this.defaultValue != null) {
			result.append(" default "
					+ new AnnotationToStringCreator().append(this.defaultValue));
		}
		return result.toString();
	}

}
