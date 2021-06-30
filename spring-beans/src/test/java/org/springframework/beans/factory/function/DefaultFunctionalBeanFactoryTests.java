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

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.Scope;
import org.springframework.beans.factory.function.FunctionalBeanDefinition.Builder;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.util.function.InstanceSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link DefaultFunctionalBeanFactory}.
 *
 * @author Phillip Webb
 */
class DefaultFunctionalBeanFactoryTests {

	private final DefaultFunctionalBeanFactory beanFactory = new DefaultFunctionalBeanFactory();

	@Test
	void registerWhenDefinitionIsNullThrowsException() {
		// FIXME
	}

	@Test
	void registerRegisteresBean() {
		// FIXME
	}

	@Test
	void registerWhenNameAlreadyInUseThrowsException() {
		this.beanFactory.register(Registrations::jdbcService);
		assertThatExceptionOfType(FunctionalBeanDefinitionOverrideException.class).isThrownBy(
				() -> this.beanFactory.register(Registrations::jdbcService));
	}

	@Test
	void registerFromCallsRegistrars() {
		// FIXME
	}

	@Test
	void getParentBeanFactoryWhenNoParentReturnsNull() {
		// FIXME
	}

	@Test
	void getParentBeanFactoryWhenHasParentReturnsParent() {
		// FIXME
	}

	@Test
	void containsLocalBeanWhenHasLocalBeanReturnsTrue() {
		this.beanFactory.register(Registrations::jdbcService);
		assertThat(this.beanFactory.containsLocalBean("jdbcService")).isTrue();
	}

	@Test
	void containsLocalBeanWhenHasNoLocalBeanReturnsFalse() {
		this.beanFactory.register(Registrations::jdbcService);
		assertThat(this.beanFactory.containsLocalBean("mongodbService")).isFalse();
	}

	@Test
	void containsLocalBeanWhenHasNoLocalAndParentHasLocalBeanBeanReturnsFalse() {
		// FIXME
	}

	@Test
	void getBeanWhenHasSingleBeanReturnsBean() {
		this.beanFactory.register(Registrations::jdbcService);
		TestService bean = this.beanFactory.getBean(BeanSelector.byType(TestService.class));
		assertThat(bean).isNotNull();
	}

	@Test
	void getBeanWhenHasNoBeanThrowsException() {
		this.beanFactory.register(Registrations::helper);
		assertThatExceptionOfType(NoSelectableBeanDefinitionException.class).isThrownBy(
				() -> this.beanFactory.getBean(BeanSelector.byType(TestService.class)));
	}

	@Test
	void getBeanWhenHasMultipleBeansThrowsException() {
		this.beanFactory.register(Registrations::jdbcService);
		this.beanFactory.register(Registrations::mongoDbService);
		assertThatExceptionOfType(NoUniqueSelectableBeanDefinitionException.class).isThrownBy(
				() -> this.beanFactory.getBean(BeanSelector.byType(TestService.class)));
	}

	@Test
	void getBeanWhenNoLocalBeanAndParentHasBeanReturnsBean() {
		// FIXME
	}

	@Test
	void getBeanWhenHasCyclicDependencyThrowsException() {
		this.beanFactory.register(builder -> buildDependencyBean(builder, "a", "b"));
		this.beanFactory.register(builder -> buildDependencyBean(builder, "b", "c"));
		this.beanFactory.register(builder -> buildDependencyBean(builder, "c", "a"));
		assertThatExceptionOfType(BeanCurrentlyInCreationException.class).isThrownBy(
				() -> this.beanFactory.getBean("a"));
	}

	private void buildDependencyBean(Builder<Object> builder, String name, String dependency) {
		builder.setName(name);
		builder.setType(Object.class);
		builder.setInstanceSupplier(injectionContext -> {
			injectionContext.getBean(dependency);
			return new Object();
		});
	}

	@Test
	void getBeanWhenSingletonReturnsSameInstance() {
		this.beanFactory.register(Registrations::jdbcService);
		Object bean1 = this.beanFactory.getBean("jdbcService");
		Object bean2 = this.beanFactory.getBean("jdbcService");
		assertThat(bean1).isSameAs(bean2).isInstanceOf(TestJdbcService.class);
	}

