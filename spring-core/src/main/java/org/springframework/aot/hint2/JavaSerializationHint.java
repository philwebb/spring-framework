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

package org.springframework.aot.hint2;

import java.io.Serializable;
import java.util.Objects;

import org.springframework.lang.Nullable;

/**
 * An immutable hint that describes the need for serialization at runtime.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 6.0
 * @see SerializationHints
 */
public final class JavaSerializationHint implements ConditionalHint {

	private final TypeReference type;

	@Nullable
	private final TypeReference reachableType;


	JavaSerializationHint(TypeReference type, @Nullable TypeReference reachableType) {
		this.type = type;
		this.reachableType = reachableType;
	}


	/**
	 * Return the {@link TypeReference type} that needs to be serialized using
	 * Java serialization at runtime.
	 * @return a {@link Serializable} type
	 */
	public TypeReference getType() {
		return this.type;
	}

	@Override
	@Nullable
	public TypeReference getReachableType() {
		return this.reachableType;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		JavaSerializationHint other = (JavaSerializationHint) obj;
		return this.type.equals(other.type) && Objects.equals(this.reachableType, other.reachableType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.type, this.reachableType);
	}

}
