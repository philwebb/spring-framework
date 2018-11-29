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

import java.util.Iterator;
import java.util.Objects;

/**
 * Abstract {@link DeclaredAttributes} base class.
 *
 * @author Phillip Webb
 * @since 5.2
 */
public abstract class AbstractDeclaredAttributes implements DeclaredAttributes {

	@Override
	public Iterator<DeclaredAttribute> iterator() {
		return names().stream().map(this::getDeclaredAttribute).filter(
				Objects::nonNull).iterator();
	}

	private DeclaredAttribute getDeclaredAttribute(String name) {
		Object value = get(name);
		return value != null ? DeclaredAttribute.of(name, value) : null;
	}

	@Override
	public String toString() {
		return AnnotationString.get(this);
	}

}
