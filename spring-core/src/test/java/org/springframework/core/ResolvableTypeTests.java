/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.assertj.core.api.AbstractAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.core.ResolvableType.VariableResolver;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThat;

import static temp.XAssert.assertEquals;
import static temp.XAssert.assertFalse;
import static temp.XAssert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ResolvableType}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 */
@SuppressWarnings("rawtypes")
@RunWith(MockitoJUnitRunner.class)
public class ResolvableTypeTests {

	@Captor
	private ArgumentCaptor<TypeVariable<?>> typeVariableCaptor;


	@Test
	public void noneReturnValues() throws Exception {
		ResolvableType none = ResolvableType.NONE;
		assertThat(none.as(Object.class)).isEqualTo(ResolvableType.NONE);
		assertThat(none.asCollection()).isEqualTo(ResolvableType.NONE);
		assertThat(none.asMap()).isEqualTo(ResolvableType.NONE);
		assertThat(none.getComponentType()).isEqualTo(ResolvableType.NONE);
		assertThat(none.getGeneric(0)).isEqualTo(ResolvableType.NONE);
		assertThat(none.getGenerics().length).isEqualTo(0);
		assertThat(none.getInterfaces().length).isEqualTo(0);
		assertThat(none.getSuperType()).isEqualTo(ResolvableType.NONE);
		assertThat(none.getType()).isEqualTo(ResolvableType.EmptyType.INSTANCE);
		assertThat(none.hasGenerics()).isEqualTo(false);
		assertThat(none.isArray()).isEqualTo(false);
		assertThat(none.resolve()).isNull();
		assertThat(none.resolve(String.class)).isEqualTo(String.class);
		assertThat(none.resolveGeneric(0)).isNull();
		assertThat(none.resolveGenerics().length).isEqualTo(0);
		assertThat(none.toString()).isEqualTo("?");
		assertThat(none.hasUnresolvableGenerics()).isEqualTo(false);
		assertThat(none.isAssignableFrom(ResolvableType.forClass(Object.class))).isEqualTo(false);
	}

