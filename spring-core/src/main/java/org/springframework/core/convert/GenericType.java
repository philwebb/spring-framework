
package org.springframework.core.convert;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Wrapper for {@link java.lang.reflect.Type}s that allows generic information to be
 * easily obtained.
 *
 * @author Phillip Webb
 * @since 3.2
 * @see #getGenerics()
 * @see #getSuperType()
 * @see #getInterfaces()
 */
public final class GenericType {

	private static final GenericType[] EMPTY_GENERIC_TYPES = {};

	private static final GenericType NONE = new GenericType(Void.class);


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
	 * Create a new {@link GenericType} instance from the specified {@link Type}.
	 *
	 * @param type the underlying type
	 */
	public GenericType(Type type) {
		this(null, type, false);
	}

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
			this.target = resolveVariable(owner, (TypeVariable<?>) type);
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

	private GenericType resolveVariable(GenericType owner, TypeVariable<?> variable) {
		GenericType candidate = owner;
		while (candidate != null) {
			GenericType candidateTarget = candidate;
			while (candidateTarget != null) {
				if (candidateTarget.getType() instanceof ParameterizedType
						&& ObjectUtils.nullSafeEquals(candidateTarget.getTargetClass(),
								variable.getGenericDeclaration())) {
					TypeVariable<?>[] typeParameters = candidateTarget.getTargetClass().getTypeParameters();
					Type[] actualTypeArguments = ((ParameterizedType) candidateTarget.getType()).getActualTypeArguments();
					for (int i = 0; i < typeParameters.length; i++) {
						if (ObjectUtils.nullSafeEquals(
								getVariableName(typeParameters[i]),
								getVariableName(variable))) {
							return new GenericType(candidateTarget, actualTypeArguments[i], false);
						}
					}
				}
				candidateTarget = candidateTarget.target;
			}
			candidate = candidate.owner;
		}
		if(owner != null) {
			if(owner.getSuperType() != null) {
				GenericType resolved = resolveVariable(owner.getSuperType(), variable);
				if(resolved != null) {
					return resolved;
				}
			}
			for (GenericType interfaceType: owner.getInterfaces()) {
				GenericType resolved = resolveVariable(interfaceType, variable);
				if(resolved != null) {
					return resolved;
				}
			}
		}
		return resolveBounds(variable.getBounds());
	}

	private String getVariableName(TypeVariable<?> variable) {
		return variable == null ? null : variable.getName();
	}

	private GenericType resolveBounds(Type[] bounds) {
		if (bounds != null && bounds.length > 0 && !Object.class.equals(bounds[0])) {
			return new GenericType(this, bounds[0], false);
		}
		return null;
	}


	/**
	 * Returns the underlying {@link Type} being managed.
	 * @return the underlying type (never {@code null}.
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

	// FIXME DC
	public boolean isArray() {
		return array;
	}

	/**
	 * Returns a {@link GenericType} for the superclass of this item or {@code null} if
	 * there is no supertype.
	 * @return the supertype or {@code null}
	 */
	public GenericType getSuperType() {
		if (this.superType == null) {
			Class<?> typeClass = getTargetClass();
			if (typeClass == null || typeClass.getGenericSuperclass() == null) {
				this.superType = NONE;
			} else {
				this.superType = new GenericType(this, typeClass.getGenericSuperclass(), isArray());
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

	//FIXME DOC
	public Class<?> getGenericTypeClass() {
		return getGenericTypeClass(0);
	}

	/**
	 * Return the generic type class at the specified index. For example if the underlying
	 * type is {@code Map<Integer, String>} calling {@code getGeneric(1)} will return
	 * {@code String}.
	 * @param index the index of the generic
	 * @return the generic type or {@code null} if there is no generic at the specified
	 *         index
	 * @see #getGeneric(int)
	 * @see #getGenerics()
	 */
	public Class<?> getGenericTypeClass(int index) {
		GenericType generic = getGeneric(index);
		return (generic != null ? generic.getTargetClass() : null);
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
	 * Search the entire inheritance hierarchy (both superclass and implemented
	 * interfaces) to find the specified class.
	 * @param typeClass the type of class to find.
	 * @return a {@link GenericType} for the found class or {@code null}.
	 */
	public GenericType find(Class<?> typeClass) {
		GenericType type = null;
		if (ObjectUtils.nullSafeEquals(getTargetClass(), typeClass)) {
			return this;
		}
		if (getSuperType() != null) {
			type = getSuperType().find(typeClass);
		}
		if (type == null) {
			for (GenericType interfaceType : getInterfaces()) {
				type = interfaceType.find(typeClass);
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

	private GenericType[] asOwnedGenericTypes(GenericType owner, Type[] types, boolean array) {
		GenericType[] genericTypes = new GenericType[types.length];
		for (int i = 0; i < types.length; i++) {
			//FIXME use factory method with agressive caching
			genericTypes[i] = new GenericType(owner, types[i], array);
		}
		return genericTypes;
	}

	public static GenericType get(Class<?> typeClass) {
		Assert.notNull(typeClass, "TypeClass must not be null");
		return new GenericType(typeClass);
	}

	public static GenericType get(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "methodParameter must not be null");
		return new GenericType(GenericTypeResolver.getTargetType(methodParameter));
	}

	public static GenericType get(Field field) {
		Assert.notNull(field, "Field must not be null");
		return new GenericType(field.getGenericType());
	}

	public static GenericType getForMethodReturnType(Method method) {
		Assert.notNull(method, "Method must not be null");
		return new GenericType(method.getGenericReturnType());
	}

	//FIXME change to get(Class<?> owner, Method methd);
	public static GenericType getForMethodReturnType(Method method,
			GenericType owner) {
		Assert.notNull(method, "Method must not be null");
		return new GenericType(owner, method.getGenericReturnType(), false);
	}
}
