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

import java.util.function.Consumer;

import org.springframework.aot.context.AotContribution;
import org.springframework.aot.generate.GeneratedMethodName;
import org.springframework.aot.generate.MethodNameGenerator;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.MethodSpec.Builder;

/**
 * Allows {@link MethodSpec MethodSpecs} to be added to the {@link AotContribution} that
 * generated bean registration code.
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 * @see BeanRegistrationCode
 */
public class BeanRegistrationMethods {

	private final MethodNameGenerator methodNameGenerator;

	private final Consumer<MethodSpec> methodSpecs;

	BeanRegistrationMethods(MethodNameGenerator methodNameGenerator, Consumer<MethodSpec> methodSpecs) {
		this.methodNameGenerator = methodNameGenerator;
		this.methodSpecs = methodSpecs;
	}

	public BeanRegistrationMethod withName(Object... methodNameParts) {
		return new BeanRegistrationMethod(this.methodNameGenerator.generatedMethodName(methodNameParts));
	}

	public class BeanRegistrationMethod {

		private final GeneratedMethodName name;

		BeanRegistrationMethod(GeneratedMethodName name) {
			this.name = name;
		}

		public GeneratedMethodName getName() {
			return this.name;
		}

		public void add(Consumer<MethodSpec.Builder> builder) {
			Builder builderToUse = MethodSpec.methodBuilder(getName().toString());
			builder.accept(builderToUse);
			add(builderToUse);
		}

		public void add(MethodSpec.Builder builder) {
			add(builder.build());
		}

		public void add(MethodSpec methodSpec) {
			// FIXME assert name
			BeanRegistrationMethods.this.methodSpecs.accept(methodSpec);
		}

	}

}
