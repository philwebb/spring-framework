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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.IllegalSelectorException;
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

import org.springframework.aot.generate.InstanceCodeGenerators.ArrayInstanceCodeGenerator;
import org.springframework.aot.generate.InstanceCodeGenerators.CharacterInstanceCodeGenerator;
import org.springframework.aot.generate.InstanceCodeGenerators.ClassInstanceCodeGenerator;
import org.springframework.aot.generate.InstanceCodeGenerators.EnumInstanceCodeGenerator;
import org.springframework.aot.generate.InstanceCodeGenerators.ListInstanceCodeGenerator;
import org.springframework.aot.generate.InstanceCodeGenerators.MapInstanceCodeGenerator;
import org.springframework.aot.generate.InstanceCodeGenerators.NullInstanceCodeGenerator;
import org.springframework.aot.generate.InstanceCodeGenerators.PrimitiveInstanceCodeGenerator;
import org.springframework.aot.generate.InstanceCodeGenerators.ResolvableTypeInstanceCodeGenerator;
import org.springframework.aot.generate.InstanceCodeGenerators.SetInstanceCodeGenerator;
import org.springframework.aot.generate.InstanceCodeGenerators.StringInstanceCodeGenerator;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.aot.test.generator.file.SourceFile;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InstanceCodeGenerators}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
class InstanceCodeGeneratorsCompilerTests {

	// ResolvableTypeGeneratorTests
	// BeanParameterGeneratorTests

	private void compile(InstanceCodeGenerators generators, Object value,
			BiConsumer<Object, Compiled> result) {
		compile(generators, value,
				(value != null) ? ResolvableType.forInstance(value) : null, result);
	}

	private void compile(InstanceCodeGenerators generators, Object value,
			ResolvableType type, BiConsumer<Object, Compiled> result) {
		CodeBlock code = generators.generateInstantiationCode(value, type);
		JavaFile javaFile = createJavaFile(generators, code);
		System.out.println(javaFile);
		TestCompiler.forSystem().compile(SourceFile.of(javaFile::writeTo),
				compiled -> result.accept(compiled.getInstance(Supplier.class).get(),
						compiled));
	}

