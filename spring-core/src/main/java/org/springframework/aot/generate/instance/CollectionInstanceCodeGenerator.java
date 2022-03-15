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

import java.util.Collection;
import java.util.Iterator;

import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.lang.Nullable;

class CollectionInstanceCodeGenerator<T extends Collection<?>>
		implements InstanceCodeGenerator {

	private final InstanceCodeGenerationService codeGenerationService;

	private final Class<?> collectionType;

	private final CodeBlock emptyResult;

	protected CollectionInstanceCodeGenerator(
			InstanceCodeGenerationService codeGenerationService, Class<?> collectionType,
			CodeBlock emptyResult) {
		this.codeGenerationService = codeGenerationService;
		this.collectionType = collectionType;
		this.emptyResult = emptyResult;
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
		return generateCollectionOf(name, collection, this.collectionType, elementType);
	}

	protected final CodeBlock generateCollectionOf(String name, Collection<?> collection,
			Class<?> collectionType, ResolvableType elementType) {
		Builder builder = CodeBlock.builder();
		builder.add("$T.of(", collectionType);
		Iterator<?> iterator = collection.iterator();
		while (iterator.hasNext()) {
			Object element = iterator.next();
			CodeBlock elementCode = this.codeGenerationService.generateCode(name, element,
					elementType);
			builder.add("$L", elementCode);
			builder.add((!iterator.hasNext()) ? "" : ", ");
		}
		builder.add(")");
		return builder.build();
	}

}