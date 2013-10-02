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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link ResolvableType}.
 */
public class ResolvableTypeTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void forClass() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.getType(), equalTo((Type) ExtendsList.class));
	}

	@Test
	public void forClassMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Class must not be null");
		ResolvableType.forClass(null);
	}

	@Test
	public void forField() throws Exception {
		Field field = Fields.class.getField("charSequenceList");
		ResolvableType type = ResolvableType.forField(field);
		assertThat(type.getType(), equalTo(field.getGenericType()));
	}

	@Test
	public void forFieldMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Field must not be null");
		ResolvableType.forField(null);
	}

	@Test
	public void forConstructorParameter() throws Exception {
		Constructor<Constructors> constructor = Constructors.class.getConstructor(List.class);
		ResolvableType type = ResolvableType.forConstructorParameter(constructor, 0);
		assertThat(type.getType(), equalTo(constructor.getGenericParameterTypes()[0]));
	}

	@Test
	public void forConstructorParameterMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Constructor must not be null");
		ResolvableType.forConstructorParameter(null, 0);
	}

	@Test
	public void forMethodParameterByIndex() throws Exception {
		Method method = Methods.class.getMethod("charSequenceParameter", List.class);
		ResolvableType type = ResolvableType.forMethodParameter(method, 0);
		assertThat(type.getType(), equalTo(method.getGenericParameterTypes()[0]));
	}

	@Test
	public void forMethodParameterByIndexMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Method must not be null");
		ResolvableType.forMethodParameter(null, 0);
	}

	@Test
	public void forMethodParameter() throws Exception {
		Method method = Methods.class.getMethod("charSequenceParameter", List.class);
		MethodParameter methodParameter = MethodParameter.forMethodOrConstructor(method,
				0);
		ResolvableType type = ResolvableType.forMethodParameter(methodParameter);
		assertThat(type.getType(), equalTo(method.getGenericParameterTypes()[0]));
	}

	@Test
	public void forMethodParameterMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("MethodParameter must not be null");
		ResolvableType.forMethodParameter(null);
	}

	@Test
	public void forMethodReturn() throws Exception {
		Method method = Methods.class.getMethod("charSequenceReturn");
		ResolvableType type = ResolvableType.forMethodReturn(method);
		assertThat(type.getType(), equalTo(method.getGenericReturnType()));
	}

	@Test
	public void forMethodReturnMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Method must not be null");
		ResolvableType.forMethodReturn(null);
	}

	@Test
	public void classType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("classType"));
		assertThat(type.getType().getClass(), equalTo((Class) Class.class));
	}

	@Test
	public void arrayClassType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("arrayClassType"));
		assertThat(type.getType(), instanceOf(Class.class));
		assertThat(((Class) type.getType()).isArray(), equalTo(true));
	}

	@Test
	public void genericArrayType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericArrayType"));
		assertThat(type.getType(), instanceOf(GenericArrayType.class));
	}

	@Test
	public void wildcardType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("wildcardType"));
		assertThat(type.getType(), instanceOf(ParameterizedType.class));
		assertThat(type.getGeneric().getType(), instanceOf(WildcardType.class));
	}

	@Test
	public void typeVariableType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("typeVariableType"));
		assertThat(type.getType(), instanceOf(ParameterizedType.class));
		assertThat(type.getGeneric().getType(), instanceOf(TypeVariable.class));
	}

	@Test
	public void getGeneric() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringList"));
		assertThat(type.getGeneric().getType(), equalTo((Type) String.class));
	}

	@Test
	public void getGenericByIndex() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringIntegerMultiValueMap"));
		assertThat(type.getGeneric(0).getType(), equalTo((Type) String.class));
		assertThat(type.getGeneric(1).getType(), equalTo((Type) Integer.class));
	}

	@Test
	public void getNestedGeneric() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringListList"));
		assertThat(type.getGeneric().getType().toString(),
				equalTo("java.util.List<java.lang.String>"));
		assertThat(type.getGeneric().getGeneric().getType(), equalTo((Type) String.class));
	}

	@Test
	public void getNestedGenericByIndexes() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringListList"));
		assertThat(type.getGeneric(0, 0).getType(), equalTo((Type) String.class));
	}

	@Test
	public void getExtendsGeneric() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("extendsCharSequenceList"));
		assertThat(type.getGeneric().getType().toString(),
				equalTo("? extends java.lang.CharSequence"));
	}

	@Test
	public void asFromInterface() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(List.class);
		assertThat(type.getType().toString(), equalTo("java.util.List<E>"));
	}

	@Test
	public void asFromInheritedInterface() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(Collection.class);
		assertThat(type.getType().toString(), equalTo("java.util.Collection<E>"));
	}

	@Test
	public void asNotFound() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(Map.class);
		assertThat(type, sameInstance(ResolvableType.NONE));
	}

	@Test
	public void asFromSuperType() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(ArrayList.class);
		assertThat(type.getType().toString(), equalTo("java.util.ArrayList<java.lang.CharSequence>"));
	}

	@Test
	public void resolveGenericClassType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringList"));
		assertThat(type.resolve(), equalTo((Class) List.class));
		assertThat(type.getGeneric().resolve(), equalTo((Class) String.class));
	}

	@Test
	public void resolveRawClassType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("classType"));
		assertThat(type.resolve(), equalTo((Class) List.class));
	}

	@Test
	public void resolveGenericInClass() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.resolve(), equalTo((Class) ExtendsList.class));
		assertThat(type.getSuperType().resolve(), equalTo((Class) ArrayList.class));
		assertThat(type.getSuperType().getGeneric().resolve(),
				equalTo((Class) CharSequence.class));
	}

	@Test
	@Ignore
	public void resolveBoundedTypeVariableResult() throws Exception {
		ResolvableType type = ResolvableType.forMethodReturn(Methods.class.getMethod("boundedTypeVaraibleResult"));
		assertThat(type.resolve(), equalTo((Class) CharSequence.class));
		//FIXME
	}

	@Test
	public void resolveArrayClassType() throws Exception {
		// FIXME
	}

	@Test
	public void resolveGenericArrayType() throws Exception {
		// FIXME
	}

	@Test
	public void resolveWildcardType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("wildcardType"));
		assertThat(type.getGeneric().resolve(), equalTo((Class) Number.class));
	}

	@Test
	public void resolveWildcardSuperType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("wildcardSuperType"));
		assertThat(type.getGeneric().resolve(), equalTo((Class) Number.class));
	}

	@Test
	public void resolveTypeVariableType() throws Exception {
		// FIXME
	}

	@Test
	public void resolveInherited() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringIntegerMultiValueMap")).as(Map.class);
		assertThat(type.getGeneric(0).resolve(), equalTo((Class) String.class));
		assertThat(type.getGeneric(1).resolve(), equalTo((Class) List.class));
		assertThat(type.getGeneric(1, 0).resolve(), equalTo((Class) Integer.class));
	}

	@Test
	public void resolveInheritedSwitched() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringIntegerMultiValueMapSwitched")).as(Map.class);
		assertThat(type.getGeneric(0).resolve(), equalTo((Class) String.class));
		assertThat(type.getGeneric(1).resolve(), equalTo((Class) List.class));
		assertThat(type.getGeneric(1, 0).resolve(), equalTo((Class) Integer.class));
	}

	@Test
	public void testName() throws Exception {
		MethodParameter parameter = MethodParameter.forMethodOrConstructor(Temp.class.getMethod("set", List.class), 0);
		GenericTypeResolver.resolveParameterType(parameter, Temp2.class);
		ResolvableType type = ResolvableType.forMethodParameter(parameter);
		ResolvableType genericAsCollection = type.getGeneric().as(Collection.class);
		assertThat(genericAsCollection.resolveGeneric(), equalTo((Class) Integer.class));
	}

	@Test
	public void testName2() throws Exception {
		MethodParameter parameter = MethodParameter.forMethodOrConstructor(Temp.class.getMethod("set", List.class), 0);
		ResolvableType type = ResolvableType.forMethodParameter(parameter, Temp2.class);
		ResolvableType genericAsCollection = type.getGeneric().as(Collection.class);
		assertThat(genericAsCollection.resolveGeneric(), equalTo((Class) Integer.class));
	}


	static class ExtendsList extends ArrayList<CharSequence> {
	}

	static class Fields<T> {

		public List classType;

		public List[] arrayClassType;

		public List<String>[] genericArrayType;

		public List<? extends Number> wildcardType;

		public List<? super Number> wildcardSuperType;

		public List<T> typeVariableType;

		public List<CharSequence> charSequenceList;

		public List<String> stringList;

		public List<List<String>> stringListList;

		public List<? extends CharSequence> extendsCharSequenceList;

		public MultiValueMap<String, Integer> stringIntegerMultiValueMap;

		public VariableNameSwitch<Integer, String> stringIntegerMultiValueMapSwitched;

	}

	static interface Methods {

		List<CharSequence> charSequenceReturn();

		void charSequenceParameter(List<CharSequence> cs);

		<T extends CharSequence & Serializable> T boundedTypeVaraibleResult();

	}

	static class Constructors {

		public Constructors(List<CharSequence> cs) {
		}
	}

	static interface Wildcard<T extends Number> extends List<T> {

	}

	static interface RawExtendsWildcard extends Wildcard {

	}

	static interface VariableNameSwitch<V, K> extends MultiValueMap<K, V> {
	}

	private static interface Temp<T> {
		public void set(List<T> listOfT);
	}

	private static interface Temp2 extends Temp<Set<Integer>> {
	}

}
