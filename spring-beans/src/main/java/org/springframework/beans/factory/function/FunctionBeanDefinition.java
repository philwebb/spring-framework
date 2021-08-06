/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.function.Consumer;

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.function.InstanceSupplier;

/**
 * A description of a bean may be registered with a
 * {@link FunctionalBeanRegistry} and ultimately instantiated by a
 * {@link FunctionalBeanFactory}. A {@code BeanRegistration} is an immutable
 * class that provides all the information that a {@link FunctionalBeanFactory}
 * will need in order to create a fully-wired bean instance.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @param <T> the type
 * @see FunctionalBeanFactory
 * @see FunctionalBeanRegistry
 * @see InjectionContext
 */
public final class FunctionBeanDefinition<T> {

	private final String name;

	private final Class<?> type;

	@Nullable
	private final ResolvableType resolvableType;

	private final InstanceSupplier<InjectionContext, T> instanceSupplier;

	private FunctionBeanDefinition(Builder<T> builder) {
		Assert.hasText(builder.name, "Name must not be empty");
		Assert.notNull(builder.type, "Type must not be null");
		Assert.notNull(builder.instanceSupplier, "InstanceSupplier must not be null");
		this.name = builder.name;
		this.type = builder.type;
		this.resolvableType = builder.resolvableType;
		this.instanceSupplier = builder.instanceSupplier;
	}

	String getName() {
		return name;
	}

	Class<?> getType() {
		return type;
	}

	@Nullable
	ResolvableType getResolvableType() {
		return resolvableType;
	}

	InstanceSupplier<InjectionContext, T> getInstanceSupplier() {
		return instanceSupplier;
	}

	static <T> FunctionBeanDefinition<T> of(Consumer<Builder<T>> builder) {
		return new Builder<>(builder).build();
	}

	public static class Builder<T> {

		@Nullable
		private String name;

		@Nullable
		private Class<?> type;

		@Nullable
		private ResolvableType resolvableType;

		@Nullable
		private InstanceSupplier<InjectionContext, T> instanceSupplier;

		Builder(Consumer<Builder<T>> initialier) {
			initialier.accept(this);
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setType(Class<?> type) {
			this.type = type;
			this.resolvableType = null;
		}

		public void setType(ResolvableType type) {
			this.type = (type != null) ? type.resolve() : null;
			this.resolvableType = type;
		}

		public void setInstanceSupplier(
				InstanceSupplier<InjectionContext, T> instanceSupplier) {
			this.instanceSupplier = instanceSupplier;
		}

		FunctionBeanDefinition<T> build() {
			return new FunctionBeanDefinition<>(this);
		}

	}

}