	@Test
	public void forClass() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.getType()).isEqualTo(ExtendsList.class);
		assertThat(type.getRawClass()).isEqualTo(ExtendsList.class);
		assertTrue(type.isAssignableFrom(ExtendsList.class));
		assertFalse(type.isAssignableFrom(ArrayList.class));
	}

	@Test
	public void forClassWithNull() throws Exception {
		ResolvableType type = ResolvableType.forClass(null);
		assertThat(type.getType()).isEqualTo(Object.class);
		assertThat(type.getRawClass()).isEqualTo(Object.class);
		assertTrue(type.isAssignableFrom(Object.class));
		assertTrue(type.isAssignableFrom(String.class));
	}

	@Test
	public void forRawClass() throws Exception {
		ResolvableType type = ResolvableType.forRawClass(ExtendsList.class);
		assertThat(type.getType()).isEqualTo(ExtendsList.class);
		assertThat(type.getRawClass()).isEqualTo(ExtendsList.class);
		assertTrue(type.isAssignableFrom(ExtendsList.class));
		assertFalse(type.isAssignableFrom(ArrayList.class));
	}

	@Test
	public void forRawClassWithNull() throws Exception {
		ResolvableType type = ResolvableType.forRawClass(null);
		assertThat(type.getType()).isEqualTo(Object.class);
		assertThat(type.getRawClass()).isEqualTo(Object.class);
		assertTrue(type.isAssignableFrom(Object.class));
		assertTrue(type.isAssignableFrom(String.class));
	}

	@Test
	public void forInstanceMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ResolvableType.forInstance(null))
			.withMessageContaining("Instance must not be null");
	}

	@Test
	public void forInstanceNoProvider() {
		ResolvableType type = ResolvableType.forInstance(new Object());
		assertThat(type.getType()).isEqualTo(Object.class);
		assertThat(type.resolve()).isEqualTo(Object.class);
	}

	@Test
	public void forInstanceProvider() {
		ResolvableType type = ResolvableType.forInstance(new MyGenericInterfaceType<>(String.class));
		assertThat(type.getRawClass()).isEqualTo(MyGenericInterfaceType.class);
		assertThat(type.getGeneric().resolve()).isEqualTo(String.class);
	}

	@Test
	public void forInstanceProviderNull() {
		ResolvableType type = ResolvableType.forInstance(new MyGenericInterfaceType<String>(null));
		assertThat(type.getType()).isEqualTo(MyGenericInterfaceType.class);
		assertThat(type.resolve()).isEqualTo(MyGenericInterfaceType.class);
	}

	@Test
	public void forField() throws Exception {
		Field field = Fields.class.getField("charSequenceList");
		ResolvableType type = ResolvableType.forField(field);
		assertThat(type.getType()).isEqualTo(field.getGenericType());
	}

	@Test
	public void forPrivateField() throws Exception {
		Field field = Fields.class.getDeclaredField("privateField");
		ResolvableType type = ResolvableType.forField(field);
		assertThat(type.getType()).isEqualTo(field.getGenericType());
		assertThat(type.resolve()).isEqualTo(List.class);
		assertThat(type.getSource()).isSameAs(field);

		Field field2 = Fields.class.getDeclaredField("otherPrivateField");
		ResolvableType type2 = ResolvableType.forField(field2);
		assertThat(type2.getType()).isEqualTo(field2.getGenericType());
		assertThat(type2.resolve()).isEqualTo(List.class);
		assertThat(type2.getSource()).isSameAs(field2);

		assertEquals(type, type2);
		assertEquals(type.hashCode(), type2.hashCode());
	}

	@Test
	public void forFieldMustNotBeNull() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ResolvableType.forField(null))
			.withMessageContaining("Field must not be null");
	}

	@Test
	public void forConstructorParameter() throws Exception {
		Constructor<Constructors> constructor = Constructors.class.getConstructor(List.class);
		ResolvableType type = ResolvableType.forConstructorParameter(constructor, 0);
		assertThat(type.getType()).isEqualTo(constructor.getGenericParameterTypes()[0]);
	}

	@Test
	public void forConstructorParameterMustNotBeNull() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ResolvableType.forConstructorParameter(null, 0))
			.withMessageContaining("Constructor must not be null");
	}

	@Test
	public void forMethodParameterByIndex() throws Exception {
		Method method = Methods.class.getMethod("charSequenceParameter", List.class);
		ResolvableType type = ResolvableType.forMethodParameter(method, 0);
		assertThat(type.getType()).isEqualTo(method.getGenericParameterTypes()[0]);
	}

	@Test
	public void forMethodParameterByIndexMustNotBeNull() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ResolvableType.forMethodParameter(null, 0))
			.withMessageContaining("Method must not be null");
	}

	@Test
	public void forMethodParameter() throws Exception {
		Method method = Methods.class.getMethod("charSequenceParameter", List.class);
		MethodParameter methodParameter = MethodParameter.forExecutable(method, 0);
		ResolvableType type = ResolvableType.forMethodParameter(methodParameter);
		assertThat(type.getType()).isEqualTo(method.getGenericParameterTypes()[0]);
	}

	@Test
	public void forMethodParameterWithNesting() throws Exception {
		Method method = Methods.class.getMethod("nested", Map.class);
		MethodParameter methodParameter = MethodParameter.forExecutable(method, 0);
		methodParameter.increaseNestingLevel();
		ResolvableType type = ResolvableType.forMethodParameter(methodParameter);
		assertThat(type.resolve()).isEqualTo(Map.class);
		assertThat(type.getGeneric(0).resolve()).isEqualTo(Byte.class);
		assertThat(type.getGeneric(1).resolve()).isEqualTo(Long.class);
	}

	@Test
	public void forMethodParameterWithNestingAndLevels() throws Exception {
		Method method = Methods.class.getMethod("nested", Map.class);
		MethodParameter methodParameter = MethodParameter.forExecutable(method, 0);
		methodParameter.increaseNestingLevel();
		methodParameter.setTypeIndexForCurrentLevel(0);
		ResolvableType type = ResolvableType.forMethodParameter(methodParameter);
		assertThat(type.resolve()).isEqualTo(Map.class);
		assertThat(type.getGeneric(0).resolve()).isEqualTo(String.class);
		assertThat(type.getGeneric(1).resolve()).isEqualTo(Integer.class);
	}

	@Test
	public void forMethodParameterMustNotBeNull() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ResolvableType.forMethodParameter(null))
			.withMessageContaining("MethodParameter must not be null");
	}

	@Test  // SPR-16210
	public void forMethodParameterWithSameSignatureAndGenerics() throws Exception {
		Method method = Methods.class.getMethod("list1");
		MethodParameter methodParameter = MethodParameter.forExecutable(method, -1);
		ResolvableType type = ResolvableType.forMethodParameter(methodParameter);
		assertThat(((MethodParameter)type.getSource()).getMethod()).isEqualTo(method);

		method = Methods.class.getMethod("list2");
		methodParameter = MethodParameter.forExecutable(method, -1);
		type = ResolvableType.forMethodParameter(methodParameter);
		assertThat(((MethodParameter)type.getSource()).getMethod()).isEqualTo(method);
	}

	@Test
	public void forMethodReturn() throws Exception {
		Method method = Methods.class.getMethod("charSequenceReturn");
		ResolvableType type = ResolvableType.forMethodReturnType(method);
		assertThat(type.getType()).isEqualTo(method.getGenericReturnType());
	}

	@Test
	public void forMethodReturnMustNotBeNull() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ResolvableType.forMethodReturnType(null))
			.withMessageContaining("Method must not be null");
	}

	@Test
	public void classType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("classType"));
		assertThat(type.getType().getClass()).isEqualTo(Class.class);
	}

	@Test
	public void paramaterizedType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("parameterizedType"));
		assertThat(type.getType()).isInstanceOf(ParameterizedType.class);
	}

	@Test
	public void arrayClassType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("arrayClassType"));
		assertThat(type.getType()).isInstanceOf(Class.class);
		assertThat(((Class) type.getType()).isArray()).isEqualTo(true);
	}

	@Test
	public void genericArrayType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericArrayType"));
		assertThat(type.getType()).isInstanceOf(GenericArrayType.class);
	}

	@Test
	public void wildcardType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("wildcardType"));
		assertThat(type.getType()).isInstanceOf(ParameterizedType.class);
		assertThat(type.getGeneric().getType()).isInstanceOf(WildcardType.class);
	}

	@Test
	public void typeVariableType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("typeVariableType"));
		assertThat(type.getType()).isInstanceOf(TypeVariable.class);
	}

	@Test
	public void getComponentTypeForClassArray() throws Exception {
		Field field = Fields.class.getField("arrayClassType");
		ResolvableType type = ResolvableType.forField(field);
		assertThat(type.isArray()).isEqualTo(true);
		assertThat(type.getComponentType().getType())
			.isEqualTo(((Class) field.getGenericType()).getComponentType());
	}

	@Test
	public void getComponentTypeForGenericArrayType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericArrayType"));
		assertThat(type.isArray()).isEqualTo(true);
		assertThat(type.getComponentType().getType()).isEqualTo(
				((GenericArrayType) type.getType()).getGenericComponentType());
	}

	@Test
	public void getComponentTypeForVariableThatResolvesToGenericArray() throws Exception {
		ResolvableType type = ResolvableType.forClass(ListOfGenericArray.class).asCollection().getGeneric();
		assertThat(type.isArray()).isEqualTo(true);
		assertThat(type.getType()).isInstanceOf(TypeVariable.class);
		assertThat(type.getComponentType().getType().toString()).isEqualTo(
				"java.util.List<java.lang.String>");
	}

	@Test
	public void getComponentTypeForNonArray() throws Exception {
		ResolvableType type = ResolvableType.forClass(String.class);
		assertThat(type.isArray()).isEqualTo(false);
		assertThat(type.getComponentType()).isEqualTo(ResolvableType.NONE);
	}

	@Test
	public void asCollection() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).asCollection();
		assertThat(type.resolve()).isEqualTo(Collection.class);
		assertThat(type.resolveGeneric()).isEqualTo(CharSequence.class);
	}

	@Test
	public void asMap() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsMap.class).asMap();
		assertThat(type.resolve()).isEqualTo(Map.class);
		assertThat(type.resolveGeneric(0)).isEqualTo(String.class);
		assertThat(type.resolveGeneric(1)).isEqualTo(Integer.class);
	}

	@Test
	public void asFromInterface() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(List.class);
		assertThat(type.getType().toString()).isEqualTo("java.util.List<E>");
	}

	@Test
	public void asFromInheritedInterface() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(Collection.class);
		assertThat(type.getType().toString()).isEqualTo("java.util.Collection<E>");
	}

	@Test
	public void asFromSuperType() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(ArrayList.class);
		assertThat(type.getType().toString()).isEqualTo("java.util.ArrayList<java.lang.CharSequence>");
	}

	@Test
	public void asFromInheritedSuperType() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(List.class);
		assertThat(type.getType().toString()).isEqualTo("java.util.List<E>");
	}

	@Test
	public void asNotFound() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).as(Map.class);
		assertThat(type).isSameAs(ResolvableType.NONE);
	}

	@Test
	public void asSelf() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.as(ExtendsList.class)).isEqualTo(type);
	}

	@Test
	public void getSuperType() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class).getSuperType();
		assertThat(type.resolve()).isEqualTo(ArrayList.class);
		type = type.getSuperType();
		assertThat(type.resolve()).isEqualTo(AbstractList.class);
		type = type.getSuperType();
		assertThat(type.resolve()).isEqualTo(AbstractCollection.class);
		type = type.getSuperType();
		assertThat(type.resolve()).isEqualTo(Object.class);
	}

	@Test
	public void getInterfaces() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.getInterfaces().length).isEqualTo(0);
		SortedSet<String> interfaces = new TreeSet<>();
		for (ResolvableType interfaceType : type.getSuperType().getInterfaces()) {
			interfaces.add(interfaceType.toString());
		}
		assertThat(interfaces.toString()).isEqualTo(
				"["
				+ "java.io.Serializable, "
				+ "java.lang.Cloneable, "
				+ "java.util.List<java.lang.CharSequence>, "
				+ "java.util.RandomAccess"
				+ "]");
	}

	@Test
	public void noSuperType() throws Exception {
		assertThat(ResolvableType.forClass(Object.class).getSuperType())
				.isEqualTo(ResolvableType.NONE);
	}

	@Test
	public void noInterfaces() throws Exception {
		assertThat(ResolvableType.forClass(Object.class).getInterfaces()).isEmpty();
	}

	@Test
	public void nested() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("nested"));
		type = type.getNested(2);
		assertThat(type.resolve()).isEqualTo(Map.class);
		assertThat(type.getGeneric(0).resolve()).isEqualTo(Byte.class);
		assertThat(type.getGeneric(1).resolve()).isEqualTo(Long.class);
	}

	@Test
	public void nestedWithIndexes() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("nested"));
		type = type.getNested(2, Collections.singletonMap(2, 0));
		assertThat(type.resolve()).isEqualTo(Map.class);
		assertThat(type.getGeneric(0).resolve()).isEqualTo(String.class);
		assertThat(type.getGeneric(1).resolve()).isEqualTo(Integer.class);
	}

	@Test
	public void nestedWithArray() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericArrayType"));
		type = type.getNested(2);
		assertThat(type.resolve()).isEqualTo(List.class);
		assertThat(type.resolveGeneric()).isEqualTo(String.class);
	}

	@Test
	public void getGeneric() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringList"));
		assertThat(type.getGeneric().getType()).isEqualTo(String.class);
	}

	@Test
	public void getGenericByIndex() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringIntegerMultiValueMap"));
		assertThat(type.getGeneric(0).getType()).isEqualTo(String.class);
		assertThat(type.getGeneric(1).getType()).isEqualTo(Integer.class);
	}

	@Test
	public void getGenericOfGeneric() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringListList"));
		assertThat(type.getGeneric().getType().toString()).isEqualTo("java.util.List<java.lang.String>");
		assertThat(type.getGeneric().getGeneric().getType()).isEqualTo(String.class);
	}

	@Test
	public void genericOfGenericWithAs() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringListList")).asCollection();
		assertThat(type.toString()).isEqualTo("java.util.Collection<java.util.List<java.lang.String>>");
		assertThat(type.getGeneric().asCollection().toString()).isEqualTo("java.util.Collection<java.lang.String>");
	}

	@Test
	public void getGenericOfGenericByIndexes() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringListList"));
		assertThat(type.getGeneric(0, 0).getType()).isEqualTo(String.class);
	}

	@Test
	public void getGenericOutOfBounds() throws Exception {
		ResolvableType type = ResolvableType.forClass(List.class, ExtendsList.class);
		assertThat(type.getGeneric(0)).isNotEqualTo(ResolvableType.NONE);
		assertThat(type.getGeneric(1)).isEqualTo(ResolvableType.NONE);
		assertThat(type.getGeneric(0, 1)).isEqualTo(ResolvableType.NONE);
	}

	@Test
	public void hasGenerics() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.hasGenerics()).isEqualTo(false);
		assertThat(type.asCollection().hasGenerics()).isEqualTo(true);
	}

	@Test
	public void getGenericsFromParameterizedType() throws Exception {
		ResolvableType type = ResolvableType.forClass(List.class, ExtendsList.class);
		ResolvableType[] generics = type.getGenerics();
		assertThat(generics.length).isEqualTo(1);
		assertThat(generics[0].resolve()).isEqualTo(CharSequence.class);
	}

	@Test
	public void getGenericsFromClass() throws Exception {
		ResolvableType type = ResolvableType.forClass(List.class);
		ResolvableType[] generics = type.getGenerics();
		assertThat(generics.length).isEqualTo(1);
		assertThat(generics[0].getType().toString()).isEqualTo("E");
	}

	@Test
	public void noGetGenerics() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		ResolvableType[] generics = type.getGenerics();
		assertThat(generics.length).isEqualTo(0);
	}

	@Test
	public void getResolvedGenerics() throws Exception {
		ResolvableType type = ResolvableType.forClass(List.class, ExtendsList.class);
		Class<?>[] generics = type.resolveGenerics();
		assertThat(generics.length).isEqualTo(1);
		assertThat(generics[0]).isEqualTo(CharSequence.class);
	}

	@Test
	public void resolveClassType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("classType"));
		assertThat(type.resolve()).isEqualTo(List.class);
	}

	@Test
	public void resolveParameterizedType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("parameterizedType"));
		assertThat(type.resolve()).isEqualTo(List.class);
	}

	@Test
	public void resolveArrayClassType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("arrayClassType"));
		assertThat(type.resolve()).isEqualTo(List[].class);
	}

	@Test
	public void resolveGenericArrayType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericArrayType"));
		assertThat(type.resolve()).isEqualTo(List[].class);
		assertThat(type.getComponentType().resolve()).isEqualTo(List.class);
		assertThat(type.getComponentType().getGeneric().resolve()).isEqualTo(String.class);
	}

	@Test
	public void resolveGenericMultiArrayType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("genericMultiArrayType"));
		assertThat(type.resolve()).isEqualTo(List[][][].class);
		assertThat(type.getComponentType().resolve()).isEqualTo(List[][].class);
	}

	@Test
	public void resolveGenericArrayFromGeneric() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringArrayList"));
		ResolvableType generic = type.asCollection().getGeneric();
		assertThat(generic.getType().toString()).isEqualTo("E");
		assertThat(generic.isArray()).isEqualTo(true);
		assertThat(generic.resolve()).isEqualTo(String[].class);
	}

	@Test
	public void resolveVariableGenericArray() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("variableTypeGenericArray"), TypedFields.class);
		assertThat(type.getType().toString()).isEqualTo("T[]");
		assertThat(type.isArray()).isEqualTo(true);
		assertThat(type.resolve()).isEqualTo(String[].class);
	}

	@Test
	public void resolveVariableGenericArrayUnknown() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("variableTypeGenericArray"));
		assertThat(type.getType().toString()).isEqualTo("T[]");
		assertThat(type.isArray()).isEqualTo(true);
		assertThat(type.resolve()).isNull();
	}

	@Test
	public void resolveVariableGenericArrayUnknownWithFallback() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("variableTypeGenericArray"));
		assertThat(type.getType().toString()).isEqualTo("T[]");
		assertThat(type.isArray()).isEqualTo(true);
		assertThat(type.toClass()).isEqualTo(Object.class);
	}

	@Test
	public void resolveWildcardTypeUpperBounds() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("wildcardType"));
		assertThat(type.getGeneric().resolve()).isEqualTo(Number.class);
	}

	@Test
	public void resolveWildcardLowerBounds() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("wildcardSuperType"));
		assertThat(type.getGeneric().resolve()).isEqualTo(Number.class);
	}

	@Test
	public void resolveVariableFromFieldType() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringList"));
		assertThat(type.resolve()).isEqualTo(List.class);
		assertThat(type.getGeneric().resolve()).isEqualTo(String.class);
	}

	@Test
	public void resolveVariableFromFieldTypeUnknown() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("parameterizedType"));
		assertThat(type.resolve()).isEqualTo(List.class);
		assertThat(type.getGeneric().resolve()).isNull();
	}

	@Test
	public void resolveVariableFromInheritedField() throws Exception {
		ResolvableType type = ResolvableType.forField(
				Fields.class.getField("stringIntegerMultiValueMap")).as(Map.class);
		assertThat(type.getGeneric(0).resolve()).isEqualTo(String.class);
		assertThat(type.getGeneric(1).resolve()).isEqualTo(List.class);
		assertThat(type.getGeneric(1, 0).resolve()).isEqualTo(Integer.class);
	}

	@Test
	public void resolveVariableFromInheritedFieldSwitched() throws Exception {
		ResolvableType type = ResolvableType.forField(
				Fields.class.getField("stringIntegerMultiValueMapSwitched")).as(Map.class);
		assertThat(type.getGeneric(0).resolve()).isEqualTo(String.class);
		assertThat(type.getGeneric(1).resolve()).isEqualTo(List.class);
		assertThat(type.getGeneric(1, 0).resolve()).isEqualTo(Integer.class);
	}

	@Test
	public void doesResolveFromOuterOwner() throws Exception {
		ResolvableType type = ResolvableType.forField(
				Fields.class.getField("listOfListOfUnknown")).as(Collection.class);
		assertThat(type.getGeneric(0).resolve()).isEqualTo(List.class);
		assertThat(type.getGeneric(0).as(Collection.class).getGeneric(0).as(Collection.class).resolve()).isNull();
	}

	@Test
	public void resolveBoundedTypeVariableResult() throws Exception {
		ResolvableType type = ResolvableType.forMethodReturnType(Methods.class.getMethod("boundedTypeVaraibleResult"));
		assertThat(type.resolve()).isEqualTo(CharSequence.class);
	}

	@Test
	public void resolveVariableNotFound() throws Exception {
		ResolvableType type = ResolvableType.forMethodReturnType(Methods.class.getMethod("typedReturn"));
		assertThat(type.resolve()).isNull();
	}

	@Test
	public void resolveTypeVaraibleFromMethodReturn() throws Exception {
		ResolvableType type = ResolvableType.forMethodReturnType(Methods.class.getMethod("typedReturn"));
		assertThat(type.resolve()).isNull();
	}

	@Test
	public void resolveTypeVaraibleFromMethodReturnWithInstanceClass() throws Exception {
		ResolvableType type = ResolvableType.forMethodReturnType(
				Methods.class.getMethod("typedReturn"), TypedMethods.class);
		assertThat(type.resolve()).isEqualTo(String.class);
	}

	@Test
	public void resolveTypeVaraibleFromSimpleInterfaceType() {
		ResolvableType type = ResolvableType.forClass(
				MySimpleInterfaceType.class).as(MyInterfaceType.class);
		assertThat(type.resolveGeneric()).isEqualTo(String.class);
	}

	@Test
	public void resolveTypeVaraibleFromSimpleCollectionInterfaceType() {
		ResolvableType type = ResolvableType.forClass(
				MyCollectionInterfaceType.class).as(MyInterfaceType.class);
		assertThat(type.resolveGeneric()).isEqualTo(Collection.class);
		assertThat(type.resolveGeneric(0, 0)).isEqualTo(String.class);
	}

	@Test
	public void resolveTypeVaraibleFromSimpleSuperclassType() {
		ResolvableType type = ResolvableType.forClass(
				MySimpleSuperclassType.class).as(MySuperclassType.class);
		assertThat(type.resolveGeneric()).isEqualTo(String.class);
	}

	@Test
	public void resolveTypeVaraibleFromSimpleCollectionSuperclassType() {
		ResolvableType type = ResolvableType.forClass(
				MyCollectionSuperclassType.class).as(MySuperclassType.class);
		assertThat(type.resolveGeneric()).isEqualTo(Collection.class);
		assertThat(type.resolveGeneric(0, 0)).isEqualTo(String.class);
	}

	@Test
	public void resolveTypeVariableFromFieldTypeWithImplementsClass() throws Exception {
		ResolvableType type = ResolvableType.forField(
				Fields.class.getField("parameterizedType"), TypedFields.class);
		assertThat(type.resolve()).isEqualTo(List.class);
		assertThat(type.getGeneric().resolve()).isEqualTo(String.class);
	}

	@Test
	public void resolveTypeVariableFromFieldTypeWithImplementsType() throws Exception {
		ResolvableType implementationType = ResolvableType.forClassWithGenerics(
				Fields.class, Integer.class);
		ResolvableType type = ResolvableType.forField(
				Fields.class.getField("parameterizedType"), implementationType);
		assertThat(type.resolve()).isEqualTo(List.class);
		assertThat(type.getGeneric().resolve()).isEqualTo(Integer.class);
	}

	@Test
	public void resolveTypeVariableFromSuperType() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsList.class);
		assertThat(type.resolve()).isEqualTo(ExtendsList.class);
		assertThat(type.asCollection().resolveGeneric())
			.isEqualTo(CharSequence.class);
	}

	@Test
	public void resolveTypeVariableFromClassWithImplementsClass() throws Exception {
		ResolvableType type = ResolvableType.forClass(
				MySuperclassType.class, MyCollectionSuperclassType.class);
		assertThat(type.resolveGeneric()).isEqualTo(Collection.class);
		assertThat(type.resolveGeneric(0, 0)).isEqualTo(String.class);
	}

	@Test
	public void resolveTypeVariableFromConstructorParameter() throws Exception {
		Constructor<?> constructor = Constructors.class.getConstructor(List.class);
		ResolvableType type = ResolvableType.forConstructorParameter(constructor, 0);
		assertThat(type.resolve()).isEqualTo(List.class);
		assertThat(type.resolveGeneric(0)).isEqualTo(CharSequence.class);
	}

	@Test
	public void resolveUnknownTypeVariableFromConstructorParameter() throws Exception {
		Constructor<?> constructor = Constructors.class.getConstructor(Map.class);
		ResolvableType type = ResolvableType.forConstructorParameter(constructor, 0);
		assertThat(type.resolve()).isEqualTo(Map.class);
		assertThat(type.resolveGeneric(0)).isNull();
	}

	@Test
	public void resolveTypeVariableFromConstructorParameterWithImplementsClass() throws Exception {
		Constructor<?> constructor = Constructors.class.getConstructor(Map.class);
		ResolvableType type = ResolvableType.forConstructorParameter(
				constructor, 0, TypedConstructors.class);
		assertThat(type.resolve()).isEqualTo(Map.class);
		assertThat(type.resolveGeneric(0)).isEqualTo(String.class);
	}

	@Test
	public void resolveTypeVariableFromMethodParameter() throws Exception {
		Method method = Methods.class.getMethod("typedParameter", Object.class);
		ResolvableType type = ResolvableType.forMethodParameter(method, 0);
		assertThat(type.resolve()).isNull();
		assertThat(type.getType().toString()).isEqualTo("T");
	}

	@Test
	public void resolveTypeVariableFromMethodParameterWithImplementsClass() throws Exception {
		Method method = Methods.class.getMethod("typedParameter", Object.class);
		ResolvableType type = ResolvableType.forMethodParameter(method, 0, TypedMethods.class);
		assertThat(type.resolve()).isEqualTo(String.class);
		assertThat(type.getType().toString()).isEqualTo("T");
	}

	@Test
	public void resolveTypeVariableFromMethodParameterType() throws Exception {
		Method method = Methods.class.getMethod("typedParameter", Object.class);
		MethodParameter methodParameter = MethodParameter.forExecutable(method, 0);
		ResolvableType type = ResolvableType.forMethodParameter(methodParameter);
		assertThat(type.resolve()).isNull();
		assertThat(type.getType().toString()).isEqualTo("T");
	}

	@Test
	public void resolveTypeVariableFromMethodParameterTypeWithImplementsClass() throws Exception {
		Method method = Methods.class.getMethod("typedParameter", Object.class);
		MethodParameter methodParameter = MethodParameter.forExecutable(method, 0);
		methodParameter.setContainingClass(TypedMethods.class);
		ResolvableType type = ResolvableType.forMethodParameter(methodParameter);
		assertThat(type.resolve()).isEqualTo(String.class);
		assertThat(type.getType().toString()).isEqualTo("T");
	}

	@Test
	public void resolveTypeVariableFromMethodParameterTypeWithImplementsType() throws Exception {
		Method method = Methods.class.getMethod("typedParameter", Object.class);
		MethodParameter methodParameter = MethodParameter.forExecutable(method, 0);
		ResolvableType implementationType = ResolvableType.forClassWithGenerics(Methods.class, Integer.class);
		ResolvableType type = ResolvableType.forMethodParameter(methodParameter, implementationType);
		assertThat(type.resolve()).isEqualTo(Integer.class);
		assertThat(type.getType().toString()).isEqualTo("T");
	}

	@Test
	public void resolveTypeVariableFromMethodReturn() throws Exception {
		Method method = Methods.class.getMethod("typedReturn");
		ResolvableType type = ResolvableType.forMethodReturnType(method);
		assertThat(type.resolve()).isNull();
		assertThat(type.getType().toString()).isEqualTo("T");
	}

	@Test
	public void resolveTypeVariableFromMethodReturnWithImplementsClass() throws Exception {
		Method method = Methods.class.getMethod("typedReturn");
		ResolvableType type = ResolvableType.forMethodReturnType(method, TypedMethods.class);
		assertThat(type.resolve()).isEqualTo(String.class);
		assertThat(type.getType().toString()).isEqualTo("T");
	}

	@Test
	public void resolveTypeVariableFromType() throws Exception {
		Type sourceType = Methods.class.getMethod("typedReturn").getGenericReturnType();
		ResolvableType type = ResolvableType.forType(sourceType);
		assertThat(type.resolve()).isNull();
		assertThat(type.getType().toString()).isEqualTo("T");
	}

	@Test
	public void resolveTypeVariableFromTypeWithVariableResolver() throws Exception {
		Type sourceType = Methods.class.getMethod("typedReturn").getGenericReturnType();
		ResolvableType type = ResolvableType.forType(
				sourceType, ResolvableType.forClass(TypedMethods.class).as(Methods.class).asVariableResolver());
		assertThat(type.resolve()).isEqualTo(String.class);
		assertThat(type.getType().toString()).isEqualTo("T");
	}

	@Test
	public void resolveTypeWithCustomVariableResolver() throws Exception {
		VariableResolver variableResolver = mock(VariableResolver.class);
		given(variableResolver.getSource()).willReturn(this);
		ResolvableType longType = ResolvableType.forClass(Long.class);
		given(variableResolver.resolveVariable(any())).willReturn(longType);

		ResolvableType variable = ResolvableType.forType(
				Fields.class.getField("typeVariableType").getGenericType(), variableResolver);
		ResolvableType parameterized = ResolvableType.forType(
				Fields.class.getField("parameterizedType").getGenericType(), variableResolver);

		assertThat(variable.resolve()).isEqualTo(Long.class);
		assertThat(parameterized.resolve()).isEqualTo(List.class);
		assertThat(parameterized.resolveGeneric()).isEqualTo(Long.class);
		verify(variableResolver, atLeastOnce()).resolveVariable(this.typeVariableCaptor.capture());
		assertThat(this.typeVariableCaptor.getValue().getName()).isEqualTo("T");
	}

	@Test
	public void resolveTypeVariableFromReflectiveParameterizedTypeReference() throws Exception {
		Type sourceType = Methods.class.getMethod("typedReturn").getGenericReturnType();
		ResolvableType type = ResolvableType.forType(ParameterizedTypeReference.forType(sourceType));
		assertThat(type.resolve()).isNull();
		assertThat(type.getType().toString()).isEqualTo("T");
	}

	@Test
	public void resolveTypeVariableFromDeclaredParameterizedTypeReference() throws Exception {
		Type sourceType = Methods.class.getMethod("charSequenceReturn").getGenericReturnType();
		ResolvableType reflectiveType = ResolvableType.forType(sourceType);
		ResolvableType declaredType = ResolvableType.forType(new ParameterizedTypeReference<List<CharSequence>>() {});
		assertEquals(reflectiveType, declaredType);
	}

	@Test
	public void toStrings() throws Exception {
		assertThat(ResolvableType.NONE.toString()).isEqualTo("?");

		assertThat(forField("classType")).hasToString("java.util.List<?>");
		assertThat(forField("typeVariableType")).hasToString("?");
		assertThat(forField("parameterizedType")).hasToString("java.util.List<?>");
		assertThat(forField("arrayClassType")).hasToString("java.util.List<?>[]");
		assertThat(forField("genericArrayType")).hasToString("java.util.List<java.lang.String>[]");
		assertThat(forField("genericMultiArrayType")).hasToString("java.util.List<java.lang.String>[][][]");
		assertThat(forField("wildcardType")).hasToString("java.util.List<java.lang.Number>");
		assertThat(forField("wildcardSuperType")).hasToString("java.util.List<java.lang.Number>");
		assertThat(forField("charSequenceList")).hasToString("java.util.List<java.lang.CharSequence>");
		assertThat(forField("stringList")).hasToString("java.util.List<java.lang.String>");
		assertThat(forField("stringListList")).hasToString("java.util.List<java.util.List<java.lang.String>>");
		assertThat(forField("stringArrayList")).hasToString("java.util.List<java.lang.String[]>");
		assertThat(forField("stringIntegerMultiValueMap")).hasToString("org.springframework.util.MultiValueMap<java.lang.String, java.lang.Integer>");
		assertThat(forField("stringIntegerMultiValueMapSwitched")).hasToString(VariableNameSwitch.class.getName() + "<java.lang.Integer, java.lang.String>");
		assertThat(forField("listOfListOfUnknown")).hasToString("java.util.List<java.util.List<?>>");

		assertThat(forTypedField("typeVariableType")).hasToString("java.lang.String");
		assertThat(forTypedField("parameterizedType")).hasToString("java.util.List<java.lang.String>");

		assertThat(ResolvableType.forClass(ListOfGenericArray.class).toString()).isEqualTo(ListOfGenericArray.class.getName());
		assertThat(ResolvableType.forClass(List.class, ListOfGenericArray.class).toString()).isEqualTo("java.util.List<java.util.List<java.lang.String>[]>");
	}

	@Test
	public void getSource() throws Exception {
		Class<?> classType = MySimpleInterfaceType.class;
		Field basicField = Fields.class.getField("classType");
		Field field = Fields.class.getField("charSequenceList");
		Method method = Methods.class.getMethod("charSequenceParameter", List.class);
		MethodParameter methodParameter = MethodParameter.forExecutable(method, 0);
		assertThat(ResolvableType.forField(basicField).getSource()).isEqualTo(basicField);
		assertThat(ResolvableType.forField(field).getSource()).isEqualTo(field);
		assertThat(ResolvableType.forMethodParameter(methodParameter).getSource()).isEqualTo(methodParameter);
		assertThat(ResolvableType.forMethodParameter(method, 0).getSource()).isEqualTo(methodParameter);
		assertThat(ResolvableType.forClass(classType).getSource()).isEqualTo(classType);
		assertThat(ResolvableType.forClass(classType).getSuperType().getSource()).isEqualTo(classType.getGenericSuperclass());
	}

	@Test
	public void resolveFromOuterClass() throws Exception {
		Field field = EnclosedInParameterizedType.InnerTyped.class.getField("field");
		ResolvableType type = ResolvableType.forField(field, TypedEnclosedInParameterizedType.TypedInnerTyped.class);
		assertThat(type.resolve()).isEqualTo(Integer.class);
	}

	@Test
	public void resolveFromClassWithGenerics() throws Exception {
		ResolvableType type = ResolvableType.forClassWithGenerics(List.class, ResolvableType.forClassWithGenerics(List.class, String.class));
		assertThat(type.asCollection().toString()).isEqualTo("java.util.Collection<java.util.List<java.lang.String>>");
		assertThat(type.asCollection().getGeneric().toString()).isEqualTo("java.util.List<java.lang.String>");
		assertThat(type.asCollection().getGeneric().asCollection().toString()).isEqualTo("java.util.Collection<java.lang.String>");
		assertThat(type.toString()).isEqualTo("java.util.List<java.util.List<java.lang.String>>");
		assertThat(type.asCollection().getGeneric().getGeneric().resolve()).isEqualTo(String.class);
	}

	@Test
	public void isAssignableFromMustNotBeNull() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ResolvableType.forClass(Object.class).isAssignableFrom((ResolvableType) null))
			.withMessageContaining("Type must not be null");
	}

	@Test
	public void isAssignableFromForNone() throws Exception {
		ResolvableType objectType = ResolvableType.forClass(Object.class);
		assertThat(objectType.isAssignableFrom(ResolvableType.NONE)).isEqualTo(false);
		assertThat(ResolvableType.NONE.isAssignableFrom(objectType)).isEqualTo(false);
	}

	@Test
	public void isAssignableFromForClassAndClass() throws Exception {
		ResolvableType objectType = ResolvableType.forClass(Object.class);
		ResolvableType charSequenceType = ResolvableType.forClass(CharSequence.class);
		ResolvableType stringType = ResolvableType.forClass(String.class);

		new ResolvableTypeAssert(objectType).isAssignableFrom(objectType, charSequenceType, stringType);
		new ResolvableTypeAssert(charSequenceType).isAssignableFrom(charSequenceType, stringType).isNotAssignableFrom(objectType);
		new ResolvableTypeAssert(stringType).isAssignableFrom(stringType).isNotAssignableFrom(objectType, charSequenceType);

		assertTrue(objectType.isAssignableFrom(String.class));
		assertTrue(objectType.isAssignableFrom(StringBuilder.class));
		assertTrue(charSequenceType.isAssignableFrom(String.class));
		assertTrue(charSequenceType.isAssignableFrom(StringBuilder.class));
		assertTrue(stringType.isAssignableFrom(String.class));
		assertFalse(stringType.isAssignableFrom(StringBuilder.class));

		assertTrue(objectType.isInstance("a String"));
		assertTrue(objectType.isInstance(new StringBuilder("a StringBuilder")));
		assertTrue(charSequenceType.isInstance("a String"));
		assertTrue(charSequenceType.isInstance(new StringBuilder("a StringBuilder")));
		assertTrue(stringType.isInstance("a String"));
		assertFalse(stringType.isInstance(new StringBuilder("a StringBuilder")));
	}

	@Test
	public void isAssignableFromCannotBeResolved() throws Exception {
		ResolvableType objectType = ResolvableType.forClass(Object.class);
		ResolvableType unresolvableVariable = ResolvableType.forField(AssignmentBase.class.getField("o"));
		assertThat(unresolvableVariable.resolve()).isNull();
		new ResolvableTypeAssert(objectType).isAssignableFrom(unresolvableVariable);
		new ResolvableTypeAssert(unresolvableVariable).isAssignableFrom(objectType);
	}

	@Test
	public void isAssignableFromForClassAndSimpleVariable() throws Exception {
		ResolvableType objectType = ResolvableType.forClass(Object.class);
		ResolvableType charSequenceType = ResolvableType.forClass(CharSequence.class);
		ResolvableType stringType = ResolvableType.forClass(String.class);

		ResolvableType objectVariable = ResolvableType.forField(AssignmentBase.class.getField("o"), Assignment.class);
		ResolvableType charSequenceVariable = ResolvableType.forField(AssignmentBase.class.getField("c"), Assignment.class);
		ResolvableType stringVariable = ResolvableType.forField(AssignmentBase.class.getField("s"), Assignment.class);

		new ResolvableTypeAssert(objectType).isAssignableFrom(objectVariable, charSequenceVariable, stringVariable);
		new ResolvableTypeAssert(charSequenceType).isAssignableFrom(charSequenceVariable, stringVariable).isNotAssignableFrom(objectVariable);
		new ResolvableTypeAssert(stringType).isAssignableFrom(stringVariable).isNotAssignableFrom(objectVariable, charSequenceVariable);

		new ResolvableTypeAssert(objectVariable).isAssignableFrom(objectType, charSequenceType, stringType);
		new ResolvableTypeAssert(charSequenceVariable).isAssignableFrom(charSequenceType, stringType).isNotAssignableFrom(objectType);
		new ResolvableTypeAssert(stringVariable).isAssignableFrom(stringType).isNotAssignableFrom(objectType, charSequenceType);

		new ResolvableTypeAssert(objectVariable).isAssignableFrom(objectVariable, charSequenceVariable, stringVariable);
		new ResolvableTypeAssert(charSequenceVariable).isAssignableFrom(charSequenceVariable, stringVariable).isNotAssignableFrom(objectVariable);
		new ResolvableTypeAssert(stringVariable).isAssignableFrom(stringVariable).isNotAssignableFrom(objectVariable, charSequenceVariable);
	}

	@Test
	public void isAssignableFromForSameClassNonExtendsGenerics() throws Exception {
		ResolvableType objectList = ResolvableType.forField(AssignmentBase.class.getField("listo"), Assignment.class);
		ResolvableType stringList = ResolvableType.forField(AssignmentBase.class.getField("lists"), Assignment.class);

		new ResolvableTypeAssert(stringList).isNotAssignableFrom(objectList);
		new ResolvableTypeAssert(objectList).isNotAssignableFrom(stringList);
		new ResolvableTypeAssert(stringList).isAssignableFrom(stringList);
	}

	@Test
	public void isAssignableFromForSameClassExtendsGenerics() throws Exception {

		// Generic assignment can be a little confusing, given:
		//
		// List<CharSequence> c1, List<? extends CharSequence> c2, List<String> s;
		//
		// c2 = s; is allowed and is often used for argument input, for example
		// see List.addAll(). You can get items from c2 but you cannot add items without
		// getting a generic type 'is not applicable for the arguments' error. This makes
		// sense since if you added a StringBuffer to c2 it would break the rules on s.
		//
		// c1 = s; not allowed. Since there is no '? extends' to cause the generic
		// 'is not applicable for the arguments' error when adding (which would pollute
		// s).

		ResolvableType objectList = ResolvableType.forField(AssignmentBase.class.getField("listo"), Assignment.class);
		ResolvableType charSequenceList = ResolvableType.forField(AssignmentBase.class.getField("listc"), Assignment.class);
		ResolvableType stringList = ResolvableType.forField(AssignmentBase.class.getField("lists"), Assignment.class);
		ResolvableType extendsObjectList = ResolvableType.forField(AssignmentBase.class.getField("listxo"), Assignment.class);
		ResolvableType extendsCharSequenceList = ResolvableType.forField(AssignmentBase.class.getField("listxc"), Assignment.class);
		ResolvableType extendsStringList = ResolvableType.forField(AssignmentBase.class.getField("listxs"), Assignment.class);

		new ResolvableTypeAssert(objectList).isNotAssignableFrom(extendsObjectList, extendsCharSequenceList, extendsStringList);
		new ResolvableTypeAssert(charSequenceList).isNotAssignableFrom(extendsObjectList, extendsCharSequenceList, extendsStringList);
		new ResolvableTypeAssert(stringList).isNotAssignableFrom(extendsObjectList, extendsCharSequenceList, extendsStringList);
		new ResolvableTypeAssert(extendsObjectList).isAssignableFrom(objectList, charSequenceList, stringList);
		new ResolvableTypeAssert(extendsObjectList).isAssignableFrom(extendsObjectList, extendsCharSequenceList, extendsStringList);
		new ResolvableTypeAssert(extendsCharSequenceList).isAssignableFrom(extendsCharSequenceList, extendsStringList).isNotAssignableFrom(extendsObjectList);
		new ResolvableTypeAssert(extendsCharSequenceList).isAssignableFrom(charSequenceList, stringList).isNotAssignableFrom(objectList);
		new ResolvableTypeAssert(extendsStringList).isAssignableFrom(extendsStringList).isNotAssignableFrom(extendsObjectList, extendsCharSequenceList);
		new ResolvableTypeAssert(extendsStringList).isAssignableFrom(stringList).isNotAssignableFrom(objectList, charSequenceList);
	}

	@Test
	public void isAssignableFromForDifferentClassesWithGenerics() throws Exception {
		ResolvableType extendsCharSequenceCollection = ResolvableType.forField(AssignmentBase.class.getField("collectionxc"), Assignment.class);
		ResolvableType charSequenceCollection = ResolvableType.forField(AssignmentBase.class.getField("collectionc"), Assignment.class);
		ResolvableType charSequenceList = ResolvableType.forField(AssignmentBase.class.getField("listc"), Assignment.class);
		ResolvableType extendsCharSequenceList = ResolvableType.forField(AssignmentBase.class.getField("listxc"), Assignment.class);
		ResolvableType extendsStringList = ResolvableType.forField(AssignmentBase.class.getField("listxs"), Assignment.class);

		new ResolvableTypeAssert(extendsCharSequenceCollection).isAssignableFrom(charSequenceCollection, charSequenceList, extendsCharSequenceList, extendsStringList);
		new ResolvableTypeAssert(charSequenceCollection).isAssignableFrom(charSequenceList).isNotAssignableFrom(extendsCharSequenceList, extendsStringList);
		new ResolvableTypeAssert(charSequenceList).isNotAssignableFrom(extendsCharSequenceCollection, charSequenceCollection);
		new ResolvableTypeAssert(extendsCharSequenceList).isNotAssignableFrom(extendsCharSequenceCollection, charSequenceCollection);
		new ResolvableTypeAssert(extendsStringList).isNotAssignableFrom(charSequenceCollection, charSequenceList, extendsCharSequenceList);
	}

	@Test
	public void isAssignableFromForArrays() throws Exception {
		ResolvableType object = ResolvableType.forField(AssignmentBase.class.getField("o"), Assignment.class);
		ResolvableType objectArray = ResolvableType.forField(AssignmentBase.class.getField("oarray"), Assignment.class);
		ResolvableType charSequenceArray = ResolvableType.forField(AssignmentBase.class.getField("carray"), Assignment.class);
		ResolvableType stringArray = ResolvableType.forField(AssignmentBase.class.getField("sarray"), Assignment.class);

		new ResolvableTypeAssert(object).isAssignableFrom(objectArray, charSequenceArray, stringArray);
		new ResolvableTypeAssert(objectArray).isAssignableFrom(objectArray, charSequenceArray, stringArray).isNotAssignableFrom(object);
		new ResolvableTypeAssert(charSequenceArray).isAssignableFrom(charSequenceArray, stringArray).isNotAssignableFrom(object, objectArray);
		new ResolvableTypeAssert(stringArray).isAssignableFrom(stringArray).isNotAssignableFrom(object, objectArray, charSequenceArray);
	}

	@Test
	public void isAssignableFromForWildcards() throws Exception {
		ResolvableType object = ResolvableType.forClass(Object.class);
		ResolvableType charSequence = ResolvableType.forClass(CharSequence.class);
		ResolvableType string = ResolvableType.forClass(String.class);
		ResolvableType extendsAnon = ResolvableType.forField(AssignmentBase.class.getField("listAnon"), Assignment.class).getGeneric();
		ResolvableType extendsObject = ResolvableType.forField(AssignmentBase.class.getField("listxo"), Assignment.class).getGeneric();
		ResolvableType extendsCharSequence = ResolvableType.forField(AssignmentBase.class.getField("listxc"), Assignment.class).getGeneric();
		ResolvableType extendsString = ResolvableType.forField(AssignmentBase.class.getField("listxs"), Assignment.class).getGeneric();
		ResolvableType superObject = ResolvableType.forField(AssignmentBase.class.getField("listso"), Assignment.class).getGeneric();
		ResolvableType superCharSequence = ResolvableType.forField(AssignmentBase.class.getField("listsc"), Assignment.class).getGeneric();
		ResolvableType superString = ResolvableType.forField(AssignmentBase.class.getField("listss"), Assignment.class).getGeneric();

		// Language Spec 4.5.1. Type Arguments and Wildcards

		// ? extends T <= ? extends S if T <: S
		new ResolvableTypeAssert(extendsCharSequence).isAssignableFrom(extendsCharSequence, extendsString).isNotAssignableFrom(extendsObject);
		new ResolvableTypeAssert(extendsCharSequence).isAssignableFrom(charSequence, string).isNotAssignableFrom(object);

		// ? super T <= ? super S if S <: T
		new ResolvableTypeAssert(superCharSequence).isAssignableFrom(superObject, superCharSequence).isNotAssignableFrom(superString);
		new ResolvableTypeAssert(superCharSequence).isAssignableFrom(object, charSequence).isNotAssignableFrom(string);

		// [Implied] super / extends cannot be mixed
		new ResolvableTypeAssert(superCharSequence).isNotAssignableFrom(extendsObject, extendsCharSequence, extendsString);
		new ResolvableTypeAssert(extendsCharSequence).isNotAssignableFrom(superObject, superCharSequence, superString);

		// T <= T
		new ResolvableTypeAssert(charSequence).isAssignableFrom(charSequence, string).isNotAssignableFrom(object);

		// T <= ? extends T
		new ResolvableTypeAssert(extendsCharSequence).isAssignableFrom(charSequence, string).isNotAssignableFrom(object);
		new ResolvableTypeAssert(charSequence).isNotAssignableFrom(extendsObject, extendsCharSequence, extendsString);
		new ResolvableTypeAssert(extendsAnon).isAssignableFrom(object, charSequence, string);

		// T <= ? super T
		new ResolvableTypeAssert(superCharSequence).isAssignableFrom(object, charSequence).isNotAssignableFrom(string);
		new ResolvableTypeAssert(charSequence).isNotAssignableFrom(superObject, superCharSequence, superString);
	}

	@Test
	public void isAssignableFromForComplexWildcards() throws Exception {
		ResolvableType complex1 = ResolvableType.forField(AssignmentBase.class.getField("complexWildcard1"));
		ResolvableType complex2 = ResolvableType.forField(AssignmentBase.class.getField("complexWildcard2"));
		ResolvableType complex3 = ResolvableType.forField(AssignmentBase.class.getField("complexWildcard3"));
		ResolvableType complex4 = ResolvableType.forField(AssignmentBase.class.getField("complexWildcard4"));

		new ResolvableTypeAssert(complex1).isAssignableFrom(complex2);
		new ResolvableTypeAssert(complex2).isNotAssignableFrom(complex1);
		new ResolvableTypeAssert(complex3).isAssignableFrom(complex4);
		new ResolvableTypeAssert(complex4).isNotAssignableFrom(complex3);
	}

	@Test
	public void hashCodeAndEquals() throws Exception {
		ResolvableType forClass = ResolvableType.forClass(List.class);
		ResolvableType forFieldDirect = ResolvableType.forField(Fields.class.getDeclaredField("stringList"));
		ResolvableType forFieldViaType = ResolvableType.forType(Fields.class.getDeclaredField("stringList").getGenericType(), (VariableResolver) null);
		ResolvableType forFieldWithImplementation = ResolvableType.forField(Fields.class.getDeclaredField("stringList"), TypedFields.class);

		assertThat(forClass).isEqualTo(forClass);
		assertThat(forClass.hashCode()).isEqualTo(forClass.hashCode());
		assertThat(forClass).isNotEqualTo(forFieldDirect);
		assertThat(forClass).isNotEqualTo(forFieldWithImplementation);

		assertThat(forFieldDirect).isEqualTo(forFieldDirect);
		assertThat(forFieldDirect).isNotEqualTo(forFieldViaType);
		assertThat(forFieldDirect).isNotEqualTo(forFieldWithImplementation);
	}

	@Test
	public void javaDocSample() throws Exception {
		ResolvableType t = ResolvableType.forField(getClass().getDeclaredField("myMap"));
		assertThat(t.toString()).isEqualTo("java.util.HashMap<java.lang.Integer, java.util.List<java.lang.String>>");
		assertThat(t.getType().getTypeName()).isEqualTo("java.util.HashMap<java.lang.Integer, java.util.List<java.lang.String>>");
		assertThat(t.getType().toString()).isEqualTo("java.util.HashMap<java.lang.Integer, java.util.List<java.lang.String>>");
		assertThat(t.getSuperType().toString()).isEqualTo("java.util.AbstractMap<java.lang.Integer, java.util.List<java.lang.String>>");
		assertThat(t.asMap().toString()).isEqualTo("java.util.Map<java.lang.Integer, java.util.List<java.lang.String>>");
		assertThat(t.getGeneric(0).resolve()).isEqualTo(Integer.class);
		assertThat(t.getGeneric(1).resolve()).isEqualTo(List.class);
		assertThat(t.getGeneric(1).toString()).isEqualTo("java.util.List<java.lang.String>");
		assertThat(t.resolveGeneric(1, 0)).isEqualTo(String.class);
	}

	@Test
	public void forClassWithGenerics() throws Exception {
		ResolvableType elementType = ResolvableType.forClassWithGenerics(Map.class, Integer.class, String.class);
		ResolvableType listType = ResolvableType.forClassWithGenerics(List.class, elementType);
		assertThat(listType.toString()).isEqualTo("java.util.List<java.util.Map<java.lang.Integer, java.lang.String>>");
		assertThat(listType.getType().getTypeName()).isEqualTo("java.util.List<java.util.Map<java.lang.Integer, java.lang.String>>");
		assertThat(listType.getType().toString()).isEqualTo("java.util.List<java.util.Map<java.lang.Integer, java.lang.String>>");
	}

	@Test
	public void classWithGenericsAs() throws Exception {
		ResolvableType type = ResolvableType.forClassWithGenerics(MultiValueMap.class, Integer.class, String.class);
		assertThat(type.asMap().toString()).isEqualTo("java.util.Map<java.lang.Integer, java.util.List<java.lang.String>>");
	}

	@Test
	public void forClassWithMismatchedGenerics() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				ResolvableType.forClassWithGenerics(Map.class, Integer.class))
			.withMessageContaining("Mismatched number of generics specified");
	}

	@Test
	public void forArrayComponent() throws Exception {
		ResolvableType elementType = ResolvableType.forField(Fields.class.getField("stringList"));
		ResolvableType type = ResolvableType.forArrayComponent(elementType);
		assertThat(type.toString()).isEqualTo("java.util.List<java.lang.String>[]");
		assertThat(type.resolve()).isEqualTo(List[].class);
	}

	@Test
	public void serialize() throws Exception {
		testSerialization(ResolvableType.forClass(List.class));
		testSerialization(ResolvableType.forField(Fields.class.getField("charSequenceList")));
		testSerialization(ResolvableType.forMethodParameter(Methods.class.getMethod("charSequenceParameter", List.class), 0));
		testSerialization(ResolvableType.forMethodReturnType(Methods.class.getMethod("charSequenceReturn")));
		testSerialization(ResolvableType.forConstructorParameter(Constructors.class.getConstructor(List.class), 0));
		testSerialization(ResolvableType.forField(Fields.class.getField("charSequenceList")).getGeneric());
		ResolvableType deserializedNone = testSerialization(ResolvableType.NONE);
		assertThat(deserializedNone).isSameAs(ResolvableType.NONE);
	}

	@Test
	public void canResolveVoid() throws Exception {
		ResolvableType type = ResolvableType.forClass(void.class);
		assertThat(type.resolve()).isEqualTo(void.class);
	}

	@Test
	public void narrow() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringList"));
		ResolvableType narrow = ResolvableType.forType(ArrayList.class, type);
		assertThat(narrow.getGeneric().resolve()).isEqualTo(String.class);
	}

	@Test
	public void hasUnresolvableGenerics() throws Exception {
		ResolvableType type = ResolvableType.forField(Fields.class.getField("stringList"));
		assertThat(type.hasUnresolvableGenerics()).isEqualTo(false);
	}

	@Test
	public void hasUnresolvableGenericsBasedOnOwnGenerics() throws Exception {
		ResolvableType type = ResolvableType.forClass(List.class);
		assertThat(type.hasUnresolvableGenerics()).isEqualTo(true);
	}

	@Test
	public void hasUnresolvableGenericsWhenSelfNotResolvable() throws Exception {
		ResolvableType type = ResolvableType.forClass(List.class).getGeneric();
		assertThat(type.hasUnresolvableGenerics()).isEqualTo(false);
	}

	@Test
	public void hasUnresolvableGenericsWhenImplementesRawInterface() throws Exception {
		ResolvableType type = ResolvableType.forClass(MySimpleInterfaceTypeWithImplementsRaw.class);
		for (ResolvableType generic : type.getGenerics()) {
			assertThat(generic.resolve()).isNotNull();
		}
		assertThat(type.hasUnresolvableGenerics()).isEqualTo(true);
	}

	@Test
	public void hasUnresolvableGenericsWhenExtends() throws Exception {
		ResolvableType type = ResolvableType.forClass(ExtendsMySimpleInterfaceTypeWithImplementsRaw.class);
		for (ResolvableType generic : type.getGenerics()) {
			assertThat(generic.resolve()).isNotNull();
		}
		assertThat(type.hasUnresolvableGenerics()).isEqualTo(true);
	}

	@Test
	public void testSpr11219() throws Exception {
		ResolvableType type = ResolvableType.forField(BaseProvider.class.getField("stuff"), BaseProvider.class);
		assertTrue(type.getNested(2).isAssignableFrom(ResolvableType.forClass(BaseImplementation.class)));
		assertEquals("java.util.Collection<org.springframework.core.ResolvableTypeTests$IBase<?>>", type.toString());
	}

	@Test
	public void testSpr12701() throws Exception {
		ResolvableType resolvableType = ResolvableType.forClassWithGenerics(Callable.class, String.class);
		Type type = resolvableType.getType();
		assertThat(type).isInstanceOf(ParameterizedType.class);
		assertThat(((ParameterizedType) type).getRawType()).isEqualTo(Callable.class);
		assertThat(((ParameterizedType) type).getActualTypeArguments().length).isEqualTo(1);
		assertThat(((ParameterizedType) type).getActualTypeArguments()[0]).isEqualTo(String.class);
	}

	@Test
	public void testSpr14648() throws Exception {
		ResolvableType collectionClass = ResolvableType.forRawClass(Collection.class);
		ResolvableType setClass = ResolvableType.forRawClass(Set.class);
		ResolvableType fromReturnType = ResolvableType.forMethodReturnType(Methods.class.getMethod("wildcardSet"));
		assertTrue(collectionClass.isAssignableFrom(fromReturnType));
		assertTrue(setClass.isAssignableFrom(fromReturnType));
	}

	@Test
	public void testSpr16456() throws Exception {
		ResolvableType genericType = ResolvableType.forField(
				UnresolvedWithGenerics.class.getDeclaredField("set")).asCollection();
		ResolvableType type = ResolvableType.forClassWithGenerics(ArrayList.class, genericType.getGeneric());
		assertThat(type.resolveGeneric()).isEqualTo(Integer.class);
	}


	private ResolvableType testSerialization(ResolvableType type) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(type);
		oos.close();
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
		ResolvableType read = (ResolvableType) ois.readObject();
		assertThat(read).isEqualTo(type);
		assertThat(read.getType()).isEqualTo(type.getType());
		assertThat(read.resolve()).isEqualTo(type.resolve());
		return read;
	}

	private ResolvableType forField(String field) throws NoSuchFieldException {
		return ResolvableType.forField(Fields.class.getField(field));
	}

	private ResolvableType forTypedField(String field) throws NoSuchFieldException {
		return ResolvableType.forField(Fields.class.getField(field), TypedFields.class);
	}

	@SuppressWarnings("unused")
	private HashMap<Integer, List<String>> myMap;


	@SuppressWarnings("serial")
	static class ExtendsList extends ArrayList<CharSequence> {
	}


	@SuppressWarnings("serial")
	static class ExtendsMap extends HashMap<String, Integer> {
	}


	static class Fields<T> {

		public List classType;

		public T typeVariableType;

		public List<T> parameterizedType;

		public List[] arrayClassType;

		public List<String>[] genericArrayType;

		public List<String>[][][] genericMultiArrayType;

		public List<? extends Number> wildcardType;

		public List<? super Number> wildcardSuperType = new ArrayList<Object>();

		public List<CharSequence> charSequenceList;

		public List<String> stringList;

		public List<List<String>> stringListList;

		public List<String[]> stringArrayList;

		public MultiValueMap<String, Integer> stringIntegerMultiValueMap;

		public VariableNameSwitch<Integer, String> stringIntegerMultiValueMapSwitched;

		public List<List> listOfListOfUnknown;

		@SuppressWarnings("unused")
		private List<String> privateField;

		@SuppressWarnings("unused")
		private List<String> otherPrivateField;

		public Map<Map<String, Integer>, Map<Byte, Long>> nested;

		public T[] variableTypeGenericArray;
	}


	static class TypedFields extends Fields<String> {
	}


	interface Methods<T> {

		List<CharSequence> charSequenceReturn();

		void charSequenceParameter(List<CharSequence> cs);

		<R extends CharSequence & Serializable> R boundedTypeVaraibleResult();

		void nested(Map<Map<String, Integer>, Map<Byte, Long>> p);

		void typedParameter(T p);

		T typedReturn();

		Set<?> wildcardSet();

		List<String> list1();

		List<String> list2();
	}


	static class AssignmentBase<O, C, S> {

		public O o;

		public C c;

		public S s;

		public List<O> listo;

		public List<C> listc;

		public List<S> lists;

		public List<?> listAnon;

		public List<? extends O> listxo;

		public List<? extends C> listxc;

		public List<? extends S> listxs;

		public List<? super O> listso;

		public List<? super C> listsc;

		public List<? super S> listss;

		public O[] oarray;

		public C[] carray;

		public S[] sarray;

		public Collection<C> collectionc;

		public Collection<? extends C> collectionxc;

		public Map<? super Integer, List<String>> complexWildcard1;

		public MultiValueMap<Number, String> complexWildcard2;

		public Collection<? extends Collection<? extends CharSequence>> complexWildcard3;

		public List<List<String>> complexWildcard4;
	}


	static class Assignment extends AssignmentBase<Object, CharSequence, String> {
	}


	interface TypedMethods extends Methods<String> {
	}


	static class Constructors<T> {

		public Constructors(List<CharSequence> p) {
		}

		public Constructors(Map<T, Long> p) {
		}
	}


	static class TypedConstructors extends Constructors<String> {

		public TypedConstructors(List<CharSequence> p) {
			super(p);
		}

		public TypedConstructors(Map<String, Long> p) {
			super(p);
		}
	}


	public interface MyInterfaceType<T> {
	}

	public class MyGenericInterfaceType<T> implements MyInterfaceType<T>, ResolvableTypeProvider {

		private final Class<T> type;

		public MyGenericInterfaceType(Class<T> type) {
			this.type = type;
		}

		@Override
		public ResolvableType getResolvableType() {
			if (this.type == null) {
				return null;
			}
			return ResolvableType.forClassWithGenerics(getClass(), this.type);
		}
	}


	public class MySimpleInterfaceType implements MyInterfaceType<String> {
	}

	public abstract class MySimpleInterfaceTypeWithImplementsRaw implements MyInterfaceType<String>, List {
	}

	public abstract class ExtendsMySimpleInterfaceTypeWithImplementsRaw extends MySimpleInterfaceTypeWithImplementsRaw {
	}


	public class MyCollectionInterfaceType implements MyInterfaceType<Collection<String>> {
	}


	public abstract class MySuperclassType<T> {
	}


	public class MySimpleSuperclassType extends MySuperclassType<String> {
	}


	public class MyCollectionSuperclassType extends MySuperclassType<Collection<String>> {
	}


	interface Wildcard<T extends Number> extends List<T> {
	}


	interface RawExtendsWildcard extends Wildcard {
	}


	interface VariableNameSwitch<V, K> extends MultiValueMap<K, V> {
	}


	interface ListOfGenericArray extends List<List<String>[]> {
	}


	static class EnclosedInParameterizedType<T> {

		static class InnerRaw {
		}

		class InnerTyped<Y> {

			public T field;
		}
	}


	static class TypedEnclosedInParameterizedType extends EnclosedInParameterizedType<Integer> {

		class TypedInnerTyped extends InnerTyped<Long> {
		}
	}


	public interface IProvider<P> {
	}

	public interface IBase<BT extends IBase<BT>> {
	}

	public abstract class AbstractBase<BT extends IBase<BT>> implements IBase<BT> {
	}

	public class BaseImplementation extends AbstractBase<BaseImplementation> {
	}

	public class BaseProvider<BT extends IBase<BT>> implements IProvider<IBase<BT>> {

		public Collection<IBase<BT>> stuff;
	}


	public abstract class UnresolvedWithGenerics {

		Set<Integer> set;
	}

	private static class ResolvableTypeAssert extends AbstractAssert<ResolvableTypeAssert, ResolvableType>{

		public ResolvableTypeAssert(ResolvableType actual) {
			super(actual, ResolvableTypeAssert.class);
		}

		public ResolvableTypeAssert isAssignableFrom(ResolvableType... types) {
			for (ResolvableType type : types) {
				if (!actual.isAssignableFrom(type)) {
					throw new AssertionError("Expecting " + decribe(actual) + " to be assignable from " + decribe(type));
				}
			}
			return this;
		}

		public ResolvableTypeAssert isNotAssignableFrom(ResolvableType... types) {
			for (ResolvableType type : types) {
				if (actual.isAssignableFrom(type)) {
					throw new AssertionError("Expecting " + decribe(actual) + " to not be assignable from " + decribe(type));
				}
			}
			return this;
		}

		private String decribe(ResolvableType type) {
			if (type == ResolvableType.NONE) {
				return "NONE";
			}
			if (type.getType().getClass().equals(Class.class)) {
				return type.toString();
			}
			return type.getType() + ":" + type;
		}

	}

}
