/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.test.file;

import java.io.IOException;
import java.util.Objects;

/**
 * {@link Content} implementation backed by a {@link String}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class StringContent implements Content {

	private String value;

	StringContent(String value) {
		this.value = value;
	}

	@Override
	public void transferTo(Appendable appendable) throws IOException {
		appendable.append(this.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		StringContent other = (StringContent) obj;
		return Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return this.value;
	}

}
