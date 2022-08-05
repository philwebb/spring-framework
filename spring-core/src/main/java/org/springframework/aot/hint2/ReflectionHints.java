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
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.springframework.aot.hint2.ReflectionHint.Category;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Hints for runtime reflection needs.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @see RuntimeHints
 */
public class ReflectionHints implements Iterable<ReflectionHint> {

	private final Map<TypeReference, ReflectionHint> hints = new ConcurrentHashMap<>();

	public TypeRegistration registerPublicClasses() {
		return new TypeRegistration(Category.PUBLIC_CLASSES);
	}

	public TypeRegistration registerDeclaredClasses() {
		return new TypeRegistration(Category.DECLARED_CLASSES);
	}

	public FieldRegistration registerRead() {
		return new FieldRegistration(FieldMode.READ, false);
	}

	public FieldRegistration registerWrite() {
		return new FieldRegistration(FieldMode.WRITE, false);
	}

	public MethodRegistration registerIntrospect() {
		return new MethodRegistration(ExecutableMode.INTROSPECT);
	}

	public MethodRegistration registerInvoke() {
		return new MethodRegistration(ExecutableMode.INVOKE);
	}

	@Override
	public Iterator<ReflectionHint> iterator() {
		return this.hints.values().iterator();
	}

	public Stream<ReflectionHint> stream() {
		return this.hints.values().stream();
	}

	public ReflectionHint get(Class<?> type) {
		return get(TypeReference.of(type));
	}

	public ReflectionHint get(TypeReference type) {
		return this.hints.getOrDefault(type, ReflectionHint.NONE);
	}

	ConditionRegistration update(TypeReference type,
			UnaryOperator<ReflectionHint> mapper) {
		return update(new TypeReference[] { type }, mapper);
	}

	ConditionRegistration update(TypeReference[] types,
			UnaryOperator<ReflectionHint> mapper) {
		for (TypeReference type : types) {
			this.hints.compute(type, (key, hint) -> mapper
					.apply((hint != null) ? hint : new ReflectionHint(type)));
		}
		return new ConditionRegistration(types);
	}

	public class TypeRegistration {

		private final Category category;

		TypeRegistration(Category category) {
			this.category = category;
		}

