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

package org.springframework.aot.hint;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import org.springframework.aot.hint2.ReflectionHints;

/**
 *
 * @author pwebb
 * @since 6.0
 */
class ReflectionHints2Tests {

	@Test
	void test() {
		ReflectionHints hints = new ReflectionHints();

		// API usage example

		Method someMethod = null;
		Constructor<?> someConstructor = null;
		Field field = null;

		hints.registerWrite().forField(field);
		hints.registerWrite().forPublicFieldsIn(SomeClass.class);
		hints.registerWrite().forPublicFieldsIn(SomeClass.class, SomeOtherClass.class);
		hints.registerRead().withUnsafeAccess().forPublicFieldsIn(SomeClass.class);


		hints.registerInvoke().forConstructor(SomeClass.class);


		// Single hint
		hints.registerInvoke().forMethod(someMethod);
		hints.registerIntrospect().forConstructor(someConstructor);

		// Single hint without needing ReflectionUtils
		hints.registerIntrospect().forMethod(SomeClass.class, "someMethod");
		hints.registerInvoke().forConstructor(SomeClass.class);

		// Applying to all methods in a class
		hints.registerInvoke().forPublicMethodsIn(SomeClass.class);
		hints.registerIntrospect().forDeclaredConstructorsIn(SomeClass.class);

		// Applying to more than one class at once (see JacksonRuntimeHints)
		hints.registerInvoke().forPublicConstructorsIn(SomeClass.class, SomeOtherClass.class);

		// Applying with conditions
		hints.registerDeclaredClasses().forTypes(SomeClass.class, SomeOtherClass.class).whenReachable(Unsafe.class);
		hints.registerInvoke().forDeclaredConstructorsIn(SomeOtherClass.class).whenReachable(SomeClass.class);



	}

	static class SomeClass {

	}

	static class SomeOtherClass {

	}


}
