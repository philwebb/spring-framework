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

import org.springframework.javapoet.MethodSpec;

/**
 * A method name generated from a {@link MethodNameGenerator}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see MethodNameGenerator
 */
public final class GeneratedMethodName {

	private final String name;

	/**
	 * Create a new {@link GeneratedMethodName} instance with the given name. This
	 * constructor is package-private since names should only be generated via a
	 * {@link MethodNameGenerator}.
	 * @param name the generated name
	 */
	GeneratedMethodName(String name) {
		this.name = name;
	}

	/**
	 * Return the method name.
	 * @return the method name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return a {@link MethodSpec#methodBuilder(String) MethodSpec builder} pre-configured
	 * with the generated name.
	 * @return a method builder
	 */
	public MethodSpec.Builder methodBuilder() {
		return MethodSpec.methodBuilder(this.name);
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
		return this.name.equals(((GeneratedMethodName) obj).name);
	}

	@Override
	public String toString() {
		return this.name;
	}

}
