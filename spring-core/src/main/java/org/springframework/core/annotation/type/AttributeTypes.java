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
import java.util.Set;

import org.springframework.lang.Nullable;

/**
 * A collection of {@link AttributeType} instances.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public interface AttributeTypes extends Iterable<AttributeType> {

	/**
	 * Constant that can be used when there are no attribute types.
	 */
	AttributeTypes NONE = new SimpleAttributeTypes(Collections.emptyList());

	/**
	 * Return all attribute names that are contained in this collection.
	 * @return all contained attribute names
	 */
	Set<String> attributeNames();

	/**
	 * Return a the matching attribute type for the given attribute name.
	 * @param name the attribute name
	 * @return the attribute type or {@code null}
	 */
	@Nullable
	AttributeType get(String name);

	/**
	 * Create a new in-memory {@link AttributeTypes} containing the specified
	 * types.
	 * @param types the contained types
	 * @return a new {@link AttributeTypes} instance
	 */
	static AttributeTypes of(AttributeType... types) {
		return new SimpleAttributeTypes(types);
	}

}
