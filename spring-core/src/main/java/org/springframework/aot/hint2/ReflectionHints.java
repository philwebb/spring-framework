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
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.FieldHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint2.ReflectionHints.ReflectionHint.Attribute;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Gather the need for reflection at runtime.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 * @see RuntimeHints
 */
public class ReflectionHints {

	private Map<TypeReference, ReflectionHint> hints;

	public TypeRegistrar registerPublicClasses() {
		return new TypeRegistrar(Attribute.PUBLIC_CLASSES);
	}

	public TypeRegistrar registerDeclaredClasses() {
		return new TypeRegistrar(Attribute.DECLARED_CLASSES);
	}

	public FieldRegistrar registerRead() {
		return new FieldRegistrar(false, false);
	}

	public FieldRegistrar registerWrite() {
		return new FieldRegistrar(true, false);
	}

	public MethodRegistrar registerIntrospect() {
		return new MethodRegistrar(ExecutableMode.INTROSPECT);
	}

	public MethodRegistrar registerInvoke() {
		return new MethodRegistrar(ExecutableMode.INVOKE);
	}

	ReflectionHint computeHint(TypeReference type,
			UnaryOperator<ReflectionHint> mergeFunction) {
		BiFunction<TypeReference, ReflectionHint, ReflectionHint> mappingFunction = (key,
				value) -> (value != null) ? value : new ReflectionHint(type);
		return hints.compute(type, mappingFunction.andThen(mergeFunction));
	}

	public class TypeRegistrar {

		private Attribute[] attributes;

		TypeRegistrar(Attribute... attributes) {
			this.attributes = attributes;
		}

