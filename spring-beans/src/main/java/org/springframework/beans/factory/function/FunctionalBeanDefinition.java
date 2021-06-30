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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.Scope;
import org.springframework.core.OrderComparator;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.function.InstanceSupplier;

/**
 * A description of a bean may be registered with a {@link FunctionalBeanRegistry} and
 * ultimately instantiated by a {@link FunctionalBeanFactory}. A {@code BeanRegistration}
 * is an immutable class that provides all the information that a
 * {@link FunctionalBeanFactory} will need in order to create fully-wired bean instances.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @param <T> the type
 * @see FunctionalBeanFactory
 * @see FunctionalBeanRegistry
 * @see InjectionContext
 */
public final class FunctionalBeanDefinition<T> {

	private final String name;

	private final FunctionalBeanDefinitionIdentifier identifier = null;

	private final Class<?> type;

	@Nullable
	private final ResolvableType resolvableType;

	@Nullable
	private Object orderSource;

	private final InstanceSupplier<InjectionContext, T> instanceSupplier;

	private final MergedAnnotations annotations;

	private final Set<Qualifier> qualifiers;

	private final String scope;

	private FunctionalBeanDefinition(Builder<T> builder) {
		Assert.hasText(builder.name, "Name must not be empty");
		Assert.notNull(builder.type, "Type must not be null");
		Assert.notNull(builder.instanceSupplier, "InstanceSupplier must not be null");
		this.name = builder.name;
		this.type = builder.type;
		this.resolvableType = builder.resolvableType;
		this.orderSource = builder.orderSource;
		this.instanceSupplier = builder.instanceSupplier;
		this.annotations = MergedAnnotations.from(this.type, SearchStrategy.TYPE_HIERARCHY);
		this.qualifiers = (builder.qualifiers.isEmpty()) ? Collections.emptySet()
				: Collections.unmodifiableSet(new LinkedHashSet<>(builder.qualifiers));
		this.scope = StringUtils.hasText(builder.scope) ? builder.scope : Scope.SINGLETON;
	}

	public String getName() {
		return this.name;
	}

	public FunctionalBeanDefinitionIdentifier getIdentifier() {
		return this.identifier;
	}

	public Class<?> getType() {
		return this.type;
	}

	@Nullable
	public ResolvableType getResolvableType() {
		return this.resolvableType;
	}

	/**
	 * Return an order source for the registration, i.e. an object that should be checked
	 * for an order value as a replacement to the given object.
	 * @return the order source
	 * @see OrderComparator.OrderSourceProvider
	 */
	public Object getOrderSource() {
		return this.orderSource;
	}

	InstanceSupplier<InjectionContext, T> getInstanceSupplier() {
		return this.instanceSupplier;
	}

	public boolean hasName(String name) {
		return this.name.equals(name);
	}

	public boolean hasType(ResolvableType type) {
		if (this.resolvableType != null) {
			return type.isAssignableFrom(this.resolvableType);
		}
		return hasType(type.resolve());
	}

	public boolean hasType(Class<?> type) {
		return type.isAssignableFrom(this.type);
	}

	public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
		return this.annotations.isPresent(annotationType);
	}

	public boolean hasQualifier(Qualifier qualifier) {
		return this.qualifiers.contains(qualifier);
	}

	public String getScope() {
		return this.scope;
	}

	public static <T> FunctionalBeanDefinition<T> of(Consumer<Builder<T>> builder) {
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
		private Object orderSource;

		@Nullable
		private InstanceSupplier<InjectionContext, T> instanceSupplier;

		private Set<Qualifier> qualifiers = new LinkedHashSet<>();

		@Nullable
		private String scope;

		Builder(Consumer<Builder<T>> initialier) {
			initialier.accept(this);
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setType(Class<? extends T> type) {
			this.type = type;
			this.resolvableType = null;
		}

		public void setType(ResolvableType type) {
			this.type = (type != null) ? type.resolve() : null;
			this.resolvableType = type;
		}

		public void setOrderSource(Object orderSource) {
			this.orderSource = orderSource;
		}

		public void setInstanceSupplier(InstanceSupplier<InjectionContext, T> instanceSupplier) {
			this.instanceSupplier = instanceSupplier;
		}

		public void addQualifier(String qualifier) {
			this.qualifiers.add(Qualifier.of(qualifier));
		}

		public void addQualifier(Qualifier qualifier) {
			Assert.notNull(qualifier, "Qualifier must not be null");
			this.qualifiers.add(qualifier);
		}

		public void setScope(String scope) {
			this.scope = scope;
		}

		FunctionalBeanDefinition<T> build() {
			return new FunctionalBeanDefinition<>(this);
		}

	}

}
