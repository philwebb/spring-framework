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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.Scope;
import org.springframework.beans.factory.function.ConcurrentHashFilter.HashCodeConsumer;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * Holds details of a bean that has been registered with a {@link FunctionalBeanRegistry}.
 *
 * @author Phillip Webb
 * @param <T> the bean type
 */
class FunctionalBeanRegistration<T> implements Comparable<FunctionalBeanRegistration<?>> {

	private final int sequence;

	private final FunctionalBeanDefinition<T> definition;

	private volatile T beanInstance;

	public FunctionalBeanRegistration(int sequence, FunctionalBeanDefinition<T> definition) {
		Assert.notNull(definition, "Definition must not be null");
		this.sequence = sequence;
		this.definition = definition;
	}

	FunctionalBeanDefinition<T> getDefinition() {
		return this.definition;
	}

	@Override
	public int compareTo(FunctionalBeanRegistration<?> other) {
		return Integer.compare(this.sequence, other.sequence);
	}

	void extractNameHashCode(HashCodeConsumer<String> consumer) {
		consumer.accept(this.definition.getName());
	}

	T getBeanInstance(InjectionContext injectionContext) {
		if (isSingleton()) {
			return getSingleton(injectionContext);
		}
		if (isPrototype()) {
			return getPrototype(injectionContext);
		}
		return getScoped(injectionContext);
	}

	private T getSingleton(InjectionContext injectionContext) {
		T beanInstance = this.beanInstance;
		if (beanInstance == null) {
			synchronized (this) {
				beanInstance = getSuppliedBeanInstance(injectionContext);
				this.beanInstance = beanInstance;
			}
		}
		return beanInstance;
	}

	private T getPrototype(InjectionContext injectionContext) {
		return getSuppliedBeanInstance(injectionContext);
	}

	@SuppressWarnings("unchecked")
	private T getScoped(InjectionContext injectionContext) {
		return (T) getScope().get(this.definition.getName(), () -> getSuppliedBeanInstance(injectionContext));
	}

	private Scope getScope() {
		return null; // FIXME
	}

	private T getSuppliedBeanInstance(InjectionContext injectionContext) {
		try {
			return this.definition.getInstanceSupplier().get(injectionContext);
		}
		catch (Throwable ex) {
			if (ex instanceof BeansException) {
				throw (BeansException) ex;
			}
			throw new BeanInstanceSupplyException(ex);
		}
	}

	boolean isSingleton() {
		return Scope.SINGLETON.equals(this.definition.getScope());
	}

	boolean isPrototype() {
		return Scope.PROTOTYPE.equals(this.definition.getScope());
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

	@Override
	public String toString() {
		return new ToStringCreator(this).append("sequence", this.sequence).append("name",
				this.definition.getName()).toString();
	}

}
