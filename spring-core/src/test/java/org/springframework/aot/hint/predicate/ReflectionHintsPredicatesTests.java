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

package org.springframework.aot.hint.predicate;


import java.lang.reflect.Constructor;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.JavaReflectionHint.Category;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ReflectionHintsPredicates}
 *
 * @author Brian Clozel
 */
class ReflectionHintsPredicatesTests {

	private static Constructor<?> privateConstructor;

	private static Constructor<?> publicConstructor;

	private final ReflectionHintsPredicates reflection = new ReflectionHintsPredicates();

	private final RuntimeHints runtimeHints = new RuntimeHints();


	@BeforeAll
	static void setupAll() throws Exception {
		privateConstructor = SampleClass.class.getDeclaredConstructor(String.class);
		publicConstructor = SampleClass.class.getConstructor();
	}

	@Nested
	class ReflectionOnType {

		@Test
		void shouldFailForNullType() {
			assertThatIllegalArgumentException().isThrownBy(() -> reflection.onType((TypeReference) null));
		}

		@Test
		void reflectionOnClassShouldMatchIntrospection() {
			runtimeHints.reflection().register().forType(SampleClass.class);
			assertPredicateMatches(reflection.onType(SampleClass.class));
		}

		@Test
		void reflectionOnTypeReferenceShouldMatchIntrospection() {
			runtimeHints.reflection().register().forType(SampleClass.class);
			assertPredicateMatches(reflection.onType(TypeReference.of(SampleClass.class)));
		}

		@Test
		void reflectionOnDifferentClassShouldNotMatchIntrospection() {
			runtimeHints.reflection().register().forType(Integer.class);
			assertPredicateDoesNotMatch(reflection.onType(TypeReference.of(SampleClass.class)));
		}

		@Test
		void typeWithCategoryFailsWithNullCategory() {
			runtimeHints.reflection().registerIntrospect().forPublicMethodsIn(SampleClass.class);
			assertThatIllegalArgumentException().isThrownBy(() -> reflection.onType(SampleClass.class).withCategory(null));
		}

		@Test
		void typeWithCategoryMatchesCategory() {
			runtimeHints.reflection().registerIntrospect().forPublicMethodsIn(SampleClass.class);
			assertPredicateMatches(reflection.onType(SampleClass.class).withCategory(Category.INTROSPECT_PUBLIC_METHODS));
		}

		@Test
		void typeWithCategoryDoesNotMatchOtherCategory() {
			runtimeHints.reflection().registerIntrospect().forPublicMethodsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onType(SampleClass.class).withCategory(Category.INVOKE_PUBLIC_METHODS));
		}

		@Test
		void typeWithMemberCategoriesMatchesCategories() {
			runtimeHints.reflection().registerIntrospect()
					.forPublicConstructorsIn(SampleClass.class)
					.forPublicMethodsIn(SampleClass.class);
			assertPredicateMatches(reflection.onType(SampleClass.class)
					.withCategories(Category.INTROSPECT_PUBLIC_CONSTRUCTORS, Category.INTROSPECT_PUBLIC_METHODS));
		}

