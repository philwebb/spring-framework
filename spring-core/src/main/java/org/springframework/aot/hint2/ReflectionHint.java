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
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

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
public final class ReflectionHint implements ConditionalHint {

	private final TypeReference type;

	@Nullable
	private final TypeReference reachableType;

	private final Set<Category> categories;

	private final Set<FieldHint> fields;

	private final Set<ConstructorHint> constructors;

	private final Set<MethodHint> methods;

	ReflectionHint(TypeReference type) {
		this.type = type;
		this.reachableType = null;
		this.categories = Collections.emptySet();
		this.fields = Collections.emptySet();
		this.constructors = Collections.emptySet();
		this.methods = Collections.emptySet();
	}

	private ReflectionHint(TypeReference type, TypeReference reachableType, Set<Category> categories,
			Set<FieldHint> fields, Set<ConstructorHint> constructors, Set<MethodHint> methods) {
		this.type = type;
		this.reachableType = reachableType;
		this.categories = categories;
		this.fields = fields;
		this.constructors = constructors;
		this.methods = methods;
	}

	ReflectionHint andReachableType(TypeReference reachableType) {
		Assert.state(this.reachableType == null, "A reachableType condition has already been applied");
		return new ReflectionHint(this.type, reachableType, this.categories, this.fields, this.constructors,
				this.methods);
	}

	ReflectionHint andCategory(Category category) {
		EnumSet<Category> categories = EnumSet.of(category);
		categories.addAll(this.categories);
		return new ReflectionHint(this.type, this.reachableType, categories, this.fields, this.constructors,
				this.methods);
	}

	ReflectionHint andField(Field field, FieldMode mode, boolean allowUnsafeAccess) {
		// FIXME
		return this;
	}

	ReflectionHint andMethod(Method method, ExecutableMode mode) {
		// FIXME
		return this;
	}

	ReflectionHint andConstructor(Constructor<?> constructor, ExecutableMode mode) {
		// FIXME
		return this;
	}

	@Override
	public TypeReference getReachableType() {
		return this.reachableType;
	}

	public static final class FieldHint {

	}

	public static abstract class ExecutableHint {

	}

	public static final class MethodHint extends ExecutableHint {

	}

	public static final class ConstructorHint extends ExecutableHint {

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