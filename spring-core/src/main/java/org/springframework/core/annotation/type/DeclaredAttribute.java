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

import java.util.Map;

import org.springframework.util.Assert;

/**
 * Provides access to a single annotation attribute.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public interface DeclaredAttribute {

	/**
	 * Return the name of the declared attribute.
	 * @return the attribute name
	 */
	String getName();

	/**
	 * Return the value of the declared attribute.
	 * @return the attribute value
	 */
	Object getValue();

	/**
	 * Create a new in-memory {@link DeclaredAttribute} instance from the
	 * specified map entry.
	 * @param entry the source map entry
	 * @return a new {@link DeclaredAttribute} instance
	 */
	static DeclaredAttribute from(Map.Entry<String, Object> entry) {
		Assert.notNull(entry, "Entry must not be null");
		return of(entry.getKey(), entry.getValue());
	}

	/**
	 * Create a new in-memory {@link DeclaredAttribute} instance from the
	 * specified name and value.
	 * @param name the attribute name
	 * @param value the attribute value
	 * @return a new {@link DeclaredAttribute} instance
	 */
	static DeclaredAttribute of(String name, Object value) {
		return new SimpleDeclaredAttribute(name, value);
	}

}
