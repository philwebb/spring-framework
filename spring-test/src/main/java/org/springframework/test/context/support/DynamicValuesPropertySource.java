/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.lang.Nullable;
import org.springframework.test.context.DynamicPropertyValues;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnumerablePropertySource} backed by a map with dynamically supplied
 * values.
 *
 * @author Phillip Webb
 * @since 5.2.5
 */
class DynamicValuesPropertySource extends EnumerablePropertySource<Map<String, Supplier<Object>>>  {

	private static final Supplier<Object> NO_VALUE = () -> null;


	DynamicValuesPropertySource(String name, Consumer<DynamicPropertyValues> values) {
		super(name, asMap(values));
	}


	@Override
	public Object getProperty(String name) {
		return this.source.getOrDefault(name, NO_VALUE).get();
	}

	@Override
	public boolean containsProperty(String name) {
		return this.source.containsKey(name);
	}

	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.source.keySet());
	}

	@Nullable
	private static Map<String, Supplier<Object>> asMap(Consumer<DynamicPropertyValues> values) {
		if (values == null) {
			return null;
		}
		Map<String, Supplier<Object>> map = new LinkedHashMap<>();
		values.accept((name, value) -> {
			Assert.hasLength(name, "'name' must not be empty");
			Assert.notNull(name, "'value' must not be null");
			map.put(name, value);
		});
		return Collections.unmodifiableMap(map);
	}

}
