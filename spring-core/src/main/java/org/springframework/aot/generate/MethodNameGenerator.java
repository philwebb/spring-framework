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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Generates unique method names that can be used in ahead-of-time generated
 * source code. This class is stateful so one instance should be used per
 * generated type.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
public class MethodNameGenerator {

	private final Map<String, AtomicInteger> sequenceGenerator = new ConcurrentHashMap<>();

	/**
	 * Create a new {@link MethodNameGenerator} instance without any reserved
	 * names.
	 */
	public MethodNameGenerator() {
	}

	/**
	 * Create a new {@link MethodNameGenerator} instance with the specified
	 * reserved names.
	 * @param reservedNames the method names to reserve
	 */
	public MethodNameGenerator(String... reservedNames) {
		this(List.of(reservedNames));
	}

	/**
	 * Create a new {@link MethodNameGenerator} instance with the specified
	 * reserved names.
	 * @param reservedNames the method names to reserve
	 */
	public MethodNameGenerator(List<String> reservedNames) {
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
	public GeneratedMethodName generateMethodName(Object... parts) {
		StringBuilder generatedName = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			String partName = getPartName(parts[i]);
			generatedName.append(i != 0 ? StringUtils.capitalize(partName)
					: StringUtils.uncapitalize(partName));
		}
		return new GeneratedMethodName(addSequence(
				generatedName.isEmpty() ? "$$aot" : generatedName.toString()));
	}

	private String getPartName(Object part) {
		String string = (part instanceof Class<?>) ? ((Class<?>) part).getName()
				: ObjectUtils.nullSafeToString(part);
		char[] chars = (string != null) ? string.toCharArray() : new char[0];
		StringBuffer name = new StringBuffer(chars.length);
		for (char ch : chars) {
			name.append((!Character.isLetter(ch)) ? "" : ch);
		}
		return name.toString();
	}

	private String addSequence(String name) {
		int sequence = this.sequenceGenerator.computeIfAbsent(name,
				(key) -> new AtomicInteger()).getAndIncrement();
		return (sequence > 0) ? name + sequence : name;
	}

}
