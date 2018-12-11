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

import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * Provides access to low-level type information relating to a single annotation
 * attribute. Similar to inspecting an annotation {@link Method}, but may be
 * implemented without using reflection.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public interface AttributeType {

	/**
	 * Return the name of the attribute.
	 * @return the attribute name
	 */
	String getAttributeName();

	/**
	 * Return the class name of the attribute type. The returned type must align
	 * with <a href=
	 * "https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-9.6.1">
	 * section 9.6.1</a> of the Java language specification, namely:
	 * <ul>
	 * <li>A primitive type</li>
	 * <li>A String</li>
	 * <li>A Class</li>
	 * <li>An enum type</li>
	 * <li>An annotation type</li>
	 * <li>An array whose component type is one of the preceding types</li>
	 * </ul>
	 * @return the attribute type
	 */
	String getClassName();

	/**
	 * Return any annotations declared on the attribute.
	 * @return the attribute annotations
	 */
	DeclaredAnnotations getDeclaredAnnotations();

	/**
	 * Return the default value for the attribute or {@code null} if the value
	 * must be specified by the user. The resulting values must be one of the
	 * types supported by {@link DeclaredAttributes#get(String)}.
	 * @return the default value or {@code null}
	 */
	@Nullable
	Object getDefaultValue();

	/**
	 * Create an in-memory {@link AttributeType} with the specified values.
	 * @param attributeName the attribute name
	 * @param className the class name of the attribute type
	 * @param declaredAnnotations the annotations declared on the attribute
	 * method
	 * @param defaultValue the default value, or {@code #null}
	 * @return a new {@link AnnotationType} instance
	 */
	static AttributeType of(String attributeName, String className,
			DeclaredAnnotations declaredAnnotations, @Nullable Object defaultValue) {
		return new SimpleAttributeType(attributeName, className, declaredAnnotations,
				defaultValue);
	}

}