	@Test
	void getBeanWhenPrototypeReturnsNewInstanceEachTime() {
		this.beanFactory.register(builder -> {
			Registrations.jdbcService(builder);
			builder.setScope(Scope.PROTOTYPE);
		});
		Object bean1 = this.beanFactory.getBean("jdbcService");
		Object bean2 = this.beanFactory.getBean("jdbcService");
		assertThat(bean1).isNotSameAs(bean2).isInstanceOf(TestJdbcService.class);
	}

	@Test
	void getBeanWithArgsWhenHasSingleBeanReturnsBean() {
		Object[] args = new String[] { "Spring", "Framework" };
		this.beanFactory.register(builder -> {
			builder.setName("jdbcService");
			builder.setType(TestJdbcService.class);
			builder.setInstanceSupplier(injectionContext -> {
				assertThat(injectionContext.getArgs()).isSameAs(args);
				return new TestJdbcService();
			});
		});
		TestService bean = this.beanFactory.getBean(BeanSelector.byType(TestService.class), args);
		assertThat(bean).isNotNull();
	}

	@Test
	void getBeanWithArgsWhenHasNoBeanThrowsException() {
		Object[] args = new String[] { "Spring", "Framework" };
		this.beanFactory.register(Registrations::helper);
		assertThatExceptionOfType(NoSelectableBeanDefinitionException.class).isThrownBy(
				() -> this.beanFactory.getBean(BeanSelector.byType(TestService.class), args));
	}

	@Test
	void getBeanWithArgsWhenHasMultipleBeansThrowsException() {
		Object[] args = new String[] { "Spring", "Framework" };
		this.beanFactory.register(Registrations::jdbcService);
		this.beanFactory.register(Registrations::mongoDbService);
		assertThatExceptionOfType(NoUniqueSelectableBeanDefinitionException.class).isThrownBy(
				() -> this.beanFactory.getBean(BeanSelector.byType(TestService.class), args));
	}

	@Test
	void getBeanWithArgsWhenNoLocalBeanAndParentHasBeanReturnsBean() {
		// FIXME
	}

	@Test
	void getBeanWithRequiredTypeWhenHasSingleBeanReturnsBean() {
		this.beanFactory.register(Registrations::jdbcService);
		TestJdbcService bean = this.beanFactory.getBean(BeanSelector.byType(TestService.class), TestJdbcService.class);
		assertThat(bean).isNotNull();
	}

	@Test
	void getBeanWithRequiredTypeWhenHasNoBeanThrowsException() {
		this.beanFactory.register(Registrations::helper);
		assertThatExceptionOfType(NoSelectableBeanDefinitionException.class).isThrownBy(
				() -> this.beanFactory.getBean(BeanSelector.byType(TestService.class), TestJdbcService.class));
	}

	@Test
	void getBeanWithRequiredTypeWhenHasMultipleBeansThrowsException() {
		this.beanFactory.register(Registrations::jdbcService);
		this.beanFactory.register(Registrations::mongoDbService);
		assertThatExceptionOfType(NoUniqueSelectableBeanDefinitionException.class).isThrownBy(
				() -> this.beanFactory.getBean(BeanSelector.byType(TestService.class), TestJdbcService.class));
	}

	@Test
	void getBeanWithRequiredTypeWhenNoLocalBeanAndParentHasBeanReturnsBean() {
		// FIXME
	}

	@Test
	void getBeanWithRequiredTypeWhenTypeDoesNotMatchThrowsException() {
		this.beanFactory.register(Registrations::mongoDbService);
		assertThatExceptionOfType(NoSelectableBeanDefinitionException.class).isThrownBy(
				() -> this.beanFactory.getBean(BeanSelector.byType(TestService.class), TestJdbcService.class));
	}

	@Test
	void containsBeanWhenContainsSingleMatchReturnsTrue() {
		this.beanFactory.register(Registrations::jdbcService);
		assertThat(this.beanFactory.containsBean(BeanSelector.byType(TestService.class))).isTrue();
	}

