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

/**
 * A generated class.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see GeneratedClasses
 * @see ClassGenerator
 */
public final class GeneratedClass implements MethodGenerator {

	private final GeneratedClassName name;

	private final GeneratedMethods methods;

	/**
	 * Create a new {@link GeneratedClass} instance with the given name. This constructor
	 * is package-private since names should only be generated via a
	 * {@link GeneratedClasses}.
	 * @param name the generated name
	 */
	GeneratedClass(GeneratedClassName name) {
		this.name = name;
		this.methods = new GeneratedMethods();
	}

	/**
	 * Return the name of the generated class.
	 * @return the name of the generated class
	 */
	public GeneratedClassName getName() {
		return this.name;
	}

	@Override
	public GeneratedMethod generateMethod(Object... methodNameParts) {
		return this.methods.generateMethod(methodNameParts);
	}

}
