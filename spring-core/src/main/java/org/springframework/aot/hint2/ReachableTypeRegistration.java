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
import org.springframework.util.Assert;

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


	ReachableTypeRegistration() {
	}


	/**
	 * Only register when the given type is reachable.
	 * @param reachableType the type that must be reachable
	 * @return this instance
	 */
	public S whenReachable(Class<?> reachableType) {
		return whenReachable(TypeReference.of(reachableType));
	}

	/**
	 * Only register when the given type name is reachable.
	 * @param reachableType the type name that must be reachable
	 * @return this instance
	 * @see TypeReference#of(String)
	 */
	public S whenReachable(String reachableType) {
		return whenReachable(TypeReference.of(reachableType));
	}

	/**
	 * Only register when the given type is reachable.
	 * @param reachableType the type that must be reachable
	 * @return this instance
	 */
	public S whenReachable(TypeReference reachableType) {
		Assert.notNull(reachableType, "'reachableType' must not be null");
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
