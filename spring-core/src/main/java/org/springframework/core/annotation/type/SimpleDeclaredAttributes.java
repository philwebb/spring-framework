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

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Simple in-memory {@link DeclaredAttributes} implementation.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see DeclaredAttributes#of
 */
class SimpleDeclaredAttributes extends AbstractDeclaredAttributes {

	static final SimpleDeclaredAttributes NONE = new SimpleDeclaredAttributes(
			Collections.emptyMap());

	private final Map<String, Object> attributes;

	SimpleDeclaredAttributes(Map<String, ?> attributes) {
		this.attributes = Collections.unmodifiableMap(attributes);
		validate();
	}

	@Override
	public Set<String> names() {
		return this.attributes.keySet();
	}

	@Override
	public Object get(String name) {
		Assert.notNull(name, "Name must not be null");
		Object value = this.attributes.get(name);
		if (value != null && value.getClass().isArray()) {
			value = cloneArray(value);
		}
		return value;
	}

	private Object cloneArray(Object value) {
		if (Array.getLength(value) == 0) {
			return value;
		}
		if (value instanceof boolean[]) {
			return ((boolean[]) value).clone();
		}
		if (value instanceof byte[]) {
			return ((byte[]) value).clone();
		}
		if (value instanceof char[]) {
			return ((char[]) value).clone();
		}
		if (value instanceof double[]) {
			return ((double[]) value).clone();
		}
		if (value instanceof float[]) {
			return ((float[]) value).clone();
		}
		if (value instanceof int[]) {
			return ((int[]) value).clone();
		}
		if (value instanceof long[]) {
			return ((long[]) value).clone();
		}
		if (value instanceof short[]) {
			return ((short[]) value).clone();
		}
		if (value instanceof Object[] && ((Object[]) value).length > 0) {
			return ((Object[]) value).clone();
		}
		return value;
	}

	static SimpleDeclaredAttributes from(@Nullable Map<String, ?> attributes) {
		if (attributes == null) {
			return NONE;
		}
		Map<String, Object> values = new LinkedHashMap<>();
		for (Map.Entry<String, ?> entry : attributes.entrySet()) {
			values.put(entry.getKey(), convert(entry.getValue()));
		}
		return new SimpleDeclaredAttributes(values);
	}

	static SimpleDeclaredAttributes of(DeclaredAttribute... attributes) {
		Assert.notNull(attributes, "Attributes must not be null");
		Map<String, Object> values = new LinkedHashMap<>();
		for (DeclaredAttribute attribute : attributes) {
			values.put(attribute.getName(), attribute.getValue());
		}
		return new SimpleDeclaredAttributes(values);
	}

	static SimpleDeclaredAttributes of(Object... pairs) {
		Assert.notNull(pairs, "Pairs must not be null");
		Assert.isTrue(pairs.length % 2 == 0,
				"Pairs must contain an even number of elements");
		Map<String, Object> values = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			values.put(pairs[i].toString(), pairs[i + 1]);
		}
		return new SimpleDeclaredAttributes(values);
	}

}
