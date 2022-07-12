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

import java.util.function.Consumer;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.Assert;

/**
 * A single generated class.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see GeneratedClasses
 */
public final class GeneratedClass {

	private final ClassName name;

	private final GeneratedMethods methods;

	private final Consumer<TypeSpec.Builder> type;

	private MethodNameGenerator methodNameGenerator;


	/**
	 * Create a new {@link GeneratedClass} instance with the given name. This
	 * constructor is package-private since names should only be generated via a
	 * {@link GeneratedClasses}.
	 * @param name the generated name
	 * @param type consumer to generate the type
	 */
	GeneratedClass(ClassName name, Consumer<TypeSpec.Builder> type) {
		Assert.notNull(type, "'type' must not be null");
		this.name = name;
		this.type = type;
		this.methodNameGenerator = new MethodNameGenerator();
		this.methods = new GeneratedMethods(this.methodNameGenerator);
	}


	/**
	 * Update this instance with a set of reserved method names that should not
	 * be used for generated methods. Reserved names are often needed when a
	 * generated class implements a specific interface.
	 * @param reservedMethodNames the reserved method names
	 * @return this instance
	 */
	public GeneratedClass reserveMethodNames(String... reservedMethodNames) {
		this.methodNameGenerator.reserveMethodNames(reservedMethodNames);
		return this;
	}

	/**
	 * Return the name of the generated class.
	 * @return the name of the generated class
	 */
	public ClassName getName() {
		return this.name;
	}

	/**
	 * Return the method generator that can be used for this generated class.
	 * @return the method generator
	 */
	public GeneratedMethods getMethods() {
		return this.methods;
	}

	JavaFile generateJavaFile() {
		TypeSpec.Builder builder = getBuilder(this.type);
		this.methods.doWithMethodSpecs(builder::addMethod);
		return JavaFile.builder(this.name.packageName(), builder.build()).build();
	}

	private TypeSpec.Builder getBuilder(Consumer<TypeSpec.Builder> type) {
		TypeSpec.Builder builder = TypeSpec.classBuilder(this.name);
		type.accept(builder);
		return builder;
	}

	void assertSameType(Consumer<TypeSpec.Builder> type) {
		Assert.state(type == this.type || getBuilder(this.type).build().equals(getBuilder(type).build()),
				"'type' consumer generated different result");
	}

}
