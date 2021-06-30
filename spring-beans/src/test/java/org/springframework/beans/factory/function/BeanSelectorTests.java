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

/**
 * Tests for {@link BeanSelector}.
 *
 * @author Phillip Webb
 */
class BeanSelectorTests {

	@Test
	void testWhenAllMatchesAllBeans() {
		FunctionalBeanDefinition<?> definition = createTestBeanDefinition(TestBean.class,
				TestBean::new);
		assertThat(BeanSelector.all().test(definition)).isTrue();
	}

	@Test
	void testWhenByNameMatchesOnlyNamed() {
		FunctionalBeanDefinition<?> definition = createTestBeanDefinition(TestBean.class,
				TestBean::new);
		assertThat(BeanSelector.byName("test").test(definition)).isTrue();
		assertThat(BeanSelector.byName("tset").test(definition)).isFalse();
	}

	@Test
	void testWhenByTypeMatchesOnlyType() {
		FunctionalBeanDefinition<?> definition = createTestBeanDefinition(TestBean.class,
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
		FunctionalBeanDefinition<?> definition = FunctionalBeanDefinition.of(builder -> {
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
		ResolvableType testBeanType = ResolvableType.forClassWithGenerics(
				TestGeneric.class, TestBean.class);
		ResolvableType testServiceType = ResolvableType.forClassWithGenerics(
				TestGeneric.class, TestService.class);
		ResolvableType testOtherType = ResolvableType.forClassWithGenerics(
				TestGeneric.class, TestOther.class);
		FunctionalBeanDefinition<?> definition = FunctionalBeanDefinition.of(builder -> {
			builder.setName("test");
			builder.setType(testBeanType);
			builder.setInstanceSupplier(InstanceSupplier.of(TestBean::new));
		});
		assertThat(BeanSelector.byType(testBeanType).test(definition)).isTrue();
		assertThat(BeanSelector.byType(testServiceType).test(definition)).isFalse();
		assertThat(BeanSelector.byType(testOtherType).test(definition)).isFalse();
	}

	@Test
	void testWhenByAnnoationMatchesAnnotatedType() {
		FunctionalBeanDefinition<?> definition = createTestBeanDefinition(TestBean.class,
				TestBean::new);
		assertThat(BeanSelector.byAnnotation(MetaTestAnnotation.class).test(
				definition)).isTrue();
		assertThat(BeanSelector.byAnnotation(TestAnnotation.class).test(
				definition)).isTrue();
		assertThat(BeanSelector.byAnnotation(OtherAnnotation.class).test(
				definition)).isFalse();
	}

	@Test
	void testWithQualifier() {
		FunctionalBeanDefinition<?> unqualified = FunctionalBeanDefinition.of(builder -> {
			builder.setName("test");
			builder.setType(TestBean.class);
			builder.setInstanceSupplier(InstanceSupplier.of(TestBean::new));
		});
		FunctionalBeanDefinition<?> qualified = FunctionalBeanDefinition.of(builder -> {
			builder.setName("test");
			builder.setType(TestBean.class);
			builder.setInstanceSupplier(InstanceSupplier.of(TestBean::new));
			builder.addQualifier("test");
		});
		BeanSelector<?> selector = BeanSelector.all().withQualifier("test");
		assertThat(selector.test(unqualified)).isFalse();
		assertThat(selector.test(qualified)).isTrue();
	}

	@Test
	void testWithFilter() {
		BeanSelector<?> selector = BeanSelector.all().withFilter("name starts with s",
				definition -> definition.getName().startsWith("s"));
		assertThat(selector.test(createTestBeanDefinition("spring", TestBean.class,
				TestBean::new))).isTrue();
		assertThat(selector.test(createTestBeanDefinition("sprung", TestBean.class,
				TestBean::new))).isTrue();
		assertThat(selector.test(createTestBeanDefinition("wrongs", TestBean.class,
				TestBean::new))).isFalse();
	}

	@Test
	void getPrimarySelectorTypeWhenAllReturnsNull() {
		assertThat(BeanSelector.all().getPrimarySelectorType()).isNull();
	}

	@Test
	void getPrimarySelectorTypeWhenByNameReturnsName() {
		assertThat(BeanSelector.byName("test").getPrimarySelectorType()).isEqualTo(
				PrimarySelectorType.NAME);
	}

	@Test
	void getPrimarySelectorTypeWhenByTypeReturnsType() {
		Class<TestBean> type = TestBean.class;
		assertThat(BeanSelector.byType(type).getPrimarySelectorType()).isEqualTo(
				PrimarySelectorType.TYPE);
	}

	@Test
	void getPrimarySelectorTypeWhenByResolvableTypeReturnsResolvableType() {
		ResolvableType type = ResolvableType.forClass(TestBean.class);
		assertThat(BeanSelector.byType(type).getPrimarySelectorType()).isEqualTo(
				PrimarySelectorType.RESOLVABLE_TYPE);
	}

	@Test
	void getPrimarySelectorTypeWhenByAnnotationReturnsAnnotationClass() {
		assertThat(BeanSelector.byAnnotation(
				MetaTestAnnotation.class).getPrimarySelectorType()).isEqualTo(
						PrimarySelectorType.ANNOTATION_TYPE);
	}

	@Test
	void getPrimarySelectorWhenAllReturnsNull() {
		assertThat(BeanSelector.all().getPrimarySelector()).isNull();
	}

	@Test
	void getPrimarySelectorWhenByNameReturnsName() {
		assertThat(BeanSelector.byName("test").getPrimarySelector()).isEqualTo("test");
	}

	@Test
	void getPrimarySelectorWhenByTypeReturnsType() {
		Class<TestBean> type = TestBean.class;
		assertThat(BeanSelector.byType(type).getPrimarySelector()).isEqualTo(type);
	}

	@Test
	void getPrimarySelectorWhenByResolvableTypeReturnsResolvableType() {
		ResolvableType type = ResolvableType.forClass(TestBean.class);
		assertThat(BeanSelector.byType(type).getPrimarySelector()).isEqualTo(type);
	}

	@Test
	void getPrimarySelectorWhenByAnnotationReturnsAnnotationClass() {
		assertThat(BeanSelector.byAnnotation(
				MetaTestAnnotation.class).getPrimarySelector()).isEqualTo(
						MetaTestAnnotation.class);
	}

	@Test
	void toStringWhenAll() {
		assertThat(BeanSelector.all()).hasToString("All beans");
	}

	@Test
	void toStringWhenByName() {
		assertThat(BeanSelector.byName("test")).hasToString("Bean named 'test'");
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
		assertThat(BeanSelector.byType(type)).hasToString("Beans matching type '"
				+ TestGeneric.class.getName() + "<" + TestBean.class.getName() + ">'");
	}

	@Test
	void toStringWhenByAnnotation() {
		assertThat(BeanSelector.byAnnotation(TestAnnotation.class)).hasToString(
				"Beans annotated with '" + TestAnnotation.class.getName() + "'");
	}

	@Test
	void toStringWhenWithQualifier() {
		assertThat(BeanSelector.all().withQualifier("test")).hasToString(
				"All beans with qualifier of 'test'");
	}

	@Test
	void toStringWhenByTypeAndWithQualifier() {
		assertThat(BeanSelector.byType(TestBean.class).withQualifier("test")).hasToString(
				"Beans matching type '" + TestBean.class.getName()
						+ "' and with qualifier of 'test'");
	}

	@Test
	void toStringWhenWithSeveralQualifiers() {
		assertThat(BeanSelector.all().withQualifier("test").withQualifier(
				"spring")).hasToString(
						"All beans with qualifier of 'test' and with qualifier of 'spring'");
	}

	@Test
	void toStringWhenWithDescribedFilter() {
		assertThat(BeanSelector.all().withFilter("with names starting 's'",
				definition -> true)).hasToString("All beans with names starting 's'");
	}

	@Test
	void toStringWhenWithAnonymousFilter() {
		assertThat(BeanSelector.all().withFilter(definition -> true)).hasToString(
				"All beans matching custom filter");
	}

	@Test
	void toStringWhenWithQualifierAndWithDescribedFilter() {
		assertThat(BeanSelector.all().withQualifier("test").withFilter(
				"with names starting 's'", definition -> true)).hasToString(
						"All beans with qualifier of 'test' and with names starting 's'");
	}

	@Test
	void toStringWhenWithQualifierAndWithAnonymousFilter() {
		assertThat(BeanSelector.all().withQualifier("test").withFilter(
				definition -> true)).hasToString(
						"All beans with qualifier of 'test' and matching custom filter");
	}

	private <T> FunctionalBeanDefinition<T> createTestBeanDefinition(Class<T> type,
			Supplier<T> supplier) {
		return createTestBeanDefinition("test", type, supplier);
	}

	private <T> FunctionalBeanDefinition<T> createTestBeanDefinition(String name,
			Class<T> type, Supplier<T> supplier) {
		return FunctionalBeanDefinition.of(builder -> {
			builder.setName(name);
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

	@Retention(RetentionPolicy.RUNTIME)
	static @interface OtherAnnotation {
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
