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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Generates unique class names that can be used in ahead-of-time generated source code.
 * This class is stateful so the same instance should be used for all name generation.
 * Most commonly the class name generator is obtained via a {@link GenerationContext}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see GeneratedClassName
 */
public final class ClassNameGenerator {

	private static final String SEPARATOR = "__";

	private static final String AOT_PACKAGE = "__";

	private final Map<String, AtomicInteger> sequenceGenerator = new ConcurrentHashMap<>();

	/**
	 * Generate a new class name for the given {@code name} / {@code featureName}
	 * combination.
	 * @param name the name of the source item. Whenever possible this should be a target
	 * {@link Class}
	 * @param featureName the name of the feature that the generated class supports
	 * @return a unique generated class name
	 */
	public GeneratedClassName generateClassName(Class<?> source, String featureName) {
		Assert.notNull(source, "'source' must not be null");
		String rootName = source.getName();
		return generateSequencedClassName(source, featureName, rootName);
	}

	/**
	 * Generate a new class name for the given {@code name} / {@code featureName}
	 * combination.
	 * @param name the name of the source item. Whenever possible this should be a target
	 * {@link Class}
	 * @param featureName the name of the feature that the generated class supports
	 * @return a unique generated class name
	 */
	public GeneratedClassName generateClassName(String source, String featureName) {
		Assert.hasLength(source, "'source' must not be empty");
		String cleanedSource = clean(source);
		String rootName = AOT_PACKAGE + "." + ((!cleanedSource.isEmpty()) ? cleanedSource : "Aot");
		return generateSequencedClassName(source, featureName, rootName);
	}

	private String clean(String name) {
		StringBuilder rootName = new StringBuilder();
		boolean lastNotLetter = true;
		for (char ch : name.toString().toCharArray()) {
			if (!Character.isLetter(ch)) {
				lastNotLetter = true;
				continue;
			}
			rootName.append(lastNotLetter ? Character.toUpperCase(ch) : ch);
			lastNotLetter = false;
		}
		return rootName.toString();
	}

	private GeneratedClassName generateSequencedClassName(Object source, String featureName, String rootName) {
		Assert.hasLength(featureName, "'featureName' must not be empty");
		Assert.isTrue(featureName.chars().allMatch(Character::isLetter), "'featureName' must contain only letters");
		String generatedName = addSequence(rootName + SEPARATOR + StringUtils.capitalize(featureName));
		return new GeneratedClassName(generatedName, source);
	}

	private String addSequence(String name) {
		int sequence = this.sequenceGenerator.computeIfAbsent(name, key -> new AtomicInteger()).getAndIncrement();
		return (sequence > 0) ? name + sequence : name;
	}

}
