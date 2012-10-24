
package org.springframework.core.convert;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 *
 * @author Phillip Webb
 */
public final class GenericType {

	private static final GenericType[] EMPTY_GENERIC_TYPES = {};

	private static final GenericType NONE = new GenericType(null, null);

	/**
	 * The owner that create this type (if any).
	 */
	private final GenericType owner;

	/**
	 * The underlying java type.
	 */
	private final Type type;

	/**
	 * Holds a reference to a nested resolved type (if any). Used when a type is
	 * recursively resolved.
	 */
	private GenericType resolvedType;

	/**
	 * The ultimate type class represented by this type (if any).
	 */
	private Class<?> typeClass;

	/**
	 * The superclass of this type or {@link #NONE} if there is no superclass. A
	 * {@code null} value indicates that the superclass has not yet been deduced.
	 */
	private GenericType superclass;

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
	 * Create a new {@link GenericType} instance from the specified {@link Type}.
	 *
	 * @param type the underlying type
	 */
	public GenericType(Type type) {
		this(null, type);
		Assert.notNull(type, "Type must not be null");
	}

	/**
	 * Internal private constructor used to create new {@link GenericType} instances.
	 *
	 * @param owner the owner (if any)
	 * @param type the underlying type
	 */
	private GenericType(GenericType owner, Type type) {
		this.owner = owner;
		this.type = type;
		if (type instanceof Class) {
			this.typeClass = (Class<?>) type;
		} else if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			this.resolvedType = new GenericType(owner, parameterizedType.getRawType());
			this.typeClass = this.resolvedType.getTypeClass();
		} else if (type instanceof TypeVariable) {
			this.resolvedType = resolveVariable(owner, (TypeVariable<?>) type);
			this.typeClass = this.resolvedType == null ? null
					: this.resolvedType.getTypeClass();
		}
		if (type instanceof GenericArrayType) {
			// FIXME
		}
		if (type instanceof WildcardType) {
			// FIXME
		}
	}

	private GenericType resolveVariable(GenericType owner, TypeVariable<?> variable) {
		if (owner == null || owner.getTypeClass() == null
				|| !(owner.getType() instanceof ParameterizedType)) {
			return null;
		}
		TypeVariable<?>[] typeParameters = owner.getTypeClass().getTypeParameters();
		Type[] actualTypeArguments = ((ParameterizedType) owner.getType()).getActualTypeArguments();
		for (int i = 0; i < typeParameters.length; i++) {
			if (ObjectUtils.nullSafeEquals(getVariableName(typeParameters[i]),
					getVariableName(variable))) {
				return new GenericType(owner.owner, actualTypeArguments[i]);
			}
		}
		return null;
	}

	private String getVariableName(TypeVariable<?> variable) {
		return variable == null ? null : variable.getName();
	}

	public Type getType() {
		return this.type;
	}

	public Class<?> getTypeClass() {
		return this.typeClass;
	}

	public GenericType getSuperclass() {
		if (this.superclass == null) {
			if (getTypeClass() == null || getTypeClass().getGenericSuperclass() == null) {
				this.superclass = NONE;
			} else {
				this.superclass = new GenericType(this,
						getTypeClass().getGenericSuperclass());
			}
		}
		return (this.superclass == NONE ? null : this.superclass);
	}

	public GenericType[] getInterfaces() {
		if (this.interfaces == null) {
			if (getTypeClass() == null) {
				this.interfaces = EMPTY_GENERIC_TYPES;
			} else {
				this.interfaces = asRelatedGenericTypes(getTypeClass().getGenericInterfaces());
			}
		}
		return this.interfaces;
	}

	public GenericType[] getGenerics() {
		if (this.generics == null) {
			if (getTypeClass() == null || (!(getType() instanceof ParameterizedType))) {
				this.generics = EMPTY_GENERIC_TYPES;
			} else {
				this.generics = asRelatedGenericTypes(((ParameterizedType) getType()).getActualTypeArguments());
			}
		}
		return this.generics;
	}

	private GenericType[] asRelatedGenericTypes(Type[] types) {
		GenericType[] genericTypes = new GenericType[types.length];
		for (int i = 0; i < types.length; i++) {
			genericTypes[i] = new GenericType(this, types[i]);
		}
		return genericTypes;
	}

	public GenericType find(Class<?> typeClass) {
		GenericType type = null;
		if (ObjectUtils.nullSafeEquals(getTypeClass(), typeClass)) {
			return this;
		}
		if (getSuperclass() != null) {
			type = getSuperclass().find(typeClass);
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
		name.append(this.resolvedType != null ? this.resolvedType
				: (getTypeClass() != null ? getTypeClass().getName() : "?"));
		GenericType[] generics = getGenerics();
		if (generics.length > 0) {
			name.append("<");
			for (int i = 0; i < generics.length; i++) {
				name.append(i > 0 ? ", " : "");
				name.append(generics[i]);
			}
			name.append(">");
		}
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

	public static GenericType get(Class<?> typeClass) {
		return new GenericType(typeClass);
	}

	public static GenericType get(MethodParameter methodParameter) {
		return new GenericType(GenericTypeResolver.getTargetType(methodParameter));
	}

	public static GenericType get(Field field) {
		return new GenericType(field.getGenericType());
	}
}
