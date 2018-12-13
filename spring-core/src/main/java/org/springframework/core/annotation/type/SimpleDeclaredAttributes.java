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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * Simple in-memory {@link DeclaredAttributes} implementation.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see DeclaredAttributes#of
 */
class SimpleDeclaredAttributes extends AbstractDeclaredAttributes {

	private final Map<String, Object> attributes;

	SimpleDeclaredAttributes(Map<String, ?> attributes) {
		Assert.notNull(attributes, "Attributes must not be null");
		this.attributes = Collections.unmodifiableMap(attributes);
	}

	SimpleDeclaredAttributes(DeclaredAttribute... attributes) {
		Assert.notNull(attributes, "Attributes must not be null");
		Map<String, Object> values = new LinkedHashMap<>();
		for (DeclaredAttribute attribute : attributes) {
			values.put(attribute.getName(), attribute.getValue());
		}
		this.attributes = Collections.unmodifiableMap(values);
	}

	SimpleDeclaredAttributes(Object... pairs) {
		Assert.notNull(pairs, "Pairs must not be null");
		Assert.isTrue(pairs.length % 2 == 0,
				"Pairs must contain an even number of elements");
		Map<String, Object> values = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			values.put(pairs[i].toString(), pairs[i + 1]);
		}
		this.attributes = Collections.unmodifiableMap(values);
	}

	@Override
	public Set<String> names() {
		return this.attributes.keySet();
	}

	@Override
	public Object get(String name) {
		Assert.notNull(name, "Name must not be null");
		Object value = this.attributes.get(name);
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

}
