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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.core.ResolvableType;

/**
 * Subclass of {@link NoUniqueBeanDefinitionException} used when a
 * {@link BeanSelector} does not match a unique {@link FunctionalBeanDefinition
 * bean definition}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class NoUniqueSelectableBeanDefinitionException
		extends NoUniqueBeanDefinitionException {

	<T> NoUniqueSelectableBeanDefinitionException(BeanSelector<T> selector,
			List<FunctionalBeanRegistration<T>> registrations) {
		this(selector, getDefinitions(registrations));
	}

	public <T> NoUniqueSelectableBeanDefinitionException(BeanSelector<T> selector,
			Collection<FunctionalBeanDefinition<T>> beanDefinitionsFound) {
		super((ResolvableType) null, getBeanNames(beanDefinitionsFound));
	}

	private static <T> Collection<FunctionalBeanDefinition<T>> getDefinitions(
			List<FunctionalBeanRegistration<T>> registrations) {
		return registrations.stream().sorted().map(
				FunctionalBeanRegistration::getDefinition).collect(
						Collectors.toCollection(LinkedHashSet::new));
	}

	private static <T> Collection<String> getBeanNames(
			Collection<FunctionalBeanDefinition<T>> definitions) {
		return definitions.stream().map(FunctionalBeanDefinition::getName).collect(
				Collectors.toCollection(LinkedHashSet::new));
	}

}
