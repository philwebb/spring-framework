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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.function.BeanSelector.PrimarySelectorType;
import org.springframework.core.ResolvableType;
import org.springframework.util.function.InstanceSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BeanSelector}.
 *
 * @author Phillip Webb
 */
class BeanSelectorTests {

	@Test
	void testWhenAllMatchesAllBeans() {
		FunctionBeanDefinition<?> definition = createTestBeanDefinition(TestBean.class,
				TestBean::new);
		assertThat(BeanSelector.all().test(definition)).isTrue();
	}

	@Test
	void testWhenByTypeMatchesOnlyType() {
		FunctionBeanDefinition<?> definition = createTestBeanDefinition(TestBean.class,
				TestBean::new);
		assertThat(BeanSelector.byType(TestBean.class).test(definition)).isTrue();
		assertThat(BeanSelector.byType(TestService.class).test(definition)).isTrue();
		assertThat(BeanSelector.byType(TestOther.class).test(definition)).isFalse();
	}

	@Test
	void testWhenBySimpleResolvableTypeMatchesOnlyType() {
		ResolvableType testBeanType = ResolvableType.forClass(TestBean.class);
		ResolvableType testServiceType = ResolvableType.forClass(TestService.class);
		ResolvableType testOtherType = ResolvableType.forClass(TestOther.class);
		FunctionBeanDefinition<?> definition = FunctionBeanDefinition.of((builder) -> {
			builder.setName("test");
			builder.setType(testBeanType);
			builder.setInstanceSupplier(InstanceSupplier.of(TestBean::new));
		});
		assertThat(BeanSelector.byType(testBeanType).test(definition)).isTrue();
		assertThat(BeanSelector.byType(testServiceType).test(definition)).isTrue();
		assertThat(BeanSelector.byType(testOtherType).test(definition)).isFalse();
	}

	@Test
	void testWhenByGenericResolvableTypeMatchesOnlyType() {
		ResolvableType testServiceType = ResolvableType.forClassWithGenerics(
				TestGeneric.class, TestService.class);
		ResolvableType testBeanType = ResolvableType.forClassWithGenerics(
				TestGeneric.class, TestBean.class);
		ResolvableType testOtherType = ResolvableType.forClassWithGenerics(
				TestGeneric.class, TestOther.class);
		FunctionBeanDefinition<?> definition = FunctionBeanDefinition.of((builder) -> {
			builder.setName("test");
			builder.setType(testBeanType);
			builder.setInstanceSupplier(InstanceSupplier.of(TestGeneric::new));
		});
		assertThat(BeanSelector.byType(testBeanType).test(definition)).isTrue();
		assertThat(BeanSelector.byType(testServiceType).test(definition)).isTrue();
		assertThat(BeanSelector.byType(testOtherType).test(definition)).isFalse();
	}

	@Test
	void testWhenByAnnoationMatchesAnnotatedType() {

	}

	@Test
	void getPrimarySelectorTypeWhenAllReturnsNull() {
		assertThat(BeanSelector.all().getPrimarySelectorType()).isNull();
	}

	@Test
	void getPrimarySelectorWhenAllReturnsNull() {
		assertThat(BeanSelector.all().getPrimarySelector()).isNull();
	}

	@Test
	void getPrimarySelectorTypeWhenByTypeReturnsType() {
		Class<TestBean> type = TestBean.class;
		assertThat(BeanSelector.byType(type).getPrimarySelectorType()).isEqualTo(
				PrimarySelectorType.TYPE);
	}

	@Test
	void getPrimarySelectorWhenByTypeReturnsType() {
		Class<TestBean> type = TestBean.class;
		assertThat(BeanSelector.byType(type).getPrimarySelector()).isEqualTo(type);
	}

	@Test
	void getPrimarySelectorTypeWhenByResolvableTypeReturnsResolvableType() {
		ResolvableType type = ResolvableType.forClass(TestBean.class);
		assertThat(BeanSelector.byType(type).getPrimarySelectorType()).isEqualTo(
				PrimarySelectorType.RESOLVABLE_TYPE);
	}

	@Test
	void getPrimarySelectorWhenResolvableTypeReturnsResolvableType() {
		ResolvableType type = ResolvableType.forClass(TestBean.class);
		assertThat(BeanSelector.byType(type).getPrimarySelector()).isEqualTo(type);
	}

	@Test
	void toStringWhenAll() {
		assertThat(BeanSelector.all()).hasToString("All beans");
	}

	@Test
	void toStringWhenByType() {
		Class<?> type = TestBean.class;
		assertThat(BeanSelector.byType(type)).hasToString(
				"Beans matching type '" + TestBean.class.getName() + "'");
	}

	@Test
	void toStringWhenByResolvableType() {
		ResolvableType type = ResolvableType.forClassWithGenerics(TestGeneric.class,
				TestBean.class);
		assertThat(BeanSelector.byType(type)).hasToString(
				"Beans matching xtype '" + TestBean.class.getName() + "'");
	}

	private <T> FunctionBeanDefinition<T> createTestBeanDefinition(Class<T> type,
			Supplier<T> supplier) {
		return FunctionBeanDefinition.of((builder) -> {
			builder.setName("test");
			builder.setType(type);
			builder.setInstanceSupplier(InstanceSupplier.of(supplier));
		});
	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface TestAnnotation {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@TestAnnotation
	static @interface MetaTestAnnotation {
	}

	static interface TestService {
	}

	@MetaTestAnnotation
	static class TestBean implements TestService {
	}

	static class TestOther {
	}

	static class TestGeneric<T> {
	}

}
