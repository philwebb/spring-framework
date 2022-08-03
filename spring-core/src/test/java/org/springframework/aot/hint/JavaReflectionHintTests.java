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

package org.springframework.aot.hint;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Supplier;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.FieldMode;
import org.springframework.aot.hint.JavaReflectionHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.JavaReflectionHint.Category;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JavaReflectionHint}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class JavaReflectionHintTests {

	@Test
	void createWithNullTypeReference() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JavaReflectionHint(null))
				.withMessage("'type' must not be null");
	}

	@Test
	void createWithType() {
		TypeReference type = TypeReference.of(String.class);
		JavaReflectionHint hint = new JavaReflectionHint(type);
		assertThat(hint.getType()).isEqualTo(type);
	}

	@Test
	void andReachableTypeReturnsNewInstance() {
		JavaReflectionHint without = new JavaReflectionHint(TypeReference.of(Function.class));
		JavaReflectionHint with = without.andReachableType(TypeReference.of(Consumer.class));
		assertThat(without).isNotSameAs(with);
		assertThat(without.getReachableType()).isNull();
		assertThat(with.getReachableType()).isEqualTo(TypeReference.of(Consumer.class));
	}

	@Test
	void andReachableTypeWhenAlreadySetWithDifferentTypeThrowsException() {
		JavaReflectionHint hint = new JavaReflectionHint(TypeReference.of(Function.class))
				.andReachableType(TypeReference.of(Consumer.class));
		assertThatIllegalStateException().isThrownBy(() -> hint.andReachableType(TypeReference.of(Supplier.class)))
				.withMessage("A reachableType condition has already been applied");
	}

	@Test
	void andReachableTypeWhenAlreadySetWithSameTypeReturnsSameInstance() {
		JavaReflectionHint hint = new JavaReflectionHint(TypeReference.of(Function.class))
				.andReachableType(TypeReference.of(Consumer.class));
		assertThat(hint.andReachableType(TypeReference.of(Consumer.class))).isSameAs(hint);
	}

	@Test
	void andCategoryReturnsNewInstance() {
		JavaReflectionHint hint = new JavaReflectionHint(TypeReference.of(Function.class));
		JavaReflectionHint withCategory = hint.andCategory(Category.DECLARED_CLASSES);
		assertThat(withCategory).isNotSameAs(hint);
		assertThat(hint.getCategories()).isEmpty();
		assertThat(withCategory.getCategories()).containsExactly(Category.DECLARED_CLASSES);
	}

	@Test
	void andCategoryWhenAlreadyContainsCategoryReturnsSameInstance() {
		JavaReflectionHint hint = new JavaReflectionHint(TypeReference.of(Function.class))
				.andCategory(Category.DECLARED_CLASSES);
		assertThat(hint.andCategory(Category.DECLARED_CLASSES)).isSameAs(hint);
	}

	@Test
	void andFieldWhenNoFieldAddsField() {
		Field field = ReflectionUtils.findField(ExampleBean.class, "field");
		JavaReflectionHint hint = new JavaReflectionHint(TypeReference.of(ExampleBean.class));
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
		JavaReflectionHint hint = new JavaReflectionHint(TypeReference.of(ExampleBean.class));
		hint = hint.andField(field, FieldMode.READ, false);
		JavaReflectionHint updatedHint = hint.andField(field, FieldMode.WRITE, true);
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
		JavaReflectionHint hint = new JavaReflectionHint(TypeReference.of(ExampleBean.class));
		hint = hint.andField(field, FieldMode.WRITE, true);
		JavaReflectionHint updatedHint = hint.andField(field, FieldMode.READ, false);
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
		JavaReflectionHint hint = new JavaReflectionHint(TypeReference.of(ExampleBean.class));
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
		JavaReflectionHint hint = new JavaReflectionHint(TypeReference.of(ExampleBean.class));
		hint = hint.andMethod(method, ExecutableMode.INTROSPECT);
		JavaReflectionHint updatedHint = hint.andMethod(method, ExecutableMode.INVOKE);
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
		JavaReflectionHint hint = new JavaReflectionHint(TypeReference.of(ExampleBean.class));
		hint = hint.andMethod(method, ExecutableMode.INVOKE);
		JavaReflectionHint updatedHint = hint.andMethod(method, ExecutableMode.INTROSPECT);
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
		JavaReflectionHint hint = new JavaReflectionHint(TypeReference.of(ExampleBean.class));
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
		JavaReflectionHint hint = new JavaReflectionHint(TypeReference.of(ExampleBean.class));
		hint = hint.andConstructor(constructor, ExecutableMode.INTROSPECT);
		JavaReflectionHint updatedHint = hint.andConstructor(constructor, ExecutableMode.INVOKE);
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
		JavaReflectionHint hint = new JavaReflectionHint(TypeReference.of(ExampleBean.class));
		hint = hint.andConstructor(constructor, ExecutableMode.INVOKE);
		JavaReflectionHint updatedHint = hint.andConstructor(constructor, ExecutableMode.INTROSPECT);
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
