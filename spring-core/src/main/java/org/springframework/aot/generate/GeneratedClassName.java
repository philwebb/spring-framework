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

import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A class name generated from a {@link ClassNameGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see ClassNameGenerator
 */
public final class GeneratedClassName {

	private final String name;

	/**
	 * Create a new {@link GeneratedClassName} instance with the given name. This
	 * constructor is package-private since names should only be generated via a
	 * {@link ClassNameGenerator}.
	 * @param name the generated name
	 */
	GeneratedClassName(String name) {
		this.name = name;
	}

	/**
	 * Return the fully-qualified class name.
	 * @return the class name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the package name.
	 * @return the package name
	 */
	public String getPackageName() {
		return ClassUtils.getPackageName(this.name);
	}

	/**
	 * Return the class name without the qualified package name.
	 * @return the short name
	 */
	public String getShortName() {
		return ClassUtils.getShortName(this.name);
	}

	/**
	 * Return a new {@link TypeSpec#classBuilder(String) TypeSpec class builder}
	 * pre-configured with the generated name.
	 * @return a {@link TypeSpec} class builder
	 */
	public TypeSpec.Builder classBuilder() {
		return TypeSpec.classBuilder(getShortName());
	}

	/**
	 * Return a new {@link JavaFile#builder(String, TypeSpec) JavaFile builder}
	 * pre-configured with the package of the generated name.
	 * @return a {@link JavaFile} builder
	 * @throws IllegalArgumentException if the type spec doesn't have the correct name
	 */
	public JavaFile.Builder javaFileBuilder(TypeSpec typeSpec) {
		Assert.notNull(typeSpec, "'typeSpec' must not be null");
		Assert.isTrue(getShortName().equals(typeSpec.name),
				() -> String.format("'typeSpec' must be named '%s' instead of '%s'", this, typeSpec.name));
		return JavaFile.builder(getPackageName(), typeSpec).skipJavaLangImports(true);
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.name.equals(((GeneratedClassName) obj).name);
	}

	@Override
	public String toString() {
		return this.name;
	}

}
