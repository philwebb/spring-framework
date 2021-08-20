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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.function.ConcurrentHashFilter.Candidates;
import org.springframework.beans.factory.function.ConcurrentHashFilter.HashCodeConsumer;
import org.springframework.core.ResolvableType;

/**
 * Internal class used to hold a collection of
 * {@link FunctionalBeanRegistration} objects.
 *
 * @author Phillip Webb
 */
class FunctionalBeanRegistrations {

	private final Map<FunctionalBeanRegistration<?>, FunctionalBeanRegistration<?>> registrations = new ConcurrentHashMap<>();

	private final Filter<String> byName = new Filter<>(this::extractName);

	private final Filter<String> byType = new Filter<>(this::extractTypeHierarchy);

	<T> void add(FunctionalBeanRegistration<T> registration) {
		addFilters(registration);
		FunctionalBeanRegistration<?> previousRegistration = this.registrations.putIfAbsent(
				registration, registration);
		if (previousRegistration != null) {
			removeFilters(registration);
			throw new FunctionalBeanDefinitionOverrideException(registration,
					previousRegistration);
		}
	}

	<T> FunctionalBeanRegistration<T> find(BeanSelector<T> selector) {
		Candidates<FunctionalBeanRegistration<?>> candidates = findCandidates(selector);
		if (candidates == null) {
			return find(this.registrations.keySet(), selector, false);
		}
		if (candidates.isEmpty()) {
			return null;
		}
		return find(candidates, selector, true);
	}

	@SuppressWarnings("unchecked")
	private <T> FunctionalBeanRegistration<T> find(
			Iterable<FunctionalBeanRegistration<?>> candidates, BeanSelector<T> selector,
			boolean checkRegistered) {
		FunctionalBeanRegistration<T> result = null;
		List<FunctionalBeanRegistration<T>> duplicates = null;
		for (FunctionalBeanRegistration<?> candidate : candidates) {
			if (checkRegistered && !this.registrations.containsKey(candidate)) {
				continue;
			}
			if (selector.test(candidate.getDefinition())) {
				if (result != null) {
					if (duplicates == null) {
						duplicates = new ArrayList<>();
						duplicates.add(result);
					}
					duplicates.add((FunctionalBeanRegistration<T>) candidate);
				}
				result = (FunctionalBeanRegistration<T>) candidate;
			}
		}
		if (duplicates != null) {
			throw new NoUniqueSelectableBeanDefinitionException(selector, duplicates);
		}
		return result;
	}

	private void addFilters(FunctionalBeanRegistration<?> registration) {
		this.byName.add(registration);
		this.byType.add(registration);
	}

	private void removeFilters(FunctionalBeanRegistration<?> registration) {
		this.byName.remove(registration);
		this.byType.remove(registration);
	}

	private Candidates<FunctionalBeanRegistration<?>> findCandidates(
			BeanSelector<?> selector) {
		switch (selector.getPrimarySelectorType()) {
			case NAME:
				return findCandidatesByName((String) selector.getPrimarySelector());
			case TYPE:
				return findCandidatesByType((Class<?>) selector.getPrimarySelector());
			case RESOLVABLE_TYPE:
				ResolvableType resolvableType = (ResolvableType) selector.getPrimarySelector();
				return findCandidatesByResolvableType(resolvableType);
			case ANNOTATION_TYPE:
				return findCandidatesByAnnotationType(
						(Class<?>) selector.getPrimarySelector());
		}
		return null;
	}

	private Candidates<FunctionalBeanRegistration<?>> findCandidatesByName(String name) {
		return this.byName.findCandidates(name);
	}

	private Candidates<FunctionalBeanRegistration<?>> findCandidatesByType(
			Class<?> type) {
		return this.byType.findCandidates(type.getName());
	}

	private Candidates<FunctionalBeanRegistration<?>> findCandidatesByResolvableType(
			ResolvableType resolvableType) {
		Class<?> type = resolvableType.resolve();
		return (type != null) ? findCandidatesByType(type) : null;
	}

	private Candidates<FunctionalBeanRegistration<?>> findCandidatesByAnnotationType(
			Class<?> annotationType) {
		return null;
	}

	private void extractName(FunctionalBeanRegistration<?> registration,
			HashCodeConsumer<String> consumer) {
		consumer.accept(registration.getDefinition().getName());
	}

	private void extractTypeHierarchy(FunctionalBeanRegistration<?> registration,
			HashCodeConsumer<String> consumer) {
		extractTypeHierarchy(registration.getDefinition().getType(), consumer);
	}

	private void extractTypeHierarchy(Class<?> type, HashCodeConsumer<String> consumer) {
		if (type == null || type == Object.class || type == Serializable.class) {
			return;
		}
		consumer.accept(type.getName());
		for (Class<?> iface : type.getInterfaces()) {
			extractTypeHierarchy(iface, consumer);
		}
		extractTypeHierarchy(type.getSuperclass(), consumer);
	}

	private static class Filter<A>
			extends ConcurrentHashFilter<FunctionalBeanRegistration<?>, A> {

		Filter(HashCodesExtractor<FunctionalBeanRegistration<?>, A> hashCodesExtractor) {
			super(hashCodesExtractor);
		}

	}

}
