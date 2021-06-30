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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.springframework.beans.factory.function.ConcurrentHashFilter.Candidates;
import org.springframework.beans.factory.function.ConcurrentHashFilter.HashCodeConsumer;
import org.springframework.core.ResolvableType;

/**
 * Internal class used to manage {@link FunctionalBeanRegistration} objects.
 *
 * @author Phillip Webb
 */
class FunctionalBeanRegistrations {

	private final Map<FunctionalBeanRegistration<?>, FunctionalBeanRegistration<?>> registrations = new ConcurrentHashMap<>();

	private final Filter<String> byName = new Filter<>(this::extractName);

	private final Filter<String> byType = new Filter<>(this::extractTypeHierarchy);

	private volatile List<FunctionalBeanRegistration<?>> inSequenceOrder;

	<T> void add(FunctionalBeanRegistration<T> registration) {
		this.inSequenceOrder = null;
		addFilters(registration);
		FunctionalBeanRegistration<?> existingRegistration = this.registrations.putIfAbsent(registration, registration);
		if (existingRegistration != null) {
			throw new FunctionalBeanDefinitionOverrideException(registration, existingRegistration);
		}
	}

	private void addFilters(FunctionalBeanRegistration<?> registration) {
		this.byName.add(registration);
		this.byType.add(registration);
	}

	<T> FunctionalBeanRegistration<T> find(BeanSelector<T> selector, boolean nonUniqueAsNull) {
		Candidates<FunctionalBeanRegistration<?>> candidates = findCandidates(selector);
		if (candidates == null) {
			return find(this.registrations.keySet(), selector, nonUniqueAsNull, false);
		}
		if (candidates.isEmpty()) {
			return null;
		}
		return find(candidates, selector, nonUniqueAsNull, true);
	}

	@SuppressWarnings("unchecked")
	private <T> FunctionalBeanRegistration<T> find(Iterable<FunctionalBeanRegistration<?>> candidates,
			BeanSelector<T> selector, boolean nonUniqueAsNull, boolean checkRegistered) {
		FunctionalBeanRegistration<T> result = null;
		List<FunctionalBeanRegistration<T>> duplicates = null;
		for (FunctionalBeanRegistration<?> candidate : candidates) {
			if (checkRegistered && !isRegistered(candidate)) {
				continue;
			}
			if (selector.test(candidate.getDefinition())) {
				if (result != null) {
					if (nonUniqueAsNull) {
						return null;
					}
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

	<T> Stream<FunctionalBeanRegistration<T>> stream(BeanSelector<T> selector) {
		Candidates<FunctionalBeanRegistration<?>> candidates = findCandidates(selector);
		if (candidates == null) {
			return filter(getInSequenceOrder().stream(), selector);
		}
		if (candidates.isEmpty()) {
			return Stream.empty();
		}
		return filter(candidates.stream(), selector).sorted();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> Stream<FunctionalBeanRegistration<T>> filter(Stream<FunctionalBeanRegistration<?>> stream,
			BeanSelector<T> selector) {
		return (Stream) stream.filter(this::isRegistered).filter(candidate -> selector.test(candidate.getDefinition()));
	}

	private boolean isRegistered(FunctionalBeanRegistration<?> candidate) {
		return this.registrations.containsKey(candidate);
	}

	<T> boolean anyMatch(BeanSelector<T> selector) {
		Candidates<FunctionalBeanRegistration<?>> candidates = findCandidates(selector);
		if (candidates == null) {
			this.registrations.entrySet().stream().anyMatch(entry -> isRegistered(entry.getKey()));
		}
		if (candidates.isEmpty()) {
			return false;
		}
		return candidates.stream().anyMatch(this::isRegistered);
	}

	int size() {
		return this.registrations.size();
	}

	String[] getNames() {
		return getInSequenceOrder().stream().map(this::getName).toArray(String[]::new);
	}

	private String getName(FunctionalBeanRegistration<?> registration) {
		return registration.getDefinition().getName();
	}

	private List<FunctionalBeanRegistration<?>> getInSequenceOrder() {
		List<FunctionalBeanRegistration<?>> inSequenceOrder = this.inSequenceOrder;
		if (inSequenceOrder != null) {
			return inSequenceOrder;
		}
		inSequenceOrder = new ArrayList<>(this.registrations.values());
		Collections.sort(inSequenceOrder);
		inSequenceOrder = Collections.unmodifiableList(inSequenceOrder);
		this.inSequenceOrder = inSequenceOrder;
		return inSequenceOrder;
	}

	private Candidates<FunctionalBeanRegistration<?>> findCandidates(BeanSelector<?> selector) {
		if (selector.getPrimarySelectorType() == null) {
			return null;
		}
		switch (selector.getPrimarySelectorType()) {
			case NAME:
				return findCandidatesByName((String) selector.getPrimarySelector());
			case TYPE:
				return findCandidatesByType((Class<?>) selector.getPrimarySelector());
			case RESOLVABLE_TYPE:
				ResolvableType resolvableType = (ResolvableType) selector.getPrimarySelector();
				return findCandidatesByResolvableType(resolvableType);
			case ANNOTATION_TYPE:
				return findCandidatesByAnnotationType((Class<?>) selector.getPrimarySelector());
		}
		return null;
	}

	private Candidates<FunctionalBeanRegistration<?>> findCandidatesByName(String name) {
		return this.byName.findCandidates(name);
	}

	private Candidates<FunctionalBeanRegistration<?>> findCandidatesByType(Class<?> type) {
		return this.byType.findCandidates(type.getName());
	}

	private Candidates<FunctionalBeanRegistration<?>> findCandidatesByResolvableType(ResolvableType resolvableType) {
		Class<?> type = resolvableType.resolve();
		return (type != null) ? findCandidatesByType(type) : null;
	}

	private Candidates<FunctionalBeanRegistration<?>> findCandidatesByAnnotationType(Class<?> annotationType) {
		return null;
	}

	private void extractName(FunctionalBeanRegistration<?> registration, HashCodeConsumer<String> consumer) {
		consumer.accept(registration.getDefinition().getName());
	}

	private void extractTypeHierarchy(FunctionalBeanRegistration<?> registration, HashCodeConsumer<String> consumer) {
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

	private static class Filter<A> extends ConcurrentHashFilter<FunctionalBeanRegistration<?>, A> {

		Filter(HashCodesExtractor<FunctionalBeanRegistration<?>, A> hashCodesExtractor) {
			super(hashCodesExtractor);
		}

	}

}
