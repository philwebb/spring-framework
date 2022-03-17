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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.MethodNameGenerator;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.AnnotationSpec;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;

/**
 * {@link InstanceCodeGenerator} to support {@link Maps Maps} and {@link LinkedHashMap LinkedHashMaps}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class MapInstanceCodeGenerator implements InstanceCodeGenerator {

	private static final CodeBlock EMPTY_RESULT = CodeBlock.of("$T.emptyMap()",
			Collections.class);

	private final InstanceCodeGenerationService codeGenerationService;

	MapInstanceCodeGenerator(DefaultInstanceCodeGenerationService generators) {
		this.codeGenerationService = generators;
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
			CodeBlock keyCode = this.codeGenerationService.generateCode(name,
					entry.getKey(), keyType);
			CodeBlock valueCode = this.codeGenerationService.generateCode(name,
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
		if (!this.codeGenerationService.supportsGeneratedMethods()) {
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
			CodeBlock keyCode = this.codeGenerationService.generateCode(name,
					entry.getKey(), keyType);
			CodeBlock valueCode = this.codeGenerationService.generateCode(name,
					entry.getValue(), valueType);
			builder.add("$T.entry($L, $L)", Map.class, keyCode, valueCode);
			builder.add((!iterator.hasNext()) ? "" : ", ");
		}
		builder.add(
				").collect($T.toMap($T::getKey, $T::getValue, (v1, v2) -> v1, $T::new))",
				Collectors.class, Map.Entry.class, Map.Entry.class, LinkedHashMap.class);
		return builder.build();
	}

	private <K, V> CodeBlock generateLinkedHashMapCodeWithMethod(String name,
			Map<K, V> map, ResolvableType keyType, ResolvableType valueType) {
		GeneratedMethod method = this.codeGenerationService.getGeneratedMethods().add(
				MethodNameGenerator.join("get", name, "map"));
		method.generateBy((builder) -> {
			builder.addAnnotation(
					AnnotationSpec.builder(SuppressWarnings.class).addMember("value",
							"{\"rawtypes\", \"unchecked\"}").build());
			builder.returns(Map.class);
			builder.addStatement("$T map = new $T($L)", Map.class, LinkedHashMap.class,
					map.size());
			map.forEach((key, value) -> {
				CodeBlock keyCode = this.codeGenerationService.generateCode(name, key,
						keyType);
				CodeBlock valueCode = this.codeGenerationService.generateCode(name, value,
						valueType);
				builder.addStatement("map.put($L, $L)", keyCode, valueCode);
			});
			builder.addStatement("return map");
		});
		return CodeBlock.of("$L()", method.getName());
	}

}