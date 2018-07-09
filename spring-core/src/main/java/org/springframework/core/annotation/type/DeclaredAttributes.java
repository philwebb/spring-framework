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

import java.util.Collections;

/**
 * Provides access to annotation attributes.
 *
 * @author Phillip Webb
 * @since 5.1
 */
@FunctionalInterface
public interface DeclaredAttributes {

	/**
	 * Constant that can be used when there are no declared attributes.
	 */
	DeclaredAttributes NONE = new SimpleDeclaredAttributes(Collections.emptyMap());

	/**
	 * Return the value of a specific annotation attribute. The resulting
	 * instance must be a type supported by <a href=
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
	 * <p>
	 * The exact java types used to represent these values here are:
	 * <ul>
	 * <li>{@code Byte} / {@code byte[]}</li>
	 * <li>{@code Boolean} / {@code boolean[]}</li>
	 * <li>{@code Character} / {@code char[]}</li>
	 * <li>{@code Short} / {@code short[]}</li>
	 * <li>{@code Integer} / {@code int[]}</li>
	 * <li>{@code Long} / {@code long[]}</li>
	 * <li>{@code Float} / {@code float[]}</li>
	 * <li>{@code Double} / {@code double[]}</li>
	 * <li>{@code String} / {@code String[]}</li>
	 * <li>{@code ClassReference} / {@code ClassReference[]}</li>
	 * <li>{@code EnumValueReference} / {@code EnumValueReference[]}</li>
	 * <li>{@code Attributes} / {@code Attributes[]}</li>
	 * </ul>
	 * <p>
	 * Any returned arrays values can be safely mutated by the caller.
	 * @param name the attribute name
	 * @return the attribute value or {@code null}
	 */
	Object get(String name);

	/**
	 * Create a new in-memory {@link DeclaredAttributes} containing the
	 * specified name/value pairs.
	 * @param pairs the names and values to add, alternating from name to value
	 * @return a new {@link DeclaredAttributes} instance
	 */
	static DeclaredAttributes of(Object... pairs) {
		return new SimpleDeclaredAttributes(pairs);
	}

}
