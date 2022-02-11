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

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.support.SpringFactoriesLoader.FactoryInstantiationFailureHandler;
import org.springframework.core.io.support.SpringFactoriesLoader.FactoryInstantiator;
import org.springframework.core.io.support.SpringFactoriesLoader.ParameterResolver;
import org.springframework.core.log.LogMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SpringFactoriesLoader}.
 *
 * @author Arjen Poutsma
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class SpringFactoriesLoaderTests {

	@BeforeAll
	static void clearCache() {
		SpringFactoriesLoader.cache.clear();
		assertThat(SpringFactoriesLoader.cache).isEmpty();
	}

	@AfterAll
	static void checkCache() {
		assertThat(SpringFactoriesLoader.cache).hasSize(3);
		SpringFactoriesLoader.cache.clear();
	}


	@Test
	void loadFactoryNames() {
		List<String> factoryNames = SpringFactoriesLoader.loadFactoryNames(DummyFactory.class, null);
		assertThat(factoryNames).containsExactlyInAnyOrder(MyDummyFactory1.class.getName(), MyDummyFactory2.class.getName());
	}

	@Test
	void loadFactoriesWithNoRegisteredImplementations() {
		List<Integer> factories = SpringFactoriesLoader.loadFactories(Integer.class, null);
		assertThat(factories).isEmpty();
	}

	@Test
	void loadFactoriesInCorrectOrderWithDuplicateRegistrationsPresent() {
		List<DummyFactory> factories = SpringFactoriesLoader.loadFactories(DummyFactory.class, null);
		assertThat(factories).hasSize(2);
		assertThat(factories.get(0)).isInstanceOf(MyDummyFactory1.class);
		assertThat(factories.get(1)).isInstanceOf(MyDummyFactory2.class);
	}

	@Test
	void loadPackagePrivateFactory() {
		List<DummyPackagePrivateFactory> factories =
				SpringFactoriesLoader.loadFactories(DummyPackagePrivateFactory.class, null);
		assertThat(factories).hasSize(1);
		assertThat(Modifier.isPublic(factories.get(0).getClass().getModifiers())).isFalse();
	}

	@Test
	void attemptToLoadFactoryOfIncompatibleType() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> SpringFactoriesLoader.loadFactories(String.class, null))
			.withMessageContaining("Unable to instantiate factory class "
					+ "[org.springframework.core.io.support.MyDummyFactory1] for factory type [java.lang.String]");
	}

	@Test
	void attemptToLoadFactoryOfIncompatibleTypeWithLoggingFailureHandler() {
		Log logger = mock(Log.class);
		FactoryInstantiationFailureHandler failureHandler = FactoryInstantiationFailureHandler.logging(logger);
		List<String> factories = SpringFactoriesLoader.loadFactories(String.class, null, failureHandler);
		assertThat(factories.isEmpty());
	}

	@Test
	void loadFactoryWithNonDefaultConstructor() {
		ParameterResolver resolver = ParameterResolver.of(String.class, "injected");
		List<DummyFactory> factories = SpringFactoriesLoader.loadFactories(DummyFactory.class, resolver, LimitedClassLoader.constructorArgumentFactories);
		assertThat(factories).hasSize(3);
		assertThat(factories.get(0)).isInstanceOf(MyDummyFactory1.class);
		assertThat(factories.get(1)).isInstanceOf(MyDummyFactory2.class);
		assertThat(factories.get(2)).isInstanceOf(ConstructorArgsDummyFactory.class);
		assertThat(factories).extracting(DummyFactory::getString).containsExactly("Foo", "Bar", "injected");
	}

	@Test
	void loadFactoryWithMultipleConstructors() {
		ParameterResolver resolver = ParameterResolver.of(String.class, "injected");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> SpringFactoriesLoader.loadFactories(DummyFactory.class, resolver, LimitedClassLoader.multipleArgumentFactories))
				.withMessageContaining("Unable to instantiate factory class "
						+ "[org.springframework.core.io.support.MultipleConstructorArgsDummyFactory] for factory type [org.springframework.core.io.support.DummyFactory]")
				.havingRootCause().withMessageContaining("Class [org.springframework.core.io.support.MultipleConstructorArgsDummyFactory] has multiple non-private constructors");
	}

	@Test
	void loadFactoryWithMissingArgumentUsingLoggingFailureHandler() {
		Log logger = mock(Log.class);
		FactoryInstantiationFailureHandler failureHandler = FactoryInstantiationFailureHandler.logging(logger);
		List<DummyFactory> factories = SpringFactoriesLoader.loadFactories(DummyFactory.class, LimitedClassLoader.multipleArgumentFactories, failureHandler);
		assertThat(factories).hasSize(2);
		assertThat(factories.get(0)).isInstanceOf(MyDummyFactory1.class);
		assertThat(factories.get(1)).isInstanceOf(MyDummyFactory2.class);
	}


	@Nested
	class FactoryInstantiationFailureHandlerTests {

		@Test
		void throwingReturnsHandlerThatThrowsIllegalArgumentException() {
			FactoryInstantiationFailureHandler handler = FactoryInstantiationFailureHandler.throwing();
			RuntimeException cause = new RuntimeException();
			assertThatIllegalArgumentException().isThrownBy(() -> handler.handleFailure(
					DummyFactory.class, MyDummyFactory1.class.getName(),
					cause)).withMessageStartingWith("Unable to instantiate factory class").withCause(cause);
		}

		@Test
		void throwingWithFactoryReturnsHandlerThatThrows() {
			FactoryInstantiationFailureHandler handler = FactoryInstantiationFailureHandler.throwing(IllegalStateException::new);
			RuntimeException cause = new RuntimeException();
			assertThatIllegalStateException().isThrownBy(() -> handler.handleFailure(
					DummyFactory.class, MyDummyFactory1.class.getName(),
					cause)).withMessageStartingWith("Unable to instantiate factory class").withCause(cause);
		}

		@Test
		void loggingReturnsHandlerThatLogs() {
			Log logger = mock(Log.class);
			FactoryInstantiationFailureHandler handler = FactoryInstantiationFailureHandler.logging(logger);
			RuntimeException cause = new RuntimeException();
			handler.handleFailure(DummyFactory.class, MyDummyFactory1.class.getName(), cause);
			verify(logger).trace(isA(LogMessage.class), eq(cause));
		}

		@Test
		void handleMessageReturnsHandlerThatAcceptsMessage() {
			List<Throwable> failures = new ArrayList<>();
			List<String> messages = new ArrayList<>();
			FactoryInstantiationFailureHandler handler = FactoryInstantiationFailureHandler.handleMessage((message, failure) -> {
				failures.add(failure);
				messages.add(message.get());
			});
			RuntimeException cause = new RuntimeException();
			handler.handleFailure(DummyFactory.class, MyDummyFactory1.class.getName(), cause);
			assertThat(failures).containsExactly(cause);
			assertThat(messages).hasSize(1);
			assertThat(messages.get(0)).startsWith("Unable to instantiate factory class");
		}

	}


	@Nested
	class ParameterResolverTests {

		@Test
		void ofValueResolvesValue() {
			ParameterResolver resolver = ParameterResolver.of(CharSequence.class, "test");
			assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
			assertThat(resolver.resolve(String.class)).isNull();
			assertThat(resolver.resolve(Integer.class)).isNull();
		}

		@Test
		void ofValueSupplierResolvesValue() {
			ParameterResolver resolver = ParameterResolver.ofSupplied(CharSequence.class, () -> "test");
			assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
			assertThat(resolver.resolve(String.class)).isNull();
			assertThat(resolver.resolve(Integer.class)).isNull();
		}

		@Test
		void fromAdaptsFunction() {
			ParameterResolver resolver = ParameterResolver.from(
					type -> CharSequence.class.equals(type) ? "test" : null);
			assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
			assertThat(resolver.resolve(String.class)).isNull();
			assertThat(resolver.resolve(Integer.class)).isNull();
		}

		@Test
		void andValueReturnsComposite() {
			ParameterResolver resolver = ParameterResolver.of(CharSequence.class, "test").and(Integer.class, 123);
			assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
			assertThat(resolver.resolve(String.class)).isNull();
			assertThat(resolver.resolve(Integer.class)).isEqualTo(123);
		}

		@Test
		void andValueWhenSameTypeReturnsCompositeResolvingFirst() {
			ParameterResolver resolver = ParameterResolver.of(CharSequence.class, "test").and(CharSequence.class, "ignore");
			assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
		}

		@Test
		void andValueSupplierReturnsComposite() {
			ParameterResolver resolver = ParameterResolver.of(CharSequence.class, "test").andSupplied(Integer.class, () -> 123);
			assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
			assertThat(resolver.resolve(String.class)).isNull();
			assertThat(resolver.resolve(Integer.class)).isEqualTo(123);
		}

		@Test
		void andValueSupplierWhenSameTypeReturnsCompositeResolvingFirst() {
			ParameterResolver resolver = ParameterResolver.of(CharSequence.class, "test").andSupplied(CharSequence.class, () -> "ignore");
			assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
		}

		@Test
		void andResolverReturnsComposite() {
			ParameterResolver resolver = ParameterResolver.of(CharSequence.class, "test").and(Integer.class, 123);
			resolver = resolver.and(ParameterResolver.of(CharSequence.class, "ignore").and(Long.class, 234L));
			assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
			assertThat(resolver.resolve(String.class)).isNull();
			assertThat(resolver.resolve(Integer.class)).isEqualTo(123);
			assertThat(resolver.resolve(Long.class)).isEqualTo(234L);
		}

	}

	@Nested
	class FactoryInstantiatorTests {

		private final ParameterResolver resolver = ParameterResolver.of(String.class, "test");

		@Test
		void defaultConstructorCreatesInstance() throws Exception {
			Object instance = FactoryInstantiator.forClass(
					DefaultConstructor.class).instantiate(this.resolver);
			assertThat(instance).isNotNull();
		}

		@Test
		void singleConstructorWithParametersCreatesInstance() throws Exception {
			Object instance = FactoryInstantiator.forClass(
					SingleConstructor.class).instantiate(this.resolver);
			assertThat(instance).isNotNull();
		}

		@Test
		void multiplePrivateAndSingleNonPrivateConstructorsCreatesInstance() throws Exception {
			Object instance = FactoryInstantiator.forClass(
					MultiplePrivateAndSingleNonPrivateConstructors.class).instantiate(this.resolver);
			assertThat(instance).isNotNull();
		}

		@Test
		void multipleNonPrivateConstructorsThrowsException() throws Exception {
			assertThatIllegalStateException().isThrownBy(
					() -> FactoryInstantiator.forClass(MultipleNonPrivateConstructors.class))
				.withMessageContaining("has multiple non-private constructor");
		}

		static class DefaultConstructor {

		}

		static class SingleConstructor {

			SingleConstructor(String param) {
			}

		}

		static class MultiplePrivateAndSingleNonPrivateConstructors {

			MultiplePrivateAndSingleNonPrivateConstructors(String param) {
				this(param, false);
			}

			private MultiplePrivateAndSingleNonPrivateConstructors(String param, boolean extra) {
			}

		}

		static class MultipleNonPrivateConstructors {

			MultipleNonPrivateConstructors(String param) {
				this(param, false);
			}

			MultipleNonPrivateConstructors(String param, boolean extra) {
			}

		}

	}

	private static class LimitedClassLoader extends URLClassLoader {

		private static final ClassLoader constructorArgumentFactories = new LimitedClassLoader("constructor-argument-factories");

		private static final ClassLoader multipleArgumentFactories = new LimitedClassLoader("multiple-arguments-factories");

		LimitedClassLoader(String location) {
			super(new URL[] { toUrl(location) });
		}

		private static URL toUrl(String location) {
			try {
				return new File("src/test/resources/org/springframework/core/io/support/" + location + "/").toURI().toURL();
			}
			catch (MalformedURLException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
