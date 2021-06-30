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

package io.spring.bean.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import io.spring.core.origin.Origin;
import io.spring.core.origin.OriginSupplier;

/*
 * DESIGN NOTES
 *
 * A final immutable class will probably help later with concurrency
 * It also should simplify post processing since an update will result in a replaced
 * rather than mutated registration.
 */

/**
 * A description of a bean may be ultimately instantiated in a
 * {@link BeanContainer}. A {@code BeanRegistration} is an immutable class that
 * provides all the information that a {@link BeanContainer} will need in order
 * to create a fully-wired bean instance.
 * <p>
 * {@code BeanRegistrations} may be registered with a {@link BeanRegistry} and
 * queried via the {@link BeanRepository} interface.
 *
 * @author Phillip Webb
 * @since 6.0.0
 * @param <T> the type
 * @see BeanContainer
 * @see BeanRegistry
 * @see BeanRepository
 */
public final class BeanRegistration<T> implements OriginSupplier {

	private final Origin origin;

	private final String name;

	private final Set<String> aliases;

	private final Class<?> type;

	private final String scope;

	private final BeanInstanceSupplier<T> instanceSupplier;

	private BeanRegistration(Builder<T> builder) {
		this.origin = builder.origin;
		this.name = builder.name;
		this.aliases = (builder.aliases != null)
				? Collections.unmodifiableSet(builder.aliases)
				: Collections.emptySet();
		this.type = builder.type;
		this.scope = builder.scope;
		this.instanceSupplier = builder.instanceSupplier;
	}

	@Override
	public Origin getOrigin() {
		return this.origin;
	}

	public String getName() {
		return (this.name != null) ? this.name : this.type.getName();
	}

	public Set<String> getAliases() {
		return this.aliases;
	}

	public Class<?> getType() {
		return this.type;
	}

	public String getScope() {
		return this.scope;
	}

	public BeanInstanceSupplier<T> getInstanceSupplier() {
		return this.instanceSupplier;
	}

	public BeanRegistration<T> withUpdates(Consumer<Builder<T>> registration) {
		return build(new Builder<>(this), registration);
	}

	public static <T> BeanRegistration<T> of(Consumer<Builder<T>> registration) {
		return build(new Builder<>(), registration);
	}

	private static <T> BeanRegistration<T> build(Builder<T> builder,
			Consumer<Builder<T>> registration) {
		registration.accept(builder);
		return new BeanRegistration<>(builder);
	}

	public static class Builder<T> {

		private Origin origin;

		private String name;

		private Set<String> aliases;

		private Class<?> type;

		private String scope;

		private BeanInstanceSupplier<T> instanceSupplier;

		private Builder() {
		}

		private Builder(BeanRegistration<T> registration) {
			this.origin = registration.origin;
			this.name = registration.name;
			this.aliases = registration.aliases;
			this.type = registration.type;
			this.scope = registration.scope;
			this.instanceSupplier = registration.instanceSupplier;
		}

		public void setOrigin(Origin origin) {
			this.origin = origin;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setAliases(String... aliases) {
			this.aliases = new LinkedHashSet<>(Arrays.asList(aliases));
		}

		public void setAliases(Set<String> aliases) {
			this.aliases = aliases;
		}

		public void setType(Class<?> type) {
			this.type = type;
		}

		public void setScope(String scope) {
			this.scope = scope;
		}

		public void setInstanceSupplier(BeanInstanceSupplier<T> instanceSupplier) {
			this.instanceSupplier = instanceSupplier;
		}

	}

}
