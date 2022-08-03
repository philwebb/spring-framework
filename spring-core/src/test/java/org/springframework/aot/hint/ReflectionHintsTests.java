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

package org.springframework.aot.hint;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.FieldMode;
import org.springframework.aot.hint.JavaReflectionHint;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.JavaReflectionHint.Category;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ReflectionHints}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ReflectionHintsTests {

	private final ReflectionHints hints = new ReflectionHints();

	@Test
	void registerPublicClassesRegistersHint() {
		this.hints.registerPublicClasses().forType(String.class);
		assertThat(this.hints.getJavaReflectionHint(String.class).getCategories())
				.containsExactly(Category.PUBLIC_CLASSES);
	}

	@Test
	void registerDeclaredClassesRegistersHint() {
		this.hints.registerDeclaredClasses().forType(String.class);
		assertThat(this.hints.getJavaReflectionHint(String.class).getCategories())
				.containsExactly(Category.DECLARED_CLASSES);
	}

	@Test
	void registerMultipleCategoriesRegistersHint() {
		this.hints.registerPublicClasses().forType(String.class);
		this.hints.registerDeclaredClasses().forType(String.class);
		assertThat(this.hints.getJavaReflectionHint(String.class).getCategories())
				.containsExactlyInAnyOrder(Category.PUBLIC_CLASSES, Category.DECLARED_CLASSES);
	}

	@Test
	void registerPublicClassesWithClassNameRegistersHint() {
		this.hints.registerPublicClasses().forType(String.class.getName());
		assertThat(this.hints.getJavaReflectionHint(String.class).getCategories())
				.containsExactly(Category.PUBLIC_CLASSES);
	}

	@Test
	void registerPublicClassesWithTypeReferenceRegistersHint() {
		this.hints.registerPublicClasses().forType(TypeReference.of(String.class));
		assertThat(this.hints.getJavaReflectionHint(String.class).getCategories())
				.containsExactly(Category.PUBLIC_CLASSES);
	}

	@Test
	void registerReadWithFieldRegisteresHint() {
		Field field = ReflectionUtils.findField(TestType.class, "field");
		this.hints.registerRead().forField(field);
		assertThat(this.hints.javaReflection()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(TestType.class.getCanonicalName());
			assertThat(typeHint.getCategories()).isEmpty();
			assertThat(typeHint.fields()).singleElement().satisfies(fieldHint -> {
				assertThat(fieldHint.getName()).isEqualTo("field");
				assertThat(fieldHint.getMode()).isEqualTo(FieldMode.READ);
			});
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
		});
	}

	@Test
	void registerReadWithFindFieldRegisteresHint() {
		this.hints.registerRead().forField(TestType.class, "field");
		assertThat(this.hints.javaReflection()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(TestType.class.getCanonicalName());
			assertThat(typeHint.fields()).singleElement().satisfies(fieldHint -> {
				assertThat(fieldHint.getName()).isEqualTo("field");
				assertThat(fieldHint.getMode()).isEqualTo(FieldMode.READ);
			});
		});
	}

	@Test
	void registerReadWithFindFieldWhenNotFoundThrowsException() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.hints.registerRead().forField(TestType.class, "nothere"))
				.withMessageContaining("Unable to find field 'nothere' in " + TestType.class.getName());
	}

	@Test
	void registerReadForPublicFieldsInRegistersHint() {
		this.hints.registerRead().forPublicFieldsIn(String.class);
		assertThat(this.hints.javaReflection()).singleElement()
				.satisfies(typeWithCategories(String.class, Category.PUBLIC_FIELDS));
	}

	@Test
	void registerReadForDeclaredFieldsInRegistersHint() {
		this.hints.registerRead().forDeclaredFieldsIn(String.class);
		assertThat(this.hints.javaReflection()).singleElement()
				.satisfies(typeWithCategories(String.class, Category.DECLARED_FIELDS));
	}

	@Test
	void registerReadWhenTypeIsPresentWhenTypePresentRegistersHint() {
		this.hints.registerRead().whenTypeIsPresent().forDeclaredFieldsIn(String.class.getName());
		assertThat(this.hints.javaReflection()).singleElement()
				.satisfies(typeWithCategories(String.class, Category.DECLARED_FIELDS));
	}

	@Test
	@SuppressWarnings("unchecked")
	void registerReadWhenTypeIsPresentWhenTypeMissingSkipsHint() {
		this.hints.registerRead().whenTypeIsPresent().forDeclaredFieldsIn("com.example.DoesNotExist");
		assertThat(this.hints.javaReflection()).isEmpty();
	}

	@Test
	void registerWriteWithFieldRegisteresHint() {
		Field field = ReflectionUtils.findField(TestType.class, "field");
		this.hints.registerWrite().forField(field);
		assertThat(this.hints.javaReflection()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(TestType.class.getCanonicalName());
			assertThat(typeHint.getCategories()).isEmpty();
			assertThat(typeHint.fields()).singleElement().satisfies(fieldHint -> {
				assertThat(fieldHint.getName()).isEqualTo("field");
				assertThat(fieldHint.getMode()).isEqualTo(FieldMode.WRITE);
			});
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
		});
	}

	@Test
	void getJavaReflectionHintWithClassWhenHasHintReturnsHint() {
		this.hints.registerRead().forDeclaredFieldsIn(String.class);
		assertThat(this.hints.getJavaReflectionHint(String.class))
				.satisfies(typeWithCategories(String.class, Category.DECLARED_FIELDS));
	}

	@Test
	void registerIntrospectForMethodRegistersHint() {
		Method method = ReflectionUtils.findMethod(TestType.class, "setName", String.class);
		this.hints.registerIntrospect().forMethod(method);
		assertThat(this.hints.javaReflection()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(TestType.class.getCanonicalName());
			assertThat(typeHint.getCategories()).isEmpty();
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).singleElement().satisfies(methodHint -> {
				assertThat(methodHint.getName()).isEqualTo("setName");
				assertThat(methodHint.getParameterTypes()).containsOnly(TypeReference.of(String.class));
				assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
			});
		});
	}

	@Test
	void registerIntrospectForMethodWithFindMethodRegisteresHint() {
		this.hints.registerIntrospect().forMethod(TestType.class, "setName", String.class);
		assertThat(this.hints.javaReflection()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(TestType.class.getCanonicalName());
			assertThat(typeHint.getCategories()).isEmpty();
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.methods()).singleElement().satisfies(methodHint -> {
				assertThat(methodHint.getName()).isEqualTo("setName");
				assertThat(methodHint.getParameterTypes()).containsOnly(TypeReference.of(String.class));
				assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
			});
		});
	}

	@Test
	void registerIntrospectForMethodWithFindMethodWhenNotFoundthrowsException() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.hints.registerIntrospect().forMethod(TestType.class, "missing", String.class))
				.withMessageContaining("Unable to find method 'missing' in");
	}

	@Test
	void registerIntrospectForConstructorRegistersHint() {
		Constructor<?> constructor = TestType.class.getDeclaredConstructors()[0];
		this.hints.registerIntrospect().forConstructor(constructor);
		assertThat(this.hints.javaReflection()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(TestType.class.getCanonicalName());
			assertThat(typeHint.getCategories()).isEmpty();
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.constructors()).singleElement().satisfies(constructorHint -> {
				assertThat(constructorHint.getParameterTypes()).isEmpty();
				assertThat(constructorHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
			});
			assertThat(typeHint.methods()).isEmpty();
		});
	}

	@Test
	void registerIntrospectForConstructorWithFindConstructorRegistersHint() {
		this.hints.registerIntrospect().forConstructor(TestType.class);
		assertThat(this.hints.javaReflection()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType().getCanonicalName()).isEqualTo(TestType.class.getCanonicalName());
			assertThat(typeHint.getCategories()).isEmpty();
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.constructors()).singleElement().satisfies(constructorHint -> {
				assertThat(constructorHint.getParameterTypes()).isEmpty();
				assertThat(constructorHint.getMode()).isEqualTo(ExecutableMode.INTROSPECT);
			});
			assertThat(typeHint.methods()).isEmpty();
		});
	}

	@Test
	void registerIntrospectForConstructorWithFindConstructorWhenNotFoundThrowsException() {
		assertThatIllegalStateException().isThrownBy(
				() -> this.hints.registerIntrospect().forConstructor(TestType.class, String.class, Integer.class))
				.withMessageContaining("Unable to find constructor in class");
	}

	@Test
	void registerIntrospectForPublicConstructorsInRegistersHint() {
		this.hints.registerIntrospect().forPublicConstructorsIn(Integer.class);
		assertThat(this.hints.javaReflection()).singleElement()
				.satisfies(typeWithCategories(Integer.class, Category.INTROSPECT_PUBLIC_CONSTRUCTORS));
	}

	@Test
	void registerIntrospectForDeclaredConstructorsInRegistersHint() {
		this.hints.registerIntrospect().forDeclaredConstructorsIn(Integer.class);
		assertThat(this.hints.javaReflection()).singleElement()
				.satisfies(typeWithCategories(Integer.class, Category.INTROSPECT_DECLARED_CONSTRUCTORS));
	}

	@Test
	void registerIntrospectForPublicMethodsInRegistersHint() {
		this.hints.registerIntrospect().forPublicMethodsIn(Integer.class);
		assertThat(this.hints.javaReflection()).singleElement()
				.satisfies(typeWithCategories(Integer.class, Category.INTROSPECT_PUBLIC_METHODS));
	}

	@Test
	void registerIntrospectForDeclaredMethodsInRegistersHint() {
		this.hints.registerIntrospect().forDeclaredConstructorsIn(Integer.class);
		assertThat(this.hints.javaReflection()).singleElement()
				.satisfies(typeWithCategories(Integer.class, Category.INTROSPECT_DECLARED_CONSTRUCTORS));
	}

	@Test
	void registerIntrospectWithMultipleTypesAppliesSameCategory() {
		this.hints.registerIntrospect().forPublicConstructorsIn(Integer.class, String.class, Double.class);
		assertThat(this.hints.javaReflection())
				.anySatisfy(typeWithCategories(Integer.class, Category.INTROSPECT_PUBLIC_CONSTRUCTORS))
				.anySatisfy(typeWithCategories(String.class, Category.INTROSPECT_PUBLIC_CONSTRUCTORS))
				.anySatisfy(typeWithCategories(Double.class, Category.INTROSPECT_PUBLIC_CONSTRUCTORS)).hasSize(3);
	}

	@Test
	void registerInvokeForMethodRegistersHint() {
		Method method = ReflectionUtils.findMethod(TestType.class, "setName", String.class);
		assertThat(method).isNotNull();
		this.hints.registerInvoke().forMethod(method);
		assertThat(this.hints.javaReflection()).singleElement().satisfies(typeHint -> {
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

	@Test
	void registerInvokeForPublicConstructorsInRegistersHint() {
		this.hints.registerInvoke().forPublicConstructorsIn(Integer.class);
		assertThat(this.hints.javaReflection()).singleElement()
				.satisfies(typeWithCategories(Integer.class, Category.INVOKE_PUBLIC_CONSTRUCTORS));
	}

	@Test
	void registerInvokeForDeclaredConstructorsInRegistersHint() {
		this.hints.registerInvoke().forDeclaredConstructorsIn(Integer.class);
		assertThat(this.hints.javaReflection()).singleElement()
				.satisfies(typeWithCategories(Integer.class, Category.INVOKE_DECLARED_CONSTRUCTORS));
	}

	@Test
	void registerInvokeForPublicMethodsInRegistersHint() {
		this.hints.registerInvoke().forPublicMethodsIn(Integer.class);
		assertThat(this.hints.javaReflection()).singleElement()
				.satisfies(typeWithCategories(Integer.class, Category.INVOKE_PUBLIC_METHODS));
	}

	@Test
	void registerInvokeForDeclaredMethodsInRegistersHint() {
		this.hints.registerInvoke().forDeclaredMethodsIn(Integer.class);
		assertThat(this.hints.javaReflection()).singleElement()
				.satisfies(typeWithCategories(Integer.class, Category.INVOKE_DECLARED_METHODS));
	}

	@Test
	void getJavaReflectionHintWithTypeReferenceWhenHasHintReturnsHint() {
		this.hints.registerRead().forDeclaredFieldsIn(String.class);
		assertThat(this.hints.getJavaReflectionHint(TypeReference.of(String.class)))
				.satisfies(typeWithCategories(String.class, Category.DECLARED_FIELDS));
	}

	@Test
	void getJavaReflectionHintForNonRegisteredTypeReturnsNull() {
		assertThat(this.hints.getJavaReflectionHint(String.class)).isNull();
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
