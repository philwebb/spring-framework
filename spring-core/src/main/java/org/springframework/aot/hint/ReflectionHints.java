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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.springframework.aot.hint.JavaReflectionHint.Category;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Hints for runtime reflection needs.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @see JavaReflectionHint
 * @see RuntimeHints
 */
public class ReflectionHints {

	private final Map<TypeReference, JavaReflectionHint> javaReflectionHints = new ConcurrentHashMap<>();


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
		return new FieldRegistration(FieldMode.READ);
	}

	/**
	 * Registration methods for fields with {@link FieldMode#WRITE write}
	 * support.
	 * @return field write registration methods
	 */
	public FieldRegistration registerWrite() {
		return new FieldRegistration(FieldMode.WRITE);
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
	 * Return an unordered {@link Stream} of the {@link JavaReflectionHint
	 * reflection hints} that have been registered.
	 * @return the registered reflection type hints
	 */
	public Stream<JavaReflectionHint> javaReflection() {
		return this.javaReflectionHints.values().stream();
	}

	/**
	 * Return the reflection type hint for the type defined by the specified
	 * {@link TypeReference}.
	 * @param type the type to inspect
	 * @return the reflection hints for this type, or {@code null}
	 */
	@Nullable
	public JavaReflectionHint getJavaReflectionHint(Class<?> type) {
		return getJavaReflectionHint(TypeReference.of(type));
	}

	/**
	 * Return the reflection type hint for the type defined by the specified
	 * {@link TypeReference}.
	 * @param type the type to inspect
	 * @return the reflection hints for this type, or {@code null}
	 */
	@Nullable
	public JavaReflectionHint getJavaReflectionHint(TypeReference type) {
		return this.javaReflectionHints.get(type);
	}

	void update(TypeReference type, ReflectionRegistration<?> registration, UnaryOperator<JavaReflectionHint> mapper) {
		update(new TypeReference[] { type }, registration, mapper);
	}

	void update(TypeReference[] types, ReflectionRegistration<?> registration,
			UnaryOperator<JavaReflectionHint> mapper) {
		for (TypeReference type : types) {
			if (registration.getPredicate().test(type)) {
				this.javaReflectionHints.compute(type, (key, hint) -> {
					TypeReference reachableType = registration.getReachableType();
					hint = (hint != null) ? hint : new JavaReflectionHint(type);
					hint = mapper.apply(hint);
					hint = (reachableType != null) ? hint.andReachableType(reachableType) : hint;
					return hint;
				});
			}
		}
	}


	/**
	 * Registration methods for reflection hints.
	 */
	public abstract class ReflectionRegistration<S extends ReflectionRegistration<S>>
			extends ReachableTypeRegistration<S> {

		private Predicate<TypeReference> predicate = type -> true;


		/**
		 * Only register when the target type is present in the
		 * {@link ClassUtils#getDefaultClassLoader() default classloader}.
		 * @return this instance
		 */
		public S whenTypeIsPresent() {
			return whenTypeIsPresent(null);
		}

		/**
		 * Only register when the target type is present in the specified class
		 * loader.
		 * @param classLoader the class loader to check
		 * @return this instance
		 */
		public S whenTypeIsPresent(@Nullable ClassLoader classLoader) {
			return when(type -> ClassUtils.isPresent(type.getCanonicalName(), classLoader));
		}

		/**
		 * Only register when the target type matches the given predicate.
		 * @param predicate the predicate used to test the target type
		 * @return this instance
		 */
		public S when(Predicate<TypeReference> predicate) {
			Assert.notNull(predicate, "'predicate' must not be null");
			this.predicate = predicate.and(predicate);
			return self();
		}

		Predicate<TypeReference> getPredicate() {
			return this.predicate;
		}

	}


	/**
	 * Registration methods for type hints.
	 */
	public final class TypeRegistration extends ReflectionRegistration<TypeRegistration> {

		private final Category category;


		TypeRegistration(Category category) {
			this.category = category;
		}


		/**
		 * Complete the hint registration for the given types.
		 * @param types the types to register
		 */
		public void forType(Class<?>... types) {
			forType(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration for the given types.
		 * @param types the type names to register
		 */
		public void forType(String... types) {
			forType(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration for the given types.
		 * @param types the types to register
		 */
		public void forType(TypeReference... types) {
			update(types, this, (hint) -> hint.andCategory(this.category));
		}

	}


	/**
	 * Registration methods for field hints.
	 */
	public final class FieldRegistration extends ReflectionRegistration<FieldRegistration> {

		private final FieldMode mode;

		private boolean allowUnsafeAccess;


		FieldRegistration(FieldMode mode) {
			this.mode = mode;
		}


		/**
		 * Register with "allow unsafe access".
		 * @return this instance
		 */
		public FieldRegistration withAllowUnsafeAccess() {
			this.allowUnsafeAccess = true;
			return self();
		}

		/**
		 * Complete the hint registration by finding a field.
		 * @param declaringClass the class that declares the field
		 * @param name the name of the field
		 */
		public void forField(Class<?> declaringClass, String name) {
			Field field = ReflectionUtils.findField(declaringClass, name);
			Assert.state(field != null,
					() -> "Unable to find field '%s' in %s".formatted(name, declaringClass.getName()));
			forField(field);
		}

		/**
		 * Complete the hint registration for the given fields.
		 * @param fields the fields to register
		 */
		public void forField(Field... fields) {
			TypeReference[] types = TypeReference.arrayOf(fields, Field::getDeclaringClass);
			for (int i = 0; i < fields.length; i++) {
				Field field = fields[i];
				update(types[i], this, hint -> hint.andField(field, this.mode, this.allowUnsafeAccess));
			}
		}

		/**
		 * Complete the hint registration for all public fields in the given
		 * types.
		 * @param types the types to consider
		 * @see Category#PUBLIC_FIELDS
		 */
		public void forPublicFieldsIn(Class<?>... types) {
			forPublicFieldsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration for all public fields in the given
		 * types.
		 * @param types the type names to consider
		 * @see Category#PUBLIC_FIELDS
		 */
		public void forPublicFieldsIn(String... types) {
			forPublicFieldsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration for all public fields in the given
		 * types.
		 * @param types the types to consider
		 * @see Category#PUBLIC_FIELDS
		 */
		public void forPublicFieldsIn(TypeReference... types) {
			updateCategory(Category.PUBLIC_FIELDS, types);
		}

		/**
		 * Complete the hint registration for all declared fields in the given
		 * types.
		 * @param types the types to consider
		 * @see Category#DECLARED_FIELDS
		 */
		public void forDeclaredFieldsIn(Class<?>... types) {
			forDeclaredFieldsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration for all declared fields in the given
		 * types.
		 * @param types the types to consider
		 * @see Category#DECLARED_FIELDS
		 */
		public void forDeclaredFieldsIn(String... types) {
			forDeclaredFieldsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration for all declared fields in the given
		 * types.
		 * @param types the types to consider
		 * @see Category#DECLARED_FIELDS
		 */
		public void forDeclaredFieldsIn(TypeReference... types) {
			updateCategory(Category.DECLARED_FIELDS, types);
		}

		private void updateCategory(Category category, TypeReference... types) {
			Assert.state(!this.allowUnsafeAccess, "'allowUnsafeAccess' cannot be set when finding fields in a type");
			update(types, this, hint -> hint.andCategory(category));
		}

	}


	/**
	 * Registration methods for method hints.
	 */
	public final class MethodRegistration extends ReflectionRegistration<MethodRegistration> {

		private final ExecutableMode mode;


		MethodRegistration(ExecutableMode mode) {
			this.mode = mode;
		}


		/**
		 * Complete the hint registration by finding a method.
		 * @param declaringClass the class that declares the method
		 * @param name the name of the method
		 * @param parameterTypes the method parameter types
		 */
		public void forMethod(Class<?> declaringClass, String name, Class<?>... parameterTypes) {
			Method method = ReflectionUtils.findMethod(declaringClass, name, parameterTypes);
			Assert.state(method != null,
					() -> "Unable to find method '%s' in class %s".formatted(name, declaringClass.getName()));
			forMethod(method);
		}

		/**
		 * Complete the hint registration for the given methods.
		 * @param methods the methods to register
		 */
		public void forMethod(Method... methods) {
			TypeReference[] types = TypeReference.arrayOf(methods, Method::getDeclaringClass);
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				update(types[i], this, hint -> hint.andMethod(method, this.mode));
			}
		}

		/**
		 * Complete the hint registration by finding a constructor.
		 * @param declaringClass the class that declares the constructor
		 * @param parameterTypes the method parameter types
		 */
		public void forConstructor(Class<?> declaringClass, Class<?>... parameterTypes) {
			try {
				forConstructor(declaringClass.getDeclaredConstructor(parameterTypes));
			}
			catch (NoSuchMethodException | SecurityException ex) {
				throw new IllegalStateException(
						"Unable to find constructor in class %s".formatted(declaringClass.getName()));
			}
		}

		/**
		 * Complete the hint registration for the given constructors.
		 * @param constructors the constructors to register
		 */
		public void forConstructor(Constructor<?>... constructors) {
			TypeReference[] types = TypeReference.arrayOf(constructors, Constructor::getDeclaringClass);
			for (int i = 0; i < constructors.length; i++) {
				Constructor<?> constructor = constructors[i];
				update(types[i], this, hint -> hint.andConstructor(constructor, this.mode));
			}
		}

		/**
		 * Complete the hint registration for all public constructors in the
		 * given types.
		 * @param types the types to consider
		 * @see Category#INTROSPECT_PUBLIC_CONSTRUCTORS
		 * @see Category#INVOKE_PUBLIC_CONSTRUCTORS
		 */
		public void forPublicConstructorsIn(Class<?>... types) {
			forPublicConstructorsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration for all public constructors in the
		 * given types.
		 * @param types the type names to consider
		 * @see Category#INTROSPECT_PUBLIC_CONSTRUCTORS
		 * @see Category#INVOKE_PUBLIC_CONSTRUCTORS
		 */
		public void forPublicConstructorsIn(String... types) {
			forPublicConstructorsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration for all public constructors in the
		 * given types.
		 * @param types the types to consider
		 * @see Category#INTROSPECT_PUBLIC_CONSTRUCTORS
		 * @see Category#INVOKE_PUBLIC_CONSTRUCTORS
		 */
		public void forPublicConstructorsIn(TypeReference... types) {
			updateCategory(Category.INTROSPECT_PUBLIC_CONSTRUCTORS, Category.INVOKE_PUBLIC_CONSTRUCTORS, types);
		}

		/**
		 * Complete the hint registration for all declared constructors in the
		 * given types.
		 * @param types the types to consider
		 * @see Category#INTROSPECT_DECLARED_CONSTRUCTORS
		 * @see Category#INVOKE_DECLARED_CONSTRUCTORS
		 */
		public void forDeclaredConstructorsIn(Class<?>... types) {
			forDeclaredConstructorsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration for all declared constructors in the
		 * given types.
		 * @param types the type names to consider
		 * @see Category#INTROSPECT_DECLARED_CONSTRUCTORS
		 * @see Category#INVOKE_DECLARED_CONSTRUCTORS
		 */
		public void forDeclaredConstructorsIn(String... types) {
			forDeclaredConstructorsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration for all declared constructors in the
		 * given types.
		 * @param types the types to consider
		 * @see Category#INTROSPECT_DECLARED_CONSTRUCTORS
		 * @see Category#INVOKE_DECLARED_CONSTRUCTORS
		 */
		public void forDeclaredConstructorsIn(TypeReference... types) {
			updateCategory(Category.INTROSPECT_DECLARED_CONSTRUCTORS, Category.INVOKE_DECLARED_CONSTRUCTORS, types);
		}

		/**
		 * Complete the hint registration for all public methods in the given
		 * types.
		 * @param types the types to consider
		 * @see Category#INTROSPECT_PUBLIC_METHODS
		 * @see Category#INVOKE_PUBLIC_METHODS
		 */
		public void forPublicMethodsIn(Class<?>... types) {
			forPublicMethodsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration for all public methods in the given
		 * types.
		 * @param types the type names to consider
		 * @see Category#INTROSPECT_PUBLIC_METHODS
		 * @see Category#INVOKE_PUBLIC_METHODS
		 */
		public void forPublicMethodsIn(String... types) {
			forPublicMethodsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the reflection hint for all public methods in the given
		 * types.
		 * @param types the types to consider
		 * @see Category#INTROSPECT_PUBLIC_METHODS
		 * @see Category#INVOKE_PUBLIC_METHODS
		 */
		public void forPublicMethodsIn(TypeReference... types) {
			updateCategory(Category.INTROSPECT_PUBLIC_METHODS, Category.INVOKE_PUBLIC_METHODS, types);
		}

		/**
		 * Complete the hint registration for all declared methods in the given
		 * types.
		 * @param types the types to consider
		 * @see Category#INTROSPECT_DECLARED_METHODS
		 * @see Category#INVOKE_DECLARED_METHODS
		 */
		public void forDeclaredMethodsIn(Class<?>... types) {
			forDeclaredMethodsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration for all declared methods in the given
		 * types.
		 * @param types the type names to consider
		 * @see Category#INTROSPECT_DECLARED_METHODS
		 * @see Category#INVOKE_DECLARED_METHODS
		 */
		public void forDeclaredMethodsIn(String... types) {
			forDeclaredMethodsIn(TypeReference.arrayOf(types));
		}

		/**
		 * Complete the hint registration for all declared methods in the given
		 * types.
		 * @param types the types to consider
		 * @see Category#INTROSPECT_DECLARED_METHODS
		 * @see Category#INVOKE_DECLARED_METHODS
		 */
		public void forDeclaredMethodsIn(TypeReference... types) {
			updateCategory(Category.INTROSPECT_DECLARED_METHODS, Category.INVOKE_DECLARED_METHODS, types);
		}

		private void updateCategory(Category introspectCategory, Category invokeCategory, TypeReference... types) {
			Category category = switch (this.mode) {
			case INTROSPECT -> introspectCategory;
			case INVOKE -> invokeCategory;
			};
			update(types, this, hint -> hint.andCategory(category));
		}

	}

}