		public ConditionRegistration forTypes(Class<?>... types) {
			return forTypes(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forTypes(String... types) {
			return forTypes(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forTypes(TypeReference... types) {
			for (TypeReference type : types) {
				computeHint(type, (hint) -> hint.mergeAttributes(this.attributes));
			}
			return new ConditionRegistration(types);
		}

	}

	public class FieldRegistrar {

		private final boolean allowWrite;

		private final boolean allowUnsafeAccess;

		FieldRegistrar(boolean allowWrite, boolean allowUnsafeAccess) {
			this.allowWrite = allowWrite;
			this.allowUnsafeAccess = allowUnsafeAccess;
		}

		public FieldRegistrar withUnsafeAccess() {
			return new FieldRegistrar(this.allowWrite, true);
		}

		public ConditionRegistration forField(Class<?> declaringClass, String name) {
			Field field = ReflectionUtils.findField(declaringClass, name);
			Assert.state(field != null, () -> "Unable to find field '%s' in $s"
					.formatted(name, declaringClass.getName()));
			return forField(field);
		}

		public ConditionRegistration forField(Field field) {
			TypeReference type = TypeReference.of(field.getDeclaringClass());
			computeHint(type, hint -> hint.mergeField(field.getName(), this.allowWrite,
					this.allowUnsafeAccess));
			return new ConditionRegistration(type);
		}

		public ConditionRegistration forPublicFieldsIn(Class<?>... types) {
			return forPublicFieldsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forPublicFieldsIn(String... types) {
			return forPublicFieldsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forPublicFieldsIn(TypeReference... types) {
			return forTypes(Attribute.PUBLIC_FIELDS, types);
		}

		public ConditionRegistration forDeclaredFieldsIn(Class<?>... types) {
			return forDeclaredFieldsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forDeclaredFieldsIn(String... types) {
			return forDeclaredFieldsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forDeclaredFieldsIn(TypeReference... types) {
			return forTypes(Attribute.DECLARED_FIELDS, types);
		}

		private ConditionRegistration forTypes(Attribute attribute,
				TypeReference... types) {
			Assert.state(!this.allowUnsafeAccess,
					"'allowUnsafeAccess' cannot be set when finding fields in a type");
			for (TypeReference type : types) {
				computeHint(type, hint -> hint.mergeAttributes(attribute));
			}
			return new ConditionRegistration(types);
		}

	}

	public class MethodRegistrar {

		private final ExecutableMode mode;

		MethodRegistrar(ExecutableMode mode) {
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
			computeHint(type, hint -> hint.mergeMethod());
			return new ConditionRegistration(type);
		}

		public ConditionRegistration forConstructor(Class<?> declaringClass,
				Class<?>... parameterTypes) {
			try {
				return forConstructor(declaringClass.getConstructor(parameterTypes));

			}
			catch (NoSuchMethodException | SecurityException ex) {
				throw new IllegalStateException("Unable to find constructor in class %s"
						.formatted(declaringClass.getName()));
			}
		}

		public ConditionRegistration forConstructor(Constructor<?> constructor) {
			TypeReference type = TypeReference.of(constructor.getDeclaringClass());
			computeHint(type, hint -> hint.mergeConstructor());
			return new ConditionRegistration(type);
		}

		public ConditionRegistration forPublicConstructorsIn(Class<?>... types) {
			return forPublicConstructorsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forPublicConstructorsIn(String... types) {
			return forPublicConstructorsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forPublicConstructorsIn(TypeReference... types) {
			return forTypes(Attribute.INTROSPECT_PUBLIC_CONSTRUCTORS,
					Attribute.INVOKE_PUBLIC_CONSTRUCTORS, types);
		}

		public ConditionRegistration forDeclaredConstructorsIn(Class<?>... types) {
			return forDeclaredConstructorsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forDeclaredConstructorsIn(String... types) {
			return forDeclaredConstructorsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forDeclaredConstructorsIn(TypeReference... types) {
			return forTypes(Attribute.INTROSPECT_DECLARED_CONSTRUCTORS,
					Attribute.INVOKE_DECLARED_CONSTRUCTORS, types);
		}

		public ConditionRegistration forPublicMethodsIn(Class<?>... types) {
			return forPublicMethodsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forPublicMethodsIn(String... types) {
			return forPublicMethodsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forPublicMethodsIn(TypeReference... types) {
			return forTypes(Attribute.INTROSPECT_PUBLIC_METHODS,
					Attribute.INVOKE_PUBLIC_METHODS, types);
		}

		public ConditionRegistration forDeclaredMethodsIn(Class<?>... types) {
			return forDeclaredMethodsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forDeclaredMethodsIn(String... types) {
			return forDeclaredMethodsIn(TypeReference.arrayOf(types));
		}

		public ConditionRegistration forDeclaredMethodsIn(TypeReference... types) {
			return forTypes(Attribute.INTROSPECT_DECLARED_METHODS,
					Attribute.INVOKE_DECLARED_METHODS, types);
		}

		private ConditionRegistration forTypes(Attribute introspectAttribute,
				Attribute invokeAttribute, TypeReference... types) {
			Attribute attribute = switch (this.mode) {
				case INTROSPECT -> introspectAttribute;
				case INVOKE -> invokeAttribute;
			};
			for (TypeReference type : types) {
				computeHint(type, hint -> hint.mergeAttributes(attribute));
			}
			return new ConditionRegistration(types);
		}

	}

	public static class ConditionRegistration {

		ConditionRegistration(TypeReference... types) {
		}

		public ConditionRegistration whenReachable(Class<?> type) {
			return this;
		}

	}

	public static class ReflectionHint {

		private final TypeReference type;

		@Nullable
		private final TypeReference reachableTypeCondition = null;

		private final Set<Attribute> attributes = null;

		private final Set<FieldHint> fields = null;

		private final Set<ExecutableHint> constructors = null;

		private final Set<ExecutableHint> methods = null;

		ReflectionHint(TypeReference type) {
			this.type = type;
		}

		ReflectionHint mergeAttributes(Attribute... attributes) {
			return this;
		}

		ReflectionHint mergeField(String name, boolean allowWrite,
				boolean allowUnsafeAccess) {
			return this;
		}

		ReflectionHint mergeMethod() {
			return this;
		}

		ReflectionHint mergeConstructor() {
			return this;
		}

		static BiFunction<TypeReference, ReflectionHint, ReflectionHint> getOrCreate() {
			return null;
		}

		public enum Attribute {

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

}
