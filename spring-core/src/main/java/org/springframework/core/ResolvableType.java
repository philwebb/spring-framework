/*
 * Copyright 2002-2013 the original author or authors.
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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Phillip Webb
 * @since 4.0
 */
public final class ResolvableType {

	// FIXME equals hc

	public static final ResolvableType NONE = new ResolvableType(null, null);

	private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

	private final Type type;

	private final ResolvableType owner;

	private ResolvableType(Type type, ResolvableType owner) {
		this.type = type;
		this.owner = owner;
	}

	public Type getType() {
		return this.type;
	}

	public boolean isArray() {
		if (this == NONE) {
			return false;
		}
		return (((this.type instanceof Class) && ((Class) this.type).isArray())
				|| this.type instanceof GenericArrayType || this.resolveType().isArray());
	}

	public ResolvableType getComponentType() {
		if (this == NONE) {
			return NONE;
		}
		if (this.type instanceof Class) {
			Class componentType = ((Class) this.type).getComponentType();
			return componentType == null ? NONE : get(componentType,
					this.owner);
		}
		if (this.type instanceof GenericArrayType) {
			return get(
					((GenericArrayType) this.type).getGenericComponentType(), this.owner);
		}
		return resolveType().getComponentType();
	}

	public ResolvableType asCollection() {
		return as(Collection.class);
	}

	public ResolvableType asMap() {
		return as(Map.class);
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

	public ResolvableType getSuperType() {
		Class<?> resolved = resolve();
		if (resolved == null || resolved.getGenericSuperclass() == null) {
			return NONE;
		}
		return get(resolved.getGenericSuperclass(), this);
	}

	public ResolvableType[] getInterfaces() {
		Class<?> resolved = resolve();
		if (resolved == null || ObjectUtils.isEmpty(resolved.getGenericInterfaces())) {
			return EMPTY_TYPES_ARRAY;
		}
		Type[] interfaceTypes = resolved.getGenericInterfaces();
		ResolvableType[] interfaces = new ResolvableType[interfaceTypes.length];
		for (int i = 0; i < interfaceTypes.length; i++) {
			interfaces[i] = get(interfaceTypes[i], this);
		}
		return interfaces;
	}

	public boolean hasGenerics() {
		return getGenerics().length > 0;
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
				generics[i] = get(genericTypes[i], this);
			}
			return generics;
		}
		return resolveType().getGenerics();
	}

	public Class<?> resolveGeneric(int... indexes) {
		return getGeneric(indexes).resolve();
	}

	public Class<?> resolve() {
		if (this.type instanceof Class<?> || this.type == null) {
			return (Class<?>) this.type;
		}
		if (this.type instanceof GenericArrayType) {
			return Array.newInstance(getComponentType().resolve(), 0).getClass();
		}
		return resolveType().resolve();
	}

	private ResolvableType resolveType() {
		Type resolved = null;
		if (this.type instanceof ParameterizedType) {
			resolved = ((ParameterizedType) this.type).getRawType();
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
		return (resolved == null ? NONE : get(resolved, this.owner));
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
			return this.owner == null ? null : this.owner.resolveVariable(variable);
		}
		if (this.type instanceof TypeVariable<?>) {
			return resolveType().resolveVariable(variable);
		}
		return null;
	}

	@Override
	public String toString() {
		if (isArray()) {
			return getComponentType() + "[]";
		}
		StringBuilder result = new StringBuilder();
		result.append(resolve() == null ? "?" : resolve().getName());
		if (hasGenerics()) {
			result.append("<" + StringUtils.arrayToDelimitedString(getGenerics(), ", ")
					+ ">");
		}
		return result.toString();
	}

	//

	public static ResolvableType forClass(Class<?> type) {
		Assert.notNull(type, "Type class must not be null");
		return get(type);
	}

	public static ResolvableType forClass(Class<?> type, Class<?> implementationClass) {
		Assert.notNull(type, "Type class must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		return get(implementationClass).as(type);
	}

	public static ResolvableType forField(Field field) {
		Assert.notNull(field, "Field must not be null");
		return get(field.getGenericType());
	}

	public static ResolvableType forField(Field field, Class<?> implementationClass) {
		Assert.notNull(field, "Field must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		ResolvableType owner = get(implementationClass).as(field.getDeclaringClass());
		return get(field.getGenericType(), owner);
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
		//FIXME not here?
		if (methodParameter.resolveClass != null) {
			return forMethodParameter(methodParameter, methodParameter.resolveClass);
		}
		return get(methodParameter.getGenericParameterType());
	}

	public static ResolvableType forMethodParameter(MethodParameter methodParameter,
			Class<?> implementationClass) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		ResolvableType owner = get(implementationClass).as(methodParameter.getDeclaringClass());
		return get(methodParameter.getGenericParameterType(), owner);
	}

	public static ResolvableType forMethodReturn(Method method) {
		Assert.notNull(method, "Method must not be null");
		return get(method.getGenericReturnType());
	}

	public static ResolvableType forMethodReturn(Method method, Class<?> implementationClass) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		ResolvableType owner = get(implementationClass).as(method.getReturnType());
		return get(method.getGenericReturnType(), owner);
	}

	private static ResolvableType get(Type type) {
		return get(type, null);
	}

	private static ResolvableType get(Type type, ResolvableType owner) {
		return new ResolvableType(type, owner); // FIXME cache
	}

}
