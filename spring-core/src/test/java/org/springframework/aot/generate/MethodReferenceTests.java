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

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MethodReference}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class MethodReferenceTests {

	private static final String EXPECTED_STATIC = "org.springframework.aot.generate.MethodReferenceTests.MyClass::someMethod";

	private static final String EXPECTED_INSTANCE = "<instance>::someMethod";

	@Test
	void ofWithStringWhenMethodNameIsNullThrowsException() {
		String methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.of(methodName))
				.withMessage("'methodName' must not be empty");
	}

	@Test
	void ofWithStringCreatesMethodReference() {
		String methodName = "someMethod";
		MethodReference reference = MethodReference.of(methodName);
		assertThat(reference).hasToString(EXPECTED_INSTANCE);
	}

	@Test
	void ofWithGeneratedMethodNameWhenMethodNameIsNullThrowsException() {
		GeneratedMethodName methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.of(methodName))
				.withMessage("'methodName' must not be null");
	}

	@Test
	void ofWithGeneratedMethodNameCreatesMethodReference() {
		GeneratedMethodName methodName = new GeneratedMethodName("someMethod");
		MethodReference reference = MethodReference.of(methodName);
		assertThat(reference).hasToString(EXPECTED_INSTANCE);
	}

	@Test
	void ofStaticWithClassAndStringWhenDeclaringClassIsNullThrowsException() {
		Class<?> declaringClass = null;
		String methodName = "someMethod";
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.ofStatic(declaringClass, methodName))
				.withMessage("'declaringClass' must not be null");
	}

	@Test
	void ofStaticWithClassAndStringWhenMethodNameIsEmptyThrowsException() {
		Class<?> declaringClass = MyClass.class;
		String methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.ofStatic(declaringClass, methodName))
				.withMessage("'methodName' must not be empty");
	}

	@Test
	void ofStaticWithClassAndStringCreatesMethodReference() {
		Class<?> declaringClass = MyClass.class;
		String methodName = "someMethod";
		MethodReference reference = MethodReference.ofStatic(declaringClass, methodName);
		assertThat(reference).hasToString(EXPECTED_STATIC);
	}

	@Test
	void ofStaticWithGeneratedClassNameAndStringWhenClassNameIsNullThrowsException() {
		GeneratedClassName declaringClass = null;
		String methodName = "someMethod";
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.ofStatic(declaringClass, methodName))
				.withMessage("'declaringClass' must not be null");
	}

	@Test
	void ofStaticWithGeneratedClassNameAndStringMethodNameIsEmptyThrowsException() {
		GeneratedClassName declaringClass = new GeneratedClassName(MyClass.class.getName(), null);
		String methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.ofStatic(declaringClass, methodName))
				.withMessage("'methodName' must not be empty");
	}

	@Test
	void ofStaticWithGeneratedClassNameAndStringCreatesMethodReference() {
		GeneratedClassName declaringClass = new GeneratedClassName(MyClass.class.getName(), null);
		String methodName = "someMethod";
		MethodReference reference = MethodReference.ofStatic(declaringClass, methodName);
		assertThat(reference).hasToString(EXPECTED_STATIC);
	}

	@Test
	void ofStaticWithGeneratedClassNameAndGeneratedMethodNameWhenDeclaringClassIsNullThrowsException() {
		GeneratedClassName declaringClass = null;
		GeneratedMethodName methodName = new GeneratedMethodName("someMethod");
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.ofStatic(declaringClass, methodName))
				.withMessage("'declaringClass' must not be null");
	}

	@Test
	void ofStaticWithGeneratedClassNameAndGeneratedMethodNameWhenMethodNameIsNullThrowsException() {
		GeneratedClassName declaringClass = new GeneratedClassName(MyClass.class.getName(), null);
		GeneratedMethodName methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.ofStatic(declaringClass, methodName))
				.withMessage("'methodName' must not be null");
	}

	@Test
	void ofStaticWithGeneratedClassNameAndGeneratedMethodNameCreatesMethodReference() {
		GeneratedClassName declaringClass = new GeneratedClassName(MyClass.class.getName(), null);
		GeneratedMethodName methodName = new GeneratedMethodName("someMethod");
		MethodReference reference = MethodReference.ofStatic(declaringClass, methodName);
		assertThat(reference).hasToString(EXPECTED_STATIC);
	}

	@Test
	void ofStaticWithClassNameAndGeneratedMethodNameWhenDeclaringClassIsNullThrowsException() {
		ClassName declaringClass = null;
		String methodName = "someMethod";
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.ofStatic(declaringClass, methodName))
				.withMessage("'declaringClass' must not be null");
	}

	@Test
	void ofStaticWithClassNameAndGeneratedMethodNameWhenMethodNameIsEmptyThrowsException() {
		ClassName declaringClass = ClassName.get(MyClass.class);
		String methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.ofStatic(declaringClass, methodName))
				.withMessage("'methodName' must not be empty");
	}

	@Test
	void ofStaticWithClassNameAndGeneratedMethodNameCreatesMethodReference() {
		ClassName declaringClass = ClassName.get(MyClass.class);
		String methodName = "someMethod";
		MethodReference reference = MethodReference.ofStatic(declaringClass, methodName);
		assertThat(reference).hasToString(EXPECTED_STATIC);
	}

	@Test
	void toCodeBlockWhenInstanceMethodReferenceAndInstanceVariableIsNull() {
		MethodReference reference = MethodReference.ofStatic(MyClass.class, "someMethod");
		assertThat(reference.toCodeBlock(null)).hasToString(EXPECTED_STATIC);
	}

	@Test
	void toCodeBlockWhenInstanceMethodReferenceAndInstanceVariableIsNotNullThrowsException() {
		MethodReference reference = MethodReference.ofStatic(MyClass.class, "someMethod");
		assertThatIllegalArgumentException().isThrownBy(() -> reference.toCodeBlock("myInstance"))
				.withMessage("'instanceVariable' must be null for static method references");
	}

	@Test
	void toCodeBlockWhenStaticMethodReferenceAndInstanceVariableIsNull() {
		MethodReference reference = MethodReference.of("someMethod");
		assertThat(reference.toCodeBlock(null)).hasToString("this::someMethod");
	}

	@Test
	void toCodeBlockWhenStaticMethodReferenceAndInstanceVariableIsNotNull() {
		MethodReference reference = MethodReference.of("someMethod");
		assertThat(reference.toCodeBlock("myInstance")).hasToString("myInstance::someMethod");
	}

	static interface MyClass {

		void someMethod();

	}

}
