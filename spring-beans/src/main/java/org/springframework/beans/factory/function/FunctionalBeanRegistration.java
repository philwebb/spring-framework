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

import org.springframework.beans.factory.function.ConcurrentHashFilter.HashCodeConsumer;
import org.springframework.util.Assert;

/**
 * Holds details of a bean that has been registered with a
 * {@link FunctionalBeanRegistry}.
 *
 * @author Phillip Webb
 */
class FunctionalBeanRegistration<T> {

	private int sequence;

	private FunctionBeanDefinition<T> definition;

	public FunctionalBeanRegistration(int sequence,
			FunctionBeanDefinition<T> definition) {
		Assert.notNull(definition, "Definition must not be null");
		this.sequence = sequence;
		this.definition = definition;
	}

	FunctionBeanDefinition<T> getDefinition() {
		return definition;
	}

	@Override
	public int hashCode() {
		return this.definition.getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		FunctionalBeanRegistration<?> other = (FunctionalBeanRegistration<?>) obj;
		return this.definition.getName().equals(other.definition.getName());
	}

	void extractNameHashCode(HashCodeConsumer<String> consumer) {
		consumer.accept(this.definition.getName());
	}

}
