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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.util.Assert;

/**
 * A managed collection of generated classes.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see GeneratedClass
 */
public class GeneratedClasses implements ClassGenerator {

	private final ClassNameGenerator classNameGenerator;

	private final Map<Key, GeneratedClassName> generatedNames = new ConcurrentHashMap<>();

	private final Map<GeneratedClassName, GeneratedClass> generatedClasses = new ConcurrentHashMap<>();

	private final ReentrantLock lock = new ReentrantLock();

	public GeneratedClasses(ClassNameGenerator classNameGenerator) {
		Assert.notNull(classNameGenerator, "'classNameGenerator' must not be null");
		this.classNameGenerator = classNameGenerator;
	}

	@Override
	public GeneratedClass getOrGenerateClass(Class<?> target, String featureName) {
		Assert.notNull(target, "'target' must not be null");
		Assert.hasLength(featureName, "'featureName' must not be empty");
		Key key = new Key(target.getName(), featureName);
		return getOrGenerateClass(key, () -> this.classNameGenerator.generateClassName(target, featureName));
	}

	@Override
	public GeneratedClass getOrGenerateClass(String target, String featureName) {
		Assert.notNull(target, "'target' must not be null");
		Assert.hasLength(featureName, "'featureName' must not be empty");
		Key key = new Key(target, featureName);
		return getOrGenerateClass(key, () -> this.classNameGenerator.generateClassName(target, featureName));
	}

	private GeneratedClass getOrGenerateClass(Key key, Supplier<GeneratedClassName> classNameGenerator) {
		GeneratedClassName name = this.generatedNames.get(key);
		if (name != null) {
			return this.generatedClasses.get(name);
		}
		this.lock.lock();
		try {
			name = this.generatedNames.computeIfAbsent(key, k -> classNameGenerator.get());
			return this.generatedClasses.computeIfAbsent(name, GeneratedClass::new);
		}
		finally {
			this.lock.unlock();
		}
	}

	private record Key(String target, String featureName) {

	}

}
