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
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A immutable hint that describes the need for reflection on a type.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Clement
 * @author Sebastien Deleuze
 * @since 6.0
 * @see ReflectionHints
 */
public final class JavaReflectionHint implements ConditionalHint {

	private final TypeReference type;

	@Nullable
	private final TypeReference reachableType;

	private final Set<Category> categories;

	private final Map<Field, FieldHint> fields;

	private final Map<Constructor<?>, ConstructorHint> constructors;

	private final Map<Method, MethodHint> methods;


	JavaReflectionHint(TypeReference type) {
		Assert.notNull(type, "'type' must not be null");
		this.type = type;
		this.reachableType = null;
		this.categories = Collections.emptySet();
		this.fields = Collections.emptyMap();
		this.constructors = Collections.emptyMap();
		this.methods = Collections.emptyMap();
	}

	private JavaReflectionHint(TypeReference type, TypeReference reachableType, Set<Category> categories,
			Map<Field, FieldHint> fields, Map<Constructor<?>, ConstructorHint> constructors,
			Map<Method, MethodHint> methods) {
		this.type = type;
		this.reachableType = reachableType;
		this.categories = categories;
		this.fields = fields;
		this.constructors = constructors;
		this.methods = methods;
	}


	JavaReflectionHint andReachableType(TypeReference reachableType) {
		if (Objects.equals(this.reachableType, reachableType)) {
			return this;
		}
		Assert.state(this.reachableType == null, "A reachableType condition has already been applied");
		return new JavaReflectionHint(this.type, reachableType, this.categories, this.fields, this.constructors,
				this.methods);
	}

	JavaReflectionHint andCategory(Category category) {
		if (this.categories.contains(category)) {
			return this;
		}
		EnumSet<Category> categories = EnumSet.of(category);
		categories.addAll(this.categories);
		return new JavaReflectionHint(this.type, this.reachableType, Set.copyOf(categories), this.fields,
				this.constructors, this.methods);
	}

	JavaReflectionHint andField(Field field, FieldMode mode, boolean allowUnsafeAccess) {
		Map<Field, FieldHint> fields = new HashMap<>(this.fields);
		fields.compute(field, (key, hint) -> (hint != null) ? hint.and(mode, allowUnsafeAccess)
				: new FieldHint(field, mode, allowUnsafeAccess));
		return new JavaReflectionHint(this.type, this.reachableType, this.categories, Map.copyOf(fields),
				this.constructors, this.methods);
	}

	JavaReflectionHint andConstructor(Constructor<?> constructor, ExecutableMode mode) {
		Map<Constructor<?>, ConstructorHint> constructors = new HashMap<>(this.constructors);
		constructors.compute(constructor,
				(key, hint) -> (hint != null) ? hint.and(mode) : new ConstructorHint(constructor, mode));
		return new JavaReflectionHint(this.type, this.reachableType, this.categories, this.fields,
				Map.copyOf(constructors), this.methods);
	}

	JavaReflectionHint andMethod(Method method, ExecutableMode mode) {
		Map<Method, MethodHint> methods = new HashMap<>(this.methods);
		methods.compute(method, (key, hint) -> (hint != null) ? hint.and(mode) : new MethodHint(method, mode));
		return new JavaReflectionHint(this.type, this.reachableType, this.categories, this.fields, this.constructors,
				Map.copyOf(methods));
	}

	/**
	 * Return the type that this hint handles.
	 * @return the type
	 */
	public TypeReference getType() {
		return this.type;
	}

	@Override
	public TypeReference getReachableType() {
		return this.reachableType;
	}

	/**
	 * Return the categories that apply.
	 * @return the categories
	 */
	public Set<Category> getCategories() {
		return this.categories;
	}

	/**
	 * Return an unordered stream of the fields requiring reflection.
	 * @return a stream of {@link FieldHint FieldHints}
	 */
	public Stream<FieldHint> fields() {
		return this.fields.values().stream();
	}

	/**
	 * Return an unordered stream of the constructors requiring reflection.
	 * @return a stream of {@link ConstructorHint ConstructorHints}
	 */
	public Stream<ConstructorHint> constructors() {
		return this.constructors.values().stream();
	}

	/**
	 * Return an unordered stream of the methods requiring reflection.
	 * @return a stream of {@link MethodHint MethodHints}
	 */
	public Stream<MethodHint> methods() {
		return this.methods.values().stream();
	}


	/**
	 * A hint that describes the need of reflection on a {@link Field}.
	 */
	public static final class FieldHint {

		private final Field field;

		private final FieldMode mode;

		private final boolean allowUnsafeAccess;


		FieldHint(Field field, FieldMode mode, boolean allowUnsafeAccess) {
			this.field = field;
			this.mode = mode;
			this.allowUnsafeAccess = allowUnsafeAccess;
		}


		FieldHint and(FieldMode mode, boolean allowUnsafeAccess) {
			return new FieldHint(this.field, this.mode != FieldMode.WRITE ? mode : FieldMode.WRITE,
					this.allowUnsafeAccess || allowUnsafeAccess);
		}

		/**
		 * Return the name of the field.
		 * @return the name
		 */
		public String getName() {
			return this.field.getName();
		}

		/**
		 * Return the {@link FieldMode} to use for the hint.
		 * @return the field mode
		 */
		public FieldMode getMode() {
			return this.mode;
		}

