/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.service.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Container for HTTP Service type registrations, initially storing HTTP Service
 * type names as {@link Registration}s, and later exposing access to those
 * registrations as {@link HttpServiceGroup}s via {@link #groups()}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
final class GroupsMetadata {

	private final Map<String, Registration> groupMap;

	public GroupsMetadata() {
		this(Collections.emptyList());
	}

	GroupsMetadata(Iterable<Registration> registrations) {
		this.groupMap = new LinkedHashMap<>();
		registrations.forEach(registration -> this.groupMap.put(registration.name(), registration));
	}

	/**
	 * Create a registration for the given group name, or return an existing
	 * registration. If there is an existing registration, merge the client
	 * types after checking they don't conflict.
	 */
	public Registration getOrCreateGroup(String groupName, HttpServiceGroup.ClientType clientType) {
		Registration registration = this.groupMap.get(groupName);
		if (registration == null || registration.clientType() != clientType) {
			registration = (registration != null) ? registration.withClientType(clientType) : new Registration(groupName, clientType);
			this.groupMap.put(groupName, registration);
		}
		return registration;
	}

	/**
	 * Merge all registrations from the given {@link GroupsMetadata} into this one.
	 */
	public void mergeWith(GroupsMetadata other) {
		other.groupMap.values().forEach(registration -> {
			Registration destination = getOrCreateGroup(registration.name(), registration.clientType());
			destination.httpServiceTypeNames().addAll(registration.httpServiceTypeNames());
		});
	}

	/**
	 * Callback to apply to all registrations with access to the group name and
	 * its HTTP service type names.
	 */
	public void forEachRegistration(BiConsumer<String, Set<String>> consumer) {
		this.groupMap.values().forEach(registration ->
				consumer.accept(registration.name(), registration.httpServiceTypeNames()));
	}

	/**
	 * Create the {@link HttpServiceGroup}s for all registrations.
	 */
	public Collection<HttpServiceGroup> groups(@Nullable ClassLoader classLoader) {
		return this.groupMap.values()
			.stream()
			.map(registration -> registration.toHttpServiceGroup(classLoader))
			.toList();
	}

	/**
	 * Return the raw {@link DefaultRegistration registrations}.
	 */
	Stream<Registration> registrations() {
		return this.groupMap.values().stream();
	}


	/**
	 * Registration metadata for an {@link HttpServiceGroup}.
	 */
	record Registration(String name, HttpServiceGroup.ClientType clientType, Set<String> httpServiceTypeNames) {

		Registration(String name, HttpServiceGroup.ClientType clientType) {
			this(name, clientType, new LinkedHashSet<>());
		}

		/**
		 * Update the client type if it does not conflict with the existing value.
		 */
		public Registration withClientType(HttpServiceGroup.ClientType other) {
			if (this.clientType.isUnspecified()) {
				return new Registration(this.name, other, this.httpServiceTypeNames);
			}
			Assert.isTrue(this.clientType == other || other.isUnspecified(),
					"ClientType conflict for HttpServiceGroup '" + this.name + "'");
			return this;
		}

		/**
		 * Create the {@link HttpServiceGroup} from the metadata.
		 */
		public HttpServiceGroup toHttpServiceGroup(@Nullable ClassLoader classLoader) {
			return new RegisteredGroup(this.name,
					(this.clientType.isUnspecified() ? HttpServiceGroup.ClientType.REST_CLIENT : this.clientType),
					this.httpServiceTypeNames.stream()
						.map((typeName) -> ClassUtils.resolveClassName(typeName, classLoader))
						.collect(Collectors.toSet()));
		}
	}


	private record RegisteredGroup(String name, ClientType clientType, Set<Class<?>> httpServiceTypes)
			implements HttpServiceGroup {

	}

}
