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

package org.springframework.aot.generate.instance;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.aot.test.generator.file.SourceFile;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultInstanceCodeGenerationService} that use the
 * {@link TestCompiler} to verify results.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see DefaultInstanceCodeGenerationServiceTests
 */
class DefaultInstanceCodeGenerationServiceCompilerTests {

	private void compile(DefaultInstanceCodeGenerationService generationService, Object value,
			BiConsumer<Object, Compiled> result) {
		compile(generationService, null, value, result);
	}

	private void compile(DefaultInstanceCodeGenerationService generationService,
			@Nullable GeneratedMethods generatedMethods, Object value, BiConsumer<Object, Compiled> result) {
		compile(generationService, generatedMethods, value, (value != null) ? ResolvableType.forInstance(value) : null,
				result);
	}

	private void compile(DefaultInstanceCodeGenerationService generationService,
			@Nullable GeneratedMethods generatedMethods, Object value, ResolvableType type,
			BiConsumer<Object, Compiled> result) {
		CodeBlock code = generationService.generateCode(value, type);
		JavaFile javaFile = createJavaFile(generationService, generatedMethods, code);
		System.out.println(javaFile);
		TestCompiler.forSystem().compile(SourceFile.of(javaFile::writeTo),
				compiled -> result.accept(compiled.getInstance(Supplier.class).get(), compiled));
	}

