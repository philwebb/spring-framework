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

package org.springframework.aot.hint2;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint2.JavaReflectionHint.Category;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReflectionHints}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ReflectionHintsTests {

	private final ReflectionHints reflectionHints = new ReflectionHints();

	@Test
	void registerReadForDeclaredFieldsInRegistersHint() {
		this.reflectionHints.registerRead().forDeclaredFieldsIn(String.class);
		assertThat(this.reflectionHints.typeHints()).singleElement()
				.satisfies(typeWithCategories(String.class, Category.DECLARED_FIELDS));
	}

	@Test
	void registerReadWhenTypeIsPresentWhenTypePresentRegistersHint() {
		this.reflectionHints.registerRead().whenTypeIsPresent().forDeclaredFieldsIn(String.class.getName());
		assertThat(this.reflectionHints.typeHints()).singleElement()
				.satisfies(typeWithCategories(String.class, Category.DECLARED_FIELDS));
	}

	@Test
	@SuppressWarnings("unchecked")
	void registerReadWhenTypeIsPresentWhenTypeMissingSkipsHint() {
		this.reflectionHints.registerRead().whenTypeIsPresent().forDeclaredFieldsIn("com.example.DoesNotExist");
		assertThat(this.reflectionHints.typeHints()).isEmpty();
	}

	@Test
	void getTypeWithClassWhenHasHintReturnsHint() {
		this.reflectionHints.registerRead().forDeclaredFieldsIn(String.class);
		assertThat(this.reflectionHints.getTypeHint(String.class))
				.satisfies(typeWithCategories(String.class, Category.DECLARED_FIELDS));
	}

	@Test
	void getTypeWithTypeReferenceWhenHasHintReturnsHint() {
		this.reflectionHints.registerRead().forDeclaredFieldsIn(String.class);
		assertThat(this.reflectionHints.getTypeHint(TypeReference.of(String.class)))
				.satisfies(typeWithCategories(String.class, Category.DECLARED_FIELDS));
	}

	@Test
	void getTypeForNonRegisteredTypeReturnsNull() {
		assertThat(this.reflectionHints.getTypeHint(String.class)).isNull();
	}

//	@Test
//	void registerTypeReuseBuilder() {
//		this.reflectionHints.registerType(TypeReference.of(String.class),
//				typeHint -> typeHint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
//		Field field = ReflectionUtils.findField(String.class, "value");
//		assertThat(field).isNotNull();
//		this.reflectionHints.registerField(field);
//		assertThat(this.reflectionHints.typeHints()).singleElement().satisfies(typeHint -> {
//			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(String.class.getCanonicalName());
//			assertThat(typeHint.fields()).singleElement()
//					.satisfies(fieldHint -> assertThat(fieldHint.getName()).isEqualTo("value"));
//			assertThat(typeHint.getMemberCategories()).containsOnly(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
//		});
//	}
//
	@Test
	void registerClass() {
		this.reflectionHints.registerInvoke().forPublicConstructorsIn(Integer.class);
		assertThat(this.reflectionHints.typeHints()).singleElement()
				.satisfies(typeWithCategories(Integer.class, Category.INVOKE_PUBLIC_CONSTRUCTORS));
	}
//
	@Test
	void registerTypesApplyTheSameHints() {
		this.reflectionHints.registerInvoke().forPublicConstructorsIn(Integer.class, String.class, Double.class);
		assertThat(this.reflectionHints.typeHints())
				.anySatisfy(typeWithCategories(Integer.class, Category.INVOKE_PUBLIC_CONSTRUCTORS))
				.anySatisfy(typeWithCategories(String.class, Category.INVOKE_PUBLIC_CONSTRUCTORS))
				.anySatisfy(typeWithCategories(Double.class, Category.INVOKE_PUBLIC_CONSTRUCTORS)).hasSize(3);
	}

	@Test
	void registerField() {
		Field field = ReflectionUtils.findField(TestType.class, "field");
		this.reflectionHints.registerRead().forField(field);
		assertThat(this.reflectionHints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(TestType.class.getCanonicalName());
			assertThat(typeHint.getCategories()).isEmpty();
			assertThat(typeHint.fields()).singleElement()
					.satisfies(fieldHint -> assertThat(fieldHint.getName()).isEqualTo("field"));
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
		});
	}

		@Test
	void registerConstructor() {
		Constructor<?> constructor = TestType.class.getDeclaredConstructors()[0];
		this.reflectionHints.registerInvoke().forConstructor(constructor);
		assertThat(this.reflectionHints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(TestType.class.getCanonicalName());
			assertThat(typeHint.getCategories()).isEmpty();
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.constructors()).singleElement().satisfies(constructorHint -> {
				assertThat(constructorHint.getParameterTypes()).isEmpty();
				assertThat(constructorHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
			});
			assertThat(typeHint.methods()).isEmpty();
		});
	}

	@Test
	void registerMethod() {
		Method method = ReflectionUtils.findMethod(TestType.class, "setName", String.class);
		assertThat(method).isNotNull();
		this.reflectionHints.registerInvoke().forMethod(method);
		assertThat(this.reflectionHints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(TestType.class.getCanonicalName());
			assertThat(typeHint.getCategories()).isEmpty();
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).singleElement().satisfies(methodHint -> {
				assertThat(methodHint.getName()).isEqualTo("setName");
				assertThat(methodHint.getParameterTypes()).containsOnly(TypeReference.of(String.class));
				assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
			});
		});
	}

	private Consumer<JavaReflectionHint> typeWithCategories(Class<?> type, Category... categories) {
		return typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(type.getCanonicalName());
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
			assertThat(typeHint.getCategories()).containsExactly(categories);
		};
	}

	@SuppressWarnings("unused")
	static class TestType {

		@Nullable
		private String field;

		void setName(String name) {

		}

	}

}