	@Test
	void containsBeanWhenContainsMultipleMatchesReturnsTrue() {
		this.beanFactory.register(Registrations::jdbcService);
		this.beanFactory.register(Registrations::mongoDbService);
		assertThat(this.beanFactory.containsBean(BeanSelector.byType(TestService.class))).isTrue();
	}

	@Test
	void containsBeanWhenNoMatchReturnsFalse() {
		this.beanFactory.register(Registrations::helper);
		assertThat(this.beanFactory.containsBean(BeanSelector.byType(TestService.class))).isFalse();
	}

	@Test
	void containsBeanWhenNoLocalMatchAndParentMatchReturnsTrue() {
		// FIXME
	}

	@Test
	void isSingletonWhenContainsSingleSingletonMatchReturnsTrue() {
		// FIXME
	}

	@Test
	void isSingletonWhenContainsSinglePrototypeMatchReturnsFalse() {
		// FIXME
	}

	@Test
	void isSingletonWhenContainsMultipleMatchesThrowsException() {
		// FIXME
	}

	@Test
	void isSingletonWhenContainsNoLocalMatchAndParentSingletonMatchReturnsTrue() {
		// FIXME
	}

	@Test
	void isSingletonWhenContainsNoLocalMatchAndParentPrototypeMatchReturnsFalse() {
		// FIXME
	}

	@Test
	void isPrototypeWhenContainsSinglePrototypeMatchReturnsTrue() {
		// FIXME
	}

	@Test
	void isPrototypeWhenContainsSingleSingletonMatchReturnsFalse() {
		// FIXME
	}

	@Test
	void isPrototypeWhenContainsMultipleMatchesThrowsException() {
		// FIXME
	}

	@Test
	void isPrototypeWhenContainsNoLocalMatchAndParentPrototypeMatchReturnsTrue() {
		// FIXME
	}

	@Test
	void isPrototypeWhenContainsNoLocalMatchAndParentSingletonMatchReturnsFalse() {
		// FIXME
	}

	@Test
	void isTypeMatchWithClassWhenHasSingleMatchAndTypeMatchReturnsTrue() {
		this.beanFactory.register(Registrations::jdbcService);
		assertThat(
				this.beanFactory.isTypeMatch(BeanSelector.byType(TestService.class), TestJdbcService.class)).isTrue();
	}

	@Test
	void isTypeMatchWithClassWhenHasSingleMatchAndNoTypeMatchReturnsFalse() {
		this.beanFactory.register(Registrations::jdbcService);
		assertThat(this.beanFactory.isTypeMatch(BeanSelector.byType(TestService.class),
				TestMongoDbService.class)).isFalse();
	}

	@Test
	void isTypeMatchWithClassWhenHasMultipleMatchesThrowsException() {
		this.beanFactory.register(Registrations::jdbcService);
		this.beanFactory.register(Registrations::mongoDbService);
		assertThatExceptionOfType(NoUniqueSelectableBeanDefinitionException.class).isThrownBy(
				() -> this.beanFactory.isTypeMatch(BeanSelector.byType(TestService.class), TestJdbcService.class));
	}

	@Test
	void isTypeMatchWithClassWhenHasNoMatchAndParentWithMatchReturnsTrue() {
		// FIXME
	}

	@Test
	void isTypeMatchWithResolvableTypeWhenHasSingleMatchAndTypeMatchReturnsTrue() {
		this.beanFactory.register(Registrations::jdbcService);
		assertThat(this.beanFactory.isTypeMatch(BeanSelector.byType(TestService.class),
				ResolvableType.forClass(TestJdbcService.class))).isTrue();
	}

	@Test
	void isTypeMatchWithResolvableTypeWhenHasSingleMatchAndNoTypeMatchReturnsFalse() {
		this.beanFactory.register(Registrations::mongoDbService);
		assertThat(this.beanFactory.isTypeMatch(BeanSelector.byType(TestService.class),
				ResolvableType.forClass(TestJdbcService.class))).isFalse();
	}

	@Test
	void isTypeMatchWithResolvableTypeWhenHasMultipleMatchesThrowsException() {
		this.beanFactory.register(Registrations::jdbcService);
		this.beanFactory.register(Registrations::mongoDbService);
		assertThatExceptionOfType(NoUniqueSelectableBeanDefinitionException.class).isThrownBy(
				() -> this.beanFactory.isTypeMatch(BeanSelector.byType(TestService.class),
						ResolvableType.forClass(TestJdbcService.class)));
	}

