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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.ResolvableType;
import org.springframework.javapoet.AnnotationSpec;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utility class that generates code to re-create an instance of an object. For
 * example, given a {@link List} containing the Strings "a", "b", "c", the
 * generators would return {@code List.of("a", "b", "c")}.
 * <p>
 * By default, the following types are supported:
 * <ul>
 * <li>{@link Character}</li>
 * <li>Primitives ({@link Boolean}, {@link Byte}, {@link Short},
 * {@link Integer}, {@link Long})</li>
 * <li>{@link String}</li>
 * <li>{@link Enum}</li>
 * <li>{@link Class}</li>
 * <li>{@link ResolvableType}</li>
 * <li>All Array Types</li>
 * <li>{@link Set}</li>
 * <li>{@link List}</li>
 * <li>{@link Map}</li>
 * </ul>
 * Additional {@link CandidateGenerator} can also be
 * {@link #add(CandidateGenerator) added} to support other types.
 * <p>
 * For simple conversion (where no additional {@link CandidateGenerator}
 * instances need adding) the {@link InstanceCodeGenerator#simple()} method may
 * be used.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class InstanceCodeGenerator {

	private static final InstanceCodeGenerator SIMPLE = new InstanceCodeGenerator(null,
			Kind.SIMPLE);

	@Nullable
	private final GeneratedMethods generatedMethods;

	private final Kind kind;

	private final List<CandidateGenerator> generators = new ArrayList<>();

	public InstanceCodeGenerator(boolean addDefaultGenerators) {
		this(null, Kind.DEFAULT);
	}

	public InstanceCodeGenerator(GeneratedMethods generatedMethods) {
		this(generatedMethods, true);
	}

	public InstanceCodeGenerator(GeneratedMethods generatedMethods,
			boolean addDefaultGenerators) {
		this(generatedMethods, (addDefaultGenerators) ? Kind.DEFAULT : Kind.EMPTY);
	}

	private InstanceCodeGenerator(@Nullable GeneratedMethods generatedMethods,
			Kind kind) {
		Assert.isTrue(generatedMethods != null || kind == Kind.SIMPLE,
				"'generatedMethods' must not be null");
		this.generatedMethods = generatedMethods;
		this.kind = kind;
		this.generators.add(NullInstanceCodeGenerator.INSTANCE);
		if (kind != Kind.EMPTY) {
			this.generators.add(CharacterInstanceCodeGenerator.INSTANCE);
			this.generators.add(PrimitiveInstanceCodeGenerator.INSTANCE);
			this.generators.add(StringInstanceCodeGenerator.INSTANCE);
			this.generators.add(EnumInstanceCodeGenerator.INSTANCE);
			this.generators.add(ClassInstanceCodeGenerator.INSTANCE);
			this.generators.add(ResolvableTypeInstanceCodeGenerator.INSTANCE);
			this.generators.add(new ArrayInstanceCodeGenerator(this));
			this.generators.add(new ListInstanceCodeGenerator(this));
			this.generators.add(new SetInstanceCodeGenerator(this));
			this.generators.add(new MapInstanceCodeGenerator(this));
		}
	}

	public static InstanceCodeGenerator simple() {
		return SIMPLE;
	}

	public void add(CandidateGenerator generator) {
		Assert.state(kind != Kind.SIMPLE,
				"'InstanceCodeGenerators.simple()' cannot be modified");
		Assert.notNull(generator, "'generator' must not be null");
		this.generators.add(0, generator);
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
		return generateInstantiationCode(null, value, type);
	}

	public CodeBlock generateInstantiationCode(@Nullable String name,
			@Nullable Object value, ResolvableType type) {
		for (CandidateGenerator generator : this.generators) {
			CodeBlock code = generator.generateCode(name, value, type);
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

	static class NullInstanceCodeGenerator implements CandidateGenerator {

		static final NullInstanceCodeGenerator INSTANCE = new NullInstanceCodeGenerator();

		private static final CodeBlock CODE_BLOCK = CodeBlock.of("null");

		@Override
		public CodeBlock generateCode(@Nullable String name, Object value,
				ResolvableType type) {
			if (value != null) {
				return null;
			}
			return CODE_BLOCK;
		}

	}

	static class CharacterInstanceCodeGenerator implements CandidateGenerator {

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
		public CodeBlock generateCode(@Nullable String name, Object value,
				ResolvableType type) {
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

	static class PrimitiveInstanceCodeGenerator implements CandidateGenerator {

		static final PrimitiveInstanceCodeGenerator INSTANCE = new PrimitiveInstanceCodeGenerator();

		@Override
		public CodeBlock generateCode(@Nullable String name, Object value,
				ResolvableType type) {
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

	static class StringInstanceCodeGenerator implements CandidateGenerator {

		static final StringInstanceCodeGenerator INSTANCE = new StringInstanceCodeGenerator();

		@Override
		public CodeBlock generateCode(@Nullable String name, Object value,
				ResolvableType type) {
			if (value instanceof String) {
				return CodeBlock.of("$S", value);
			}
			return null;
		}

	}

	static class EnumInstanceCodeGenerator implements CandidateGenerator {

		static final EnumInstanceCodeGenerator INSTANCE = new EnumInstanceCodeGenerator();

		@Override
		public CodeBlock generateCode(@Nullable String name, Object value,
				ResolvableType type) {
			if (value instanceof Enum<?> enumValue) {
				return CodeBlock.of("$T.$L", enumValue.getDeclaringClass(),
						enumValue.name());
			}
			return null;
		}

	}

	static class ClassInstanceCodeGenerator implements CandidateGenerator {

		static final ClassInstanceCodeGenerator INSTANCE = new ClassInstanceCodeGenerator();

		@Override
		public CodeBlock generateCode(@Nullable String name, Object value,
				ResolvableType type) {
			if (value instanceof Class<?> clazz) {
				return CodeBlock.of("$T.class", ClassUtils.getUserClass(clazz));
			}
			return null;
		}

	}

	static class ResolvableTypeInstanceCodeGenerator implements CandidateGenerator {

		static final ResolvableTypeInstanceCodeGenerator INSTANCE = new ResolvableTypeInstanceCodeGenerator();

		@Override
		public CodeBlock generateCode(@Nullable String name, Object value,
				ResolvableType type) {
			if (value instanceof ResolvableType resolvableType) {
				return generateCode(resolvableType, false);
			}
			return null;
		}

		private CodeBlock generateCode(ResolvableType resolvableType,
				boolean allowClassResult) {
			if (ResolvableType.NONE.equals(resolvableType)) {
				return CodeBlock.of("$T.NONE", ResolvableType.class);
			}
			Class<?> type = ClassUtils.getUserClass(resolvableType.toClass());
			if (resolvableType.hasGenerics()) {
				return generateCodeWithGenerics(resolvableType, type);
			}
			if (allowClassResult) {
				return CodeBlock.of("$T.class", type);
			}
			return CodeBlock.of("$T.forClass($T.class)", ResolvableType.class, type);
		}

		private CodeBlock generateCodeWithGenerics(ResolvableType target, Class<?> type) {
			ResolvableType[] generics = target.getGenerics();
			boolean hasNoNestedGenerics = Arrays.stream(generics).noneMatch(
					ResolvableType::hasGenerics);
			CodeBlock.Builder builder = CodeBlock.builder();
			builder.add("$T.forClassWithGenerics($T.class", ResolvableType.class, type);
			for (ResolvableType generic : generics) {
				builder.add(", $L", generateCode(generic, hasNoNestedGenerics));
			}
			builder.add(")");
			return builder.build();
		}

	}

	public static class ArrayInstanceCodeGenerator implements CandidateGenerator {

		private final InstanceCodeGenerator generators;

		public ArrayInstanceCodeGenerator(InstanceCodeGenerator generators) {
			this.generators = generators;
		}

		protected final InstanceCodeGenerator getGenerators() {
			return generators;
		}

		@Override
		public CodeBlock generateCode(@Nullable String name, Object value,
				ResolvableType type) {
			if (type.isArray()) {
				ResolvableType componentType = type.getComponentType();
				int length = Array.getLength(value);
				CodeBlock.Builder builder = CodeBlock.builder();
				builder.add("new $T {", type.toClass());
				for (int i = 0; i < length; i++) {
					Object component = Array.get(value, i);
					builder.add((i != 0) ? ", $L" : "$L",
							getGenerators().generateInstantiationCode(name, component,
									componentType));
				}
				builder.add("}");
				return builder.build();
			}
			return null;
		}

	}

	public static class CollectionInstanceCodeGenerator<T extends Collection<?>>
			implements CandidateGenerator {

		private final InstanceCodeGenerator generators;

		private final Class<?> collectionType;

		private final CodeBlock emptyResult;

		protected CollectionInstanceCodeGenerator(InstanceCodeGenerator generators,
				Class<?> collectionType, CodeBlock emptyResult) {
			this.generators = generators;
			this.collectionType = collectionType;
			this.emptyResult = emptyResult;
		}

		protected final InstanceCodeGenerator getGenerators() {
			return generators;
		}

		@Override
		@SuppressWarnings("unchecked")
		public CodeBlock generateCode(@Nullable String name, Object value,
				ResolvableType type) {
			if (this.collectionType.isInstance(value)) {
				T collection = (T) value;
				return generateCollectionCode(name, type, collection);
			}
			return null;
		}

		private CodeBlock generateCollectionCode(String name, ResolvableType type,
				T collection) {
			if (collection.isEmpty()) {
				return this.emptyResult;
			}
			ResolvableType elementType = type.as(this.collectionType).getGeneric();
			return generateCollectionCode(name, collection, elementType);
		}

		protected CodeBlock generateCollectionCode(String name, T collection,
				ResolvableType elementType) {
			return generateCollectionOf(name, collection, this.collectionType,
					elementType);
		}

		protected final CodeBlock generateCollectionOf(String name,
				Collection<?> collection, Class<?> collectionType,
				ResolvableType elementType) {
			Builder builder = CodeBlock.builder();
			builder.add("$T.of(", collectionType);
			Iterator<?> iterator = collection.iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();
				builder.add("$L", getGenerators().generateInstantiationCode(name, element,
						elementType));
				builder.add((!iterator.hasNext()) ? "" : ", ");
			}
			builder.add(")");
			return builder.build();
		}

	}

	public class ListInstanceCodeGenerator
			extends CollectionInstanceCodeGenerator<List<?>> {

		protected static final CodeBlock EMPTY_RESULT = CodeBlock.of("$T.emptyList()",
				Collections.class);

		public ListInstanceCodeGenerator(InstanceCodeGenerator generators) {
			super(generators, List.class, EMPTY_RESULT);
		}
	}

	public class SetInstanceCodeGenerator
			extends CollectionInstanceCodeGenerator<Set<?>> {

		protected static final CodeBlock EMPTY_RESULT = CodeBlock.of("$T.emptySet()",
				Collections.class);

		public SetInstanceCodeGenerator(InstanceCodeGenerator generators) {
			super(generators, Set.class, EMPTY_RESULT);
		}

		@Override
		protected CodeBlock generateCollectionCode(String name, Set<?> set,
				ResolvableType elementType) {
			if (set instanceof LinkedHashSet) {
				return CodeBlock.of("new $T($L)", LinkedHashSet.class,
						generateCollectionOf(name, set, List.class, elementType));
			}
			set = orderForCodeConsistency(set);
			return super.generateCollectionCode(name, set, elementType);
		}

		private Set<?> orderForCodeConsistency(Set<?> set) {
			return new TreeSet<Object>(set);
		}

	}

	public static class MapInstanceCodeGenerator implements CandidateGenerator {

		protected static final CodeBlock EMPTY_RESULT = CodeBlock.of("$T.emptyMap()",
				Collections.class);

		private final InstanceCodeGenerator generators;

		public MapInstanceCodeGenerator(InstanceCodeGenerator generators) {
			this.generators = generators;
		}

		protected final InstanceCodeGenerator getGenerators() {
			return this.generators;
		}

		@Override
		public CodeBlock generateCode(@Nullable String name, Object value,
				ResolvableType type) {
			if (value instanceof Map<?, ?> map) {
				return generateMapCode(name, type, map);
			}
			return null;
		}

		private <K, V> CodeBlock generateMapCode(String name, ResolvableType type,
				Map<K, V> map) {
			if (map.isEmpty()) {
				return EMPTY_RESULT;
			}
			ResolvableType keyType = type.as(Map.class).getGeneric(0);
			ResolvableType valueType = type.as(Map.class).getGeneric(1);
			if (map instanceof LinkedHashMap<?, ?>) {
				return generateLinkedHashMapCode(name, map, keyType, valueType);
			}
			map = orderForCodeConsistency(map);
			boolean useOfEntries = map.size() > 10;
			CodeBlock.Builder builder = CodeBlock.builder();
			builder.add("$T" + ((!useOfEntries) ? ".of(" : ".ofEntries("), Map.class);
			Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<K, V> entry = iterator.next();
				CodeBlock keyCode = getGenerators().generateInstantiationCode(name,
						entry.getKey(), keyType);
				CodeBlock valueCode = getGenerators().generateInstantiationCode(name,
						entry.getValue(), valueType);
				if (!useOfEntries) {
					builder.add("$L, $L", keyCode, valueCode);
				}
				else {
					builder.add("$T.entry($L,$L)", Map.class, keyCode, valueCode);
				}
				builder.add((!iterator.hasNext()) ? "" : ", ");
			}
			builder.add(")");
			return builder.build();
		}

		private <K, V> Map<K, V> orderForCodeConsistency(Map<K, V> map) {
			return new TreeMap<>(map);
		}

		private <K, V> CodeBlock generateLinkedHashMapCode(String name, Map<K, V> map,
				ResolvableType keyType, ResolvableType valueType) {
			if (getGenerators().kind == Kind.SIMPLE) {
				return generateLinkedHashMapCodeWithStream(name, map, keyType, valueType);
			}
			return generateLinkedHashMapCodeWithMethod(name, map, keyType, valueType);
		}

		private <K, V> CodeBlock generateLinkedHashMapCodeWithStream(String name,
				Map<K, V> map, ResolvableType keyType, ResolvableType valueType) {
			CodeBlock.Builder builder = CodeBlock.builder();
			builder.add("$T.of(", Stream.class);
			Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<K, V> entry = iterator.next();
				CodeBlock keyCode = getGenerators().generateInstantiationCode(name,
						entry.getKey(), keyType);
				CodeBlock valueCode = getGenerators().generateInstantiationCode(name,
						entry.getValue(), valueType);
				builder.add("$T.entry($L, $L)", Map.class, keyCode, valueCode);
				builder.add((!iterator.hasNext()) ? "" : ", ");
			}
			builder.add(
					").collect($T.toMap($T::getKey, $T::getValue, (v1, v2) -> v1, $T::new))",
					Collectors.class, Map.Entry.class, Map.Entry.class,
					LinkedHashMap.class);
			return builder.build();
		}

		private <K, V> CodeBlock generateLinkedHashMapCodeWithMethod(String name,
				Map<K, V> map, ResolvableType keyType, ResolvableType valueType) {
			GeneratedMethod method = getGenerators().getGeneratedMethods().add(
					MethodNameGenerator.join("get", name, "map"));
			method.generateBy((builder) -> {
				builder.addAnnotation(
						AnnotationSpec.builder(SuppressWarnings.class).addMember("value",
								"{\"rawtypes\", \"unchecked\"}").build());
				builder.returns(Map.class);
				builder.addStatement("$T map = new $T($L)", Map.class,
						LinkedHashMap.class, map.size());
				map.forEach((key, value) -> {
					CodeBlock keyCode = getGenerators().generateInstantiationCode(name,
							key, keyType);
					CodeBlock valueCode = getGenerators().generateInstantiationCode(name,
							value, valueType);
					builder.addStatement("map.put($L, $L)", keyCode, valueCode);
				});
				builder.addStatement("return map");
			});
			return CodeBlock.of("$L()", method.getName());
		}

	}

	/**
	 *
	 * @author Stephane Nicoll
	 * @author Phillip Webb
	 * @author Andy Wilkinson
	 * @since 6.0
	 */
	public interface CandidateGenerator {

		@Nullable
		CodeBlock generateCode(@Nullable String name, Object value, ResolvableType type);

	}

}
