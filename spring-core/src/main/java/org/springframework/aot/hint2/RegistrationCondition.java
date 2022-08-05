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

import java.util.function.Consumer;

/**
 * Base class for condition classes that are returned when registering hints.
 * Allows hints to be conditionally applied based on the reachability of a given
 * type.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public abstract class RegistrationCondition<S extends RegistrationCondition<S>> {

	private final Consumer<TypeReference> action;

	RegistrationCondition(Consumer<TypeReference> action) {
		this.action = action;
	}

	public S whenReachable(Class<?> reachableType) {
		return whenReachable(TypeReference.of(reachableType));
	}

	public S whenReachable(String reachableType) {
		return whenReachable(TypeReference.of(reachableType));
	}

	public S whenReachable(TypeReference reachableType) {
		this.action.accept(reachableType);
		return self();
	}

	@SuppressWarnings("unchecked")
	private S self() {
		return (S) this;
	}

}
