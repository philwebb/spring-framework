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

package org.springframework.core.io.support;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Brian Clozel
 */
class LimitedClassLoader extends URLClassLoader {

	static final ClassLoader constructorArgumentFactories = new LimitedClassLoader("constructor-argument-factories");

	static final ClassLoader multipleArgumentFactories = new LimitedClassLoader("multiple-arguments-factories");

	LimitedClassLoader(String location) {
		super(new URL[] {toUrl(location)});
	}

	private static URL toUrl(String location) {
		try {
			return new File("src/test/resources/org/springframework/core/io/support/" + location + "/").toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