	private JavaFile createJavaFile(InstanceCodeGenerators generators, CodeBlock code) {
		TypeSpec.Builder builder = TypeSpec.classBuilder("InstanceSupplier");
		builder.addModifiers(Modifier.PUBLIC);
		builder.addSuperinterface(
				ParameterizedTypeName.get(Supplier.class, Object.class));
		builder.addMethod(
				MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC).returns(
						Object.class).addStatement("return $L", code).build());
		GeneratedMethods generatedMethods = getGeneratedMethods(generators);
		if (generatedMethods != null) {
			generatedMethods.doWithMethodSpecs(builder::addMethod);
		}
		return JavaFile.builder("com.example", builder.build()).build();
	}

	private GeneratedMethods getGeneratedMethods(InstanceCodeGenerators generators) {
		try {
			return generators.getGeneratedMethods();
		}
		catch (IllegalStateException ex) {
			return null;
		}
	}

	/**
	 * Tests for {@link NullInstanceCodeGenerator}.
	 */
	@Nested
	class NullInstanceCodeGeneratorTests {

		@Test
		void generateWhenNull() {
			compile(InstanceCodeGenerators.simple(), null,
					(instance, compiled) -> assertThat(instance).isNull());
		}

	}

	/**
	 * Tests for {@link CharacterInstanceCodeGenerator}.
	 */
	@Nested
	class CharacterInstanceCodeGeneratorTests {

		@Test
		void generateReturnsCharacterInstance() {
			compile(InstanceCodeGenerators.simple(), 'a', (instance, compiled) -> {
				assertThat(instance).isEqualTo('a');
				assertThat(compiled.getSourceFile()).contains("'a'");
			});
		}

		@Test
		void generateWhenSimpleEscapedReturnsEscaped() {
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
		void generatedWhenUnicodeEscapedReturnsEscaped() {
			testEscaped('\u007f', "'\\u007f'");
		}

		private void testEscaped(char value, String expectedSourceContent) {
			compile(InstanceCodeGenerators.simple(), value, (instance, compiled) -> {
				assertThat(instance).isEqualTo(value);
				assertThat(compiled.getSourceFile()).contains(expectedSourceContent);
			});
		}

	}

	/**
	 * Tests for {@link PrimitiveInstanceCodeGenerator}.
	 */
	@Nested
	class PrimitiveInstanceCodeGeneratorTests {

		@Test
		void generateWhenBoolean() {
			compile(InstanceCodeGenerators.simple(), true, (instance, compiled) -> {
				assertThat(instance).isEqualTo(Boolean.TRUE);
				assertThat(compiled.getSourceFile()).contains("true");
			});
		}

		@Test
		void generateWhenByte() {
			compile(InstanceCodeGenerators.simple(), (byte) 2, (instance, compiled) -> {
				assertThat(instance).isEqualTo((byte) 2);
				assertThat(compiled.getSourceFile()).contains("(byte) 2");
			});
		}

		@Test
		void generateWhenShort() {
			compile(InstanceCodeGenerators.simple(), (short) 3, (instance, compiled) -> {
				assertThat(instance).isEqualTo((short) 3);
				assertThat(compiled.getSourceFile()).contains("(short) 3");
			});
		}

		@Test
		void generateWhenInt() {
			compile(InstanceCodeGenerators.simple(), 4, (instance, compiled) -> {
				assertThat(instance).isEqualTo(4);
				assertThat(compiled.getSourceFile()).contains("return 4;");
			});
		}

		@Test
		void generateWhenLong() {
			compile(InstanceCodeGenerators.simple(), 5L, (instance, compiled) -> {
				assertThat(instance).isEqualTo(5L);
				assertThat(compiled.getSourceFile()).contains("5L");
			});
		}

		@Test
		void generateWhenFloat() {
			compile(InstanceCodeGenerators.simple(), 0.1F, (instance, compiled) -> {
				assertThat(instance).isEqualTo(0.1F);
				assertThat(compiled.getSourceFile()).contains("0.1F");
			});
		}

		@Test
		void generateWhenDouble() {
			compile(InstanceCodeGenerators.simple(), 0.2, (instance, compiled) -> {
				assertThat(instance).isEqualTo(0.2);
				assertThat(compiled.getSourceFile()).contains("(double) 0.2");
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
			compile(InstanceCodeGenerators.simple(), "test\n", (instance, compiled) -> {
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
			compile(InstanceCodeGenerators.simple(), ChronoUnit.DAYS,
					(instance, compiled) -> {
						assertThat(instance).isEqualTo(ChronoUnit.DAYS);
						assertThat(compiled.getSourceFile()).contains("ChronoUnit.DAYS");
					});
		}

		@Test
		void generateWhenEnumWithClassBody() {
			compile(InstanceCodeGenerators.simple(), EnumWithClassBody.TWO,
					(instance, compiled) -> {
						assertThat(instance).isEqualTo(EnumWithClassBody.TWO);
						assertThat(compiled.getSourceFile()).contains(
								"EnumWithClassBody.TWO");
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
			compile(InstanceCodeGenerators.simple(), InputStream.class,
					(instance, compiled) -> {
						assertThat(instance).isEqualTo(InputStream.class);
					});
		}

		@Test
		void generateWhenCglibClass() {
			compile(InstanceCodeGenerators.simple(), ExampleClass$$GeneratedBy.class,
					(instance, compiled) -> {
						assertThat(instance).isEqualTo(ExampleClass.class);
					});
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
			compile(InstanceCodeGenerators.simple(), resolvableType, (instance,
					compiled) -> assertThat(instance).isEqualTo(resolvableType));
		}

		@Test
		void generateWhenNoneResolvableType() {
			ResolvableType resolvableType = ResolvableType.NONE;
			compile(InstanceCodeGenerators.simple(), resolvableType,
					(instance, compiled) -> {
						assertThat(instance).isEqualTo(resolvableType);
						assertThat(compiled.getSourceFile()).contains(
								"ResolvableType.NONE");
					});
		}

		@Test
		void generateWhenGenericResolvableType() {
			ResolvableType resolvableType = ResolvableType.forClassWithGenerics(
					List.class, String.class);
			compile(InstanceCodeGenerators.simple(), resolvableType, (instance,
					compiled) -> assertThat(instance).isEqualTo(resolvableType));
		}

		@Test
		void generateWhenNestedGenericResolvableType() {
			ResolvableType stringList = ResolvableType.forClassWithGenerics(List.class,
					String.class);
			ResolvableType resolvableType = ResolvableType.forClassWithGenerics(Map.class,
					ResolvableType.forClass(Integer.class), stringList);
			compile(InstanceCodeGenerators.simple(), resolvableType, (instance,
					compiled) -> assertThat(instance).isEqualTo(resolvableType));
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
			compile(InstanceCodeGenerators.simple(), bytes, (instance, compiler) -> {
				assertThat(instance).isEqualTo(bytes);
				assertThat(compiler.getSourceFile()).contains("new byte[]");
			});
		}

		@Test
		void generateWhenWrapperArray() {
			Byte[] bytes = { 0, 1, 2 };
			compile(InstanceCodeGenerators.simple(), bytes, (instance, compiler) -> {
				assertThat(instance).isEqualTo(bytes);
				assertThat(compiler.getSourceFile()).contains("new Byte[]");
			});
		}

		@Test
		void generateWhenClassArray() {
			Class<?>[] classes = new Class<?>[] { InputStream.class, OutputStream.class };
			compile(InstanceCodeGenerators.simple(), classes, (instance, compiler) -> {
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
			compile(InstanceCodeGenerators.simple(), list, (instance, compiler) -> {
				assertThat(instance).isEqualTo(list);
			});
		}

		@Test
		void generateWhenEmptyList() {
			List<String> list = List.of();
			compile(InstanceCodeGenerators.simple(), list, (instance, compiler) -> {
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
			compile(InstanceCodeGenerators.simple(), set, (instance, compiler) -> {
				assertThat(instance).isEqualTo(set);
			});
		}

		@Test
		void generateWhenEmptySet() {
			Set<String> set = Set.of();
			compile(InstanceCodeGenerators.simple(), set, (instance, compiler) -> {
				assertThat(instance).isEqualTo(set);
				assertThat(compiler.getSourceFile()).contains("Collections.emptySet();");
			});
		}

		@Test
		void generateWhenLinkedHashSet() {
			Set<String> set = new LinkedHashSet<>(List.of("a", "b", "c"));
			compile(InstanceCodeGenerators.simple(), set, (instance, compiler) -> {
				assertThat(instance).isEqualTo(set).isInstanceOf(LinkedHashSet.class);
				assertThat(compiler.getSourceFile()).contains(
						"new LinkedHashSet(List.of(");
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
			compile(InstanceCodeGenerators.simple(), map, (instance, compiler) -> {
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
			compile(InstanceCodeGenerators.simple(), map, (instance, compiler) -> {
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
			InstanceCodeGenerators generators = new InstanceCodeGenerators(
					new GeneratedMethods());
			compile(generators, map, (instance, compiler) -> {
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
			compile(InstanceCodeGenerators.simple(), map, (instance, compiler) -> {
				assertThat(instance).isEqualTo(map).isInstanceOf(LinkedHashMap.class);
				assertThat(compiler.getSourceFile()).contains("Collectors.toMap(");
			});
		}

	}

}
