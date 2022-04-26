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

package org.springframework.beans.factory.aot.registration;

import java.lang.reflect.Member;

import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.javapoet.CodeBlock;

/**
 * Internal code generator used to support injection post processing.
 * <p>
 * Generates code in the form:<pre class="code">{@code
 * Object injectItem(RegisteredBean registeredBean, Object instance) {
 * 	instance.field = value
 * }
 *
 * }</pre>
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 6.0
 */
class InjectionPostProcessorMethodGenerator {

	InjectionPostProcessorMethodGenerator(RuntimeHints hints, MethodGenerator methodGenerator) {
		// TODO Auto-generated constructor stub
	}

	MethodReference generateMethod(String name, Member member, CodeBlock value) {
		return null;
	}

}
