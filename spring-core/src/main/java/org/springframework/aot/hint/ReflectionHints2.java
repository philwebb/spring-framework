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
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Gather the need for reflection at runtime.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
public class ReflectionHints2 {

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
		return new MethodRegistrar(false);
	}

	public MethodRegistrar registerInvoke() {
		return new MethodRegistrar(true);
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
			return forTypes(Arrays.stream(types).map(TypeReference::of)
					.toArray(TypeReference[]::new));
		}

		public ConditionRegistration forTypes(TypeReference... types) {
			for (TypeReference type : types) {
				computeHint(type, (hint) -> hint.merge(this.attributes));
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
			Assert.state(field != null, "Unable to find field '%s' in $s".formatted(name,
					declaringClass.getName()));
			return forField(field);
		}

		public ConditionRegistration forField(Field field) {
			TypeReference type = TypeReference.of(field.getDeclaringClass());
			computeHint(type, hint -> hint.merge(this));
			return new ConditionRegistration(type);
		}

		public ConditionRegistration forPublicFieldsIn(Class<?>... types) {
			return forFieldsIn(Attribute.PUBLIC_FIELDS, types);
		}

		public ConditionRegistration forPublicFieldsIn(String... types) {
			return forFieldsIn(Attribute.PUBLIC_FIELDS, types);
		}

		public ConditionRegistration forPublicFieldsIn(TypeReference... types) {
			return forFieldsIn(Attribute.PUBLIC_FIELDS, types);
		}

		public ConditionRegistration forDeclaredFieldsIn(Class<?>... types) {
			return forFieldsIn(Attribute.DECLARED_FIELDS, types);
		}

		public ConditionRegistration forDeclaredFieldsIn(String... types) {
			return forFieldsIn(Attribute.DECLARED_FIELDS, types);
		}

		public ConditionRegistration forDeclaredFieldsIn(TypeReference... types) {
			return forFieldsIn(Attribute.DECLARED_FIELDS, types);
		}

		public ConditionRegistration forFieldsIn(Attribute attribute, Class<?>... types) {
			return forFieldsIn(attribute, Arrays.stream(types).map(TypeReference::of)
					.toArray(TypeReference[]::new));
		}

		public ConditionRegistration forFieldsIn(Attribute attribute, String... types) {
			return forFieldsIn(attribute, Arrays.stream(types).map(TypeReference::of)
					.toArray(TypeReference[]::new));
		}

		public ConditionRegistration forFieldsIn(Attribute attribute,
				TypeReference... types) {
			Assert.state(!this.allowUnsafeAccess,
					"'allowUnsafeAccess' cannot be set when finding fields in a type");
			for (TypeReference type : types) {
				computeHint(type, hint -> hint.merge(attribute));
			}
			return new ConditionRegistration(types);
		}

	}

	public class MethodRegistrar {

		private final boolean invoke;

		MethodRegistrar(boolean invoke) {
			this.invoke = invoke;
		}

		public void forMethod(Method method) {
		}

		public void forConstructor(Constructor<?> constructor) {

		}

		public void forPublicConstructorsIn(Class<?>... types) {

		}

		public void forDeclaredConstructorsIn(Class<?>... types) {

		}

		public void forPublicMethodsIn(Class<?>... type) {

		}

		public void forDeclaredMethodsIn(Class<?>... type) {

		}

	}

	public static class ConditionRegistration {

		ConditionRegistration(TypeReference... types) {
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

		ReflectionHint merge(FieldRegistrar fieldRegistrar) {
			// FIXME explicit args
			return this;
		}

		ReflectionHint merge(Attribute... attributes) {
			return this;
		}

		static BiFunction<TypeReference, ReflectionHint, ReflectionHint> getOrCreate() {
			return null;
		}

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

	// hints.reflection.registerRead().withUnsafe().forField(field)
	// hints.reflection.registerRead().forPublicFields(type);
	// hints.reflection.registerRead().forDeclaredFields(type);
	// hints.reflection.registerWrite().forField(...);
	// hints.reflection.registerInvoke().forMethod("");
	// hints.reflection.registerIntrospect().forMethod("");
	// hints.reflection.registerIntrospect().forConstructor("");
	// hints.reflection.registerIntrospect().forPublicConstructorsOn(type);
	// hints.reflection.registerIntrospect().forDeclaredConstructorsOn(type);
	// hints.reflection.registerIntrospect().forPublicMethods(type);
	// hints.reflection.registerIntrospect().forDeclaredMethods(type);
	// hints.reglection.registerPublicClasses().forType(...);
	// hints.reglection.registerDeclaredClasses().forType(..);

}
