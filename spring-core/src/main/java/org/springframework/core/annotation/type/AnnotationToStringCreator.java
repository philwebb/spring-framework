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

import java.lang.reflect.Array;

/**
 * Internal utility to create nice looking {@code toString()} results for
 * annotations.
 *
 * @author Phillip Webb
 * @since 5.1
 */
class AnnotationToStringCreator {

	private boolean attributes;

	private final StringBuilder result = new StringBuilder();

	public AnnotationToStringCreator append(String name, Object value) {
		this.attributes = true;
		this.result.append(this.result.length() == 0 ? "" : ", ");
		this.result.append(name);
		this.result.append(" = ");
		return append(value);
	}

	public AnnotationToStringCreator append(Object value) {
		if (value instanceof ClassReference) {
			this.result.append(value + ".class");
		}
		else if (value instanceof String) {
			this.result.append("\"" + value + "\"");
		}
		else if (value instanceof Character) {
			this.result.append("'" + value + "'");
		}
		else if (value.getClass().isArray()) {
			this.result.append("{ ");
			for (int i = 0; i < Array.getLength(value); i++) {
				this.result.append(i > 0 ? ", " : "");
				append(Array.get(value, i));
			}
			this.result.append(" }");
		}
		else {
			this.result.append(value);
		}
		return this;
	}

	@Override
	public String toString() {
		if (this.attributes) {
			return "(" + this.result + ")";
		}
		return this.result.toString();
	}

}
