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

/**
 * Provides access to low-level type information relating to a single
 * annotation. Similar to inspecting an annotation {@link Class}, but may be
 * implemented without using reflection.
 *
 * @author Phillip Webb
 * @since 5.1
 * @see AttributeType
 */
public interface AnnotationType {

	/**
	 * Return the class name of the annotation type.
	 * @return the class name
	 */
	String getClassName();

	/**
	 * Return any type-level meta-annotations declared on this annotation.
	 * @return the meta-annotations
	 */
	DeclaredAnnotations getDeclaredAnnotations();

	/**
	 * Return the attribute types of the annotation.
	 * @return attribute types
	 */
	AttributeTypes getAttributeTypes();

	/**
	 * Create a new in-memory {@link AnnotationType} with the specific values.
	 * @param className the annotation class name
	 * @param declaredAnnotations the annotations declared on the annotation itself
	 * @param attributeTypes the annotation attribute types
	 * @return a new {@link AnnotationType} instance
	 */
	static AnnotationType of(String className, DeclaredAnnotations declaredAnnotations,
			AttributeTypes attributeTypes) {
		return new SimpleAnnotationType(className, declaredAnnotations, attributeTypes);
	}

}
