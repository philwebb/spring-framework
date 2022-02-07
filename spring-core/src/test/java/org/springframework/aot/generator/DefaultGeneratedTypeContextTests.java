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

package org.springframework.aot.generator;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultGeneratedTypeContext}.
 *
 * @author Stephane Nicoll
 */
class DefaultGeneratedTypeContextTests {

	@Test
	void getRuntimeHints() {
		DefaultGeneratedTypeContext context = createComAcmeContext();
		assertThat(context.getRuntimeHints()).isNotNull();
	}

	@Test
	void getGeneratedTypeMatchesGetMainGeneratedTypeForMainPackage() {
		DefaultGeneratedTypeContext context = createComAcmeContext();
		assertThat(context.getMainGeneratedType().getClassName()).isEqualTo(ClassName.get("com.acme", "Main"));
		assertThat(context.getGeneratedType("com.acme")).isSameAs(context.getMainGeneratedType());
	}

	@Test
	void getMainGeneratedTypeIsLazilyCreated() {
		DefaultGeneratedTypeContext context = createComAcmeContext();
		assertThat(context.hasGeneratedType("com.acme")).isFalse();
		context.getMainGeneratedType();
		assertThat(context.hasGeneratedType("com.acme")).isTrue();
	}

	@Test
	void getGeneratedTypeRegisterInstance() {
		DefaultGeneratedTypeContext context = createComAcmeContext();
		assertThat(context.hasGeneratedType("com.example")).isFalse();
		GeneratedType generatedType = context.getGeneratedType("com.example");
		assertThat(generatedType).isNotNull();
		assertThat(generatedType.getClassName().simpleName()).isEqualTo("Main");
		assertThat(context.hasGeneratedType("com.example")).isTrue();
	}

	@Test
	void getGeneratedTypeReuseInstance() {
		DefaultGeneratedTypeContext context = createComAcmeContext();
		GeneratedType generatedType = context.getGeneratedType("com.example");
		assertThat(generatedType.getClassName().packageName()).isEqualTo("com.example");
		assertThat(context.getGeneratedType("com.example")).isSameAs(generatedType);
	}

	@Test
	void toJavaFilesWithNoTypeIsEmpty() {
		DefaultGeneratedTypeContext writerContext = createComAcmeContext();
		assertThat(writerContext.toJavaFiles()).hasSize(0);
	}

	@Test
	void toJavaFilesWithDefaultTypeIsAddedLazily() {
		DefaultGeneratedTypeContext writerContext = createComAcmeContext();
		writerContext.getMainGeneratedType();
		assertThat(writerContext.toJavaFiles()).hasSize(1);
	}

	@Test
	void toJavaFilesWithDefaultTypeAndAdditionaTypes() {
		DefaultGeneratedTypeContext writerContext = createComAcmeContext();
		writerContext.getGeneratedType("com.example");
		writerContext.getGeneratedType("com.another");
		writerContext.getGeneratedType("com.another.another");
		assertThat(writerContext.toJavaFiles()).hasSize(3);
	}

	@Test
	void forkProvideMainGeneratedTypeUsingFactory() {
		DefaultGeneratedTypeContext context = createComAcmeContext();
		Function<String, GeneratedType> factory = mockGeneratedTypeFactory();
		GeneratedType mainGeneratedType = GeneratedType.of(ClassName.get("com.acme", "Main"));
		given(factory.apply("com.acme")).willReturn(mainGeneratedType);
		GeneratedTypeContext forkedContext = context.fork("test", factory);
		assertThat(forkedContext).isNotNull();
		assertThat(forkedContext.getMainGeneratedType()).isSameAs(mainGeneratedType);
		verify(factory).apply("com.acme");
	}

	@Test
	void forkProvideGeneratedTypeUsingFactory() {
		DefaultGeneratedTypeContext writerContext = createComAcmeContext();
		Function<String, GeneratedType> factory = mockGeneratedTypeFactory();
		GeneratedType generatedType = GeneratedType.of(ClassName.get("com.example.another", "Main"));
		given(factory.apply("com.example.another")).willReturn(generatedType);
		GeneratedTypeContext context = writerContext.fork("test", factory);
		assertThat(context).isNotNull();
		assertThat(context.getGeneratedType("com.example.another")).isSameAs(generatedType);
		verify(factory).apply("com.example.another");
	}

	@Test
	void forkWithIsThatIsAlreadyRegistered() {
		DefaultGeneratedTypeContext context = createComAcmeContext();
		context.fork("example", mockGeneratedTypeFactory());
		assertThatIllegalArgumentException().isThrownBy(() -> context.fork("example", mockGeneratedTypeFactory()))
				.withMessageContaining("'example'");
	}

	@Test
	void compositeKeepsTrackOfJavaFiles() {
		DefaultGeneratedTypeContext context = createComAcmeContext();
		GeneratedTypeContext test1 = context.fork("test1", publicGeneratedTypeFactory("Test1"));
		test1.getMainGeneratedType();
		test1.getGeneratedType("com.acme.test1");
		GeneratedTypeContext test2 = context.fork("test2", publicGeneratedTypeFactory("Test2"));
		test2.getGeneratedType("com.acme.test2");
		List<JavaFile> javaFiles = context.toJavaFiles();
		assertThat(javaFiles.stream().map(javaFile -> javaFile.packageName + "." + javaFile.typeSpec.name)).containsOnly(
				"com.acme.Test1", "com.acme.test1.Test1", "com.acme.test2.Test2");
	}

	@Test
	void compositeKeepsTrackOfNativeConfiguration() {
		DefaultGeneratedTypeContext writerContext = createComAcmeContext();
		writerContext.fork("test1", publicGeneratedTypeFactory("Test1")).getRuntimeHints()
				.reflection().registerType(String.class, hint -> {
				});
		writerContext.fork("test2", publicGeneratedTypeFactory("Test2")).getRuntimeHints()
				.reflection().registerType(Integer.class, hint -> {
				});
		List<TypeReference> reflectionTypes = writerContext.getRuntimeHints().reflection().typeHints()
				.map(TypeHint::getType).collect(Collectors.toList());
		assertThat(reflectionTypes).containsOnly(TypeReference.of(String.class), TypeReference.of(Integer.class));
	}

	@SuppressWarnings("unchecked")
	private Function<String, GeneratedType> mockGeneratedTypeFactory() {
		return mock(Function.class);
	}

	private Function<String, GeneratedType> publicGeneratedTypeFactory(String name) {
		return packageName -> GeneratedType.of(ClassName.get(packageName, name));
	}

	private DefaultGeneratedTypeContext createComAcmeContext() {
		return new DefaultGeneratedTypeContext("com.acme", packageName ->
				GeneratedType.of(ClassName.get(packageName, "Main"), type -> type.addModifiers(Modifier.PUBLIC)));
	}

}