	@Test
	void isTypeMatchWithResolvableTypeWhenHasNoMatchAndParentWithMatchReturnsTrue() {
		// FIXME
	}

	@Test
	void getTypeWhenSingleMatchReturnsType() {
		this.beanFactory.register(Registrations::jdbcService);
		assertThat(this.beanFactory.getType(BeanSelector.byType(TestService.class))).isEqualTo(TestJdbcService.class);
	}

	@Test
	void getTypeWhenNoMatchThrowsException() {
		this.beanFactory.register(Registrations::helper);
		assertThatExceptionOfType(NoSelectableBeanDefinitionException.class).isThrownBy(
				() -> this.beanFactory.getType(BeanSelector.byType(TestService.class)));
	}

	@Test
	void getTypeWhenNoMatchAndParentMatchReturnsTrue() {
		// FIXME
	}

	@Test
	void getAliasesReturnsEmptyStringArray() {
		assertThat(this.beanFactory.getAliases("test")).isEmpty();
	}

	@Test
	void containsBeanDefinitionWhenSingleMatchReturnsTrue() {
		this.beanFactory.register(Registrations::jdbcService);
		assertThat(this.beanFactory.containsBeanDefinition(BeanSelector.byType(TestService.class))).isTrue();
	}

	@Test
	void containsBeanDefinitionWhenMultipleMatchReturnsTrue() {
		this.beanFactory.register(Registrations::jdbcService);
		this.beanFactory.register(Registrations::mongoDbService);
		assertThat(this.beanFactory.containsBeanDefinition(BeanSelector.byType(TestService.class))).isTrue();
	}

	@Test
	void containsBeanDefinitionWhenNoMatchReturnsFalse() {
		this.beanFactory.register(Registrations::helper);
		assertThat(this.beanFactory.containsBeanDefinition(BeanSelector.byType(TestService.class))).isFalse();
	}

	@Test
	void containsBeanDefinitionWhenNoMatchAndParentMatchReturnsFalse() {
		// FIXME
	}

	@Test
	void getBeanDefinitionCountReturnsCount() {
		this.beanFactory.register(Registrations::jdbcService);
		this.beanFactory.register(Registrations::mongoDbService);
		this.beanFactory.register(Registrations::helper);
		assertThat(this.beanFactory.getBeanDefinitionCount()).isEqualTo(3);
	}

	@Test
	void getBeanDefinitionNamesReturnsNames() {
		this.beanFactory.register(Registrations::jdbcService);
		this.beanFactory.register(Registrations::mongoDbService);
		this.beanFactory.register(Registrations::helper);
		assertThat(this.beanFactory.getBeanDefinitionNames()).containsExactly("jdbcService", "mongoDbService",
				"helper");
	}

	@Test
	void getBeanDefinitionNamesWhenInDifferentRegistrationOrderReturnsNames() {
		this.beanFactory.register(Registrations::helper);
		this.beanFactory.register(Registrations::mongoDbService);
		this.beanFactory.register(Registrations::jdbcService);
		assertThat(this.beanFactory.getBeanDefinitionNames()).containsExactly("helper", "mongoDbService",
				"jdbcService");
	}

	@Test
	void getBeanDefinitionWhenSingleMatchReturnsDefinition() {
		this.beanFactory.register(Registrations::jdbcService);
		FunctionalBeanDefinition<?> definition = this.beanFactory.getBeanDefinition("jdbcService");
		assertThat(definition).isNotNull();
		assertThat(definition.getType()).isEqualTo(TestJdbcService.class);
	}

	@Test
	void getBeanDefinitionWhenNoMatchThrowsException() {
		this.beanFactory.register(Registrations::helper);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(
				() -> this.beanFactory.getBeanDefinition("jdbcService"));
	}

	@Test
	void getBeanDefinitionWhenNoMatchAndParentMatchThrowsException() {
		// FIXME
	}

