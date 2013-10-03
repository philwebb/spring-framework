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
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
	public void noneReturnValues() throws Exception {
		// FIXME
	}

	@Test
	public void forClass() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.getType(), equalTo((Type) ExtendsList.class));
	}

	@Test
	public void forClassMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Type class must not be null");
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
	public void forMethodParameterWithNesting() throws Exception {
		// FIXME
	}

	@Test
	public void forResolvedMethodParameter() throws Exception {
		// FIXME
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
	public void paramaterizedType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("parameterizedType"));
		assertThat(type.getType(), instanceOf(ParameterizedType.class));
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
		assertThat(type.getType(), instanceOf(TypeVariable.class));
	}

	@Test
	public void getComponentTypeForClassArray() throws Exception {
		Field field = Fields.class.getField("arrayClassType");
		ResolvableType type = ResolvableType.forField(field);
		assertThat(type.isArray(), equalTo(true));
		assertThat(type.getComponentType().getType(),
				equalTo((Type) ((Class) field.getGenericType()).getComponentType()));
	}

	@Test
	public void getComponentTypeForGenericArrayType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericArrayType"));
		assertThat(type.isArray(), equalTo(true));
		assertThat(type.getComponentType().getType(),
				equalTo(((GenericArrayType) type.getType()).getGenericComponentType()));
	}

	@Test
	public void getComponentTypeForVariableThatResolvesToGenericArray() throws Exception {
		ResolvableType type = ResolvableType.forClass(ListOfGenericArray.class).asCollection().getGeneric();
		assertThat(type.isArray(), equalTo(true));
		assertThat(type.getType(), instanceOf(TypeVariable.class));
		assertThat(type.getComponentType().getType().toString(), equalTo("java.util.List<java.lang.String>"));
	}

	@Test
	public void getComponentTypeForNonArray() throws Exception {
		ResolvableType type = ResolvableType.forClass(String.class);
		assertThat(type.isArray(), equalTo(false));
		assertThat(type.getComponentType(), equalTo(ResolvableType.NONE));
	}

	@Test
	public void asCollection() throws Exception {
		// FIXME
	}

	@Test
	public void asMap() throws Exception {
		// FIXME
	}

	@Test
	public void asFromInterface() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(List.class);
		assertThat(type.getType().toString(), equalTo("java.util.List<E>"));
	}

	@Test
	public void asFromInheritedInterface() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(
				Collection.class);
		assertThat(type.getType().toString(), equalTo("java.util.Collection<E>"));
	}

	@Test
	public void asFromSuperType() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(
				ArrayList.class);
		assertThat(type.getType().toString(),
				equalTo("java.util.ArrayList<java.lang.CharSequence>"));
	}

	@Test
	public void asFromInheritedSuperType() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(List.class);
		assertThat(type.getType().toString(), equalTo("java.util.List<E>"));
	}

	@Test
	public void asNotFound() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(Map.class);
		assertThat(type, sameInstance(ResolvableType.NONE));
	}

	@Test
	public void getSuperType() throws Exception {
		// FIXME
	}

	@Test
	public void getInterfaces() throws Exception {
		// FIXME
	}

	@Test
	public void noSuperType() throws Exception {
		// FIXME
	}

	@Test
	public void noInterfaces() throws Exception {
		// FIXME
	}

	@Test
	public void getNestedGeneric() throws Exception {
		// FIXME pick last convention
	}

	@Test
	public void getNestedGenericWithTypeIndexesPerLevel() throws Exception {
		// FIXME
	}

	@Test
	public void getNestedGenericWithNullTypeIndexesPerLevel() throws Exception {
		// FIXME
	}

	@Test
	public void testNestedGenericWithDerrivedType() throws Exception {
		// FIXME test the hack
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
	public void getGenericOfGeneric() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringListList"));
		assertThat(type.getGeneric().getType().toString(),
				equalTo("java.util.List<java.lang.String>"));
		assertThat(type.getGeneric().getGeneric().getType(), equalTo((Type) String.class));
	}

	@Test
	public void getGenericOfGenericByIndexes() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringListList"));
		assertThat(type.getGeneric(0, 0).getType(), equalTo((Type) String.class));
	}

	@Test
	public void getGenericOutOfBounds() throws Exception {
		// FIXME
	}

	@Test
	public void getGenerics() throws Exception {
		// FIXME
	}

	@Test
	public void noGetGenerics() throws Exception {
		// FIXME
	}

	@Test
	public void getResolvedGenerics() throws Exception {
		// FIXME
	}

	@Test
	public void resolveClassType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("classType"));
		assertThat(type.resolve(), equalTo((Class) List.class));
	}

	@Test
	public void resolveParameterizedType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("parameterizedType"));
		assertThat(type.resolve(), equalTo((Class) List.class));
	}

	@Test
	public void resolveArrayClassType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("arrayClassType"));
		assertThat(type.resolve(), equalTo((Class) List[].class));
	}

	@Test
	public void resolveGenericArrayType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericArrayType"));
		assertThat(type.resolve(), equalTo((Class) List[].class));
		assertThat(type.getComponentType().resolve(), equalTo((Class) List.class));
		assertThat(type.getComponentType().getGeneric().resolve(), equalTo((Class) String.class));
	}

	@Test
	public void resolveGenericMultiArrayType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericMultiArrayType"));
		assertThat(type.resolve(), equalTo((Class) List[][][].class));
		assertThat(type.getComponentType().resolve(), equalTo((Class) List[][].class));
	}

	@Test
	public void resolveGenericArrayFromGeneric() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringArrayList"));
		ResolvableType generic = type.asCollection().getGeneric();
		assertThat(generic.getType().toString(), equalTo("E"));
		assertThat(generic.isArray(), equalTo(true));
		assertThat(generic.resolve(), equalTo((Class) String[].class));
	}

	@Test
	public void resolveWildcardTypeUpperBounds() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("wildcardType"));
		assertThat(type.getGeneric().resolve(), equalTo((Class) Number.class));
	}

	@Test
	public void resolveWildcardLowerBounds() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("wildcardSuperType"));
		assertThat(type.getGeneric().resolve(), equalTo((Class) Number.class));
	}

	@Test
	public void resolveVariableFromFieldType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringList"));
		assertThat(type.resolve(), equalTo((Class) List.class));
		assertThat(type.getGeneric().resolve(), equalTo((Class) String.class));
	}

	@Test
	public void resolveVariableFromSuperType() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.resolve(), equalTo((Class) ExtendsList.class));
		assertThat(type.asCollection().resolveGeneric(),
				equalTo((Class) CharSequence.class));
	}

	@Test
	public void resolveVariableFromInheritedField() throws Exception {
		ResolvableType type = ResolvableType.forField(
				Fields.class.getField("stringIntegerMultiValueMap")).as(Map.class);
		assertThat(type.getGeneric(0).resolve(), equalTo((Class) String.class));
		assertThat(type.getGeneric(1).resolve(), equalTo((Class) List.class));
		assertThat(type.getGeneric(1, 0).resolve(), equalTo((Class) Integer.class));
	}

	@Test
	public void resolveVariableFromInheritedFieldSwitched() throws Exception {
		ResolvableType type = ResolvableType.forField(
				Fields.class.getField("stringIntegerMultiValueMapSwitched")).as(Map.class);
		assertThat(type.getGeneric(0).resolve(), equalTo((Class) String.class));
		assertThat(type.getGeneric(1).resolve(), equalTo((Class) List.class));
		assertThat(type.getGeneric(1, 0).resolve(), equalTo((Class) Integer.class));
	}

	@Test
	public void doesResolveFromOuterOwner() throws Exception {
		ResolvableType type = ResolvableType.forField(
				Fields.class.getField("listOfListOfUnknown")).as(Collection.class);
		assertThat(type.getGeneric(0).resolve(), equalTo((Class) List.class));
		assertThat(type.getGeneric(0).as(Collection.class).getGeneric(0).as(Collection.class).resolve(), nullValue());
		assertThat(type.getNestedGeneric(2).as(Collection.class).resolveGeneric(), nullValue());
	}

	@Test
	public void resolveVariableNotFound() throws Exception {
		// FIXME
	}

	@Test
	public void resolveBoundedTypeVariableResult() throws Exception {
		ResolvableType type = ResolvableType.forMethodReturn(Methods.class.getMethod("boundedTypeVaraibleResult"));
		assertThat(type.resolve(), equalTo((Class) CharSequence.class));
	}

	@Test
	public void resolveTypeVaraibleFromMethod() throws Exception {
		//FIXME
	}

	@Test
	public void resolveTypeVaraibleFromMethodAndOwner() throws Exception {
		//FIXME
	}

	@Test
	public void resolveTypeVaraibleFromSimpleInterfaceType() {
		ResolvableType type = ResolvableType.forClass(MySimpleInterfaceType.class).as(MyInterfaceType.class);
		assertThat(type.resolveGeneric(), equalTo((Class) String.class));
	}

	@Test
	public void resolveTypeVaraibleFromSimpleCollectionInterfaceType() {
		ResolvableType type = ResolvableType.forClass(MyCollectionInterfaceType.class).as(MyInterfaceType.class);
		assertThat(type.resolveGeneric(), equalTo((Class) Collection.class));
		assertThat(type.resolveGeneric(0, 0), equalTo((Class) String.class));
	}

	@Test
	public void resolveTypeVaraibleFromSimpleSuperclassType() {
		ResolvableType type = ResolvableType.forClass(MySimpleSuperclassType.class).as(MySuperclassType.class);
		assertThat(type.resolveGeneric(), equalTo((Class) String.class));
	}

	@Test
	public void resolveTypeVaraibleFromSimpleCollectionSuperclassType() {
		ResolvableType type = ResolvableType.forClass(MyCollectionSuperclassType.class).as(MySuperclassType.class);
		assertThat(type.resolveGeneric(), equalTo((Class) Collection.class));
		assertThat(type.resolveGeneric(0, 0), equalTo((Class) String.class));
	}

	@Test
	public void toStrings() throws Exception {
		assertThat(ResolvableType.NONE.toString(), equalTo("?"));

		assertFieldToStringValue("classType", "java.util.List");
		assertFieldToStringValue("typeVariableType", "?");
		assertFieldToStringValue("parameterizedType", "java.util.List<?>");
		assertFieldToStringValue("arrayClassType", "java.util.List[]");
		assertFieldToStringValue("genericArrayType", "java.util.List<java.lang.String>[]");
		assertFieldToStringValue("genericMultiArrayType", "java.util.List<java.lang.String>[][][]");
		assertFieldToStringValue("wildcardType", "java.util.List<java.lang.Number>"); // FIXME do we want to show wildcards?
		assertFieldToStringValue("wildcardSuperType", "java.util.List<java.lang.Number>");
		assertFieldToStringValue("charSequenceList", "java.util.List<java.lang.CharSequence>");
		assertFieldToStringValue("stringList", "java.util.List<java.lang.String>");
		assertFieldToStringValue("stringListList", "java.util.List<java.util.List<java.lang.String>>");
		assertFieldToStringValue("stringArrayList", "java.util.List<java.lang.String[]>");
		assertFieldToStringValue("extendsCharSequenceList", "java.util.List<java.lang.CharSequence>");
		assertFieldToStringValue("stringIntegerMultiValueMap", "org.springframework.util.MultiValueMap<java.lang.String, java.lang.Integer>");
		assertFieldToStringValue("stringIntegerMultiValueMapSwitched", VariableNameSwitch.class.getName() + "<java.lang.Integer, java.lang.String>");
		assertFieldToStringValue("listOfListOfUnknown", "java.util.List<java.util.List>");

		assertTypedFieldToStringValue("typeVariableType", "java.lang.String");
		assertTypedFieldToStringValue("parameterizedType", "java.util.List<java.lang.String>");

		assertThat(ResolvableType.forClass(ListOfGenericArray.class).toString(), equalTo(ListOfGenericArray.class.getName()));
		assertThat(ResolvableType.forClass(List.class, ListOfGenericArray.class).toString(), equalTo("java.util.List<java.util.List<java.lang.String>[]>"));
	}

	private void assertFieldToStringValue(String field, String expected) throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField(field));
		assertThat("field " + field + " toString", type.toString(), equalTo(expected));
	}

	private void assertTypedFieldToStringValue(String field, String expected) throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField(field), TypeFields.class);
		assertThat("field " + field + " toString", type.toString(), equalTo(expected));
	}

	// FIXME would be nice to support resolveReturnTypeForGenericMethod style

	// FIXME sort here down

	// FIXME Array getSuperType and Array getInterfaces ?

	// FIXME hasGenerics

	// FIXME asSelf

	// FIXME woth ownerTypes


	@Test
	public void getExtendsGeneric() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("extendsCharSequenceList"));
		assertThat(type.getGeneric().getType().toString(),
				equalTo("? extends java.lang.CharSequence"));
	}

	static class ExtendsList extends ArrayList<CharSequence> {
	}

	static class Fields<T> {

		public List classType;

		public T typeVariableType;

		public List<T> parameterizedType;

		public List[] arrayClassType;

		public List<String>[] genericArrayType;

		public List<String>[][][] genericMultiArrayType;

		public List<? extends Number> wildcardType;

		public List<? super Number> wildcardSuperType;

		public List<CharSequence> charSequenceList;

		public List<String> stringList;

		public List<List<String>> stringListList;

		public List<String[]> stringArrayList;

		public List<? extends CharSequence> extendsCharSequenceList;

		public MultiValueMap<String, Integer> stringIntegerMultiValueMap;

		public VariableNameSwitch<Integer, String> stringIntegerMultiValueMapSwitched;

		public List<List> listOfListOfUnknown;

	}

	static class TypeFields extends Fields<String> {
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

	public interface MyInterfaceType<T> {
	}

	public class MySimpleInterfaceType implements MyInterfaceType<String> {
	}

	public class MyCollectionInterfaceType implements MyInterfaceType<Collection<String>> {
	}

	public abstract class MySuperclassType<T> {
	}

	public class MySimpleSuperclassType extends MySuperclassType<String> {
	}

	public class MyCollectionSuperclassType extends MySuperclassType<Collection<String>> {
	}

	static interface Wildcard<T extends Number> extends List<T> {

	}

	static interface RawExtendsWildcard extends Wildcard {

	}

	static interface VariableNameSwitch<V, K> extends MultiValueMap<K, V> {
	}

	static interface ListOfGenericArray extends List<List<String>[]> {
	}
}
