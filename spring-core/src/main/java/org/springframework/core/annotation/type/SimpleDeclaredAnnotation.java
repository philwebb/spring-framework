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
 * Simple in-memory {@link DeclaredAnnotation} implementation.
 *
 * @author Phillip Webb
 * @since 5.1
 * @see DeclaredAnnotation#of
 */
class SimpleDeclaredAnnotation implements DeclaredAnnotation {

	private final String className;

	private final DeclaredAttributes attributes;

	SimpleDeclaredAnnotation(String className, DeclaredAttributes attributes) {
		Assert.hasText(className, "ClassName must not be empty");
		Assert.notNull(attributes, "Attributes must not be null");
		this.className = className;
		this.attributes = attributes;
	}

	@Override
	public String getClassName() {
		return this.className;
	}

	@Override
	public DeclaredAttributes getAttributes() {
		return this.attributes;
	}

	@Override
	public String toString() {
		return "@" + this.className + this.attributes;
	}
}
