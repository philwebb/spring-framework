/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * @author Phillip Webb
 * @since 4.0
 */
public final class ResolvableType {

	// FIXME toString()
	// FIXME test getNestedGeneric

	public static final ResolvableType NONE = new ResolvableType(null, null, null);

	private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

	private final Type type;

	private final ResolvableType owner;

	private Map<TypeVariable<?>, Type> typeVariableMap;

	private ResolvableType(Type type, ResolvableType owner,
			Map<TypeVariable<?>, Type> typeVariableMap) {
		this.type = type;
		this.owner = owner;
		this.typeVariableMap = typeVariableMap;
	}

	public Type getType() {
		return this.type;
	}

	public ResolvableType getSuperType() {
		Class<?> resolved = resolve();
		if (resolved == null || resolved.getGenericSuperclass() == null) {
			return NONE;
		}
		return new ResolvableType(resolved.getGenericSuperclass(), this, null);
	}

	public ResolvableType[] getInterfaces() {
		Class<?> resolved = resolve();
		if (resolved == null || ObjectUtils.isEmpty(resolved.getGenericInterfaces())) {
			return EMPTY_TYPES_ARRAY;
		}
		Type[] interfaceTypes = resolved.getGenericInterfaces();
		ResolvableType[] interfaces = new ResolvableType[interfaceTypes.length];
		for (int i = 0; i < interfaceTypes.length; i++) {
			interfaces[i] = new ResolvableType(interfaceTypes[i], this, null);
		}
		return interfaces;
	}

	public ResolvableType as(Class<?> type) {
		if (this == NONE) {
			return NONE;
		}
		if (ObjectUtils.nullSafeEquals(resolve(), type)) {
			return this;
		}
		for (ResolvableType interfaceType : getInterfaces()) {
			ResolvableType interfaceAsType = interfaceType.as(type);
			if (interfaceAsType != null) {
				return interfaceAsType;
			}
		}
		return getSuperType().as(type);
	}

	public ResolvableType getNestedGeneric(int nestingLevel) {
		return getNestedGeneric(nestingLevel, null);
	}

	public ResolvableType getNestedGeneric(int nestingLevel,
			Map<Integer, Integer> typeIndexesPerLevel) {
		if (typeIndexesPerLevel == null) {
			typeIndexesPerLevel = Collections.emptyMap();
		}
		ResolvableType type = this;
		for (int levelIndex = 1; levelIndex < nestingLevel; levelIndex++) {
			Integer genericIndex = typeIndexesPerLevel.get(levelIndex);
			type = type.getGeneric(genericIndex == null ? 0 : genericIndex);
		}
		return type;
	}

	public Class<?> resolveGeneric(int... indexes) {
		return getGeneric(indexes).resolve();
	}

	public ResolvableType getGeneric(int... indexes) {
		try {
			if (indexes == null || indexes.length == 0) {
				return getGenerics()[0];
			}
			ResolvableType rtn = this;
			for (int index : indexes) {
				rtn = rtn.getGenerics()[index];
			}
			return rtn;
		}
		catch (IndexOutOfBoundsException ex) {
			return NONE;
		}
	}

	public ResolvableType[] getGenerics() {
		if (this == NONE) {
			return EMPTY_TYPES_ARRAY;
		}
		if (this.type instanceof ParameterizedType) {
			Type[] genericTypes = ((ParameterizedType) getType()).getActualTypeArguments();
			ResolvableType[] generics = new ResolvableType[genericTypes.length];
			for (int i = 0; i < genericTypes.length; i++) {
				generics[i] = new ResolvableType(genericTypes[i], this, null);
			}
			return generics;
		}
		return resolveType().getGenerics();
	}

	public Class<?> resolve() {
		if (this.type instanceof Class<?> || this.type == null) {
			return (Class<?>) type;
		}
		return resolveType().resolve();
	}

	private ResolvableType resolveType() {
		Type resolved = null;
		if (this.type instanceof ParameterizedType) {
			resolved = ((ParameterizedType) type).getRawType();
		}
		else if (this.type instanceof WildcardType) {
			resolved = resolveBounds(((WildcardType) this.type).getUpperBounds());
			resolved = resolved != null ? resolved
					: resolveBounds(((WildcardType) this.type).getLowerBounds());
		}
		else if (this.type instanceof TypeVariable) {
			if (this.owner != null) {
				resolved = this.owner.resolveVariable((TypeVariable) this.type);
			}
			resolved = resolved != null ? resolved
					: resolveBounds(((TypeVariable<?>) this.type).getBounds());
		}
		return (resolved == null ? NONE : new ResolvableType(resolved, this.owner,
				this.typeVariableMap));
	}

	private Type resolveBounds(Type[] bounds) {
		if (ObjectUtils.isEmpty(bounds) || Object.class.equals(bounds[0])) {
			return null;
		}
		return bounds[0];
	}

	private Type resolveVariable(TypeVariable variable) {
		if (this.type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) this.type;
			if (parameterizedType.getRawType().equals(variable.getGenericDeclaration())) {
				TypeVariable<?>[] typeParameters = resolve().getTypeParameters();
				for (int i = 0; i < typeParameters.length; i++) {
					if (ObjectUtils.nullSafeEquals(typeParameters[i].getName(),
							variable.getName())) {
						return parameterizedType.getActualTypeArguments()[i];
					}
				}
			}
		}
		if (this.type instanceof TypeVariable<?>) {
			System.out.println(this.type + " " + variable);
			return resolveType().resolveVariable(variable);
		}
		return this.owner == null ? null : owner.resolveVariable(variable);
	}

	//

	public static ResolvableType forClass(Class<?> type) {
		Assert.notNull(type, "Class must not be null");
		return forType(type);
	}

	public static ResolvableType forField(Field field) {
		Assert.notNull(field, "Field must not be null");
		return forType(field.getGenericType());
	}

	public static ResolvableType forConstructorParameter(Constructor<?> constructor,
			int parameterIndex) {
		Assert.notNull(constructor, "Constructor must not be null");
		return forMethodParameter(MethodParameter.forMethodOrConstructor(constructor,
				parameterIndex));
	}

	public static ResolvableType forMethodParameter(Method method, int parameterIndex) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(MethodParameter.forMethodOrConstructor(method,
				parameterIndex));
	}

	public static ResolvableType forMethodParameter(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		return forMethodParameter(methodParameter, methodParameter.resolveClass);
	}

	public static ResolvableType forMethodParameter(MethodParameter methodParameter,
			Class<?> resolveClass) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		ResolvableType ownerType = null;
		if (resolveClass != null) {
			ownerType = forClass(resolveClass).as(methodParameter.getDeclaringClass());
		}
		Type parameterType = methodParameter.getGenericParameterType();
		return new ResolvableType(parameterType, ownerType, null).getNestedGeneric(
				methodParameter.getNestingLevel(), methodParameter.typeIndexesPerLevel);
	}

	public static ResolvableType forMethodReturn(Method method) {
		Assert.notNull(method, "Method must not be null");
		return forType(method.getGenericReturnType());
	}

	public static ResolvableType forType(Type type) {
		return new ResolvableType(type, null, null);
	}

}
