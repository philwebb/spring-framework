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

package org.springframework.aot.hint2;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Supplier;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint2.ReflectionTypeHint.Category;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ReflectionTypeHint}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ReflectionTypeHintTests {

	@Test
	void createWithNullTypeReference() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ReflectionTypeHint(null))
				.withMessage("'type' must not be null");
	}

	@Test
	void createWithType() {
		TypeReference type = TypeReference.of(String.class);
		ReflectionTypeHint hint = new ReflectionTypeHint(type);
		assertThat(hint.getType()).isEqualTo(type);
	}

	@Test
	void andReachableTypeReturnsNewInstance() {
		ReflectionTypeHint without = new ReflectionTypeHint(TypeReference.of(Function.class));
		ReflectionTypeHint with = without.andReachableType(TypeReference.of(Consumer.class));
		assertThat(without).isNotSameAs(with);
		assertThat(without.getReachableType()).isNull();
		assertThat(with.getReachableType()).isEqualTo(TypeReference.of(Consumer.class));
	}

	@Test
	void andReachableTypeWhenAlreadySetWithDifferentTypeThrowsException() {
		ReflectionTypeHint hint = new ReflectionTypeHint(TypeReference.of(Function.class))
				.andReachableType(TypeReference.of(Consumer.class));
		assertThatIllegalStateException().isThrownBy(() -> hint.andReachableType(TypeReference.of(Supplier.class)))
				.withMessage("A reachableType condition has already been applied");
	}

	@Test
	void andReachableTypeWhenAlreadySetWithSameTypeReturnsSameInstance() {
		ReflectionTypeHint hint = new ReflectionTypeHint(TypeReference.of(Function.class))
				.andReachableType(TypeReference.of(Consumer.class));
		assertThat(hint.andReachableType(TypeReference.of(Consumer.class))).isSameAs(hint);
	}

	@Test
	void andCategoryReturnsNewInstance() {
		ReflectionTypeHint hint = new ReflectionTypeHint(TypeReference.of(Function.class));
		ReflectionTypeHint withCategory = hint.andCategory(Category.DECLARED_CLASSES);
		assertThat(withCategory).isNotSameAs(hint);
		assertThat(hint.getCategories()).isEmpty();
		assertThat(withCategory.getCategories()).containsExactly(Category.DECLARED_CLASSES);
	}

	@Test
	void andCategoryWhenAlreadyContainsCategoryReturnsSameInstance() {
		ReflectionTypeHint hint = new ReflectionTypeHint(TypeReference.of(Function.class))
				.andCategory(Category.DECLARED_CLASSES);
		assertThat(hint.andCategory(Category.DECLARED_CLASSES)).isSameAs(hint);
	}

	@Test
	void andFieldWhenNoFieldAddsField() {
		Field field = ReflectionUtils.findField(ExampleBean.class, "field");
		ReflectionTypeHint hint = new ReflectionTypeHint(TypeReference.of(ExampleBean.class));
		hint = hint.andField(field, FieldMode.READ, false);
		assertThat(hint.fields()).singleElement().satisfies(fieldHint -> {
			assertThat(fieldHint.getName()).isEqualTo("field");
			assertThat(fieldHint.getMode()).isEqualTo(FieldMode.READ);
			assertThat(fieldHint.isAllowUnsafeAccess()).isFalse();
		});
	}

	@Test
	void andFieldWhenHasFieldWithLowerValuesUpdatesField() {
		Field field = ReflectionUtils.findField(ExampleBean.class, "field");
		ReflectionTypeHint hint = new ReflectionTypeHint(TypeReference.of(ExampleBean.class));
		hint = hint.andField(field, FieldMode.READ, false);
		ReflectionTypeHint updatedHint = hint.andField(field, FieldMode.WRITE, true);
		assertThat(hint.fields()).singleElement().satisfies(fieldHint -> {
			assertThat(fieldHint.getName()).isEqualTo("field");
			assertThat(fieldHint.getMode()).isEqualTo(FieldMode.READ);
			assertThat(fieldHint.isAllowUnsafeAccess()).isFalse();
		});
		assertThat(updatedHint.fields()).singleElement().satisfies(fieldHint -> {
			assertThat(fieldHint.getName()).isEqualTo("field");
			assertThat(fieldHint.getMode()).isEqualTo(FieldMode.WRITE);
			assertThat(fieldHint.isAllowUnsafeAccess()).isTrue();
		});
	}

	@Test
	void andFieldWhenHasFieldWithHigherUpdatesField() {
		Field field = ReflectionUtils.findField(ExampleBean.class, "field");
		ReflectionTypeHint hint = new ReflectionTypeHint(TypeReference.of(ExampleBean.class));
		hint = hint.andField(field, FieldMode.WRITE, true);
		ReflectionTypeHint updatedHint = hint.andField(field, FieldMode.READ, false);
		assertThat(hint.fields()).singleElement().satisfies(fieldHint -> {
			assertThat(fieldHint.getName()).isEqualTo("field");
			assertThat(fieldHint.getMode()).isEqualTo(FieldMode.WRITE);
			assertThat(fieldHint.isAllowUnsafeAccess()).isTrue();
		});
		assertThat(updatedHint.fields()).singleElement().satisfies(fieldHint -> {
			assertThat(fieldHint.getName()).isEqualTo("field");
			assertThat(fieldHint.getMode()).isEqualTo(FieldMode.WRITE);
			assertThat(fieldHint.isAllowUnsafeAccess()).isTrue();
		});
	}

	@Test
	void andMethodWhenNoMethodAddsMethod() {
		Method method = ReflectionUtils.findMethod(ExampleBean.class, "method");
		ReflectionTypeHint hint = new ReflectionTypeHint(TypeReference.of(ExampleBean.class));
		hint = hint.andMethod(method, ExecutableMode.INTROSPECT);
		assertThat(hint.methods()).singleElement().satisfies(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("method");
			assertThat(methodHint.getParameterTypes()).isEmpty();
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
		});
	}

	@Test
	void andMethodWhenHasMethodWithLowerValuesUpdatesMethod() {
		Method method = ReflectionUtils.findMethod(ExampleBean.class, "method");
		ReflectionTypeHint hint = new ReflectionTypeHint(TypeReference.of(ExampleBean.class));
		hint = hint.andMethod(method, ExecutableMode.INTROSPECT);
		ReflectionTypeHint updatedHint = hint.andMethod(method, ExecutableMode.INVOKE);
		assertThat(hint.methods()).singleElement().satisfies(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("method");
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
		});
		assertThat(updatedHint.methods()).singleElement().satisfies(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("method");
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	@Test
	void andMethodWhenHasMethodWithHigherUpdatesMethod() {
		Method method = ReflectionUtils.findMethod(ExampleBean.class, "method");
		ReflectionTypeHint hint = new ReflectionTypeHint(TypeReference.of(ExampleBean.class));
		hint = hint.andMethod(method, ExecutableMode.INVOKE);
		ReflectionTypeHint updatedHint = hint.andMethod(method, ExecutableMode.INTROSPECT);
		assertThat(hint.methods()).singleElement().satisfies(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("method");
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
		assertThat(updatedHint.methods()).singleElement().satisfies(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo("method");
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	@Test
	void andConstructorWhenNoMethodAddsMethod() throws Exception {
		Constructor<?> constructor = ExampleBean.class.getDeclaredConstructor();
		ReflectionTypeHint hint = new ReflectionTypeHint(TypeReference.of(ExampleBean.class));
		hint = hint.andConstructor(constructor, ExecutableMode.INTROSPECT);
		assertThat(hint.constructors()).singleElement().satisfies(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo(ExampleBean.class.getName());
			assertThat(methodHint.getParameterTypes()).isEmpty();
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
		});
	}

	@Test
	void andConstructorWhenHasMethodWithLowerValuesUpdatesMethod() throws Exception {
		Constructor<?> constructor = ExampleBean.class.getDeclaredConstructor();
		ReflectionTypeHint hint = new ReflectionTypeHint(TypeReference.of(ExampleBean.class));
		hint = hint.andConstructor(constructor, ExecutableMode.INTROSPECT);
		ReflectionTypeHint updatedHint = hint.andConstructor(constructor, ExecutableMode.INVOKE);
		assertThat(hint.constructors()).singleElement().satisfies(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo(ExampleBean.class.getName());
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
		});
		assertThat(updatedHint.constructors()).singleElement().satisfies(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo(ExampleBean.class.getName());
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	@Test
	void andConstructorWhenHasMethodWithHigherUpdatesMethod() throws Exception {
		Constructor<?> constructor = ExampleBean.class.getDeclaredConstructor();
		ReflectionTypeHint hint = new ReflectionTypeHint(TypeReference.of(ExampleBean.class));
		hint = hint.andConstructor(constructor, ExecutableMode.INVOKE);
		ReflectionTypeHint updatedHint = hint.andConstructor(constructor, ExecutableMode.INTROSPECT);
		assertThat(hint.constructors()).singleElement().satisfies(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo(ExampleBean.class.getName());
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
		assertThat(updatedHint.constructors()).singleElement().satisfies(methodHint -> {
			assertThat(methodHint.getName()).isEqualTo(ExampleBean.class.getName());
			assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
		});
	}

	static class ExampleBean {

		String field;

		void method() {
		}

	}

}
