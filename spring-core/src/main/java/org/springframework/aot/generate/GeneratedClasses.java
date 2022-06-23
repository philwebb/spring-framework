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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.javapoet.ClassName;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A managed collection of generated classes. This class is stateful so the
 * same instance should be used for all class generation.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see GeneratedClass
 */
public class GeneratedClasses implements ClassGenerator {

	private final ClassNameGenerator classNameGenerator;

	private final List<GeneratedClass> classes;

	private final Map<Owner, GeneratedClass> classesByOwner;


	/**
	 * Create a new instance using the specified naming conventions.
	 * @param classNameGenerator the class name generator to use
	 */
	public GeneratedClasses(ClassNameGenerator classNameGenerator) {
		this(classNameGenerator, new ArrayList<>(), new ConcurrentHashMap<>());
	}

	private GeneratedClasses(ClassNameGenerator classNameGenerator,
			List<GeneratedClass> classes, Map<Owner, GeneratedClass> classesByOwner) {
		Assert.notNull(classNameGenerator, "'classNameGenerator' must not be null");
		this.classNameGenerator = classNameGenerator;
		this.classes = classes;
		this.classesByOwner = classesByOwner;
	}


	public GeneratedClass getOrGenerateClass(String featureName) {
		return getOrGenerateClass(featureName, null);
	}

	public GeneratedClass getOrGenerateClass(String featureName, Class<?> target) {
		Assert.hasLength(featureName, "'featureName' must not be empty");
		Owner owner = new Owner(featureName, target);
		return this.classesByOwner.computeIfAbsent(owner, key -> generateClass(featureName, target));
	}

	public GeneratedClass generateClass(String featureName) {
		return generateClass(featureName, null);
	}

	public GeneratedClass generateClass(String featureName, @Nullable Class<?> target) {
		Assert.hasLength(featureName, "'featureName' must not be empty");
		ClassName className = this.classNameGenerator.generateClassName(featureName, target);
		GeneratedClass generatedClass = new GeneratedClass(className);
		this.classes.add(generatedClass);
		return generatedClass;
	}

	/**
	 * Write the {@link GeneratedClass generated classes} using the given
	 * {@link GeneratedFiles} instance.
	 * @param generatedFiles where to write the generated classes
	 * @throws IOException on IO error
	 */
	void writeTo(GeneratedFiles generatedFiles) throws IOException {
		Assert.notNull(generatedFiles, "'generatedFiles' must not be null");
		List<GeneratedClass> generatedClasses = new ArrayList<>(this.classes);
		generatedClasses.sort(Comparator.comparing(GeneratedClass::getName));
		for (GeneratedClass generatedClass : generatedClasses) {
			generatedFiles.addSourceFile(generatedClass.generateJavaFile());
		}
	}

	GeneratedClasses withName(String name) {
		return new GeneratedClasses(this.classNameGenerator.usingFeatureNamePrefix(name),
				this.classes, this.classesByOwner);
	}


	private record Owner(String featureName, @Nullable Class<?> target) {

	}

}