	@Test
	void getBeanNamesReturnsNamesOfMatchingBeans() {
		this.beanFactory.register(Registrations::jdbcService);
		this.beanFactory.register(Registrations::mongoDbService);
		this.beanFactory.register(Registrations::helper);
		String[] names = this.beanFactory.getBeanNames(BeanSelector.byType(TestService.class));
		assertThat(names).containsExactly("jdbcService", "mongoDbService");
	}

	@Test
	void getBeansReturnsMatchingBeans() {
		this.beanFactory.register(Registrations::jdbcService);
		this.beanFactory.register(Registrations::mongoDbService);
		this.beanFactory.register(Registrations::helper);
		Map<String, TestService> beans = this.beanFactory.getBeans(BeanSelector.byType(TestService.class));
		assertThat(beans.keySet()).containsExactly("jdbcService", "mongoDbService");
		assertThat(beans.get("jdbcService")).isInstanceOf(TestJdbcService.class);
		assertThat(beans.get("mongoDbService")).isInstanceOf(TestMongoDbService.class);
	}

	@Test
	void resolveEmbeddedValueCallsResolvers() {
		// FIXME
	}

	@Nested
	class GetBeanProviderTests {

		private final DefaultFunctionalBeanFactory beanFactory = DefaultFunctionalBeanFactoryTests.this.beanFactory;

		@Test
		void getObjectWhenSingleMatchReturnsBeanInstance() {
			this.beanFactory.register(Registrations::jdbcService);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			assertThat(beanProvider.getObject()).isNotNull().isInstanceOf(TestJdbcService.class);
		}

		@Test
		void getObjectWhenMultipleMatchesThrowsException() {
			this.beanFactory.register(Registrations::jdbcService);
			this.beanFactory.register(Registrations::mongoDbService);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			assertThatExceptionOfType(NoUniqueSelectableBeanDefinitionException.class).isThrownBy(
					() -> assertThat(beanProvider.getObject()));
		}

		@Test
		void getObjectWhenNoMatchThrowsException() {
			this.beanFactory.register(Registrations::helper);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			assertThatExceptionOfType(NoSelectableBeanDefinitionException.class).isThrownBy(
					() -> assertThat(beanProvider.getObject()));
		}

		@Test
		void getObjectWithArgsWhenSingleMatchReturnsBeanInstance() {
			Object[] args = new String[] { "Spring", "Framework" };
			this.beanFactory.register(builder -> {
				builder.setName("jdbcService");
				builder.setType(TestJdbcService.class);
				builder.setInstanceSupplier(injectionContext -> {
					assertThat(injectionContext.getArgs()).isSameAs(args);
					return new TestJdbcService();
				});
			});
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			assertThat(beanProvider.getObject(args)).isNotNull().isInstanceOf(TestJdbcService.class);
		}

		@Test
		void getObjectWithArgsWhenMultipleMatchesThrowsException() {
			Object[] args = new String[] { "Spring", "Framework" };
			this.beanFactory.register(Registrations::jdbcService);
			this.beanFactory.register(Registrations::mongoDbService);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			assertThatExceptionOfType(NoUniqueSelectableBeanDefinitionException.class).isThrownBy(
					() -> assertThat(beanProvider.getObject(args)));
		}

		@Test
		void getObjectWithArgsWhenNoMatchThrowsException() {
			Object[] args = new String[] { "Spring", "Framework" };
			this.beanFactory.register(Registrations::helper);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			assertThatExceptionOfType(NoSelectableBeanDefinitionException.class).isThrownBy(
					() -> assertThat(beanProvider.getObject(args)));
		}

		@Test
		void getIfAvailableWhenSingleMatchReturnsBean() {
			this.beanFactory.register(Registrations::jdbcService);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			assertThat(beanProvider.getIfAvailable()).isNotNull().isInstanceOf(TestJdbcService.class);
		}

		@Test
		void getIfAvailableWhenNoMatchReturnsNull() {
			this.beanFactory.register(Registrations::helper);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			assertThat(beanProvider.getIfAvailable()).isNull();
		}

