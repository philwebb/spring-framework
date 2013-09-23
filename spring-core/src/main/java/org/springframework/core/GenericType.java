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

import org.springframework.util.Assert;

/**
 * @author Phillip Webb
 * @since 4.0
 */
public final class GenericType {

	private final Type type;

	private GenericType[] generics;

	private GenericType(Type type) {
		Assert.notNull(type, "Type must not be null");
		this.type = type;
	}

	public Type getType() {
		return this.type;
	}

	public GenericType getGeneric(int... indexes) {
		if (indexes == null || indexes.length == 0) {
			return getGenerics()[0];
		}
		GenericType rtn = this;
		for (int index : indexes) {
			rtn = rtn.getGenerics()[index];
		}
		return rtn;
	}

	public GenericType[] getGenerics() {
		if(this.generics == null) {
			synchronized (this) {
				Assert.isInstanceOf(ParameterizedType.class, getType());
				Type[] actualTypeArguments = ((ParameterizedType) getType()).getActualTypeArguments();
				GenericType[] convertedGenerics = new GenericType[actualTypeArguments.length];
				for (int i = 0; i < convertedGenerics.length; i++) {
					convertedGenerics[i] = new GenericType(actualTypeArguments[i]);
				}
				this.generics = convertedGenerics;
			}
		}
		return this.generics;
	}

	//

	public static GenericType forClass(Class<?> type) {
		Assert.notNull(type, "Class must not be null");
		return new GenericType(type);
	}

	public static GenericType forField(Field field) {
		Assert.notNull(field, "Field must not be null");
		return new GenericType(field.getGenericType());
	}

	public static GenericType forConstructorParameter(Constructor<?> constructor,
			int parameterIndex) {
		Assert.notNull(constructor, "Constructor must not be null");
		return forMethodParameter(MethodParameter.forMethodOrConstructor(constructor,
				parameterIndex));
	}

	public static GenericType forMethodParameter(Method method, int parameterIndex) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(MethodParameter.forMethodOrConstructor(method,
				parameterIndex));
	}

	public static GenericType forMethodParameter(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		return new GenericType(methodParameter.getGenericParameterType());
	}

	public static GenericType forMethodReturn(Method method) {
		Assert.notNull(method, "Method must not be null");
		return new GenericType(method.getGenericReturnType());
	}

}