		@Test
		void typeWithMemberCategoriesDoesNotMatchMissingCategory() {
			runtimeHints.reflection().registerIntrospect().forPublicMethodsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onType(SampleClass.class)
					.withCategories(Category.INTROSPECT_PUBLIC_CONSTRUCTORS, Category.INTROSPECT_PUBLIC_METHODS));
		}

		@Test
		void typeWithAnyCategoryFailsWithNullCategories() {
			runtimeHints.reflection().registerIntrospect().forPublicMethodsIn(SampleClass.class);
			assertThatIllegalArgumentException().isThrownBy(() -> reflection.onType(SampleClass.class).withAnyCategory(new Category[0]));
		}

		@Test
		void typeWithAnyCategoryMatchesCategory() {
			runtimeHints.reflection().registerIntrospect().forPublicMethodsIn(SampleClass.class);
			runtimeHints.reflection().registerInvoke().forPublicMethodsIn(SampleClass.class);
			assertPredicateMatches(reflection.onType(SampleClass.class).withAnyCategory(Category.INTROSPECT_PUBLIC_METHODS));
		}

		@Test
		void typeWithAnyCategoryDoesNotMatchOtherCategory() {
			runtimeHints.reflection().registerInvoke().forPublicMethodsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onType(SampleClass.class).withAnyCategory(Category.INVOKE_DECLARED_METHODS));
		}

	}

	@Nested
	class ReflectionOnConstructor {

		@Test
		void constructorIntrospectionDoesNotMatchMissingHint() {
			assertPredicateDoesNotMatch(reflection.onConstructor(publicConstructor).introspect());
		}

		@Test
		void constructorIntrospectionMatchesConstructorHint() {
			runtimeHints.reflection().registerIntrospect().forConstructor(SampleClass.class);
			assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
		}

		@Test
		void constructorIntrospectionMatchesIntrospectPublicConstructors() {
			runtimeHints.reflection().registerIntrospect().forPublicConstructorsIn(SampleClass.class);
			assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
		}

		@Test
		void constructorIntrospectionMatchesInvokePublicConstructors() {
			runtimeHints.reflection().registerInvoke().forPublicConstructorsIn(SampleClass.class);
			assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
		}

		@Test
		void constructorIntrospectionMatchesIntrospectDeclaredConstructors() {
			runtimeHints.reflection().registerIntrospect().forDeclaredConstructorsIn(SampleClass.class);
			assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
		}

		@Test
		void constructorIntrospectionMatchesInvokeDeclaredConstructors() {
			runtimeHints.reflection().registerInvoke().forDeclaredConstructorsIn(SampleClass.class);
			assertPredicateMatches(reflection.onConstructor(publicConstructor).introspect());
		}

		@Test
		void constructorInvocationDoesNotMatchConstructorHint() {
			runtimeHints.reflection().registerIntrospect().forConstructor(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onConstructor(publicConstructor).invoke());
		}

		@Test
		void constructorInvocationMatchesConstructorInvocationHint() {
			runtimeHints.reflection().registerInvoke().forConstructor(SampleClass.class);
			assertPredicateMatches(reflection.onConstructor(publicConstructor).invoke());
		}

		@Test
		void constructorInvocationDoesNotMatchIntrospectPublicConstructors() {
			runtimeHints.reflection().registerIntrospect().forPublicConstructorsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onConstructor(publicConstructor).invoke());
		}

		@Test
		void constructorInvocationMatchesInvokePublicConstructors() {
			runtimeHints.reflection().registerInvoke().forPublicConstructorsIn(SampleClass.class);
			assertPredicateMatches(reflection.onConstructor(publicConstructor).invoke());
		}

		@Test
		void constructorInvocationDoesNotMatchIntrospectDeclaredConstructors() {
			runtimeHints.reflection().registerIntrospect().forDeclaredConstructorsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onConstructor(publicConstructor).invoke());
		}

		@Test
		void constructorInvocationMatchesInvokeDeclaredConstructors() {
			runtimeHints.reflection().registerInvoke().forDeclaredConstructorsIn(SampleClass.class);
			assertPredicateMatches(reflection.onConstructor(publicConstructor).invoke());
		}

		@Test
		void privateConstructorIntrospectionMatchesConstructorHint() {
			runtimeHints.reflection().registerIntrospect().forConstructor(SampleClass.class, String.class);
			assertPredicateMatches(reflection.onConstructor(privateConstructor).introspect());
		}

		@Test
		void privateConstructorIntrospectionDoesNotMatchIntrospectPublicConstructors() {
			runtimeHints.reflection().registerIntrospect().forPublicConstructorsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).introspect());
		}

		@Test
		void privateConstructorIntrospectionDoesNotMatchInvokePublicConstructors() {
			runtimeHints.reflection().registerInvoke().forPublicConstructorsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).introspect());
		}

		@Test
		void privateConstructorIntrospectionMatchesIntrospectDeclaredConstructors() {
			runtimeHints.reflection().registerIntrospect().forDeclaredConstructorsIn(SampleClass.class);
			assertPredicateMatches(reflection.onConstructor(privateConstructor).introspect());
		}

		@Test
		void privateConstructorIntrospectionMatchesInvokeDeclaredConstructors() {
			runtimeHints.reflection().registerInvoke().forDeclaredConstructorsIn(SampleClass.class);
			assertPredicateMatches(reflection.onConstructor(privateConstructor).introspect());
		}

		@Test
		void privateConstructorInvocationDoesNotMatchConstructorHint() {
			runtimeHints.reflection().registerIntrospect().forConstructor(SampleClass.class, String.class);
			assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).invoke());
		}

		@Test
		void privateConstructorInvocationMatchesConstructorInvocationHint() {
			runtimeHints.reflection().registerInvoke().forConstructor(SampleClass.class, String.class);
			assertPredicateMatches(reflection.onConstructor(privateConstructor).invoke());
		}

		@Test
		void privateConstructorInvocationDoesNotMatchIntrospectPublicConstructors() {
			runtimeHints.reflection().registerIntrospect().forPublicConstructorsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).invoke());
		}

		@Test
		void privateConstructorInvocationDoesNotMatchInvokePublicConstructors() {
			runtimeHints.reflection().registerInvoke().forPublicConstructorsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).invoke());
		}

		@Test
		void privateConstructorInvocationDoesNotMatchIntrospectDeclaredConstructors() {
			runtimeHints.reflection().registerIntrospect().forDeclaredConstructorsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onConstructor(privateConstructor).invoke());
		}

		@Test
		void privateConstructorInvocationMatchesInvokeDeclaredConstructors() {
			runtimeHints.reflection().registerInvoke().forDeclaredConstructorsIn(SampleClass.class);
			assertPredicateMatches(reflection.onConstructor(privateConstructor).invoke());
		}

	}

	@Nested
	class ReflectionOnMethod {

		@Test
		void methodIntrospectionMatchesMethodHint() {
			runtimeHints.reflection().registerIntrospect().forMethod(SampleClass.class, "publicMethod");
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodIntrospectionMatchesIntrospectPublicMethods() {
			runtimeHints.reflection().registerIntrospect().forPublicMethodsIn(SampleClass.class);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodIntrospectionMatchesInvokePublicMethods() {
			runtimeHints.reflection().registerInvoke().forPublicMethodsIn(SampleClass.class);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodIntrospectionMatchesIntrospectDeclaredMethods() {
			runtimeHints.reflection().registerIntrospect().forDeclaredMethodsIn(SampleClass.class);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodIntrospectionMatchesInvokeDeclaredMethods() {
			runtimeHints.reflection().registerInvoke().forPublicMethodsIn(SampleClass.class);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").introspect());
		}

		@Test
		void methodInvocationDoesNotMatchMethodHint() {
			runtimeHints.reflection().registerIntrospect().forMethod(SampleClass.class, "publicMethod");
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationMatchesMethodInvocationHint() {
			runtimeHints.reflection().registerInvoke().forMethod(SampleClass.class, "publicMethod");
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationDoesNotMatchIntrospectPublicMethods() {
			runtimeHints.reflection().registerIntrospect().forPublicMethodsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationMatchesInvokePublicMethods() {
			runtimeHints.reflection().registerInvoke().forPublicMethodsIn(SampleClass.class);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationDoesNotMatchIntrospectDeclaredMethods() {
			runtimeHints.reflection().registerIntrospect().forDeclaredMethodsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void methodInvocationMatchesInvokeDeclaredMethods() {
			runtimeHints.reflection().registerInvoke().forDeclaredMethodsIn(SampleClass.class);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "publicMethod").invoke());
		}

		@Test
		void privateMethodIntrospectionMatchesMethodHint() {
			runtimeHints.reflection().registerIntrospect().forMethod(SampleClass.class, "privateMethod");
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodIntrospectionDoesNotMatchIntrospectPublicMethods() {
			runtimeHints.reflection().registerIntrospect().forPublicMethodsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodIntrospectionDoesNotMatchInvokePublicMethods() {
			runtimeHints.reflection().registerInvoke().forPublicMethodsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodIntrospectionMatchesIntrospectDeclaredMethods() {
			runtimeHints.reflection().registerIntrospect().forDeclaredMethodsIn(SampleClass.class);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodIntrospectionMatchesInvokeDeclaredMethods() {
			runtimeHints.reflection().registerInvoke().forDeclaredMethodsIn(SampleClass.class);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").introspect());
		}

		@Test
		void privateMethodInvocationDoesNotMatchMethodHint() {
			runtimeHints.reflection().registerIntrospect().forMethod(SampleClass.class, "privateMethod");
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationMatchesMethodInvocationHint() {
			runtimeHints.reflection().registerInvoke().forMethod(SampleClass.class, "privateMethod");
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationDoesNotMatchIntrospectPublicMethods() {
			runtimeHints.reflection().registerIntrospect().forPublicMethodsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationDoesNotMatchInvokePublicMethods() {
			runtimeHints.reflection().registerInvoke().forPublicMethodsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationDoesNotMatchIntrospectDeclaredMethods() {
			runtimeHints.reflection().registerIntrospect().forDeclaredMethodsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

		@Test
		void privateMethodInvocationMatchesInvokeDeclaredMethods() {
			runtimeHints.reflection().registerInvoke().forDeclaredMethodsIn(SampleClass.class);
			assertPredicateMatches(reflection.onMethod(SampleClass.class, "privateMethod").invoke());
		}

	}

	@Nested
	class ReflectionOnField {

		@Test
		void shouldFailForMissingField() {
			assertThatIllegalArgumentException().isThrownBy(() -> reflection.onField(SampleClass.class, "missingField"));
		}

		@Test
		void fieldReflectionMatchesFieldHint() {
			runtimeHints.reflection().registerRead().forField(SampleClass.class, "publicField");
			assertPredicateMatches(reflection.onField(SampleClass.class, "publicField"));
		}

		@Test
		void fieldWriteReflectionDoesNotMatchFieldHint() {
			runtimeHints.reflection().registerRead().forField(SampleClass.class, "publicField");
			assertPredicateDoesNotMatch(reflection.onField(SampleClass.class, "publicField").allowWrite());
		}

		@Test
		void fieldUnsafeReflectionDoesNotMatchFieldHint() {
			runtimeHints.reflection().registerRead().forField(SampleClass.class, "publicField");
			assertPredicateDoesNotMatch(reflection.onField(SampleClass.class, "publicField").allowUnsafeAccess());
		}

		@Test
		void fieldWriteReflectionMatchesFieldHintWithWrite() {
			runtimeHints.reflection().registerWrite().forField(SampleClass.class, "publicField");
			assertPredicateMatches(reflection.onField(SampleClass.class, "publicField").allowWrite());
		}

		@Test
		void fieldUnsafeReflectionMatchesFieldHintWithUnsafe() {
			runtimeHints.reflection().registerRead().withAllowUnsafeAccess().forField(SampleClass.class, "publicField");
			assertPredicateMatches(reflection.onField(SampleClass.class, "publicField").allowUnsafeAccess());
		}

		@Test
		void fieldReflectionMatchesPublicFieldsHint() {
			runtimeHints.reflection().registerWrite().forPublicFieldsIn(SampleClass.class);
			assertPredicateMatches(reflection.onField(SampleClass.class, "publicField"));
		}

		@Test
		void fieldReflectionMatchesDeclaredFieldsHint() {
			runtimeHints.reflection().registerWrite().forDeclaredFieldsIn(SampleClass.class);
			assertPredicateMatches(reflection.onField(SampleClass.class, "publicField"));
		}

		@Test
		void privateFieldReflectionMatchesFieldHint() {
			runtimeHints.reflection().registerRead().forField(SampleClass.class, "privateField");
			assertPredicateMatches(reflection.onField(SampleClass.class, "privateField"));
		}

		@Test
		void privateFieldReflectionDoesNotMatchPublicFieldsHint() {
			runtimeHints.reflection().registerWrite().forPublicFieldsIn(SampleClass.class);
			assertPredicateDoesNotMatch(reflection.onField(SampleClass.class, "privateField"));
		}

		@Test
		void privateFieldReflectionMatchesDeclaredFieldsHint() {
			runtimeHints.reflection().registerWrite().forDeclaredFieldsIn(SampleClass.class);
			assertPredicateMatches(reflection.onField(SampleClass.class, "privateField"));
		}

	}

	private void assertPredicateMatches(Predicate<RuntimeHints> predicate) {
		assertThat(predicate).accepts(this.runtimeHints);
	}

	private void assertPredicateDoesNotMatch(Predicate<RuntimeHints> predicate) {
		assertThat(predicate).rejects(this.runtimeHints);
	}


	@SuppressWarnings("unused")
	static class SampleClass {

		private String privateField;

		public String publicField;

		public SampleClass() {
		}

		private SampleClass(String message) {
		}

		public void publicMethod() {
		}

		private void privateMethod() {
		}

	}

}
