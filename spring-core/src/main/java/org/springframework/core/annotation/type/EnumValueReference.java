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
 * @since 5.2
 * @see DeclaredAttributes
 * @see ClassReference
 */
public final class EnumValueReference {

	private final String enumType;

	private final String value;

	private EnumValueReference(String enumType, String value) {
		this.enumType = enumType;
		this.value = value;
	}

	/**
	 * Return the enum type class name.
	 * @return the enum type
	 */
	public String getEnumType() {
		return this.enumType;
	}

	/**
	 * Return the value of the enum.
	 * @return the enum value
	 */
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
		return this.value;
	}

	@Override
	public int hashCode() {
		return 31 * this.enumType.hashCode() + this.value.hashCode();
	}

	/**
	 * Create a new {@link EnumValueReference} instance from the specified enum
	 * value.
	 * @param enumValue the source enum value
	 * @return a new {@link EnumValueReference} instance
	 */
	public static EnumValueReference from(Enum<?> enumValue) {
		Assert.notNull(enumValue, "EnumValue must not be null");
		return of(enumValue.getDeclaringClass().getName(), enumValue.name());
	}

	/**
	 * Create a new {@link EnumValueReference} instance for the specified enum
	 * type and value.
	 * @param enumType the enum type
	 * @param value the enum value
	 * @return a new {@link EnumValueReference} instance
	 */
	public static EnumValueReference of(String enumType, String value) {
		Assert.hasText(enumType, "EnumType must not be empty");
		Assert.hasText(value, "Value must not be empty");
		return new EnumValueReference(enumType, value);
	}

}
