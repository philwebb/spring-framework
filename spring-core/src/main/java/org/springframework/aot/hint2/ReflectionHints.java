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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.springframework.aot.hint2.ReflectionTypeHint.Category;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Hints for runtime reflection needs.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @see ReflectionTypeHint
 * @see RuntimeHints
 */
public class ReflectionHints {

	private final Map<TypeReference, ReflectionTypeHint> typeHints = new ConcurrentHashMap<>();

	/**
	 * Registration methods for {@link Category#PUBLIC_CLASSES public classes}
	 * support.
	 * @return public classes registration methods
	 */
	public TypeRegistration registerPublicClasses() {
		return new TypeRegistration(Category.PUBLIC_CLASSES);
	}

	/**
	 * Registration methods for {@link Category#DECLARED_CLASSES declared
	 * classes} support.
	 * @return declared classes registration methods
	 */
	public TypeRegistration registerDeclaredClasses() {
		return new TypeRegistration(Category.DECLARED_CLASSES);
	}

	/**
	 * Registration methods for fields with {@link FieldMode#READ read} support.
	 * @return field read registration methods
	 */
	public FieldRegistration registerRead() {
		return new FieldRegistration(FieldMode.READ, false);
	}

	/**
	 * Registration methods for fields with {@link FieldMode#WRITE write}
	 * support.
	 * @return field write registration methods
	 */
	public FieldRegistration registerWrite() {
		return new FieldRegistration(FieldMode.WRITE, false);
	}

	/**
	 * Registration methods for constructors or methods with
	 * {@link ExecutableMode#INTROSPECT introspect} support.
	 * @return method introspect registration methods
	 */
	public MethodRegistration registerIntrospect() {
		return new MethodRegistration(ExecutableMode.INTROSPECT);
	}

	/**
	 * Registration methods for constructor or methods with
	 * {@link ExecutableMode#INVOKE invoke} support.
	 * @return method invoke registration methods
	 */
	public MethodRegistration registerInvoke() {
		return new MethodRegistration(ExecutableMode.INVOKE);
	}

	/**
	 * Return an unordered {@link Stream} if {@link ReflectionTypeHint
	 * ReflectionTypeHints} that have been registered.
	 * @return the registered reflection type hints
	 */
	public Stream<ReflectionTypeHint> typeHints() {
		return this.typeHints.values().stream();
	}

	/**
	 * Return the reflection type hint for the type defined by the specified
	 * {@link TypeReference}.
	 * @param type the type to inspect
	 * @return the reflection hints for this type, or {@code null}
	 */
	@Nullable
	public ReflectionTypeHint getTypeHint(Class<?> type) {
		return getReflectionHint(TypeReference.of(type));
	}

	/**
	 * Return the reflection hints for the type defined by the specified
	 * {@link TypeReference}.
	 * @param type the type to inspect
	 * @return the reflection hints for this type, or {@code null}
	 */
	@Nullable
	public ReflectionTypeHint getReflectionHint(TypeReference type) {
		return this.typeHints.get(type);
	}

	Condition update(TypeReference type, UnaryOperator<ReflectionTypeHint> mapper) {
		return update(new TypeReference[] { type }, mapper);
	}

	Condition update(TypeReference[] types, UnaryOperator<ReflectionTypeHint> mapper) {
		for (TypeReference type : types) {
			this.typeHints.compute(type,
					(key, hint) -> mapper.apply((hint != null) ? hint : new ReflectionTypeHint(type)));
		}
		return condition(types);
	}

	Condition condition(TypeReference... types) {
		return new Condition(types, reachableType -> update(types, hint -> hint.andReachableType(reachableType)));
	}

	public abstract class Registration<S extends Registration<S>> extends ReachableTypeRegistration<S> {

		public S whenTypeIsPresent() {
			return whenTypeIsPresent(null);
		}

		public S whenTypeIsPresent(@Nullable ClassLoader classLoader) {
			return null;
		}

	}

	/**
	 * Registration methods for type hints.
	 */
	public class TypeRegistration extends Registration<TypeRegistration> {

		private final Category category;

		TypeRegistration(Category category) {
			this.category = category;
		}