		/**
		 * Return whether if using {@code Unsafe} on the field should be
		 * allowed.
		 * @return {@code true} to allow unsafe access
		 */
		public boolean isAllowUnsafeAccess() {
			return this.allowUnsafeAccess;
		}

	}


	/**
	 * Base class for a hint that describes the need of reflection on a
	 * {@link Executable}.
	 */
	public abstract static class ExecutableHint {

		private final Executable executable;

		private final ExecutableMode mode;


		ExecutableHint(Executable executable, ExecutableMode mode) {
			this.executable = executable;
			this.mode = mode;
		}


		/**
		 * Return the name of the executable.
		 * @return the name
		 */
		public String getName() {
			return this.executable.getName();
		}

		/**
		 * Return the parameter types of the executable.
		 * @return the parameter types
		 * @see Executable#getParameterTypes()
		 */
		public TypeReference[] getParameterTypes() {
			return TypeReference.arrayOf(this.executable.getParameterTypes());
		}

		/**
		 * Return the {@link ExecutableMode} to use for the hint.
		 * @return the executable mode
		 */
		public ExecutableMode getMode() {
			return this.mode;
		}
	}


	/**
	 * A hint that describes the need of reflection on a {@link Method}.
	 */
	public static final class MethodHint extends ExecutableHint {

		private final Method method;


		MethodHint(Method method, ExecutableMode mode) {
			super(method, mode);
			this.method = method;
		}


		MethodHint and(ExecutableMode mode) {
			return new MethodHint(this.method, (getMode() != ExecutableMode.INVOKE ? mode : ExecutableMode.INVOKE));
		}

	}


	/**
	 * A hint that describes the need of reflection on a {@link Constructor}.
	 */
	public static final class ConstructorHint extends ExecutableHint {

		private final Constructor<?> constructor;


		ConstructorHint(Constructor<?> constructor, ExecutableMode mode) {
			super(constructor, mode);
			this.constructor = constructor;
		}

		ConstructorHint and(ExecutableMode mode) {
			return new ConstructorHint(this.constructor,
					(getMode() != ExecutableMode.INVOKE ? mode : ExecutableMode.INVOKE));
		}

	}

	/**
	 * Represent predefined categories for reflection hints.
	 */
	public enum Category {

		/**
		 * A category that represents public {@linkplain Field fields}.
		 * @see Class#getFields()
		 */
		PUBLIC_FIELDS,

		/**
		 * A category that represents {@linkplain Class#getDeclaredFields()
		 * declared fields}, that is all fields defined by the class, but not
		 * inherited ones.
		 * @see Class#getDeclaredFields()
		 */
		DECLARED_FIELDS,

		/**
		 * A category that defines public {@linkplain Constructor constructors}
		 * can be introspected, but not invoked.
		 * @see Class#getConstructors()
		 * @see ExecutableMode#INTROSPECT
		 */
		INTROSPECT_PUBLIC_CONSTRUCTORS,

		/**
		 * A category that defines {@linkplain Class#getDeclaredConstructors()
		 * all constructors} can be introspected, but not invoked.
		 * @see Class#getDeclaredConstructors()
		 * @see ExecutableMode#INTROSPECT
		 */
		INTROSPECT_DECLARED_CONSTRUCTORS,

		/**
		 * A category that defines public {@linkplain Constructor constructors}
		 * can be invoked.
		 * @see Class#getConstructors()
		 * @see ExecutableMode#INVOKE
		 */
		INVOKE_PUBLIC_CONSTRUCTORS,

		/**
		 * A category that defines {@linkplain Class#getDeclaredConstructors()
		 * all constructors} can be invoked.
		 * @see Class#getDeclaredConstructors()
		 * @see ExecutableMode#INVOKE
		 */
		INVOKE_DECLARED_CONSTRUCTORS,

		/**
		 * A category that defines public {@linkplain Method methods}, including
		 * inherited ones can be introspect, but not invoked.
		 * @see Class#getMethods()
		 * @see ExecutableMode#INTROSPECT
		 */
		INTROSPECT_PUBLIC_METHODS,

		/**
		 * A category that defines {@linkplain Class#getDeclaredMethods() all
		 * methods}, excluding inherited ones can be introspected, but not
		 * invoked.
		 * @see Class#getDeclaredMethods()
		 * @see ExecutableMode#INTROSPECT
		 */
		INTROSPECT_DECLARED_METHODS,

		/**
		 * A category that defines public {@linkplain Method methods}, including
		 * inherited ones can be invoked.
		 * @see Class#getMethods()
		 * @see ExecutableMode#INVOKE
		 */
		INVOKE_PUBLIC_METHODS,

		/**
		 * A category that defines {@linkplain Class#getDeclaredMethods() all
		 * methods}, excluding inherited ones can be invoked.
		 * @see Class#getDeclaredMethods()
		 * @see ExecutableMode#INVOKE
		 */
		INVOKE_DECLARED_METHODS,

		/**
		 * A category that represents public {@linkplain Class#getClasses()
		 * inner classes}. Contrary to other categories, this does not register
		 * any particular reflection for them but rather make sure they are
		 * available via a call to {@link Class#getClasses}.
		 */
		PUBLIC_CLASSES,

		/**
		 * A category that represents all {@linkplain Class#getDeclaredClasses()
		 * inner classes}. Contrary to other categories, this does not register
		 * any particular reflection for them but rather make sure they are
		 * available via a call to {@link Class#getDeclaredClasses}.
		 */
		DECLARED_CLASSES;

	}

}