		public ConditionRegistration forTypes(Class<?>... types) {
			return forTypes(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forTypes(String... types) {
			return forTypes(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forTypes(TypeReference... types) {
			return update(types, (hint) -> hint.andCategory(this.category));
		}

	}

	public class FieldRegistration {

		private final FieldMode mode;

		private final boolean allowUnsafeAccess;

		FieldRegistration(FieldMode mode, boolean allowUnsafeAccess) {
			this.mode = mode;
			this.allowUnsafeAccess = allowUnsafeAccess;
		}

		public FieldRegistration withAllowUnsafeAccess() {
			return new FieldRegistration(this.mode, true);
		}

		public ConditionRegistration forField(Class<?> declaringClass, String name) {
			Field field = ReflectionUtils.findField(declaringClass, name);
			Assert.state(field != null, () -> "Unable to find field '%s' in $s"
					.formatted(name, declaringClass.getName()));
			return forField(field);
		}

		public ConditionRegistration forField(Field field) {
			TypeReference type = TypeReference.of(field.getDeclaringClass());
			return update(type,
					hint -> hint.andField(field, this.mode, this.allowUnsafeAccess));
		}

		public ConditionRegistration forPublicFieldsIn(Class<?>... types) {
			return forPublicFieldsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forPublicFieldsIn(String... types) {
			return forPublicFieldsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forPublicFieldsIn(TypeReference... types) {
			return updateCategory(Category.PUBLIC_FIELDS, types);
		}

		public ConditionRegistration forDeclaredFieldsIn(Class<?>... types) {
			return forDeclaredFieldsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forDeclaredFieldsIn(String... types) {
			return forDeclaredFieldsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forDeclaredFieldsIn(TypeReference... types) {
			return updateCategory(Category.DECLARED_FIELDS, types);
		}

		private ConditionRegistration updateCategory(Category category,
				TypeReference... types) {
			Assert.state(!this.allowUnsafeAccess,
					"'allowUnsafeAccess' cannot be set when finding fields in a type");
			return update(types, hint -> hint.andCategory(category));
		}

	}

	public class MethodRegistration {

		private final ExecutableMode mode;

		MethodRegistration(ExecutableMode mode) {
			this.mode = mode;
		}

		public ConditionRegistration forMethod(Class<?> declaringClass, String name,
				Class<?>... parameterTypes) {
			Method method = ReflectionUtils.findMethod(declaringClass, name,
					parameterTypes);
			Assert.state(method != null, () -> "Unable to find method %s in class %s"
					.formatted(name, declaringClass.getName()));
			return forMethod(method);
		}

		public ConditionRegistration forMethod(Method method) {
			TypeReference type = TypeReference.of(method.getDeclaringClass());
			return update(type, hint -> hint.andMethod(method, this.mode));
		}

		public ConditionRegistration forConstructor(Class<?> declaringClass,
				Class<?>... parameterTypes) {
			try {
				return forConstructor(declaringClass.getDeclaredConstructor(parameterTypes));
			}
			catch (NoSuchMethodException | SecurityException ex) {
				throw new IllegalStateException("Unable to find constructor in class %s"
						.formatted(declaringClass.getName()));
			}
		}

		public ConditionRegistration forConstructor(Constructor<?> constructor) {
			TypeReference type = TypeReference.of(constructor.getDeclaringClass());
			return update(type, hint -> hint.andConstructor(constructor, this.mode));
		}

		public ConditionRegistration forPublicConstructorsIn(Class<?>... types) {
			return forPublicConstructorsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forPublicConstructorsIn(String... types) {
			return forPublicConstructorsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forPublicConstructorsIn(TypeReference... types) {
			return updateCategory(Category.INTROSPECT_PUBLIC_CONSTRUCTORS,
					Category.INVOKE_PUBLIC_CONSTRUCTORS, types);
		}

		public ConditionRegistration forDeclaredConstructorsIn(Class<?>... types) {
			return forDeclaredConstructorsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forDeclaredConstructorsIn(String... types) {
			return forDeclaredConstructorsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forDeclaredConstructorsIn(TypeReference... types) {
			return updateCategory(Category.INTROSPECT_DECLARED_CONSTRUCTORS,
					Category.INVOKE_DECLARED_CONSTRUCTORS, types);
		}

		public ConditionRegistration forPublicMethodsIn(Class<?>... types) {
			return forPublicMethodsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forPublicMethodsIn(String... types) {
			return forPublicMethodsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forPublicMethodsIn(TypeReference... types) {
			return updateCategory(Category.INTROSPECT_PUBLIC_METHODS,
					Category.INVOKE_PUBLIC_METHODS, types);
		}

		public ConditionRegistration forDeclaredMethodsIn(Class<?>... types) {
			return forDeclaredMethodsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forDeclaredMethodsIn(String... types) {
			return forDeclaredMethodsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forDeclaredMethodsIn(TypeReference... types) {
			return updateCategory(Category.INTROSPECT_DECLARED_METHODS,
					Category.INVOKE_DECLARED_METHODS, types);
		}

		private ConditionRegistration updateCategory(Category introspectCategory,
				Category invokeCategory, TypeReference... types) {
			Category category = switch (this.mode) {
			case INTROSPECT -> introspectCategory;
			case INVOKE -> invokeCategory;
			};
			return update(types, hint -> hint.andCategory(category));
		}

	}

	public class ConditionRegistration
			extends AbstractConditionalRegistration<ConditionRegistration> {

		private final TypeReference[] types;

		ConditionRegistration(TypeReference... types) {
			this.types = types;
		}

		@Override
		protected void apply(TypeReference reachableType) {
			update(this.types, hint -> hint.andReachableType(reachableType));
		}

	}

}
