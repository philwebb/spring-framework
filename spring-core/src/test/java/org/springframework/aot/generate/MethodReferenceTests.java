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

	private static final String NAME = MethodReferenceTests.class.getName();

	private static final String EXPECTED_STATIC = "org.springframework.aot.generate.MethodReferenceTests::someMethod";

	private static final String EXPECTED_ANONYMOUS_INSTANCE = "<instance>::someMethod";

	private static final String EXPECTED_DECLARED_INSTANCE = "<org.springframework.aot.generate.MethodReferenceTests>::someMethod";

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
		assertThat(reference).hasToString(EXPECTED_ANONYMOUS_INSTANCE);
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
		assertThat(reference).hasToString(EXPECTED_ANONYMOUS_INSTANCE);
	}

	@Test
	void ofWithClassAndStringWhenDeclaringClassIsNullThrowsException() {
		Class<?> declaringClass = null;
		String methodName = "someMethod";
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.of(declaringClass, methodName))
				.withMessage("'declaringClass' must not be null");
	}

	@Test
	void ofWithClassAndStringWhenMethodNameIsNullThrowsException() {
		Class<?> declaringClass = MethodReferenceTests.class;
		String methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.of(declaringClass, methodName))
				.withMessage("'methodName' must not be empty");
	}

	@Test
	void ofWithClassAndStringCreatesMethodReference() {
		Class<?> declaringClass = MethodReferenceTests.class;
		String methodName = "someMethod";
		MethodReference reference = MethodReference.of(declaringClass, methodName);
		assertThat(reference).hasToString(EXPECTED_DECLARED_INSTANCE);
	}

	@Test
	void ofWithGeneratedClassNameAndGeneratedMethodNameWhenDeclaringClassIsNullThrowsException() {
		GeneratedClassName declaringClass = null;
		GeneratedMethodName methodName = new GeneratedMethodName("someMethod");
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.of(declaringClass, methodName))
				.withMessage("'declaringClass' must not be null");
	}

	@Test
	void ofWithGeneratedClassNameAndGeneratedMethodNameWhenMethodNameIsNullThrowsException() {
		GeneratedClassName declaringClass = new GeneratedClassName(NAME);
		GeneratedMethodName methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.of(declaringClass, methodName))
				.withMessage("'methodName' must not be null");
	}

	@Test
	void ofWithGeneratedClassNameAndGeneratedMethodNameCreateMethodReference() {
		GeneratedClassName declaringClass = new GeneratedClassName(NAME);
		GeneratedMethodName methodName = new GeneratedMethodName("someMethod");
		MethodReference reference = MethodReference.of(declaringClass, methodName);
		assertThat(reference).hasToString(EXPECTED_DECLARED_INSTANCE);
	}

	@Test
	void ofWithGeneratedClassNameAndStringMethodNameWhenDeclaringClassIsNullThrowsException() {
		GeneratedClassName declaringClass = null;
		String methodName = "someMethod";
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.of(declaringClass, methodName))
				.withMessage("'declaringClass' must not be null");
	}

	@Test
	void ofWithGeneratedClassNameAndStringMethodNameWhenMethodNameIsNullThrowsException() {
		GeneratedClassName declaringClass = new GeneratedClassName(NAME);
		String methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.of(declaringClass, methodName))
				.withMessage("'methodName' must not be empty");
	}

	@Test
	void ofWithGeneratedClassNameAndStringMethodNameCreateMethodReference() {
		GeneratedClassName declaringClass = new GeneratedClassName(NAME);
		String methodName = "someMethod";
		MethodReference reference = MethodReference.of(declaringClass, methodName);
		assertThat(reference).hasToString(EXPECTED_DECLARED_INSTANCE);
	}

	@Test
	void ofWithClassNameAndStringWhenDeclaringClassIsNullThrowsException() {
		ClassName declaringClass = null;
		String methodName = "someMethod";
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.of(declaringClass, methodName))
				.withMessage("'declaringClass' must not be null");
	}

	@Test
	void ofWithClassNameAndStringWhenMethodNameIsNullThrowsException() {
		ClassName declaringClass = ClassName.get(MethodReferenceTests.class);
		String methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.of(declaringClass, methodName))
				.withMessage("'methodName' must not be empty");
	}

	@Test
	void ofWithClassNameAndStringCreateMethodReference() {
		ClassName declaringClass = ClassName.get(MethodReferenceTests.class);
		String methodName = "someMethod";
		MethodReference reference = MethodReference.of(declaringClass, methodName);
		assertThat(reference).hasToString(EXPECTED_DECLARED_INSTANCE);
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
		Class<?> declaringClass = MethodReferenceTests.class;
		String methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.ofStatic(declaringClass, methodName))
				.withMessage("'methodName' must not be empty");
	}

	@Test
	void ofStaticWithClassAndStringCreatesMethodReference() {
		Class<?> declaringClass = MethodReferenceTests.class;
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
		GeneratedClassName declaringClass = new GeneratedClassName(NAME);
		GeneratedMethodName methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.ofStatic(declaringClass, methodName))
				.withMessage("'methodName' must not be null");
	}

	@Test
	void ofStaticWithGeneratedClassNameAndGeneratedMethodNameCreatesMethodReference() {
		GeneratedClassName declaringClass = new GeneratedClassName(NAME);
		GeneratedMethodName methodName = new GeneratedMethodName("someMethod");
		MethodReference reference = MethodReference.ofStatic(declaringClass, methodName);
		assertThat(reference).hasToString(EXPECTED_STATIC);
	}

	@Test
	void ofStaticWithGeneratedClassNameAndStringMethodNameWhenDeclaringClassIsNullThrowsException() {
		GeneratedClassName declaringClass = null;
		String methodName = "someMethod";
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.ofStatic(declaringClass, methodName))
				.withMessage("'declaringClass' must not be null");
	}

	@Test
	void ofStaticWithGeneratedClassNameAndStringMethodNameWhenMethodNameIsNullThrowsException() {
		GeneratedClassName declaringClass = new GeneratedClassName(NAME);
		String methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.ofStatic(declaringClass, methodName))
				.withMessage("'methodName' must not be empty");
	}

	@Test
	void ofStaticWithGeneratedClassNameAndStringMethodNameCreatesMethodReference() {
		GeneratedClassName declaringClass = new GeneratedClassName(NAME);
		String methodName = "someMethod";
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
		ClassName declaringClass = ClassName.get(MethodReferenceTests.class);
		String methodName = null;
		assertThatIllegalArgumentException().isThrownBy(() -> MethodReference.ofStatic(declaringClass, methodName))
				.withMessage("'methodName' must not be empty");
	}

	@Test
	void ofStaticWithClassNameAndGeneratedMethodNameCreatesMethodReference() {
		ClassName declaringClass = ClassName.get(MethodReferenceTests.class);
		String methodName = "someMethod";
		MethodReference reference = MethodReference.ofStatic(declaringClass, methodName);
		assertThat(reference).hasToString(EXPECTED_STATIC);
	}

	@Test
	void toCodeBlockWhenInstanceMethodReferenceAndInstanceVariableIsNull() {
		MethodReference reference = MethodReference.of("someMethod");
		assertThat(reference.toCodeBlock(null)).hasToString("this::someMethod");
	}

	@Test
	void toCodeBlockWhenInstanceMethodReferenceAndInstanceVariableIsNotNull() {
		MethodReference reference = MethodReference.of("someMethod");
		assertThat(reference.toCodeBlock("myInstance")).hasToString("myInstance::someMethod");
	}

	@Test
	void toCodeBlockWhenStaticMethodReferenceAndInstanceVariableIsNull() {
		MethodReference reference = MethodReference.ofStatic(MethodReferenceTests.class, "someMethod");
		assertThat(reference.toCodeBlock(null)).hasToString(EXPECTED_STATIC);
	}

	@Test
	void toCodeBlockWhenStaticMethodReferenceAndInstanceVariableIsNotNullThrowsException() {
		MethodReference reference = MethodReference.ofStatic(MethodReferenceTests.class, "someMethod");
		assertThatIllegalArgumentException().isThrownBy(() -> reference.toCodeBlock("myInstance"))
				.withMessage("'instanceVariable' must be null for static method references");
	}

	@Test
	void toInvokeCodeBlockWhenInstanceMethodReferenceAndInstanceVariableIsNull() {
		MethodReference reference = MethodReference.of("someMethod");
		assertThat(reference.toInvokeCodeBlock()).hasToString("someMethod()");
	}

	@Test
	void toInvokeCodeBlockWhenInstanceMethodReferenceAndInstanceVariableIsNullAndHasDecalredClass() {
		MethodReference reference = MethodReference.of(MethodReferenceTests.class, "someMethod");
		assertThat(reference.toInvokeCodeBlock())
				.hasToString("new org.springframework.aot.generate.MethodReferenceTests().someMethod()");
	}

	@Test
	void toInvokeCodeBlockWhenInstanceMethodReferenceAndInstanceVariableIsNotNull() {
		MethodReference reference = MethodReference.of("someMethod");
		assertThat(reference.toInvokeCodeBlock("myInstance")).hasToString("myInstance.someMethod()");
	}

	@Test
	void toInvokeCodeBlockWhenStaticMethodReferenceAndInstanceVariableIsNull() {
		MethodReference reference = MethodReference.ofStatic(MethodReferenceTests.class, "someMethod");
		assertThat(reference.toInvokeCodeBlock())
				.hasToString("org.springframework.aot.generate.MethodReferenceTests.someMethod()");
	}

	@Test
	void toInvokeCodeBlockWhenStaticMethodReferenceAndInstanceVariableIsNotNullThrowsException() {
		MethodReference reference = MethodReference.ofStatic(MethodReferenceTests.class, "someMethod");
		assertThatIllegalArgumentException().isThrownBy(() -> reference.toInvokeCodeBlock("myInstance"))
				.withMessage("'instanceVariable' must be null for static method references");
	}

}
