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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;

/**
 * {@link InstanceCodeGenerator} to support {@link Set Sets} and
 * {@link LinkedHashSet LinkedHashSet}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class SetInstanceCodeGenerator extends CollectionInstanceCodeGenerator<Set<?>> {

	private static final CodeBlock EMPTY_RESULT = CodeBlock.of("$T.emptySet()",
			Collections.class);

	SetInstanceCodeGenerator(DefaultInstanceCodeGenerationService generators) {
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