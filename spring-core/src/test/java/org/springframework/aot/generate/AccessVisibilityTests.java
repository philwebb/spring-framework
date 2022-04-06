/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.generate;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.core.testfixture.aot.generator.visibility.ProtectedGenericParameter;
import org.springframework.core.testfixture.aot.generator.visibility.ProtectedParameter;
import org.springframework.core.testfixture.aot.generator.visibility.PublicFactoryBean;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AccessVisibility}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class AccessVisibilityTests {

	@Test
	void forMemberWhenPublicConstructor() throws NoSuchMethodException {
		Member member = PublicClass.class.getConstructor();
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PUBLIC);
	}

	@Test
	void forMemberWhenPackagePrivateConstructor() {
		Member member = ProtectedAccessor.class.getDeclaredConstructors()[0];
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forMemberWhenPackagePrivateClassWithPublicConstructor() {
		Member member = PackagePrivateClass.class.getDeclaredConstructors()[0];
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forMemberWhenPackagePrivateClassWithPublicMethod() {
		Member member = method(PackagePrivateClass.class, "stringBean");
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateConstructorParameter() {
		Member member = ProtectedParameter.class.getConstructors()[0];
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateGenericOnConstructorParameter() {
		Member member = ProtectedGenericParameter.class.getConstructors()[0];
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateMethod() {
		Member member = method(PublicClass.class, "getProtectedMethod");
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateMethodReturnType() {
		Member member = method(ProtectedAccessor.class, "methodWithProtectedReturnType");
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateMethodParameter() {
		Member member = method(ProtectedAccessor.class, "methodWithProtectedParameter", PackagePrivateClass.class);
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateField() {
		Field member = field(PublicClass.class, "protectedField");
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forMemberWhenPublicClassWithPublicFieldAndPackagePrivateFieldType() {
		Member member = field(PublicClass.class, "protectedClassField");
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forMemberWhenPublicClassWithPublicMethodAndPackagePrivateGenericOnReturnType() {
		Member member = method(PublicFactoryBean.class, "protectedTypeFactoryBean");
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forMemberWhenPublicClassWithPackagePrivateArrayComponent() {
		Member member = field(PublicClass.class, "packagePrivateClasses");
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forResolvableTypeWhenPackagePrivateGeneric() {
		ResolvableType resolvableType = PublicFactoryBean.resolveToProtectedGenericParameter();
		assertThat(AccessVisibility.forResolvableType(resolvableType)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forResolvableTypeWhenRecursiveType() {
		ResolvableType resolvableType = ResolvableType.forClassWithGenerics(SelfReference.class, SelfReference.class);
		assertThat(AccessVisibility.forResolvableType(resolvableType)).isEqualTo(AccessVisibility.PACKAGE);
	}

	@Test
	void forMemberWhenPublicClassWithPrivateField() {
		Member member = field(PublicClass.class, "privateField");
		assertThat(AccessVisibility.forMember(member)).isEqualTo(AccessVisibility.PRIVATE);
	}

	private static Method method(Class<?> type, String name, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(type, name, parameterTypes);
		assertThat(method).isNotNull();
		return method;
	}

	private static Field field(Class<?> type, String name) {
		Field field = ReflectionUtils.findField(type, name);
		assertThat(field).isNotNull();
		return field;
	}

	static class SelfReference<T extends SelfReference<T>> {

		@SuppressWarnings("unchecked")
		T getThis() {
			return (T) this;
		}

	}
}
