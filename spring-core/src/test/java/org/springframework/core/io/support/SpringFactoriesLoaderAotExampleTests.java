/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core.io.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.support.SpringFactoriesLoader.FactoryInstantiationFailureHandler;
import org.springframework.core.io.support.SpringFactoriesLoader.ParameterResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example showing how {@link SpringFactoriesLoader} could be implemented with AOT.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class SpringFactoriesLoaderAotExampleTests {

	@Test
	void exampleCall() {
		ParameterResolver resolver = ParameterResolver.of(String.class, "test");
		List<DummyFactory> loaded = SubstituteSpringFactoriesLoader.loadFactories(DummyFactory.class, resolver, null, null);
		assertThat(loaded).hasSize(2);
		assertThat(loaded.get(0)).isInstanceOf(MyDummyFactory1.class);
		assertThat(loaded.get(1)).isInstanceOf(ConstructorArgsDummyFactory.class);
	}

	/**
	 * A substitute method for {@code loadFactories}.
	 */
	class SubstituteSpringFactoriesLoader {

		public static <T> List<T> loadFactories(Class<T> factoryType, @Nullable ParameterResolver parameterResolver,
				@Nullable ClassLoader classLoader, @Nullable FactoryInstantiationFailureHandler failureHandler) {
			FactoryInstantiationFailureHandler failureHandlerToUse = (failureHandler != null)
					? failureHandler
					: FactoryInstantiationFailureHandler.throwing();
			List<T> result = new ArrayList<>();
			for (Instantiator<T> instantiator : CodeGeneratedFromSpringFactories.getInstantiators(factoryType)) {
				T instance = instantiator.instantiate(factoryType, parameterResolver, failureHandlerToUse);
				if (instance != null) {
					result.add(instance);
				}
			}
			AnnotationAwareOrderComparator.sort(result);
			return result;
		}

	}

	/**
	 * Example of code that could be generated.
	 */
	class CodeGeneratedFromSpringFactories {

		private static final Map<Class<?>, List<Instantiator<?>>> INSTANTIATORS;

		static {
			MultiValueMap<Class<?>, Instantiator<?>> instantiators = new LinkedMultiValueMap<>();
			instantiators.add(DummyFactory.class, Instantiator.of(ConstructorArgsDummyFactory.class,
					resolver -> new ConstructorArgsDummyFactory(resolver.resolve(String.class))));
			instantiators.add(DummyFactory.class, Instantiator.of(MyDummyFactory1.class,
					MyDummyFactory1::new));
			INSTANTIATORS = Collections.unmodifiableMap(instantiators);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		static <T> List<Instantiator<T>> getInstantiators(Class<T> factoryType) {
			return (List) INSTANTIATORS.getOrDefault(factoryType, Collections.emptyList());
		}

	}

	/**
	 * Helper class for AOT generated code. Not sure exactly where this would live.
	 */
	@FunctionalInterface
	interface Instantiator<T> {

		T instantiate(Class<T> factoryType, ParameterResolver parameterResolver,
				FactoryInstantiationFailureHandler failureHandler);

		static <T> Instantiator<T> of(Class<T> factoryImplementationType, Supplier<T> supplier) {
			return of(factoryImplementationType, parameterResolver -> supplier.get());
		}

		static <T> Instantiator<T> of(Class<T> factoryImplementationType, Function<ParameterResolver, T> factory) {
			return (factoryType, parameterResolver, failureHandler) -> {
				try {
					return factory.apply(parameterResolver);
				}
				catch (Throwable ex) {
					failureHandler.handleFailure(factoryType, factoryImplementationType.getName(), ex);
					return null;
				}
			};
		}
	}

}
