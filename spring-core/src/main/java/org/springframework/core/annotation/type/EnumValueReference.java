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
 * A {@link DeclaredAttributes} value that a references to an enum value.
 *
 * @author Phillip Webb
 * @since 5.1
 * @see DeclaredAttributes
 * @see ClassReference
 */
public final class EnumValueReference {

	private final ClassReference enumType;

	private final String value;

	private EnumValueReference(ClassReference enumType, String value) {
		this.enumType = enumType;
		this.value = value;
	}

	public ClassReference getEnumType() {
		return this.enumType;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		EnumValueReference other = (EnumValueReference) obj;
		return Objects.equals(this.enumType, other.enumType)
				&& Objects.equals(this.value, other.value);
	}

	@Override
	public String toString() {
		return this.enumType + "." + this.value;
	}

	@Override
	public int hashCode() {
		return 31 * this.enumType.hashCode() + this.value.hashCode();
	}

	public static EnumValueReference of(Enum<?> enumValue) {
		return of(ClassReference.of(enumValue.getDeclaringClass()), enumValue.name());
	}

	public static EnumValueReference of(String enumType, String value) {
		return of(ClassReference.of(enumType), value);
	}

	public static EnumValueReference of(ClassReference enumType, String value) {
		Assert.notNull(enumType, "EnumClass must not be null");
		Assert.hasLength(value, "Value must not be empty");
		return new EnumValueReference(enumType, value);
	}

}
