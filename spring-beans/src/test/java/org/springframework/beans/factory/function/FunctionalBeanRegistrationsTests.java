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

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.util.function.InstanceSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link FunctionalBeanRegistrations}.
 *
 * @author Phillip Webb
 */
class FunctionalBeanRegistrationsTests {

	private static final BeanSelector<Object> BY_NAME_TEST = BeanSelector.byName("test");

	private FunctionalBeanRegistrations registrations = new FunctionalBeanRegistrations();

	@Test
	void addWhenBeanNameNotPresentAddsRegistration() {
		FunctionalBeanRegistration<?> registration = createTestRegistration("test");
		this.registrations.add(registration);
		assertThat(this.registrations.find(BY_NAME_TEST)).isSameAs(registration);
	}

	@Test
	void addWhenBeanNamePresentThrowsException() {
		FunctionalBeanRegistration<?> existingRegistration = createTestRegistration("test");
		FunctionalBeanRegistration<?> attemptedRegistration = createTestRegistration("test");
		this.registrations.add(existingRegistration);
		assertThatExceptionOfType(FunctionalBeanDefinitionOverrideException.class).isThrownBy(
				() -> this.registrations.add(attemptedRegistration)).satisfies((ex) -> {
					assertThat(ex.getBeanDefinition()).isSameAs(attemptedRegistration.getDefinition());
					assertThat(ex.getExistingDefinition()).isSameAs(existingRegistration.getDefinition());
				});
		assertThat(this.registrations.find(BY_NAME_TEST)).isSameAs(existingRegistration);
	}

	@Test
	void findByNameFindsBean() {
		FunctionalBeanRegistration<?> registration = createTestRegistration("test");
		this.registrations.add(registration);
		this.registrations.add(createTestRegistration("other", OtherBean.class, OtherBean::new));
		assertThat(this.registrations.find(BY_NAME_TEST)).isSameAs(registration);
		assertThat(this.registrations.find(BeanSelector.byName("tset"))).isNull();
	}

	@Test
	void findByTypeFindsBean() {
		FunctionalBeanRegistration<?> registration = createTestRegistration("test");
		this.registrations.add(registration);
		this.registrations.add(createTestRegistration("other", OtherBean.class, OtherBean::new));
		assertThat(this.registrations.find(BeanSelector.byType(TestBean.class))).isSameAs(registration);
		assertThat(this.registrations.find(BeanSelector.byType(AbstractTestBean.class))).isSameAs(registration);
		assertThat(this.registrations.find(BeanSelector.byType(TestBeanInterface.class))).isSameAs(registration);
		assertThat(this.registrations.find(BeanSelector.byType(UnregisteredBean.class))).isNull();
	}

	@Test
	void findWithoutPrimarySelectorFindsBean() {
		FunctionalBeanRegistration<?> registration = createTestRegistration("test");
		this.registrations.add(registration);
		this.registrations.add(createTestRegistration("other", OtherBean.class, OtherBean::new));
		BeanSelector<?> selector = BeanSelector.all().withFilter(
				(candidate) -> candidate.equals(registration.getDefinition()));
		assertThat(this.registrations.find(selector)).isSameAs(registration);
	}

	@Test
	void findWhenNoSelectableBeanReturnsNull() {
		this.registrations.add(createTestRegistration("test"));
		assertThat(this.registrations.find(BeanSelector.byType(UnregisteredBean.class))).isNull();
	}

	@Test
	void findWhenNoUniqueSelectableBeanThrowsException() {
		this.registrations.add(createTestRegistration("test"));
		this.registrations.add(createTestRegistration("other", OtherTestBean.class, OtherTestBean::new));
		assertThatExceptionOfType(NoUniqueSelectableBeanDefinitionException.class).isThrownBy(
				() -> this.registrations.find(BeanSelector.byType(TestBeanInterface.class))).satisfies((ex) -> {
				});
	}

	private FunctionalBeanRegistration<TestBean> createTestRegistration(String name) {
		return createTestRegistration(name, TestBean.class, TestBean::new);
	}

	private <T> FunctionalBeanRegistration<T> createTestRegistration(String name, Class<T> type, Supplier<T> supplier) {
		return new FunctionalBeanRegistration<>(0, FunctionalBeanDefinition.of((builder) -> {
			builder.setName(name);
			builder.setType(type);
			builder.setInstanceSupplier(InstanceSupplier.of(supplier));
		}));
	}

	static class AbstractTestBean {
	}

	static interface TestBeanInterface {
	}

	static class TestBean extends AbstractTestBean implements TestBeanInterface {
	}

	static class OtherTestBean implements TestBeanInterface {
	}

	static class OtherBean {
	}

	static class UnregisteredBean {
	}

}
