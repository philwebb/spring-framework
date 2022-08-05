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

/**
 * A immutable hint that describes the need for reflection on a type.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
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

	private ReflectionHint(TypeReference type, TypeReference reachableType,
			Set<Category> categories, Set<FieldHint> fields,
			Set<ConstructorHint> constructors, Set<MethodHint> methods) {
		this.type = type;
		this.reachableType = reachableType;
		this.categories = categories;
		this.fields = fields;
		this.constructors = constructors;
		this.methods = methods;
	}

	ReflectionHint andReachableType(TypeReference reachableType) {
		return this;
	}

	ReflectionHint andCategory(Category category) {
		EnumSet<Category> categories = EnumSet.of(category);
		categories.addAll(this.categories);
		return new ReflectionHint(this.type, this.reachableType, categories, this.fields,
				this.constructors, this.methods);
	}

	ReflectionHint andField(Field field, FieldMode mode, boolean allowUnsafeAccess) {
		return this;
	}

	ReflectionHint andMethod(Method method, ExecutableMode mode) {
		return this;
	}

	ReflectionHint andConstructor(Constructor<?> constructor, ExecutableMode mode) {
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

	public enum Category {

		PUBLIC_FIELDS,

		DECLARED_FIELDS,

		INTROSPECT_PUBLIC_CONSTRUCTORS,

		INTROSPECT_DECLARED_CONSTRUCTORS,

		INVOKE_PUBLIC_CONSTRUCTORS,

		INVOKE_DECLARED_CONSTRUCTORS,

		INTROSPECT_PUBLIC_METHODS,

		INTROSPECT_DECLARED_METHODS,

		INVOKE_PUBLIC_METHODS,

		INVOKE_DECLARED_METHODS,

		PUBLIC_CLASSES,

		DECLARED_CLASSES;

	}

}