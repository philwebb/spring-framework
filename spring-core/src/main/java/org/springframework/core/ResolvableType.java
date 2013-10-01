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
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * @author Phillip Webb
 * @since 4.0
 */
public final class ResolvableType {

	public static final ResolvableType NONE = new ResolvableType(null, null, null);

	private static final ResolvableType[] NO_TYPES = new ResolvableType[0];

	private ResolvableType owner;

	private OwnerType ownerType;

	private final Type type;

	private TypeVariableResolver variableResolver;

	private ResolvableType(Type type, ResolvableType owner, OwnerType ownerType) {
		this.type = type;
		this.owner = owner;
		this.ownerType = ownerType;
	}

	public Type getType() {
		return this.type;
	}

	public ResolvableType getSuperType() {
		Class<?> resolved = resolveClass();
		if (resolved == null || resolved.getGenericSuperclass() == null) {
			return NONE;
		}
		return getResolvableType(resolved.getGenericSuperclass(), this, OwnerType.SUBTYPE);
	}

	public ResolvableType[] getInterfaces() {
		Class<?> resolved = resolveClass();
		if (resolved == null || ObjectUtils.isEmpty(resolved.getGenericInterfaces())) {
			return NO_TYPES;
		}
		Type[] interfaceTypes = resolved.getGenericInterfaces();
		ResolvableType[] interfaces = new ResolvableType[interfaceTypes.length];
		for (int i = 0; i < interfaceTypes.length; i++) {
			interfaces[i] = getResolvableType(interfaceTypes[i], this, OwnerType.SUBTYPE);
		}
		return interfaces;
	}

	public ResolvableType as(Class<?> type) {
		if (this == NONE) {
			return NONE;
		}
		if (ObjectUtils.nullSafeEquals(resolveClass(), type)) {
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

	// FIXME toString()

	public ResolvableType getNestedGeneric(int nestingLevel) {
		return getNestedGeneric(nestingLevel, null);
	}

	public ResolvableType getNestedGeneric(int nestingLevel,
			Map<Integer, Integer> typeIndexesPerLevel) {
		// FIXME test
		ResolvableType type = this;
		for (int levelIndex = 1; levelIndex < nestingLevel; levelIndex++) {
			Integer genericIndex = typeIndexesPerLevel == null ? null
					: typeIndexesPerLevel.get(levelIndex);
			type = type.getGeneric(genericIndex == null ? 0 : genericIndex);
		}
		return type;
	}

	public Class<?> resolveGeneric(int... indexes) {
		return getGeneric(indexes).resolveClass();
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
		if(this == NONE) {
			return NO_TYPES;
		}
		if (this.type instanceof ParameterizedType) {
			Type[] genericTypes = ((ParameterizedType) getType()).getActualTypeArguments();
			ResolvableType[] generics = new ResolvableType[genericTypes.length];
			for (int i = 0; i < genericTypes.length; i++) {
				generics[i] = getResolvableType(genericTypes[i], this, OwnerType.GENERIC);
			}
			return generics;
		}
		return resolve().getGenerics();
	}

	public Class<?> resolveClass() {
		if (this.type instanceof Class<?> || this.type == null) {
			return (Class<?>) type;
		}
		return resolve().resolveClass();
	}

	private ResolvableType resolve() {
		Type resolvedType = resolveType();
		return resolvedType == null ? NONE : getResolvableType(resolvedType, this.owner, this.ownerType);
	}

	private Type resolveType() {
		if (this.type instanceof ParameterizedType) {
			return ((ParameterizedType) type).getRawType();
		}
		if (this.type instanceof WildcardType) {
			Type resolved = resolveBounds(((WildcardType) this.type).getUpperBounds());
			resolved = resolved != null ? resolved : resolveBounds(((WildcardType) this.type).getLowerBounds());
			return resolved;
		}
		if (this.type instanceof TypeVariable) {
			return resolveVariable((TypeVariable) this.type);
		}
		return null;
	}

	private Type resolveBounds(Type[] bounds) {
		if (ObjectUtils.isEmpty(bounds) || Object.class.equals(bounds[0])) {
			return null;
		}
		return bounds[0];
	}

	private Type resolveVariable(TypeVariable variable) {
		if(this.variableResolver != null) {
			Type resolved = this.variableResolver.resolve(variable);
			if(resolved != null) {
				return resolved;
			}
		}
		if(this.type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) this.type;
			if (parameterizedType.getRawType().equals(variable.getGenericDeclaration())) {
				TypeVariable<?>[] typeParameters = resolveClass().getTypeParameters();
				for (int i = 0; i < typeParameters.length; i++) {
					if (ObjectUtils.nullSafeEquals(typeParameters[i].getName(),
							variable.getName())) {
						return parameterizedType.getActualTypeArguments()[i];
					}
				}
			}
		}
		if(this.type instanceof TypeVariable<?> && this.variableResolver != null) {
			Type resolved = this.variableResolver.resolve((TypeVariable<?>) this.type);
			if(resolved != null) {
				return getResolvableType(resolved, owner, ownerType).resolveType();
			}
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

	public static ResolvableType forMethodParameter(final MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		ResolvableType type = forType(methodParameter.getGenericParameterType());
		type.variableResolver = new TypeVariableResolver() {
			@Override
			public Type resolve(TypeVariable<?> variable) {
				if(methodParameter.typeVariableMap == null) {
					return null;
				}
				return methodParameter.typeVariableMap.get(variable);
			}
		};
		type = type.getNestedGeneric(methodParameter.getNestingLevel(),
				methodParameter.typeIndexesPerLevel);
		// FIXME methodParameter.typeVariableMap;
		return type;
	}

	public static ResolvableType forMethodReturn(Method method) {
		Assert.notNull(method, "Method must not be null");
		return forType(method.getGenericReturnType());
	}

	public static ResolvableType forType(Type type) {
		return getResolvableType(type, null, null);
	}

	private static ResolvableType getResolvableType(Type type, ResolvableType owner,
			OwnerType ownerType) {
		return new ResolvableType(type, owner, ownerType);
	}

	private static enum OwnerType {
		GENERIC, SUBTYPE
	}

	private static interface TypeVariableResolver {
		Type resolve(TypeVariable<?> variable);
	}
}
