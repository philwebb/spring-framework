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

import java.util.Objects;

import org.springframework.util.Assert;

/**
 * A {@link DeclaredAttributes} value that a reference to a specific class. The
 * contained name may refer to Object or primitive types (e.g.
 * {@code java.lang.Integer} or {@code int}. The {@code []} suffix is used to
 * indicate array type.
 *
 * @author Phillip Webb
 * @since 5.2
 * @see DeclaredAttributes
 * @see EnumValueReference
 */
public final class ClassReference {

	private final String className;

	private ClassReference(String className) {
		this.className = className;
	}

	/**
	 * Return the referenced class name.
	 * @return the class name
	 */
	public String getClassName() {
		return this.className;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Objects.equals(this.className, ((ClassReference) obj).className);
	}

	@Override
	public int hashCode() {
		return this.className.hashCode();
	}

	@Override
	public String toString() {
		return this.className;
	}

	/**
	 * Create a {@link ClassReference} from the given class.
	 * @param classType the source class type
	 * @return a {@link ClassReference} instance.
	 */
	public static ClassReference from(Class<?> classType) {
		Assert.notNull(classType, "ClassType must not be null");
		return of(classType.getName());
	}

	/**
	 * Create a {@link ClassReference} for the given class name.
	 * @param className the source class name
	 * @return a {@link ClassReference} instance.
	 */
	public static ClassReference of(String className) {
		Assert.hasText(className, "ClassName must not be empty");
		return new ClassReference(className);
	}

}
