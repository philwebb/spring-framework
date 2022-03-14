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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.support.MultiCodeBlock;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class InstanceCodeGenerators {

	private static final InstanceCodeGenerators SIMPLE = new InstanceCodeGenerators(null,
			Kind.SIMPLE);

	@Nullable
	private final GeneratedMethods generatedMethods;

	private final Kind kind;

	private final List<InstanceCodeGenerator> generators = new ArrayList<>();

	public InstanceCodeGenerators(GeneratedMethods generatedMethods, boolean addDefault) {
		this(generatedMethods, (addDefault) ? Kind.DEFAULT : Kind.EMPTY);
	}

	private InstanceCodeGenerators(@Nullable GeneratedMethods generatedMethods,
			Kind kind) {
		this.generatedMethods = generatedMethods;
		this.kind = kind;
		Assert.isTrue(generatedMethods != null || kind == Kind.SIMPLE,
				"'generatedMethods' must not be null");
		this.generators.add(NullInstanceCodeGenerator.INSTANCE);
		if (kind == Kind.SIMPLE || kind == Kind.DEFAULT) {
			this.generators.add(CharacterInstanceCodeGenerator.INSTANCE);
			this.generators.add(PrimitiveInstanceCodeGenerator.INSTANCE);
			this.generators.add(StringInstanceCodeGenerator.INSTANCE);
			this.generators.add(EnumInstanceCodeGenerator.INSTANCE);
			this.generators.add(ClassInstanceCodeGenerator.INSTANCE);
			this.generators.add(ResolvableTypeInstanceCodeGenerator.INSTANCE);
			this.generators.add(new ArrayInstanceCodeGenerator());
		}
		if (kind == Kind.DEFAULT) {
			this.generators.add(new ListInstanceCodeGenerator());
			this.generators.add(new SetInstanceCodeGenerator());
			this.generators.add(new MapInstanceCodeGenerator());
		}
	}

	public static InstanceCodeGenerators simple() {
		return SIMPLE;
	}

	public void add(InstanceCodeGenerator generator) {
		Assert.state(kind != Kind.SIMPLE,
				"'InstanceCodeGenerators.simple()' cannot be modified");
		Assert.notNull(generator, "'generator' must not be null");
		this.generators.add(generator);
	}

	public GeneratedMethods getGeneratedMethods() {
		Assert.state(this.generatedMethods != null,
				"No GeneratedMethods instance available");
		return this.generatedMethods;
	}

	public CodeBlock generateInstantiationCode(@Nullable Object value) {
		return generateInstantiationCode(value,
				(value != null) ? ResolvableType.forInstance(value)
						: ResolvableType.NONE);
	}

	public CodeBlock generateInstantiationCode(@Nullable Object value,
			ResolvableType type) {
		for (InstanceCodeGenerator generator : this.generators) {
			CodeBlock code = generator.generateCode(value, type);
			if (code != null) {
				return code;
			}
		}
		throw new IllegalArgumentException(
				"'type' " + type + " must be a supported type");
	}

	private enum Kind {

		DEFAULT,

		EMPTY,

		SIMPLE

	}

	static class NullInstanceCodeGenerator implements InstanceCodeGenerator {

		static final NullInstanceCodeGenerator INSTANCE = new NullInstanceCodeGenerator();

		private static final CodeBlock CODE_BLOCK = CodeBlock.of("null");

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type) {
			if (value != null) {
				return null;
			}
			return CODE_BLOCK;
		}

	}

	static class CharacterInstanceCodeGenerator implements InstanceCodeGenerator {

		static final CharacterInstanceCodeGenerator INSTANCE = new CharacterInstanceCodeGenerator();

		private static final Map<Character, String> ESCAPES;
		static {
			Map<Character, String> escapes = new HashMap<>();
			escapes.put('\b', "\\b");
			escapes.put('\t', "\\t");
			escapes.put('\n', "\\n");
			escapes.put('\f', "\\f");
			escapes.put('\r', "\\r");
			escapes.put('\"', "\"");
			escapes.put('\'', "\\'");
			escapes.put('\\', "\\\\");
			ESCAPES = Collections.unmodifiableMap(escapes);
		}

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type) {
			if (value instanceof Character character) {
				return CodeBlock.of("'$L'", escape(character));
			}
			return null;
		}

		private String escape(char ch) {
			String escaped = ESCAPES.get(ch);
			if (escaped != null) {
				return escaped;
			}
			return (!Character.isISOControl(ch)) ? Character.toString(ch)
					: String.format("\\u%04x", (int) ch);
		}
	}

	static class PrimitiveInstanceCodeGenerator implements InstanceCodeGenerator {

		static final PrimitiveInstanceCodeGenerator INSTANCE = new PrimitiveInstanceCodeGenerator();

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type) {
			if (value instanceof Boolean || value instanceof Integer) {
				return CodeBlock.of("$L", value);
			}
			if (value instanceof Byte) {
				return CodeBlock.of("(byte) $L", value);
			}
			if (value instanceof Short) {
				return CodeBlock.of("(short) $L", value);
			}
			if (value instanceof Long) {
				return CodeBlock.of("$LL", value);
			}
			if (value instanceof Float) {
				return CodeBlock.of("$LF", value);
			}
			if (value instanceof Double) {
				return CodeBlock.of("(double) $L", value);
			}
			return null;
		}

	}

	static class StringInstanceCodeGenerator implements InstanceCodeGenerator {

		static final StringInstanceCodeGenerator INSTANCE = new StringInstanceCodeGenerator();

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type) {
			if (value instanceof String) {
				return CodeBlock.of("$S", value);
			}
			return null;
		}

	}

	static class EnumInstanceCodeGenerator implements InstanceCodeGenerator {

		static final EnumInstanceCodeGenerator INSTANCE = new EnumInstanceCodeGenerator();

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type) {
			if (value instanceof Enum<?> enumValue) {
				return CodeBlock.of("$T.$L", enumValue.getDeclaringClass(),
						enumValue.name());
			}
			return null;
		}

	}

	static class ClassInstanceCodeGenerator implements InstanceCodeGenerator {

		static final ClassInstanceCodeGenerator INSTANCE = new ClassInstanceCodeGenerator();

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type) {
			if (value instanceof Class<?> clazz) {
				return CodeBlock.of("$T.class", ClassUtils.getUserClass(clazz));
			}
			return null;
		}

	}

	static class ResolvableTypeInstanceCodeGenerator implements InstanceCodeGenerator {

		static final ResolvableTypeInstanceCodeGenerator INSTANCE = new ResolvableTypeInstanceCodeGenerator();

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type) {
			if (value instanceof ResolvableType resolvableType) {
				return generateCode(resolvableType, true);
			}
			return null;
		}

		private CodeBlock generateCode(ResolvableType resolvableType, boolean forceResolvableType) {
			CodeBlock.Builder builder = CodeBlock.builder();
			buildCode(builder, resolvableType, true);
			return builder.build();
		}

		private void buildCode(CodeBlock.Builder builder, ResolvableType target,
				boolean forceResolvableType) {
			if (target.hasGenerics()) {
				buildCodeWithGenerics(builder, target, forceResolvableType);
				return;
			}
			Class<?> type = ClassUtils.getUserClass(target.toClass());
			if (forceResolvableType) {
				builder.add("$T.forClass($T.class)", ResolvableType.class, type);
				return;
			}
			builder.add("$T.class", type);
		}

		private void buildCodeWithGenerics(Builder builder, ResolvableType target,
				boolean forceResolvableType) {
			Class<?> type = ClassUtils.getUserClass(target.toClass());
			ResolvableType[] generics = target.getGenerics();
			boolean hasNestedGenerics = Arrays.stream(generics).anyMatch(ResolvableType::hasGenerics);
			builder.add("$T.forClassWithGenerics($T.class", ResolvableType.class, type);
			for (ResolvableType generic : generics) {
				builder.add(",$L", generateCode(generic, hasNestedGenerics));
			}
			builder.add(")");
		}

	}

	class ArrayInstanceCodeGenerator implements InstanceCodeGenerator {

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type) {
			return null;
		}

	}

	class ListInstanceCodeGenerator implements InstanceCodeGenerator {

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type) {
			return null;
		}

	}

	class SetInstanceCodeGenerator implements InstanceCodeGenerator {

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type) {
			return null;
		}

	}

	class MapInstanceCodeGenerator implements InstanceCodeGenerator {

		@Override
		public CodeBlock generateCode(Object value, ResolvableType type) {
			return null;
		}

	}

}
