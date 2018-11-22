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

import org.springframework.util.Assert;

/**
 * Simple in-memory {@link DeclaredAttributes} implementation.
 *
 * @author Phillip Webb
 * @since 5.1
 * @see DeclaredAttributes#of
 */
class SimpleDeclaredAttributes implements DeclaredAttributes {

	private final Map<String, Object> values;

	SimpleDeclaredAttributes(Map<String, ?> values) {
		this.values = Collections.unmodifiableMap(values);
	}

	SimpleDeclaredAttributes(Object... pairs) {
		Map<String, Object> map = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			map.put(pairs[i].toString(), pairs[i + 1]);
		}
		this.values = Collections.unmodifiableMap(map);
	}

	@Override
	public Object get(String name) {
		Assert.notNull(name, "Name must not be null");
		Object value = this.values.get(name);
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
