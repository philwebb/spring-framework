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

import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.MethodSpec.Builder;
import org.springframework.util.Assert;

/**
 * A bean registration method that has been added to a {@link GeneratedMethods}
 * collection.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see GeneratedMethods
 */
public final class GeneratedMethod {

	private final GeneratedMethodName name;

	private MethodSpec spec;

	/**
	 * Create a new {@link GeneratedMethod} instance with the given name. This
	 * constructor is package-private since names should only be generated via a
	 * {@link GeneratedMethods}.
	 * @param name the generated name
	 */
	GeneratedMethod(GeneratedMethodName name) {
		this.name = name;
	}

	/**
	 * Return the generated name of the method.
	 * @return
	 */
	public GeneratedMethodName getName() {
		return this.name;
	}

	/**
	 * Return the {@link MethodSpec} for this generated method.
	 * @return the method spec
	 * @throws IllegalStateException if one of the {@code generateBy(...)}
	 * methods has not been called
	 */
	public MethodSpec getSpec() {
		Assert.state(this.spec != null,
				() -> String.format("Method '%s' has no method spec defined", this.name));
		return this.spec;
	}

	/**
	 * Generate the method using the given consumer.
	 * @param builder a consumer that will accept a method spec builder and
	 * configure it as necessary
	 */
	public void generateBy(Consumer<MethodSpec.Builder> builder) {
		Builder builderToUse = getName().methodBuilder();
		builder.accept(builderToUse);
		generateBy(builderToUse);
	}

	/**
	 * Generate the method using the given spec builder.
	 * @param builder the method spec builder
	 */
	public void generateBy(MethodSpec.Builder builder) {
		generateBy(builder.build());
	}

	/**
	 * Generate the method using the given spec.
	 * @param spec the method spec
	 */
	public void generateBy(MethodSpec spec) {
		Assert.isTrue(this.name.toString().equals(spec.name),
				() -> String.format("'spec' must use the generated name \"%s\"",
						this.name));
		this.spec = spec;
	}

}