	private JavaFile createJavaFile(DefaultInstanceCodeGenerationService generationService,
			@Nullable GeneratedMethods generatedMethods, CodeBlock code) {
		TypeSpec.Builder builder = TypeSpec.classBuilder("InstanceSupplier");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(ParameterizedTypeName.get(Supplier.class, Object.class));
		builder.addMethod(MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC).returns(Object.class)
				.addStatement("return $L", code).build());
		if (generatedMethods != null) {
			generatedMethods.doWithMethodSpecs(builder::addMethod);
		}
		return JavaFile.builder("com.example", builder.build()).build();
	}

	/**
	 * Tests for {@link NullInstanceCodeGenerator}.
	 */
	@Nested
	class NullInstanceCodeGeneratorTests {

		@Test
		void generateWhenNull() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), null,
					(instance, compiled) -> assertThat(instance).isNull());
		}

	}

	/**
	 * Tests for {@link PrimitiveInstanceCodeGenerator}.
	 */
	@Nested
	class PrimitiveInstanceCodeGeneratorTests {

		@Test
		void generateWhenBoolean() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), true, (instance, compiled) -> {
				assertThat(instance).isEqualTo(Boolean.TRUE);
				assertThat(compiled.getSourceFile()).contains("true");
			});
		}

		@Test
		void generateWhenByte() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), (byte) 2, (instance, compiled) -> {
				assertThat(instance).isEqualTo((byte) 2);
				assertThat(compiled.getSourceFile()).contains("(byte) 2");
			});
		}

		@Test
		void generateWhenShort() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), (short) 3, (instance, compiled) -> {
				assertThat(instance).isEqualTo((short) 3);
				assertThat(compiled.getSourceFile()).contains("(short) 3");
			});
		}

		@Test
		void generateWhenInt() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), 4, (instance, compiled) -> {
				assertThat(instance).isEqualTo(4);
				assertThat(compiled.getSourceFile()).contains("return 4;");
			});
		}

		@Test
		void generateWhenLong() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), 5L, (instance, compiled) -> {
				assertThat(instance).isEqualTo(5L);
				assertThat(compiled.getSourceFile()).contains("5L");
			});
		}

		@Test
		void generateWhenFloat() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), 0.1F, (instance, compiled) -> {
				assertThat(instance).isEqualTo(0.1F);
				assertThat(compiled.getSourceFile()).contains("0.1F");
			});
		}

		@Test
		void generateWhenDouble() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), 0.2, (instance, compiled) -> {
				assertThat(instance).isEqualTo(0.2);
				assertThat(compiled.getSourceFile()).contains("(double) 0.2");
			});
		}


		@Test
		void generateWhenChar() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), 'a', (instance, compiled) -> {
				assertThat(instance).isEqualTo('a');
				assertThat(compiled.getSourceFile()).contains("'a'");
			});
		}

		@Test
		void generateWhenSimpleEscapedCharReturnsEscaped() {
			testEscaped('\b', "'\\b'");
			testEscaped('\t', "'\\t'");
			testEscaped('\n', "'\\n'");
			testEscaped('\f', "'\\f'");
			testEscaped('\r', "'\\r'");
			testEscaped('\"', "'\"'");
			testEscaped('\'', "'\\''");
			testEscaped('\\', "'\\\\'");
		}

		@Test
		void generatedWhenUnicodeEscapedCharReturnsEscaped() {
			testEscaped('\u007f', "'\\u007f'");
		}

		private void testEscaped(char value, String expectedSourceContent) {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), value, (instance, compiled) -> {
				assertThat(instance).isEqualTo(value);
				assertThat(compiled.getSourceFile()).contains(expectedSourceContent);
			});
		}

	}

	/**
	 * Tests for {@link StringInstanceCodeGenerator}.
	 */
	@Nested
	class StringInstanceCodeGeneratorTests {

		@Test
		void generateWhenString() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), "test\n", (instance, compiled) -> {
				assertThat(instance).isEqualTo("test\n");
				assertThat(compiled.getSourceFile()).contains("\n");
			});
		}

	}

	/**
	 * Test for {@link EnumInstanceCodeGenerator}.
	 */
	@Nested
	class EnumInstanceCodeGeneratorTests {

		@Test
		void generateWhenEnum() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), ChronoUnit.DAYS, (instance, compiled) -> {
				assertThat(instance).isEqualTo(ChronoUnit.DAYS);
				assertThat(compiled.getSourceFile()).contains("ChronoUnit.DAYS");
			});
		}

		@Test
		void generateWhenEnumWithClassBody() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), EnumWithClassBody.TWO,
					(instance, compiled) -> {
						assertThat(instance).isEqualTo(EnumWithClassBody.TWO);
						assertThat(compiled.getSourceFile()).contains("EnumWithClassBody.TWO");
					});
		}

	}

	/**
	 * Tests for {@link ClassInstanceCodeGenerator}.
	 */
	@Nested
	class ClassInstanceCodeGeneratorTests {

		@Test
		void generateWhenClass() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), InputStream.class,
					(instance, compiled) -> assertThat(instance).isEqualTo(InputStream.class));
		}

		@Test
		void generateWhenCglibClass() {
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), ExampleClass$$GeneratedBy.class,
					(instance, compiled) -> assertThat(instance).isEqualTo(ExampleClass.class));
		}

	}

	/**
	 * Tests for {@link ResolvableTypeInstanceCodeGenerator}.
	 */
	@Nested
	class ResolvableTypeInstanceCodeGeneratorTests {

		@Test
		void generateWhenSimpleResolvableType() {
			ResolvableType resolvableType = ResolvableType.forClass(String.class);
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), resolvableType,
					(instance, compiled) -> assertThat(instance).isEqualTo(resolvableType));
		}

		@Test
		void generateWhenNoneResolvableType() {
			ResolvableType resolvableType = ResolvableType.NONE;
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), resolvableType, (instance, compiled) -> {
				assertThat(instance).isEqualTo(resolvableType);
				assertThat(compiled.getSourceFile()).contains("ResolvableType.NONE");
			});
		}

		@Test
		void generateWhenGenericResolvableType() {
			ResolvableType resolvableType = ResolvableType.forClassWithGenerics(List.class, String.class);
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), resolvableType,
					(instance, compiled) -> assertThat(instance).isEqualTo(resolvableType));
		}

		@Test
		void generateWhenNestedGenericResolvableType() {
			ResolvableType stringList = ResolvableType.forClassWithGenerics(List.class, String.class);
			ResolvableType resolvableType = ResolvableType.forClassWithGenerics(Map.class,
					ResolvableType.forClass(Integer.class), stringList);
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), resolvableType,
					(instance, compiled) -> assertThat(instance).isEqualTo(resolvableType));
		}

	}

	/**
	 * Tests for {@link ArrayInstanceCodeGenerator}.
	 */
	@Nested
	class ArrayInstanceCodeGeneratorTests {

		@Test
		void generateWhenPrimitiveArray() {
			byte[] bytes = { 0, 1, 2 };
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), bytes, (instance, compiler) -> {
				assertThat(instance).isEqualTo(bytes);
				assertThat(compiler.getSourceFile()).contains("new byte[]");
			});
		}

		@Test
		void generateWhenWrapperArray() {
			Byte[] bytes = { 0, 1, 2 };
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), bytes, (instance, compiler) -> {
				assertThat(instance).isEqualTo(bytes);
				assertThat(compiler.getSourceFile()).contains("new Byte[]");
			});
		}

		@Test
		void generateWhenClassArray() {
			Class<?>[] classes = new Class<?>[] { InputStream.class, OutputStream.class };
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), classes, (instance, compiler) -> {
				assertThat(instance).isEqualTo(classes);
				assertThat(compiler.getSourceFile()).contains("new Class[]");
			});
		}

	}

	/**
	 * Tests for {@link ListInstanceCodeGenerator}.
	 */
	@Nested
	class ListInstanceCodeGeneratorTests {

		@Test
		void generateWhenStringList() {
			List<String> list = List.of("a", "b", "c");
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), list,
					(instance, compiler) -> assertThat(instance).isEqualTo(list));
		}

		@Test
		void generateWhenEmptyList() {
			List<String> list = List.of();
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), list, (instance, compiler) -> {
				assertThat(instance).isEqualTo(list);
				assertThat(compiler.getSourceFile()).contains("Collections.emptyList();");
			});
		}

	}

	/**
	 * Tests for {@link SetInstanceCodeGenerator}.
	 */
	@Nested
	class SetInstanceCodeGeneratorTests {

		@Test
		void generateWhenStringSet() {
			Set<String> set = Set.of("a", "b", "c");
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), set,
					(instance, compiler) -> assertThat(instance).isEqualTo(set));
		}

		@Test
		void generateWhenEmptySet() {
			Set<String> set = Set.of();
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), set, (instance, compiler) -> {
				assertThat(instance).isEqualTo(set);
				assertThat(compiler.getSourceFile()).contains("Collections.emptySet();");
			});
		}

		@Test
		void generateWhenLinkedHashSet() {
			Set<String> set = new LinkedHashSet<>(List.of("a", "b", "c"));
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), set, (instance, compiler) -> {
				assertThat(instance).isEqualTo(set).isInstanceOf(LinkedHashSet.class);
				assertThat(compiler.getSourceFile()).contains("new LinkedHashSet(List.of(");
			});
		}

	}

	/**
	 * Tests for {@link MapInstanceCodeGenerator}.
	 */
	@Nested
	class MapInstanceCodeGeneratorTests {

		@Test
		void generateWhenSmallMap() {
			Map<String, String> map = Map.of("k1", "v1", "k2", "v2");
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), map, (instance, compiler) -> {
				assertThat(instance).isEqualTo(map);
				assertThat(compiler.getSourceFile()).contains("Map.of(");
			});
		}

		@Test
		void generateWhenMapWithOverTenElements() {
			Map<String, String> map = new HashMap<>();
			for (int i = 1; i <= 11; i++) {
				map.put("k" + i, "v" + i);
			}
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), map, (instance, compiler) -> {
				assertThat(instance).isEqualTo(map);
				assertThat(compiler.getSourceFile()).contains("Map.ofEntries(");
			});
		}

		@Test
		void generateWhenLinkedHashMapAndCanGenerateMethod() {
			Map<String, String> map = new LinkedHashMap<>();
			map.put("a", "A");
			map.put("b", "B");
			map.put("c", "C");
			GeneratedMethods generatedMethods = new GeneratedMethods();
			DefaultInstanceCodeGenerationService generators = new DefaultInstanceCodeGenerationService(
					generatedMethods::add);
			compile(generators, generatedMethods, map, (instance, compiler) -> {
				assertThat(instance).isEqualTo(map).isInstanceOf(LinkedHashMap.class);
				assertThat(compiler.getSourceFile()).contains("getMap()");
			});
		}

		@Test
		void generateWhenLinkedHashMapAndCannotGenerateMethod() {
			Map<String, String> map = new LinkedHashMap<>();
			map.put("a", "A");
			map.put("b", "B");
			map.put("c", "C");
			compile(DefaultInstanceCodeGenerationService.getSharedInstance(), map, (instance, compiler) -> {
				assertThat(instance).isEqualTo(map).isInstanceOf(LinkedHashMap.class);
				assertThat(compiler.getSourceFile()).contains("Collectors.toMap(");
			});
		}

	}

}
