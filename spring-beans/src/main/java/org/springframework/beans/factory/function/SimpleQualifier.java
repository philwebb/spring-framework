/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.function;

import org.springframework.util.Assert;

/**
 * Simple {@link Qualifier} implementation.
 *
 * @author Phillip Webb
 * @see Qualifier#of(String)
 */
class SimpleQualifier implements Qualifier {

	private String value;

	SimpleQualifier(String value) {
		Assert.hasText(value, "Value must not be empty");
		this.value = value;
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SimpleQualifier other = (SimpleQualifier) obj;
		return this.value.equals(other.value);
	}

	@Override
	public String toString() {
		return this.value;
	}

}