		@Test
		void getIfAvailableWhenMultipleMatchesThrowsException() {
			this.beanFactory.register(Registrations::jdbcService);
			this.beanFactory.register(Registrations::mongoDbService);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			assertThatExceptionOfType(NoUniqueSelectableBeanDefinitionException.class).isThrownBy(
					() -> beanProvider.getIfAvailable());
		}

		@Test
		void getIfUniqueWhenSingleMatchReturnsBean() {
			this.beanFactory.register(Registrations::jdbcService);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			assertThat(beanProvider.getIfUnique()).isNotNull().isInstanceOf(TestJdbcService.class);
		}

		@Test
		void getIfUniqueWhenNoMatchReturnsNull() {
			this.beanFactory.register(Registrations::helper);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			assertThat(beanProvider.getIfUnique()).isNull();
		}

		@Test
		void getIfUniqueWhenMultipleMatchesReturnsNull() {
			this.beanFactory.register(Registrations::jdbcService);
			this.beanFactory.register(Registrations::mongoDbService);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			assertThat(beanProvider.getIfUnique()).isNull();
		}

		@Test
		void streamReturnsSelectedBeans() {
			this.beanFactory.register(Registrations::jdbcService);
			this.beanFactory.register(Registrations::mongoDbService);
			this.beanFactory.register(Registrations::helper);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			Stream<Class<?>> classes = beanProvider.stream().map(Object::getClass);
			assertThat(classes).containsExactly(TestJdbcService.class, TestMongoDbService.class);
		}

		@Test
		void streamWhenInDifferentRegistrationOrderReturnsSelectedBeans() {
			this.beanFactory.register(Registrations::helper);
			this.beanFactory.register(Registrations::mongoDbService);
			this.beanFactory.register(Registrations::jdbcService);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			Stream<Class<?>> classes = beanProvider.stream().map(Object::getClass);
			assertThat(classes).containsExactly(TestMongoDbService.class, TestJdbcService.class);
		}

		@Test
		void orderedStreamReturnsBeans() {
			this.beanFactory.register(Registrations::jdbcService);
			this.beanFactory.register(Registrations::mongoDbService);
			this.beanFactory.register(Registrations::helper);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			Stream<Class<?>> classes = beanProvider.orderedStream().map(Object::getClass);
			assertThat(classes).containsExactly(TestJdbcService.class, TestMongoDbService.class);
		}

		@Test
		void orderedStreamWhenInDifferentRegistrationOrderReturnsBeans() {
			this.beanFactory.register(Registrations::helper);
			this.beanFactory.register(Registrations::mongoDbService);
			this.beanFactory.register(Registrations::jdbcService);
			ObjectProvider<TestService> beanProvider = getBeanProvider();
			Stream<Class<?>> classes = beanProvider.orderedStream().map(Object::getClass);
			assertThat(classes).containsExactly(TestJdbcService.class, TestMongoDbService.class);
		}

		private ObjectProvider<TestService> getBeanProvider() {
			return this.beanFactory.getBeanProvider(BeanSelector.byType(TestService.class));
		}

	}

	static class Registrations {

		static void jdbcService(FunctionalBeanDefinition.Builder<Object> builder) {
			builder.setName("jdbcService");
			builder.setType(TestJdbcService.class);
			builder.setInstanceSupplier(InstanceSupplier.of(TestJdbcService::new));
			builder.setOrderSource(new Order(1));
		}

		static void mongoDbService(FunctionalBeanDefinition.Builder<Object> builder) {
			builder.setName("mongoDbService");
			builder.setType(TestMongoDbService.class);
			builder.setInstanceSupplier(InstanceSupplier.of(TestMongoDbService::new));
			builder.setOrderSource(new Order(2));
		}

		static void helper(FunctionalBeanDefinition.Builder<Object> builder) {
			builder.setName("helper");
			builder.setType(TestHelper.class);
			builder.setInstanceSupplier(InstanceSupplier.of(TestHelper::new));
			builder.setOrderSource(new Order(3));
		}

	}

	static interface TestService {
	}

	static class TestJdbcService implements TestService {
	}

	static class TestMongoDbService implements TestService {
	}

	static class TestHelper {
	}

	static class Missing {
	}

	static class Order implements Ordered {

		private final int order;

		Order(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

	}

}
