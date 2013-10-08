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
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Phillip Webb
 * @since 4.0
 */
public final class ResolvableType implements TypeVariableResolver {

	private static ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache = new ConcurrentReferenceHashMap<ResolvableType, ResolvableType>();

	public static final ResolvableType NONE = new ResolvableType(null, null);

	private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

	private final Type type;

	private final TypeVariableResolver variableResolver;

	private Class<?> resolved;

	private ResolvableType(Type type, TypeVariableResolver variableResolver) {
		this.type = type;
		this.variableResolver = variableResolver;
	}

	public Type getType() {
		return this.type;
	}

	public boolean isAssignableFrom(ResolvableType type) {
		return isAssignableFrom(false, type);
	}

	private boolean isAssignableFrom(boolean checkingGeneric, ResolvableType type) {
		Assert.notNull(type, "Type must not be null");
		if (resolve() == null || type.resolve() == null) {
			return false;
		}

		if (isArray()) {
			return (type.isArray() && getComponentType().isAssignableFrom(
					type.getComponentType()));
		}

		WildcardBounds ourBounds = WildcardBounds.get(this);
		WildcardBounds typeBounds = WildcardBounds.get(type);

		if (typeBounds != null) {
			return ourBounds != null && ourBounds.isSameType(typeBounds)
					&& ourBounds.isAssignableFrom(typeBounds.getBounds());
		}

		if (ourBounds != null) {
			return ourBounds.isAssignableFrom(type);
		}

		boolean rtn = true;
		rtn &= (!checkingGeneric || resolve().equals(type.resolve()));
		rtn &= resolve().isAssignableFrom(type.resolve());
		for (int i = 0; i < getGenerics().length; i++) {
			rtn &= getGeneric(i).isAssignableFrom(true, type.as(resolve()).getGeneric(i));
		}
		return rtn;
	}

	public boolean isArray() {
		if (this == NONE) {
			return false;
		}
		return (((this.type instanceof Class) && ((Class<?>) this.type).isArray())
				|| this.type instanceof GenericArrayType || this.resolveType().isArray());
	}

	public ResolvableType getComponentType() {
		if (this == NONE) {
			return NONE;
		}
		if (this.type instanceof Class) {
			Class<?> componentType = ((Class<?>) this.type).getComponentType();
			return componentType == null ? NONE : forType(componentType,
					this.variableResolver);
		}
		if (this.type instanceof GenericArrayType) {
			return forType(((GenericArrayType) this.type).getGenericComponentType(),
					this.variableResolver);
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
			if (interfaceAsType != NONE) {
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
		return forType(resolved.getGenericSuperclass(), this);
	}

	public ResolvableType[] getInterfaces() {
		Class<?> resolved = resolve();
		if (resolved == null || ObjectUtils.isEmpty(resolved.getGenericInterfaces())) {
			return EMPTY_TYPES_ARRAY;
		}
		Type[] interfaceTypes = resolved.getGenericInterfaces();
		ResolvableType[] interfaces = new ResolvableType[interfaceTypes.length];
		for (int i = 0; i < interfaceTypes.length; i++) {
			interfaces[i] = forType(interfaceTypes[i], this);
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
				generics[i] = forType(genericTypes[i], this);
			}
			return generics;
		}
		return resolveType().getGenerics();
	}

	public Class<?>[] resolveGenerics() {
		ResolvableType[] generics = getGenerics();
		Class<?>[] resolvedGenerics = new Class<?>[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvedGenerics[i] = generics[i].resolve();
		}
		return resolvedGenerics;
	}

	public Class<?> resolveGeneric(int... indexes) {
		return getGeneric(indexes).resolve();
	}

	public Class<?> resolve() {
		return resolve(null);
	}

	public Class<?> resolve(Class<?> fallback) {
		if (this.resolved == null) {
			synchronized (this) {
				this.resolved = resolveClass();
				this.resolved = (this.resolved == null ? void.class : this.resolved);
			}
		}
		return (this.resolved == void.class ? fallback : this.resolved);
	}

	private Class<?> resolveClass() {
		if (this.type instanceof Class<?> || this.type == null) {
			return (Class<?>) this.type;
		}
		if (this.type instanceof GenericArrayType) {
			return Array.newInstance(getComponentType().resolve(), 0).getClass();
		}
		return resolveType().resolve();
	}

	private ResolvableType resolveType(Class<? extends Type> targetType) {
		if (targetType.isInstance(this.type) || this == NONE) {
			return this;
		}
		return resolveType().resolveType(targetType);
	}

	ResolvableType resolveType() {
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
			if (this.variableResolver != null) {
				resolved = this.variableResolver.resolveVariable((TypeVariable<?>) this.type);
			}
			resolved = resolved != null ? resolved
					: resolveBounds(((TypeVariable<?>) this.type).getBounds());
		}
		return (resolved == null ? NONE : forType(resolved, this.variableResolver));
	}

	private Type resolveBounds(Type[] bounds) {
		if (ObjectUtils.isEmpty(bounds) || Object.class.equals(bounds[0])) {
			return null;
		}
		return bounds[0];
	}

