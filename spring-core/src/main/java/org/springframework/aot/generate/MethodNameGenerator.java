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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Internal class used to generate unique method names that can be used in
 * ahead-of-time generated source code. This class is stateful so one instance
 * should be used per generated type.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class MethodNameGenerator {

	private final Map<String, AtomicInteger> sequenceGenerator = new ConcurrentHashMap<>();


	/**
	 * Create a new {@link MethodNameGenerator} instance without any reserved
	 * names.
	 */
	MethodNameGenerator() {
	}

	/**
	 * Create a new {@link MethodNameGenerator} instance with the specified
	 * reserved names.
	 * @param reservedNames the method names to reserve
	 */
	MethodNameGenerator(String... reservedNames) {
		this(List.of(reservedNames));
	}

	// FIXME reserved names not used but should be. Perhaps on generated class

	/**
	 * Create a new {@link MethodNameGenerator} instance with the specified
	 * reserved names.
	 * @param reservedNames the method names to reserve
	 */
	public MethodNameGenerator(Iterable<String> reservedNames) {
		Assert.notNull(reservedNames, "'reservedNames' must not be null");
		for (String reservedName : reservedNames) {
			addSequence(StringUtils.uncapitalize(reservedName));
		}
	}


	/**
	 * Generate a new method name from the given parts.
	 * @param parts the parts used to build the name.
	 * @return the generated method name
	 */
	public String generateMethodName(String... parts) {
		String generatedName = join(parts);
		return addSequence(generatedName.isEmpty() ? "$$aot" : generatedName);
	}

	private String addSequence(String name) {
		int sequence = this.sequenceGenerator
				.computeIfAbsent(name, key -> new AtomicInteger()).getAndIncrement();
		return (sequence > 0) ? name + sequence : name;
	}



	void reserveMethodNames(String... reservedMethodNames) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
