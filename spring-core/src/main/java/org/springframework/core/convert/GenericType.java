
package org.springframework.core.convert;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 *
 * @author Phillip Webb
 */
public abstract class GenericType {

	private static final GenericType[] EMPTY_GENERIC_TYPES = {};

	private Type type;

	private Class<?> typeClass;

	private TypeVariable<?>[] typeParameters;

	private Type[] actualTypeArguments;

	public GenericType(Type type) {
		initialize(type);
	}

	protected void initialize(Type type) {
		Assert.notNull(type);
		this.type = type;
		if (type instanceof Class) {
			this.typeClass = (Class<?>) type;
		}
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			if (parameterizedType.getRawType() instanceof Class) {
				typeClass = (Class<?>) parameterizedType.getRawType();
				this.typeParameters = typeClass.getTypeParameters();
				this.actualTypeArguments = parameterizedType.getActualTypeArguments();
			}
		}
	}

	public GenericType getSuperclass() {
		if (getTypeClass() == null || getTypeClass().getGenericSuperclass() == null) {
			return null;
		}
		return new RelatedGenericType(this, getTypeClass().getGenericSuperclass());
	}

	public GenericType[] getInterfaces() {
		if (getTypeClass() == null) {
			return EMPTY_GENERIC_TYPES;
		}
		return asRelatedGenericTypes(getTypeClass().getGenericInterfaces());
	}

	public GenericType[] getGenerics() {
		if (getTypeClass() == null || (!(getType() instanceof ParameterizedType))) {
			return EMPTY_GENERIC_TYPES;
		}
		return asRelatedGenericTypes(((ParameterizedType) getType()).getActualTypeArguments());
	}

	public Type getType() {
		return type;
	}

	public Class<?> getTypeClass() {
		return typeClass;
	}

	@Override
	public String toString() {
		StringBuilder name = new StringBuilder();
		name.append(getTypeClass() == null ? "?" : getTypeClass().getName());
		GenericType[] generics = getGenerics();
		if(generics.length > 0) {
			name.append("<");
			for(int i=0;i<generics.length;i++) {
				name.append(i > 0 ? ", " : "");
				name.append(generics[i]);
			}
			name.append(">");
		}
		return name.toString();
	}

	protected GenericType getOwner() {
		return null;
	}

	protected Class<?> resolve(TypeVariable<?> variable) {
		GenericType owner = getOwner();
		if(typeParameters != null) {
			for(int i=0; i<typeParameters.length; i++) {
				if(ObjectUtils.nullSafeEquals(variable.getName(), typeParameters[i].getName())) {
					Type type = actualTypeArguments[i];
					if(type instanceof TypeVariable && owner != null) {
						return owner.resolve((TypeVariable<?>) type);
					}
					if(type instanceof Class) {
						return (Class<?>) type;
					}
					//FIXME could be Param Type or class
				}
			}
		}
		return null;
	}

	private GenericType[] asRelatedGenericTypes(Type[] types) {
		GenericType[] genericTypes = new GenericType[types.length];
		for (int i = 0; i < types.length; i++) {
			genericTypes[i] = new RelatedGenericType(this, types[i]);
		}
		return genericTypes;

	}

	private static class RootGenericType extends GenericType {

		public RootGenericType(Type type) {
			super(type);
		}

		@Override
		protected void initialize(Type type) {
			super.initialize(type);
			if (getTypeClass() == null) {
				throw new IllegalArgumentException("Unsupported type " + type);
			}
		}
	}

	private static class RelatedGenericType extends GenericType {

		private GenericType owner;

		public RelatedGenericType(GenericType owner, Type type) {
			super(type);
			this.owner = owner;
		}

		@Override
		public Class<?> getTypeClass() {
			Type type = getType();
			Class<?> typeClass = super.getTypeClass();
			if(typeClass == null && owner != null && type instanceof TypeVariable) {
				typeClass = owner.resolve((TypeVariable<?>)type);
			}
			return typeClass;
		}

		public GenericType getOwner() {
			return owner;
		}
	}

	public static GenericType get(Class<?> typeClass) {
		return new RootGenericType(typeClass);
	}

	public static GenericType get(MethodParameter methodParameter) {
		return new RootGenericType(GenericTypeResolver.getTargetType(methodParameter));
	}

	public static GenericType get(Field field) {
		return new RootGenericType(field.getGenericType());
	}
}