		/**
		 * Complete the reflection hint registration for the given types.
		 * @param types the types to register
		 */
		public void forType(Class<?>... types) {
			forType(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint registration for the given types.
		 * @param types the type names to register
		 */
		public void forType(String... types) {
			forType(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint registration for the given types.
		 * @param types the types to register
		 */
		public void forType(TypeReference... types) {
			update(types, (hint) -> hint.andCategory(this.category));
		}

	}

	/**
	 * Registration methods for field hints.
	 */
	public class FieldRegistration extends Registration<FieldRegistration> {

		private final FieldMode mode;

		private final boolean allowUnsafeAccess;

		FieldRegistration(FieldMode mode, boolean allowUnsafeAccess) {
			this.mode = mode;
			this.allowUnsafeAccess = allowUnsafeAccess;
		}

		/**
		 * Return a new {@link FieldRegistration} that registers items with
		 * "allow unsafe access".
		 * @return a new {@link FieldRegistration} instance
		 */
		public FieldRegistration withAllowUnsafeAccess() {
			return new FieldRegistration(this.mode, true);
		}

		/**
		 * Complete the reflection hint registration by finding a field.
		 * @param declaringClass the class that declares the field
		 * @param name the name of the field
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 */
		public Condition forField(Class<?> declaringClass, String name) {
			Field field = ReflectionUtils.findField(declaringClass, name);
			Assert.state(field != null,
					() -> "Unable to find field '%s' in $s".formatted(name, declaringClass.getName()));
			return forField(field);
		}

		/**
		 * Complete the reflection hint registration for the given fields.
		 * @param fields the fields to register
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 */
		public Condition forField(Field... fields) {
			TypeReference[] types = TypeReference.arrayOf(fields, Field::getDeclaringClass);
			for (int i = 0; i < fields.length; i++) {
				Field field = fields[i];
				update(types[i], hint -> hint.andField(field, this.mode, this.allowUnsafeAccess));
			}
			return condition(types);
		}

		/**
		 * Complete the reflection hint for all public fields in the given
		 * types.
		 * @param types the types to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#PUBLIC_FIELDS
		 */
		public Condition forPublicFieldsIn(Class<?>... types) {
			return forPublicFieldsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint for all public fields in the given
		 * types.
		 * @param types the type names to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#PUBLIC_FIELDS
		 */
		public Condition forPublicFieldsIn(String... types) {
			return forPublicFieldsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint for all public fields in the given
		 * types.
		 * @param types the types to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#PUBLIC_FIELDS
		 */
		public Condition forPublicFieldsIn(TypeReference... types) {
			return updateCategory(Category.PUBLIC_FIELDS, types);
		}

		/**
		 * Complete the reflection hint for all declared fields in the given
		 * types.
		 * @param types the types to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#DECLARED_FIELDS
		 */
		public Condition forDeclaredFieldsIn(Class<?>... types) {
			return forDeclaredFieldsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint for all declared fields in the given
		 * types.
		 * @param types the types to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#DECLARED_FIELDS
		 */
		public Condition forDeclaredFieldsIn(String... types) {
			return forDeclaredFieldsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint for all declared fields in the given
		 * types.
		 * @param types the types to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#DECLARED_FIELDS
		 */
		public Condition forDeclaredFieldsIn(TypeReference... types) {
			return updateCategory(Category.DECLARED_FIELDS, types);
		}

		private Condition updateCategory(Category category, TypeReference... types) {
			Assert.state(!this.allowUnsafeAccess, "'allowUnsafeAccess' cannot be set when finding fields in a type");
			return update(types, hint -> hint.andCategory(category));
		}

	}

	/**
	 * Registration methods for method hints.
	 */
	public class MethodRegistration {

		private final ExecutableMode mode;

		MethodRegistration(ExecutableMode mode) {
			this.mode = mode;
		}

		/**
		 * Complete the reflection hint registration by finding a method.
		 * @param declaringClass the class that declares the method
		 * @param name the name of the method
		 * @param parameterTypes the method parameter types
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 */
		public Condition forMethod(Class<?> declaringClass, String name, Class<?>... parameterTypes) {
			Method method = ReflectionUtils.findMethod(declaringClass, name, parameterTypes);
			Assert.state(method != null,
					() -> "Unable to find method %s in class %s".formatted(name, declaringClass.getName()));
			return forMethod(method);
		}

		/**
		 * Complete the reflection hint registration for the given methods.
		 * @param methods the methods to register
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 */
		public Condition forMethod(Method... methods) {
			TypeReference[] types = TypeReference.arrayOf(methods, Method::getDeclaringClass);
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				update(types[i], hint -> hint.andMethod(method, this.mode));
			}
			return condition(types);
		}

		/**
		 * Complete the reflection hint registration by finding a constructor.
		 * @param declaringClass the class that declares the constructor
		 * @param parameterTypes the method parameter types
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 */
		public Condition forConstructor(Class<?> declaringClass, Class<?>... parameterTypes) {
			try {
				return forConstructor(declaringClass.getDeclaredConstructor(parameterTypes));
			}
			catch (NoSuchMethodException | SecurityException ex) {
				throw new IllegalStateException(
						"Unable to find constructor in class %s".formatted(declaringClass.getName()));
			}
		}

		/**
		 * Complete the reflection hint registration for the given constructors.
		 * @param constructors the constructors to register
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 */
		public Condition forConstructor(Constructor<?>... constructors) {
			TypeReference[] types = TypeReference.arrayOf(constructors, Constructor::getDeclaringClass);
			for (int i = 0; i < constructors.length; i++) {
				Constructor<?> constructor = constructors[i];
				update(types[i], hint -> hint.andConstructor(constructor, this.mode));
			}
			return condition(types);
		}

		/**
		 * Complete the reflection hint for all public constructors in the given
		 * types.
		 * @param types the types to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#INTROSPECT_PUBLIC_CONSTRUCTORS
		 * @see Category#INVOKE_PUBLIC_CONSTRUCTORS
		 */
		public Condition forPublicConstructorsIn(Class<?>... types) {
			return forPublicConstructorsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint for all public constructors in the given
		 * types.
		 * @param types the type names to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#INTROSPECT_PUBLIC_CONSTRUCTORS
		 * @see Category#INVOKE_PUBLIC_CONSTRUCTORS
		 */
		public Condition forPublicConstructorsIn(String... types) {
			return forPublicConstructorsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint for all public constructors in the given
		 * types.
		 * @param types the types to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#INTROSPECT_PUBLIC_CONSTRUCTORS
		 * @see Category#INVOKE_PUBLIC_CONSTRUCTORS
		 */
		public Condition forPublicConstructorsIn(TypeReference... types) {
			return updateCategory(Category.INTROSPECT_PUBLIC_CONSTRUCTORS, Category.INVOKE_PUBLIC_CONSTRUCTORS, types);
		}

		/**
		 * Complete the reflection hint for all declared constructors in the
		 * given types.
		 * @param types the types to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#INTROSPECT_DECLARED_CONSTRUCTORS
		 * @see Category#INVOKE_DECLARED_CONSTRUCTORS
		 */
		public Condition forDeclaredConstructorsIn(Class<?>... types) {
			return forDeclaredConstructorsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint for all declared constructors in the
		 * given types.
		 * @param types the type names to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#INTROSPECT_DECLARED_CONSTRUCTORS
		 * @see Category#INVOKE_DECLARED_CONSTRUCTORS
		 */
		public Condition forDeclaredConstructorsIn(String... types) {
			return forDeclaredConstructorsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint for all declared constructors in the
		 * given types.
		 * @param types the types to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#INTROSPECT_DECLARED_CONSTRUCTORS
		 * @see Category#INVOKE_DECLARED_CONSTRUCTORS
		 */
		public Condition forDeclaredConstructorsIn(TypeReference... types) {
			return updateCategory(Category.INTROSPECT_DECLARED_CONSTRUCTORS, Category.INVOKE_DECLARED_CONSTRUCTORS,
					types);
		}

		/**
		 * Complete the reflection hint for all public methods in the given
		 * types.
		 * @param types the types to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#INTROSPECT_PUBLIC_METHODS
		 * @see Category#INVOKE_PUBLIC_METHODS
		 */
		public Condition forPublicMethodsIn(Class<?>... types) {
			return forPublicMethodsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint for all public methods in the given
		 * types.
		 * @param types the type names to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#INTROSPECT_PUBLIC_METHODS
		 * @see Category#INVOKE_PUBLIC_METHODS
		 */
		public Condition forPublicMethodsIn(String... types) {
			return forPublicMethodsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint for all public methods in the given
		 * types.
		 * @param types the types to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#INTROSPECT_PUBLIC_METHODS
		 * @see Category#INVOKE_PUBLIC_METHODS
		 */
		public Condition forPublicMethodsIn(TypeReference... types) {
			return updateCategory(Category.INTROSPECT_PUBLIC_METHODS, Category.INVOKE_PUBLIC_METHODS, types);
		}

		/**
		 * Complete the reflection hint for all declared methods in the given
		 * types.
		 * @param types the types to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#INTROSPECT_DECLARED_METHODS
		 * @see Category#INVOKE_DECLARED_METHODS
		 */
		public Condition forDeclaredMethodsIn(Class<?>... types) {
			return forDeclaredMethodsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint for all declared methods in the given
		 * types.
		 * @param types the type names to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#INTROSPECT_DECLARED_METHODS
		 * @see Category#INVOKE_DECLARED_METHODS
		 */
		public Condition forDeclaredMethodsIn(String... types) {
			return forDeclaredMethodsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint for all declared methods in the given
		 * types.
		 * @param types the types to consider
		 * @return a {@link Condition} class that can be used to apply
		 * conditions
		 * @see Category#INTROSPECT_DECLARED_METHODS
		 * @see Category#INVOKE_DECLARED_METHODS
		 */
		public Condition forDeclaredMethodsIn(TypeReference... types) {
			return updateCategory(Category.INTROSPECT_DECLARED_METHODS, Category.INVOKE_DECLARED_METHODS, types);
		}

		private Condition updateCategory(Category introspectCategory, Category invokeCategory, TypeReference... types) {
			Category category = switch (this.mode) {
			case INTROSPECT -> introspectCategory;
			case INVOKE -> invokeCategory;
			};
			return update(types, hint -> hint.andCategory(category));
		}

	}

	/**
	 * {@link ReachableTypeRegistration} for reflection hints.
	 */
	public class Condition {
	}

}
