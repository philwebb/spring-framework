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

import org.springframework.util.Assert;

/**
 * Simple in-memory {@link DeclaredAttribute} implementation.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class SimpleDeclaredAttribute implements DeclaredAttribute {

	private final String name;

	private final Object value;

	SimpleDeclaredAttribute(String name, Object value) {
		Assert.hasText(name, "Name must not be empty");
		Assert.notNull(value, "Value must not be null");
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Object getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return AnnotationString.get(this);
	}

}
