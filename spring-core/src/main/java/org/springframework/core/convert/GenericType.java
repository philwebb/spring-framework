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

package org.springframework.core.convert;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Allows generics declared on {@link #fromField(Field) fields},
 * {@link #fromMethodParameter(MethodParameter) method parameters},
 * {@link #fromMethodReturn(Method) method return types} or {@link #fromClass(Class)
 * classes} to be obtained easily.
 *
 * <p>
 * Most methods on this class will themselves return a {@code GenericType}. Use
 * {@link #getTypeClass()} or {@link #getType()} if you need to convert back to a standard
 * Java types.
 *
 * <p>
 * Generics declared on the type can be obtained using the {@link #getGenerics()} method.
 * There are also a number of convenience methods ({@link #getGeneric()},
 * {@link #getGeneric(int)}, {@link #getGenericTypeClass()} &amp;
 * {@link #getGenericTypeClass(int)}) that can be used when working with generics.
 *
 * <p>
 * Generic information is tracked when navigating type hierarchies. For example, given a
 * {@code MultiValueMap} field declaration "
 * {@code org.springframework.util.MultiValueMap<Integer, String> multiValueMap}" when
 * obtaining the super type using "{@code GenericType.fromField(multiValueMap).getSuper()}"
 * the resulting generics are resolved using the original owning field to
 * "{@code java.util.Map<Integer, java.util.List<String>>}". If you need to navigate to a
 * specific superclass or interface use the {@link #get(Class)} method. For example to
 * return a {@code Map} value type
 * {@code genericType.get(Map.class).getGenericTypeClass(1)}.
 * <p>
 * When working with array types both {@link #getSuper()} and {@link #getInterfaces()}
 * will infer array results. For example: {@code ArrayList<String>[]} will return
 * interfaces {@code List<String>[]} and {@code Collection<String>[]}. The
 * {@link #isArray()} method can be used to determine if type is an array.
 *
 * @author Phillip Webb
 * @since 3.2
 * @see #getGenerics()
 * @see #getSuper()
 * @see #getInterfaces()
 */
public final class GenericType {


	private static final GenericType[] EMPTY_GENERIC_TYPES = {};

	private static final GenericType NONE = new GenericType(null, Void.class, false);


	/**
	 * The owner that created this type (if any).
	 */
	private final GenericType owner;

	/**
	 * The underlying java type (never null).
	 */
	private final Type type;

	/**
	 * The target or {@code null} if this object is the target. This provides a way to
	 * reference raw types, resolved variable, array elements or wildcard bounds.
	 */
	private GenericType target;

	/**
	 * The super type of this type or {@link #NONE} if there is no super type. A
	 * {@code null} value indicates that the super type has not yet been deduced.
	 */
	private GenericType superType;

	/**
	 * The interfaces implemented by this type or {@link #EMPTY_GENERIC_TYPES} if there
	 * are no interfaces. A {@code null} value indicates that the interfaces have not yet
	 * been deduced.
	 */
	private GenericType[] interfaces;

	/**
	 * The generics on this type or {@link #EMPTY_GENERIC_TYPES} if there are no generics.
	 * A {@code null} value indicates that the generics have not yet been deduced.
	 */
	private GenericType[] generics;

	/**
	 * If this type represents an array.
	 */
	private boolean array;


	/**
	 * Internal private constructor used to create new {@link GenericType} instances.
	 *
	 * @param owner the owner (if any)
	 * @param type the underlying type
	 */
	private GenericType(GenericType owner, Type type, boolean array) {
		Assert.notNull(type, "Type must not be null");
		this.owner = owner;
		this.type = type;
		this.array = array;
		if (type instanceof ParameterizedType) {
			this.target = new GenericType(owner, ((ParameterizedType) type).getRawType(), false);
		} else if (type instanceof TypeVariable) {
			TypeVariable<?> variable = (TypeVariable<?>) type;
			this.target = resolveVariable(variable, owner);
			if(this.target == null) {
				this.target = resolveBounds(variable.getBounds());
			}
		} else if (type instanceof GenericArrayType) {
			this.array = true;
			this.target = new GenericType(owner, ((GenericArrayType) type).getGenericComponentType(), false);
		} else if (type instanceof WildcardType) {
			WildcardType wildcardType = (WildcardType) type;
			this.target = resolveBounds(wildcardType.getUpperBounds());
			if (this.target == null) {
				this.target = resolveBounds(wildcardType.getLowerBounds());
			}
		}
		if(target != null && target.isArray()) {
			this.array = true;
		}
	}

	private GenericType resolveVariable(TypeVariable<?> variable, GenericType type) {
		if(type == null) {
			return null;
		}
		GenericType resolved = resolveVariableAgainstParameterizedType(variable, type);
		resolved = (resolved != null ? resolved : resolveVariable(variable, type.target));
		resolved = (resolved != null ? resolved : resolveVariable(variable, type.owner));
		resolved = (resolved != null ? resolved : resolveVariable(variable, type.getSuper()));
		for (GenericType interfaceType : owner.getInterfaces()) {
			resolved = (resolved != null ? resolved : resolveVariable(variable, interfaceType));
		}
		return resolved;
	}

	private GenericType resolveVariableAgainstParameterizedType(TypeVariable<?> variable, GenericType type) {
		if (type.getType() instanceof ParameterizedType
				&& ObjectUtils.nullSafeEquals(type.getTargetClass(), variable.getGenericDeclaration())) {
			TypeVariable<?>[] typeParameters = type.getTargetClass().getTypeParameters();
			Type[] actualTypeArguments = ((ParameterizedType) type.getType()).getActualTypeArguments();
			for (int i = 0; i < typeParameters.length; i++) {
				if (isSameVariableName(variable, typeParameters[i])) {
					return get(type, actualTypeArguments[i], false);
				}
			}
		}
		return null;
	}

	private boolean isSameVariableName(TypeVariable<?> variable, TypeVariable<?> typeParameters) {
		return ObjectUtils.nullSafeEquals(getVariableName(typeParameters), getVariableName(variable));
	}

	private String getVariableName(TypeVariable<?> variable) {
		return variable == null ? null : variable.getName();
	}

	private GenericType resolveBounds(Type[] bounds) {
		if (bounds != null && bounds.length > 0 && !Object.class.equals(bounds[0])) {
			return get(this, bounds[0], false);
		}
		return null;
	}


	/**
	 * Returns the underlying {@link Type} being managed.
	 * @return the underlying type (never {@code null}).
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * Returns the type {@link Class} or {@code null} if this type cannot be resolved to a
	 * class.
	 * @return the resolved type class or {@code null}
	 */
	public Class<?> getTypeClass() {
		Class<?> typeClass = getTargetClass();
		if(typeClass != null && isArray()) {
			return Array.newInstance(typeClass, 0).getClass();
		}
		return typeClass;
	}

	private Class<?> getTargetClass() {
		if(target != null) {
			return target.getTargetClass();
		}
		if(type instanceof Class) {
			return (Class<?>)type;
		}
		return null;
	}

	/**
	 * Return {@code true} if this type represents an array.
	 * @return {@code true} if this is an array type
	 */
	public boolean isArray() {
		return array;
	}

	/**
	 * Returns the first generic type or {@code null} if the generic cannot be obtained.
	 * @return the generic type class or {@code null}
	 */
	public Class<?> getGenericTypeClass() {
		return getGenericTypeClass(0);
	}

	/**
	 * Return the generic type class at the specified index. For example if the underlying
	 * type is {@code Map<Integer, String>} calling {@code getGeneric(1)} will return
	 * {@code String}.
	 * @param index the index of the generic
	 * @return the generic type class or {@code null} if there is no generic at the
	 *         specified index
	 * @see #getGeneric(int)
	 * @see #getGenerics()
	 */
	public Class<?> getGenericTypeClass(int index) {
		GenericType generic = getGeneric(index);
		return (generic != null ? generic.getTargetClass() : null);
	}

	/**
	 * Returns the first generic or {@code null} if the generic cannot be obtained.
	 * @return the generic type or {@code null}
	 */
	public GenericType getGeneric() {
		return getGeneric(0);
	}

	/**
	 * Return the generic type at the specified index. For example if the underlying type
	 * is {@code Map<K, V>} calling {@code getGeneric(1)} will return {@code V}.
	 * @param index the index of the generic
	 * @return the generic type or {@code null} if there is no generic at the specified
	 *         index
	 * @see #getGenerics()
	 */
	public GenericType getGeneric(int index) {
		GenericType[] generics = getGenerics();
		if(index >= 0 && index < generics.length) {
			return generics[index];
		}
		return null;
	}

	/**
	 * Return all generics defined on this type.  For example if the underlying
	 * type is {@code Map<Integer,List<String>>} this method will return the array
	 * containing the two types {@code Integer, List<String>}.  If no generics are
	 * defined an empty array is returned.
	 * @return the generics or an empty array
	 */
	public GenericType[] getGenerics() {
		if (this.generics == null) {
			this.generics = EMPTY_GENERIC_TYPES;
			GenericType candidate = this;
			while(candidate != null) {
				Type candidateType = candidate.getType();
				if(candidateType instanceof ParameterizedType && candidate.getTargetClass() != null) {
					this.generics = asOwnedGenericTypes(this, ((ParameterizedType) candidateType).getActualTypeArguments(), false);
					break;
				}
				candidate = candidate.target;
			}
		}
		return this.generics;
	}

	/**
	 * Returns a {@link GenericType} for the superclass of this item or {@code null} if
	 * there is no supertype.
	 * @return the supertype or {@code null}
	 */
	public GenericType getSuper() {
		if (this.superType == null) {
			Class<?> typeClass = getTargetClass();
			if (typeClass == null || typeClass.getGenericSuperclass() == null) {
				this.superType = NONE;
			} else {
				this.superType = get(this, typeClass.getGenericSuperclass(), isArray());
			}
		}
		return (this.superType == NONE ? null : this.superType);
	}

	/**
	 * Returns an array {@link GenericType}s for each interface implemented. If no
	 * interfaces are implemented an empty array is returned.
	 * @return the interfaces or an empty array
	 */
	public GenericType[] getInterfaces() {
		if (this.interfaces == null) {
			Class<?> typeClass = getTargetClass();
			if (typeClass == null) {
				this.interfaces = EMPTY_GENERIC_TYPES;
			} else {
				this.interfaces = asOwnedGenericTypes(this, typeClass.getGenericInterfaces(), isArray());
			}
		}
		return this.interfaces;
	}

	private GenericType[] asOwnedGenericTypes(GenericType owner, Type[] types, boolean array) {
		GenericType[] genericTypes = new GenericType[types.length];
		for (int i = 0; i < types.length; i++) {
			genericTypes[i] = get(owner, types[i], array);
		}
		return genericTypes;
	}

	/**
	 * Search the entire inheritance hierarchy (both superclass and implemented
	 * interfaces) to get the specified class.
	 * @param typeClass the type of class to get.
	 * @return a {@link GenericType} for the found class or {@code null}.
	 */
	public GenericType get(Class<?> typeClass) {
		GenericType type = null;
		if (ObjectUtils.nullSafeEquals(getTargetClass(), typeClass)) {
			return this;
		}
		if (getSuper() != null) {
			type = getSuper().get(typeClass);
		}
		if (type == null) {
			for (GenericType interfaceType : getInterfaces()) {
				type = interfaceType.get(typeClass);
				if (type != null) {
					break;
				}
			}
		}
		return type;
	}

	@Override
	public String toString() {
		StringBuilder name = new StringBuilder();
		name.append(getTargetClass() != null ? getTargetClass().getName() : "?");
		GenericType[] generics = getGenerics();
		if (generics.length > 0) {
			name.append("<");
			for (int i = 0; i < generics.length; i++) {
				name.append(i > 0 ? ", " : "");
				name.append(generics[i]);
			}
			name.append(">");
		}
		name.append(isArray() ? "[]" : "");
		return name.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj == null || obj.getClass() != GenericType.class) {
			return false;
		}
		GenericType other = (GenericType) obj;
		return ObjectUtils.nullSafeEquals(this.type, other.type)
				&& ObjectUtils.nullSafeEquals(this.owner, other.owner);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.type) * 31
				+ ObjectUtils.nullSafeHashCode(this.owner);
	}


	/**
	 * Get a {@link GenericType} for the specified {@link MethodParameter}.
	 * @param methodParameter the method parameter
	 * @return a generic type
	 */
	public static GenericType fromMethodParameter(MethodParameter methodParameter) {
		return fromMethodParameter(methodParameter, null);
	}

	/**
	 * Get a {@link GenericType} for the specified {@link MethodParameter}.
	 * @param methodParameter the method parameter
	 * @param clazz The actual class used to resolve method parameter variables or {@code null}
	 * @return a generic type
	 */
	public static GenericType fromMethodParameter(MethodParameter methodParameter, Class<?> clazz) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		return get(null, methodParameter.getGenericParameterType(), false);
	}

	/**
	 * Get a {@link GenericType} for the specified {@link Field}.
	 * @param field the field
	 * @return a generic type
	 */
	public static GenericType fromField(Field field) {
		return fromField(field, null);
	}

	/**
	 * Get a {@link GenericType} for the specified {@link Field}.
	 * @param field the method parameter
	 * @param clazz The actual class used to resolve field variables or {@code null}
	 * @return a generic type
	 */
	public static GenericType fromField(Field field, Class<?> clazz) {
		Assert.notNull(field, "Field must not be null");
		return get(null, field.getGenericType(), false);
	}

	/**
	 * Get a {@link GenericType} for the specified {@link Method} return.
	 * @param method the method
	 * @return a generic type
	 */
	public static GenericType fromMethodReturn(Method method) {
		return fromMethodReturn(method, null);
	}

	/**
	 * Get a {@link GenericType} for the specified {@link Method} return.
	 * @param method the method
	 * @param clazz The actual class used to resolve method return variables or {@code null}
	 * @return a generic type
	 */
	public static GenericType fromMethodReturn(Method method, Class<?> clazz) {
		Assert.notNull(method, "Method must not be null");
		GenericType owner = (clazz == null ? null : fromClass(clazz));
		return get(owner, method.getGenericReturnType(), false);
	}

	/**
	 * Get a {@link GenericType} for the specified {@link Class}.
	 * @param clazz the class
	 * @return a generic type
	 */
	public static GenericType fromClass(Class<?> clazz) {
		Assert.notNull(clazz, "ClassType must not be null");
		return get(null, clazz, false);
	}

	private static GenericType get(GenericType owner, Type type, boolean array) {
		if(type == null) {
			return null;
		}
		//FIXME we should probably cache here based on owner, type & array
		return new GenericType(owner, type, array);
	}
}
