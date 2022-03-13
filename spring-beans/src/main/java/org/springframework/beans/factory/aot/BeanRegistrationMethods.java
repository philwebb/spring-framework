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

package org.springframework.beans.factory.aot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.aot.context.AotContribution;
import org.springframework.aot.generate.GeneratedMethodName;
import org.springframework.aot.generate.MethodNameGenerator;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.MethodSpec.Builder;
import org.springframework.util.Assert;

/**
 * Allows {@link MethodSpec MethodSpecs} to be added to the {@link AotContribution} that
 * generates bean registration code.
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationCode
 */
public class BeanRegistrationMethods {

	private final MethodNameGenerator methodNameGenerator;

	private final List<BeanRegistrationMethod> methods = new ArrayList<>();

	BeanRegistrationMethods(MethodNameGenerator methodNameGenerator) {
		this.methodNameGenerator = methodNameGenerator;
	}

	/**
	 * Add a new {@link BeanRegistrationMethod}. The returned instance must define the
	 * method spec by calling {@code generateBy(...)}.
	 * @param methodNameParts the method name parts that should be used to generate a
	 * unique method name
	 * @return the newly added {@link BeanRegistrationMethod}
	 */
	public BeanRegistrationMethod add(Object... methodNameParts) {
		GeneratedMethodName name = this.methodNameGenerator.generatedMethodName(methodNameParts);
		BeanRegistrationMethod method = new BeanRegistrationMethod(name);
		this.methods.add(method);
		return method;
	}

	void doWithMethodSpecs(Consumer<MethodSpec> action) {
		for (BeanRegistrationMethod method : this.methods) {
			Assert.state(method.spec != null,
					() -> String.format("Method '%s' has no method spec defined", method.name));
			action.accept(method.spec);
		}
	}

	/**
	 * A bean registration method that has been added.
	 */
	public static class BeanRegistrationMethod {

		private final GeneratedMethodName name;

		private MethodSpec spec;

		BeanRegistrationMethod(GeneratedMethodName name) {
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
		 * Generate the method using the given consumer.
		 * @param builder a consumer that will accept a method spec builder and configure
		 * it as necessary
		 */
		public void generateBy(Consumer<MethodSpec.Builder> builder) {
			Builder builderToUse = MethodSpec.methodBuilder(getName().toString());
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
					() -> String.format("'spec' must use the generated name \"%s\"", this.name));
			this.spec = spec;
		}

	}

}
