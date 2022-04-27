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

import javax.annotation.Nullable;

import org.springframework.javapoet.ClassName;
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
public final class GeneratedClassName implements Comparable<GeneratedClassName> {

	private final String name;

	private final Object target;

	/**
	 * Create a new {@link GeneratedClassName} instance with the given name. This
	 * constructor is package-private since names should only be generated via a
	 * {@link ClassNameGenerator}.
	 * @param name the generated name
	 * @param target the target of the generated class
	 */
	GeneratedClassName(String name, Object target) {
		this.name = name;
		this.target = target;
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
		int lastDotIndex = this.name.lastIndexOf('.');
		return (lastDotIndex != -1) ? this.name.substring(lastDotIndex + 1) : this.name;
	}

	/**
	 * Return the target class for this generated name or {@code null}.
	 * @return the target class
	 */
	@Nullable
	public Class<?> getTargetClass() {
		if (this.target instanceof Class<?> targetClass) {
			return targetClass;
		}
		try {
			return ClassUtils.forName((String) this.target, null);
		}
		catch (Exception ex) {
			return null;
		}
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
	 * Return a new {@link JavaFile} using this class name and a {@link TypeSpec} from the
	 * given {@code typeSpecBuilder}.
	 * @param typeSpecBuilder the type spec builder
	 * @return a new {@link JavaFile}.
	 */
	public JavaFile toJavaFile(TypeSpec.Builder typeSpecBuilder) {
		Assert.notNull(typeSpecBuilder, "'typeSpecBuilder' must not be null");
		return toJavaFile(typeSpecBuilder.build());
	}

	/**
	 * Return a new {@link JavaFile} using this class name and the given {@link TypeSpec}.
	 * @param typeSpec the type spec
	 * @return a new {@link JavaFile}.
	 */
	public JavaFile toJavaFile(TypeSpec typeSpec) {
		Assert.notNull(typeSpec, "'typeSpec' must not be null");
		return javaFileBuilder(typeSpec).build();
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
				() -> String.format("'typeSpec' must be named '%s' instead of '%s'", getShortName(), typeSpec.name));
		return JavaFile.builder(getPackageName(), typeSpec).skipJavaLangImports(true);
	}

	/**
	 * Return this instance as a Javapoet {@link ClassName}.
	 * @return a {@link ClassName} instance
	 */
	public ClassName toClassName() {
		return ClassName.get(getPackageName(), getShortName());
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

	@Override
	public int compareTo(GeneratedClassName other) {
		return this.name.compareTo(other.name);
	}

}
