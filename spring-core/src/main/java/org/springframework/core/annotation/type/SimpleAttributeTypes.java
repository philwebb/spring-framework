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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import org.springframework.util.Assert;

/**
 * Simple in-memory {@link AttributeTypes} implementation.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class SimpleAttributeTypes implements AttributeTypes {

	private final Map<String, AttributeType> types;

	SimpleAttributeTypes(AttributeType[] types) {
		Assert.notNull(types, "Types must not be null");
		this.types = asMap(Arrays.asList(types));
	}

	SimpleAttributeTypes(Collection<? extends AttributeType> types) {
		Assert.notNull(types, "Types must not be null");
		this.types = asMap(types);
	}

	private Map<String, AttributeType> asMap(Collection<? extends AttributeType> source) {
		Map<String, AttributeType> types = new LinkedHashMap<>();
		source.forEach(type -> types.put(type.getAttributeName(), type));
		return Collections.unmodifiableMap(types);
	}

	@Override
	public Set<String> attributeNames() {
		return this.types.keySet();
	}

	@Override
	public AttributeType get(String name) {
		return this.types.get(name);
	}

	@NotNull
	@Override
	public Iterator<AttributeType> iterator() {
		return this.types.values().iterator();
	}

	@Override
	public String toString() {
		return AnnotationString.get(this);
	}

}
