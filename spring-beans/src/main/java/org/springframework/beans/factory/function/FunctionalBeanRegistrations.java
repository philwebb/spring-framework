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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal class used to hold a collection of
 * {@link FunctionalBeanRegistration} objects.
 *
 * @author Phillip Webb
 */
class FunctionalBeanRegistrations {

	private final Map<FunctionalBeanRegistration<?>, FunctionalBeanRegistration<?>> registrations = new ConcurrentHashMap<>();

	<T> void add(FunctionalBeanRegistration<T> registration) {
		FunctionalBeanRegistration<?> previousRegistration = this.registrations.putIfAbsent(
				registration, registration);
		if (previousRegistration != null) {
			throw new FunctionalBeanDefinitionOverrideException(registration,
					previousRegistration);
		}
	}

	@SuppressWarnings("unchecked")
	<T> FunctionalBeanRegistration<T> find(BeanSelector<T> selector) {
		FunctionalBeanRegistration<?> result = null;
		List<FunctionalBeanRegistration<?>> duplicates = null;
		for (FunctionalBeanRegistration<?> registration : this.registrations.keySet()) {
			if (selector.test(registration.getDefinition())) {
				if (result != null) {
					duplicates = (duplicates != null) ? duplicates : new ArrayList<>();
					duplicates.add(registration);
				}
			}
		}
		if (duplicates != null) {
			// FIXME throw
		}
		if (result == null) {
			// FIXME throw
		}
		return (FunctionalBeanRegistration<T>) result;
	}

}
