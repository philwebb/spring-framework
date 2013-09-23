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

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link GenericType}.
 */
public class GenericTypeTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void forClass() throws Exception {
		GenericType type = GenericType.forClass(MyList.class);
		assertThat(type.getType(), equalTo((Type) MyList.class));
	}

	@Test
	public void forClassMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Class must not be null");
		GenericType.forClass(null);
	}

	@Test
	public void forField() throws Exception {
		Field field = Fields.class.getField("charSequenceList");
		GenericType type = GenericType.forField(field);
		assertThat(type.getType(), equalTo(field.getGenericType()));
	}

	@Test
	public void forFieldMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Field must not be null");
		GenericType.forField(null);
	}

	@Test
	public void forConstructorParameter() throws Exception {
		Constructor<Constructors> constructor = Constructors.class.getConstructor(List.class);
		GenericType type = GenericType.forConstructorParameter(constructor, 0);
		assertThat(type.getType(), equalTo(constructor.getGenericParameterTypes()[0]));
	}

	@Test
	public void forConstructorParameterMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Constructor must not be null");
		GenericType.forConstructorParameter(null, 0);
	}

	@Test
	public void forMethodParameterByIndex() throws Exception {
		Method method = Methods.class.getMethod("charSequenceParameter", List.class);
		GenericType type = GenericType.forMethodParameter(method, 0);
		assertThat(type.getType(), equalTo(method.getGenericParameterTypes()[0]));
	}

	@Test
	public void forMethodParameterByIndexMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Method must not be null");
		GenericType.forMethodParameter(null, 0);
	}

	@Test
	public void forMethodParameter() throws Exception {
		Method method = Methods.class.getMethod("charSequenceParameter", List.class);
		MethodParameter methodParameter = MethodParameter.forMethodOrConstructor(method,
				0);
		GenericType type = GenericType.forMethodParameter(methodParameter);
		assertThat(type.getType(), equalTo(method.getGenericParameterTypes()[0]));
	}

	@Test
	public void forMethodParameterMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("MethodParameter must not be null");
		GenericType.forMethodParameter(null);
	}

	@Test
	public void forMethodReturn() throws Exception {
		Method method = Methods.class.getMethod("charSequenceReturn");
		GenericType type = GenericType.forMethodReturn(method);
		assertThat(type.getType(), equalTo(method.getGenericReturnType()));
	}

	@Test
	public void forMethodReturnMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Method must not be null");
		GenericType.forMethodReturn(null);
	}

	@Test
	public void classType() throws Exception {
		GenericType type = GenericType.forField(Fields.class.getField("classType"));
		assertThat(type.getType().getClass(), equalTo((Class) Class.class));
	}

	@Test
	public void arrayClassType() throws Exception {
		GenericType type = GenericType.forField(Fields.class.getField("arrayClassType"));
		assertThat(type.getType(), instanceOf(Class.class));
		assertThat(((Class)type.getType()).isArray(), equalTo(true));
	}
	@Test
	public void genericArrayType() throws Exception {
		GenericType type = GenericType.forField(Fields.class.getField("genericArrayType"));
		assertThat(type.getType(), instanceOf(GenericArrayType.class));
	}

	@Test
	public void wildcardType() throws Exception {
		GenericType type = GenericType.forField(Fields.class.getField("wildcardType"));
		assertThat(type.getType(), instanceOf(ParameterizedType.class));
		assertThat(type.getGeneric().getType(), instanceOf(WildcardType.class));
	}

	@Test
	public void typeVariableType() throws Exception {
		GenericType type = GenericType.forField(Fields.class.getField("typeVariableType"));
		assertThat(type.getType(), instanceOf(ParameterizedType.class));
		assertThat(type.getGeneric().getType(), instanceOf(TypeVariable.class));
	}

	@Test
	public void getGeneric() throws Exception {
		GenericType type = GenericType.forField(Fields.class.getField("stringList"));
		assertThat(type.getGeneric().getType(), equalTo((Type) String.class));
	}

	@Test
	public void getGenericByIndex() throws Exception {
		GenericType type = GenericType.forField(Fields.class.getField("stringIntegerMultiValueMap"));
		assertThat(type.getGeneric(0).getType(), equalTo((Type) String.class));
		assertThat(type.getGeneric(1).getType(), equalTo((Type) Integer.class));
	}

	@Test
	public void getNestedGeneric() throws Exception {
		GenericType type = GenericType.forField(Fields.class.getField("stringListList"));
		assertThat(type.getGeneric().getType().toString(),
				equalTo("java.util.List<java.lang.String>"));
		assertThat(type.getGeneric().getGeneric().getType(), equalTo((Type) String.class));
	}

	@Test
	public void getNestedGenericByIndexes() throws Exception {
		GenericType type = GenericType.forField(Fields.class.getField("stringListList"));
		assertThat(type.getGeneric(0, 0).getType(), equalTo((Type) String.class));
	}

	@Test
	public void getExtendsGeneric() throws Exception {
		GenericType type = GenericType.forField(Fields.class.getField("extendsCharSequenceList"));
		assertThat(type.getGeneric().getType().toString(),
				equalTo("? extends java.lang.CharSequence"));
	}

	@Test
	public void dunno() throws Exception {
		Field field = Fields.class.getField("stringIntegerMultiValueMap");
		GenericType type = GenericType.forField(field);
		ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
		Type x = ((Class) parameterizedType.getRawType()).getGenericInterfaces()[0];
		System.out.println(x);
		System.out.println(((ParameterizedType)x).getActualTypeArguments()[0]);
		System.out.println(Arrays.asList(List.class.getTypeParameters()));
		System.out.println(ArrayList.class.getGenericSuperclass().toString());
		System.out.println(ArrayList.class.getGenericSuperclass().getClass());

		TypeVariable<Class<Wibble>>[] typeParameters = Wibble.class.getTypeParameters();
		for (TypeVariable<Class<Wibble>> typeVariable : typeParameters) {
			System.out.println(typeVariable);
			System.out.println(Arrays.asList(typeVariable.getBounds()));
		}

		System.out.println(GenericTypeResolver.resolveTypeArgument(MyWibble.class, Wibble.class));
		System.out.println(MyWibble.class.getGenericSuperclass());
		System.out.println(GenericTypeResolver.resolveTypeArgument(ArrayList.class, Map.class));
		//GenericType.forClass(Wibble.class).withGenerics(String.class);
		// GenericArrayType
		// ParameterizedType
		// TypeVariable
		// WildcardType

		// Class<?>

		// GenericType.forClass(List.class).getGeneric().resolve();
		// GenericType.forClass(stringList).getGeneric().resolve();
		// GenericType.forClass(stringList).resolveGenerics();


		// GenericType.forMethodReturn(Wibble::get, ConcreteWibble.class);
		// GenericType.forMethodReturn(Wibble::get(M), ConcreteWibble.class);
	}


	private static class Wibble<T extends CharSequence & Serializable> {
		public T get() {
			return null;
		}

		public <M> M get(Class<M> x) {
			return null;
		}
	}

	private static class MyWibble<T extends CharSequence & Serializable> extends Wibble<T> {
	}

	private static class ConcreteWibble extends Wibble<String> {
	}

	static class MyList extends ArrayList<CharSequence> {
	}

	static class Fields<T> {

		public List classType;

		public List[] arrayClassType;

		public List<String>[] genericArrayType;

		public List<? extends Number> wildcardType;

		public List<T> typeVariableType;

		public List<CharSequence> charSequenceList;

		public List<String> stringList;

		public List<List<String>> stringListList;

		public List<? extends CharSequence> extendsCharSequenceList;

		public MultiValueMap<String, Integer> stringIntegerMultiValueMap;


	}

	static interface Methods {

		List<CharSequence> charSequenceReturn();

		void charSequenceParameter(List<CharSequence> cs);

		<T extends CharSequence & Serializable> T doIt();

	}

	static class Constructors {

		public Constructors(List<CharSequence> cs) {
		}
	}

}
