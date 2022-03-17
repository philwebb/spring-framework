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

package org.springframework.beans.factory.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.support.SuppliedRootBeanDefinitionBuilder.InstanceSupplier;
import org.springframework.beans.factory.support.SuppliedRootBeanDefinitionBuilder.Using;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SuppliedRootBeanDefinitionBuilder}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
class SuppliedRootBeanDefinitionBuilderTests {

	@Test
	void createWhenClassTypeIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SuppliedRootBeanDefinitionBuilder(null, (Class<?>) null))
				.withMessage("'beanType' must not be null");
	}

	@Test
	void createWhenResolvableTypeIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SuppliedRootBeanDefinitionBuilder(null, (ResolvableType) null))
				.withMessage("'beanType' must not be null");
	}

	@Test
	void createWhenResolvableTypeCannotBeResolvedThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SuppliedRootBeanDefinitionBuilder(null, ResolvableType.NONE))
				.withMessage("'beanType' must be resolvable");
	}

	@Test
	void usingConstructorWhenNoConstructorFoundThrowsException() {

	}

	@Test
	void usingConstructorWhenFound() {

	}

	@Test
	void usingMethodWhenNoMethodFoundThrowsException() {

	}

	@Test
	void usingWhenMethodOnInterface() {

	}

	@Test
	void usingMethodWhenFound() {

	}

	@Nested
	class UsingConstructors { // see InjectedConstructionResolverTests

		@Test
		void getArgumentsWhenNoArgConstructor() {
			Using using = usingConstructor(NoArgConstructor.class);
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			assertThat(getArguments(using, beanFactory)).isEmpty();
		}

		@ParameterizedTest
		@MethodSource("usingSingleArgConstruction")
		void getArgumentsWhenSingleArg(Using using) {
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			beanFactory.registerSingleton("one", "1");
			assertThat(getArguments(using, beanFactory)).containsExactly("1");
		}

		@ParameterizedTest
		@MethodSource("usingSingleArgConstruction")
		void getArgumentsWhenSingleUnsatisfiedArgThrowsException(Using using) {
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			assertThatExceptionOfType(UnsatisfiedDependencyException.class)
					.isThrownBy(() -> getArguments(using, beanFactory)).satisfies(ex -> {
						assertThat(ex.getBeanName()).isEqualTo("test");
						assertThat(ex.getInjectionPoint()).isNotNull();
						assertThat(ex.getInjectionPoint().getMember()).isEqualTo(using.getExecutable());
					});
		}

		@ParameterizedTest
		@MethodSource("usingArrayOfBeansConstruction")
		void getArgumentsWhenSingleArrayOfBeans(Using using) {
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			beanFactory.registerSingleton("one", "1");
			beanFactory.registerSingleton("two", "2");
			Object[] arguments = getArguments(using, beanFactory);
			assertThat(arguments).hasSize(1);
			assertThat((Object[]) arguments[0]).containsExactly("1", "2");
		}

		@ParameterizedTest
		@MethodSource("usingArrayOfBeansConstruction")
		void getArgumentsWhenSingleUnsatisfiedArrayOfBeans(Using using) {
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			Object[] arguments = getArguments(using, beanFactory);
			assertThat(arguments).hasSize(1);
			assertThat((Object[]) arguments[0]).isEmpty();
		}

		private Object[] getArguments(Using using, DefaultListableBeanFactory beanFactory) {
			RootBeanDefinition beanDefinition = new RootBeanDefinition();
			InstanceSupplier supplier = using.createInstanceSupplier(beanFactory, beanDefinition, null);
			return supplier.getArguments();
		}

		static Stream<Arguments> usingSingleArgConstruction() {
			List<Using> using = new ArrayList<>();
			using.add(usingConstructor(SingleArgConstructor.class, String.class));
			using.add(usingFactoryMethod(String.class, SingleArgFactory.class, "single", String.class));
			return using.stream().map(Arguments::of);
		}

		static Stream<Arguments> usingArrayOfBeansConstruction() {
			List<Using> using = new ArrayList<>();
			using.add(usingConstructor(BeansCollectionConstructor.class, String[].class));
			using.add(usingFactoryMethod(String.class, BeansCollectionFactory.class, "array", String[].class));
			return using.stream().map(Arguments::of);
		}

		static Using usingConstructor(Class<?> beanType, Class<?>... parameterTypes) {
			return RootBeanDefinition.supply("test", beanType).usingConstructor(parameterTypes);
		}

		static Using usingFactoryMethod(Class<?> beanType, Class<?> declaringClass, String methodName,
				Class<?>... parameterTypes) {
			return RootBeanDefinition.supply("test", beanType).usingFactoryMethod(declaringClass, methodName,
					parameterTypes);
		}

	}

	static class NoArgConstructor {

	}

	@SuppressWarnings("unused")
	static class SingleArgConstructor {

		public SingleArgConstructor(String s) {
		}

	}

	@SuppressWarnings("unused")
	static class SingleArgFactory {

		String single(String s) {
			return s;
		}

	}

	@SuppressWarnings("unused")
	static class BeansCollectionConstructor {

		public BeansCollectionConstructor(String[] beans) {

		}

		public BeansCollectionConstructor(List<String> beans) {

		}

		public BeansCollectionConstructor(Set<String> beans) {

		}

		public BeansCollectionConstructor(Map<String, String> beans) {

		}

	}

	@SuppressWarnings("unused")
	static class BeansCollectionFactory {

		public String array(String[] beans) {
			return "test";
		}

		public String list(List<String> beans) {
			return "test";
		}

		public String set(Set<String> beans) {
			return "test";
		}

		public String map(Map<String, String> beans) {
			return "test";
		}

	}
}