	public Type resolveVariable(TypeVariable<?> variable) {
		Assert.notNull("Variable must not be null");
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
			Type resolved = (this.variableResolver == null ? null
					: this.variableResolver.resolveVariable(variable));
			if (resolved == null && parameterizedType.getOwnerType() != null) {
				resolved = forType(parameterizedType.getOwnerType(),
						this.variableResolver).resolveVariable(variable);
			}
			return resolved;
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

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.type) * 31
				+ ObjectUtils.nullSafeHashCode(this.variableResolver);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof ResolvableType) {
			ResolvableType other = (ResolvableType) obj;
			return ObjectUtils.nullSafeEquals(this.type, other.type)
					&& ObjectUtils.nullSafeEquals(this.variableResolver,
							other.variableResolver);
		}
		return false;
	}

	public static ResolvableType forClass(Class<?> type) {
		Assert.notNull(type, "Type class must not be null");
		return forType(type);
	}

	public static ResolvableType forClass(Class<?> type, Class<?> implementationClass) {
		Assert.notNull(type, "Type class must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		return forType(implementationClass).as(type);
	}

	public static ResolvableType forField(Field field) {
		Assert.notNull(field, "Field must not be null");
		return forType(field.getGenericType());
	}

	public static ResolvableType forField(Field field, Class<?> implementationClass) {
		Assert.notNull(field, "Field must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		TypeVariableResolver variableResolver = forType(implementationClass).as(
				field.getDeclaringClass());
		return forType(field.getGenericType(), variableResolver);
	}

	public static ResolvableType forConstructorParameter(Constructor<?> constructor,
			int parameterIndex) {
		Assert.notNull(constructor, "Constructor must not be null");
		return forMethodParameter(MethodParameter.forMethodOrConstructor(constructor,
				parameterIndex));
	}

	public static ResolvableType forConstructorParameter(Constructor<?> constructor,
			int parameterIndex, Class<?> implementationClass) {
		Assert.notNull(constructor, "Constructor must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		return forMethodParameter(
				MethodParameter.forMethodOrConstructor(constructor, parameterIndex),
				implementationClass);
	}

	public static ResolvableType forMethodParameter(Method method, int parameterIndex) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(MethodParameter.forMethodOrConstructor(method,
				parameterIndex));
	}

	public static ResolvableType forMethodParameter(Method method, int parameterIndex,
			Class<?> implementationClass) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(
				MethodParameter.forMethodOrConstructor(method, parameterIndex),
				implementationClass);
	}

	public static ResolvableType forMethodParameter(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		if (methodParameter.resolveClass != null) {
			return forMethodParameter(methodParameter, methodParameter.resolveClass);
		}
		return forType(methodParameter.getGenericParameterType());
	}

	public static ResolvableType forMethodParameter(MethodParameter methodParameter,
			Class<?> implementationClass) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		TypeVariableResolver variableResolver = forType(implementationClass).as(
				methodParameter.getMember().getDeclaringClass());
		return forType(methodParameter.getGenericParameterType(), variableResolver);
	}

	public static ResolvableType forMethodReturn(Method method) {
		Assert.notNull(method, "Method must not be null");
		return forType(method.getGenericReturnType());
	}

	public static ResolvableType forMethodReturn(Method method,
			Class<?> implementationClass) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		TypeVariableResolver variableResolver = forType(implementationClass).as(
				method.getDeclaringClass());
		return forType(method.getGenericReturnType(), variableResolver);
	}

	public static ResolvableType forType(Type type) {
		return forType(type, null);
	}

	public static ResolvableType forType(Type type, TypeVariableResolver variableResolver) {
		ResolvableType key = new ResolvableType(type, variableResolver);
		ResolvableType resolvableType = cache.get(key);
		if (resolvableType == null) {
			resolvableType = key;
			cache.put(key, resolvableType);
		}
		return resolvableType;
	}

	private static class WildcardBounds {

		private final BoundsType type;

		private final ResolvableType[] bounds;

		private WildcardBounds(BoundsType type, ResolvableType[] bounds) {
			this.type = type;
			this.bounds = bounds;
		}

		public boolean isSameType(WildcardBounds bounds) {
			return this.type == bounds.type;
		}

		public boolean isAssignableFrom(ResolvableType... types) {
			for (ResolvableType bound : this.bounds) {
				for (ResolvableType type : types) {
					if (!this.type.isAssignable( bound, type)) {
						return false;
					}
				}
			}
			return true;
		}

		public ResolvableType[] getBounds() {
			return bounds;
		}

		public static WildcardBounds get(ResolvableType type) {
			ResolvableType resolveToWildcard = type.resolveType(WildcardType.class);
			if (resolveToWildcard == NONE) {
				return null;
			}
			WildcardType wildcardType = (WildcardType) resolveToWildcard.type;
			BoundsType boundsType = (wildcardType.getLowerBounds().length > 0 ? BoundsType.LOWER
					: BoundsType.UPPER);
			Type[] bounds = boundsType == BoundsType.UPPER ? wildcardType.getUpperBounds()
					: wildcardType.getLowerBounds();
			ResolvableType[] resolvableBounds = new ResolvableType[bounds.length];
			for (int i = 0; i < bounds.length; i++) {
				resolvableBounds[i] = forType(bounds[i], type.variableResolver);
			}
			return new WildcardBounds(boundsType, resolvableBounds);
		}

		static enum BoundsType {

			UPPER {

				@Override
				public boolean isAssignable(ResolvableType type, ResolvableType from) {
					return type.isAssignableFrom(from);
				}
			},
			LOWER {

				@Override
				public boolean isAssignable(ResolvableType type, ResolvableType from) {
					return from.isAssignableFrom(type);
				}
			};

			public abstract boolean isAssignable(ResolvableType type, ResolvableType from);
		}
	}

}
