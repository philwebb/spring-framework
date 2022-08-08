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

import org.springframework.lang.Nullable;

/**
 * Base class for registration classes that allow hints to be conditionally
 * applied based on the reachability of a given type.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public abstract class ReachableTypeRegistration<S extends ReachableTypeRegistration<S>> {

	@Nullable
	private TypeReference reachableType;

	public S whenReachable(Class<?> reachableType) {
		return whenReachable(TypeReference.of(reachableType));
	}

	public S whenReachable(String reachableType) {
		return whenReachable(TypeReference.of(reachableType));
	}

	public S whenReachable(TypeReference reachableType) {
		this.reachableType = reachableType;
		return self();
	}

	@Nullable
	protected final TypeReference getReachableType() {
		return this.reachableType;
	}

	@SuppressWarnings("unchecked")
	protected final S self() {
		return (S) this;
	}

}
